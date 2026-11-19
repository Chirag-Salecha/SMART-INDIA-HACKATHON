package com.sih.bidcompliance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sih.bidcompliance.dto.BidderGovernmentVerificationItemResponse;
import com.sih.bidcompliance.dto.BidderVerificationSummaryResponse;
import com.sih.bidcompliance.dto.DebarmentCheckResult;
import com.sih.bidcompliance.dto.GovernmentLookupResult;
import com.sih.bidcompliance.dto.LocalContentCheckResult;
import com.sih.bidcompliance.entity.*;
import com.sih.bidcompliance.exception.ResourceNotFoundException;
import com.sih.bidcompliance.repository.BidderGovernmentVerificationRepository;
import com.sih.bidcompliance.repository.BidderRepository;
import com.sih.bidcompliance.repository.DocumentRepository;
import com.sih.bidcompliance.repository.VerificationResultRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates government-identifier verification for a bidder: queries the
 * simulated GST/PAN/Udyam providers using AI-extracted document values,
 * compares each against the bidder's profile, runs a cross-source
 * name-consistency check, and persists an auditable record of every check.
 *
 * IMPORTANT: all "government" data here comes from our own simulated
 * PostgreSQL tables (see GovernmentDataSeeder). This never queries a real
 * government system.
 */
@Service
@RequiredArgsConstructor
public class BidderGovernmentVerificationService {

    private static final Logger log = LoggerFactory.getLogger(BidderGovernmentVerificationService.class);

    private static final Set<String> COMPLIANT_STATUSES = Set.of("ACTIVE", "VALID");

    private static final List<GovernmentVerificationStatus> SEVERITY_ORDER = List.of(
            GovernmentVerificationStatus.NON_COMPLIANT,
            GovernmentVerificationStatus.MISMATCH,
            GovernmentVerificationStatus.NOT_FOUND,
            GovernmentVerificationStatus.REQUIRES_REVIEW,
            GovernmentVerificationStatus.VERIFIED
    );

    private final BidderRepository bidderRepository;
    private final BidderGovernmentVerificationRepository verificationRepository;
    private final DocumentRepository documentRepository;
    private final VerificationResultRepository verificationResultRepository;
    private final GstVerificationProvider gstProvider;
    private final PanVerificationProvider panProvider;
    private final UdyamVerificationProvider udyamProvider;
    private final DebarmentVerificationProvider debarmentProvider;
    private final LocalContentEvaluator localContentEvaluator;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final FuzzyNameMatcher fuzzyNameMatcher;

    // Configurable name-match thresholds (spec section 5): >=90 VERIFIED,
    // 70-89 REQUIRES_REVIEW, <70 MISMATCH. Field injection (not constructor)
    // so this stays independent of the Lombok-generated constructor.
    @Value("${compliance.name-match.pass-threshold:90}")
    private int nameMatchPassThreshold = 90;

    @Value("${compliance.name-match.review-threshold:70}")
    private int nameMatchReviewThreshold = 70;

    @Transactional
    public GovernmentVerificationStatus verifyExtractedDocument(
            Bidder bidder, Document document, Map<String, Object> extractedData
    ) {
        if (bidder == null || document == null) {
            return null;
        }
        DocumentType docType = document.getDocumentType();
        if (docType == null) {
            return null;
        }

        BidderGovernmentVerification record = null;
        switch (docType) {
            case GST -> {
                String gstin = extractFirstNonBlank(extractedData, "gstin", "possibleGstin");
                record = evaluateSource(bidder, GovernmentVerificationType.GST, gstin, gstProvider);
            }
            case PAN -> {
                String pan = extractFirstNonBlank(extractedData, "pan", "possiblePan");
                record = evaluateSource(bidder, GovernmentVerificationType.PAN, pan, panProvider);
            }
            case UDYAM -> {
                String udyam = extractFirstNonBlank(extractedData, "udyamNumber", "udyam_number");
                record = evaluateSource(bidder, GovernmentVerificationType.UDYAM, udyam, udyamProvider);
            }
            case MAKE_IN_INDIA -> {
                record = evaluateLocalContent(bidder);
            }
            default -> {
                return null;
            }
        }

        if (record != null) {
            verificationRepository.save(record);

            Long tenderId = bidder.getTender() != null ? bidder.getTender().getId() : null;
            auditService.log(AuditEventType.GOVERNMENT_VERIFICATION_COMPLETED, tenderId, bidder.getId(), document.getId(),
                    "Government reference verification completed for document " + document.getOriginalFileName()
                            + " (" + docType + "): " + record.getResultStatus());

            if (docType == DocumentType.GST || docType == DocumentType.PAN || docType == DocumentType.UDYAM) {
                checkAndSaveCrossSource(bidder);
            }

            return record.getResultStatus();
        }

        return null;
    }

    private void checkAndSaveCrossSource(Bidder bidder) {
        Optional<BidderGovernmentVerification> gstOpt = verificationRepository
                .findFirstByBidderIdAndVerificationTypeOrderByVerifiedAtDesc(bidder.getId(), GovernmentVerificationType.GST);
        Optional<BidderGovernmentVerification> panOpt = verificationRepository
                .findFirstByBidderIdAndVerificationTypeOrderByVerifiedAtDesc(bidder.getId(), GovernmentVerificationType.PAN);
        Optional<BidderGovernmentVerification> udyamOpt = verificationRepository
                .findFirstByBidderIdAndVerificationTypeOrderByVerifiedAtDesc(bidder.getId(), GovernmentVerificationType.UDYAM);

        if (gstOpt.isPresent() && panOpt.isPresent() && udyamOpt.isPresent()) {
            BidderGovernmentVerification cross = evaluateCrossSource(bidder, gstOpt.get(), panOpt.get(), udyamOpt.get());
            if (cross != null) {
                verificationRepository.save(cross);
            }
        }
    }

    @Transactional
    public BidderVerificationSummaryResponse verifyBidder(Long bidderId) {
        Bidder bidder = bidderRepository.findById(bidderId)
                .orElseThrow(() -> new ResourceNotFoundException("Bidder not found with id " + bidderId));
        Long tenderId = bidder.getTender() != null ? bidder.getTender().getId() : null;

        auditService.log(AuditEventType.GOVERNMENT_VERIFICATION_STARTED, tenderId, bidderId, null,
                "Government verification started for " + bidder.getCompanyName());

        // FIX 1 & 3: Identifier lookup MUST come from the AI-extracted document data, NOT bidder profile!
        String extractedGstin = getExtractedIdentifier(bidderId, DocumentType.GST, "gstin", "possibleGstin");
        String extractedPan = getExtractedIdentifier(bidderId, DocumentType.PAN, "pan", "possiblePan");
        String extractedUdyam = getExtractedIdentifier(bidderId, DocumentType.UDYAM, "udyamNumber", "udyam_number");

        BidderGovernmentVerification gstResult = evaluateSource(bidder, GovernmentVerificationType.GST, extractedGstin, gstProvider);
        BidderGovernmentVerification panResult = evaluateSource(bidder, GovernmentVerificationType.PAN, extractedPan, panProvider);
        BidderGovernmentVerification udyamResult = evaluateSource(bidder, GovernmentVerificationType.UDYAM, extractedUdyam, udyamProvider);
        BidderGovernmentVerification debarmentResult = evaluateDebarment(bidder, extractedPan, extractedGstin);
        BidderGovernmentVerification localContentResult = evaluateLocalContent(bidder);

        List<BidderGovernmentVerification> results = new ArrayList<>(
                List.of(gstResult, panResult, udyamResult, debarmentResult, localContentResult));

        BidderGovernmentVerification crossSource = evaluateCrossSource(bidder, gstResult, panResult, udyamResult);
        if (crossSource != null) {
            results.add(crossSource);
        }

        results.forEach(verificationRepository::save);

        GovernmentVerificationStatus overall = computeOverallStatus(results);

        auditService.log(AuditEventType.GOVERNMENT_VERIFICATION_COMPLETED, tenderId, bidderId, null,
                "Government verification completed for " + bidder.getCompanyName() + " with overall status " + overall);

        return toSummary(bidderId, overall, results);
    }

    @Transactional(readOnly = true)
    public BidderVerificationSummaryResponse getLatestSummary(Long bidderId) {
        if (!bidderRepository.existsById(bidderId)) {
            throw new ResourceNotFoundException("Bidder not found with id " + bidderId);
        }

        List<BidderGovernmentVerification> all = verificationRepository.findByBidderIdOrderByVerifiedAtDesc(bidderId);

        Map<GovernmentVerificationType, BidderGovernmentVerification> latestByType = new LinkedHashMap<>();
        for (BidderGovernmentVerification v : all) {
            latestByType.putIfAbsent(v.getVerificationType(), v);
        }

        List<BidderGovernmentVerification> latest = new ArrayList<>(latestByType.values());
        GovernmentVerificationStatus overall = latest.isEmpty() ? null : computeOverallStatus(latest);

        return toSummary(bidderId, overall, latest);
    }

    private BidderGovernmentVerification evaluateSource(
            Bidder bidder, GovernmentVerificationType type, String identifier, GovernmentVerificationProvider provider
    ) {
        BidderGovernmentVerification record = new BidderGovernmentVerification();
        record.setBidder(bidder);
        record.setVerificationType(type);
        record.setIdentifierChecked(identifier);
        record.setMatchedValue(bidder.getCompanyName());
        record.setSource(provider.getSourceName());
        record.setSourceType(VerificationSourceType.REFERENCE_DATABASE);

        if (identifier == null || identifier.isBlank()) {
            record.setResultStatus(GovernmentVerificationStatus.REQUIRES_REVIEW);
            record.setDiscrepancyDescription("No " + type + " document uploaded or identifier could not be extracted from document.");
            return record;
        }

        GovernmentLookupResult lookup = provider.verify(identifier);
        record.setGovernmentValue(lookup.getName());
        record.setEvidenceDetails(toJson(lookup.getAttributes()));

        if (!lookup.isFound()) {
            record.setResultStatus(GovernmentVerificationStatus.NOT_FOUND);
            record.setDiscrepancyDescription(lookup.getMessage());
            return record;
        }

        if (lookup.getStatus() == null || !COMPLIANT_STATUSES.contains(lookup.getStatus().trim().toUpperCase())) {
            record.setResultStatus(GovernmentVerificationStatus.NON_COMPLIANT);
            record.setDiscrepancyDescription(
                    type + " record status is '" + lookup.getStatus() + "', expected an active/valid status.");
            return record;
        }

        int similarity = fuzzyNameMatcher.similarity(bidder.getCompanyName(), lookup.getName());
        if (similarity >= nameMatchPassThreshold) {
            record.setResultStatus(GovernmentVerificationStatus.VERIFIED);
        } else if (similarity >= nameMatchReviewThreshold) {
            record.setResultStatus(GovernmentVerificationStatus.REQUIRES_REVIEW);
            record.setDiscrepancyDescription(
                    type + " record name '" + lookup.getName() + "' only partially matches bidder profile name '"
                            + bidder.getCompanyName() + "' (" + similarity + "% similarity) - manual review recommended.");
        } else {
            record.setResultStatus(GovernmentVerificationStatus.MISMATCH);
            record.setDiscrepancyDescription(
                    type + " record name '" + lookup.getName() + "' does not match bidder profile name '"
                            + bidder.getCompanyName() + "' (" + similarity + "% similarity).");
        }
        return record;
    }

    private BidderGovernmentVerification evaluateDebarment(Bidder bidder) {
        return evaluateDebarment(bidder, null, null);
    }

    private BidderGovernmentVerification evaluateDebarment(Bidder bidder, String extractedPan, String extractedGstin) {
        BidderGovernmentVerification record = new BidderGovernmentVerification();
        record.setBidder(bidder);
        record.setVerificationType(GovernmentVerificationType.BLACKLISTING);
        record.setMatchedValue(bidder.getCompanyName());
        record.setSource("SIMULATED_DEBARMENT_REGISTRY");
        record.setSourceType(VerificationSourceType.REFERENCE_DATABASE);

        String panToCheck = (extractedPan != null && !extractedPan.isBlank()) ? extractedPan : bidder.getPanNumber();
        String gstinToCheck = (extractedGstin != null && !extractedGstin.isBlank()) ? extractedGstin : bidder.getGstin();

        DebarmentCheckResult check = debarmentProvider.check(panToCheck, gstinToCheck, bidder.getCompanyName());
        record.setEvidenceDetails(toJson(check));

        if (!check.isMatchFound()) {
            record.setResultStatus(GovernmentVerificationStatus.VERIFIED);
            record.setDiscrepancyDescription("No debarment/blacklisting match found.");
            return record;
        }

        record.setIdentifierChecked(check.getMatchedIdentifierValue());
        record.setGovernmentValue(check.getEntityName());
        // A debarment match NEVER auto-disqualifies the bidder - it is always
        // surfaced as REQUIRES_REVIEW for the Procurement Officer to assess.
        record.setResultStatus(GovernmentVerificationStatus.REQUIRES_REVIEW);
        record.setDiscrepancyDescription(
                "Potential debarment match on " + check.getMatchedOn() + " (" + check.getAuthority()
                        + "). Reason: " + check.getReason() + ". Period: " + check.getDebarmentPeriod()
                        + ". Final decision remains with the Procurement Officer."
        );
        return record;
    }

    private BidderGovernmentVerification evaluateLocalContent(Bidder bidder) {
        BidderGovernmentVerification record = new BidderGovernmentVerification();
        record.setBidder(bidder);
        record.setVerificationType(GovernmentVerificationType.MAKE_IN_INDIA);
        record.setMatchedValue(bidder.getCompanyName());
        record.setSource("LOCAL_CONTENT_RULE_ENGINE");
        record.setSourceType(VerificationSourceType.RULE_BASED);

        Integer tenderThreshold = bidder.getTender() != null ? bidder.getTender().getLocalContentThresholdPercent() : null;
        LocalContentCheckResult check = localContentEvaluator.evaluate(bidder, tenderThreshold);
        record.setEvidenceDetails(toJson(check));
        record.setGovernmentValue(check.getActualPercentage() != null ? check.getActualPercentage() + "%" : null);
        record.setIdentifierChecked("Required: " + check.getRequiredPercentage() + "%");

        if (!check.isDocumentFound()) {
            record.setResultStatus(GovernmentVerificationStatus.NOT_FOUND);
            record.setDiscrepancyDescription(check.getMessage());
            return record;
        }

        if (check.getPassed() == null) {
            record.setResultStatus(GovernmentVerificationStatus.REQUIRES_REVIEW);
            record.setDiscrepancyDescription(check.getMessage());
            return record;
        }

        if (check.getPassed()) {
            record.setResultStatus(GovernmentVerificationStatus.VERIFIED);
            record.setDiscrepancyDescription(
                    "Local content " + check.getActualPercentage() + "% meets the required " + check.getRequiredPercentage() + "%.");
        } else {
            record.setResultStatus(GovernmentVerificationStatus.NON_COMPLIANT);
            record.setDiscrepancyDescription(
                    "Local content " + check.getActualPercentage() + "% is below the required " + check.getRequiredPercentage() + "%.");
        }
        return record;
    }

    private BidderGovernmentVerification evaluateCrossSource(            Bidder bidder,
            BidderGovernmentVerification gst,
            BidderGovernmentVerification pan,
            BidderGovernmentVerification udyam
    ) {
        Map<String, String> namesBySource = new LinkedHashMap<>();
        namesBySource.put("bidder profile", bidder.getCompanyName());
        if (gst.getGovernmentValue() != null) namesBySource.put("GST", gst.getGovernmentValue());
        if (pan.getGovernmentValue() != null) namesBySource.put("PAN", pan.getGovernmentValue());
        if (udyam.getGovernmentValue() != null) namesBySource.put("Udyam", udyam.getGovernmentValue());

        if (namesBySource.size() < 2) {
            return null; // not enough sources to cross-check
        }

        List<String> keys = new ArrayList<>(namesBySource.keySet());
        String baselineKey = keys.get(0);
        String baselineName = namesBySource.get(baselineKey);

        List<String> consistentKeys = new ArrayList<>(List.of(baselineKey));
        List<String> inconsistentKeys = new ArrayList<>();
        for (int i = 1; i < keys.size(); i++) {
            String key = keys.get(i);
            if (fuzzyNameMatcher.isMatch(baselineName, namesBySource.get(key), nameMatchReviewThreshold)) {
                consistentKeys.add(key);
            } else {
                inconsistentKeys.add(key);
            }
        }

        if (inconsistentKeys.isEmpty()) {
            return null; // fully consistent - no need to record a row
        }

        BidderGovernmentVerification record = new BidderGovernmentVerification();
        record.setBidder(bidder);
        record.setVerificationType(GovernmentVerificationType.CROSS_SOURCE);
        record.setResultStatus(GovernmentVerificationStatus.REQUIRES_REVIEW);
        record.setSource("CROSS_SOURCE_ANALYSIS");
        record.setSourceType(VerificationSourceType.RULE_BASED);
        record.setMatchedValue(baselineName);
        record.setGovernmentValue(
                inconsistentKeys.stream().map(k -> k + "=" + namesBySource.get(k)).collect(Collectors.joining("; "))
        );

        String inconsistentDescription = inconsistentKeys.stream()
                .map(k -> k + " ('" + namesBySource.get(k) + "')")
                .collect(Collectors.joining(" and "));
        String consistentDescription = String.join(", ", consistentKeys);

        record.setDiscrepancyDescription(
                inconsistentDescription + " name differs from the legal name found in the " + consistentDescription + " records."
        );

        return record;
    }

    private GovernmentVerificationStatus computeOverallStatus(List<BidderGovernmentVerification> results) {
        for (GovernmentVerificationStatus status : SEVERITY_ORDER) {
            if (results.stream().anyMatch(r -> r.getResultStatus() == status)) {
                return status;
            }
        }
        return GovernmentVerificationStatus.REQUIRES_REVIEW;
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Failed to serialize verification evidence: {}", e.getMessage());
            return null;
        }
    }

    private BidderVerificationSummaryResponse toSummary(
            Long bidderId, GovernmentVerificationStatus overall, List<BidderGovernmentVerification> results
    ) {
        List<BidderGovernmentVerificationItemResponse> items = results.stream()
                .map(r -> new BidderGovernmentVerificationItemResponse(
                        r.getVerificationType().name(),
                        r.getResultStatus().name(),
                        r.getIdentifierChecked(),
                        r.getMatchedValue(),
                        r.getGovernmentValue(),
                        r.getDiscrepancyDescription(),
                        r.getSource(),
                        r.getSourceType() != null ? r.getSourceType().name() : null,
                        r.getVerifiedAt()
                ))
                .toList();

        return new BidderVerificationSummaryResponse(
                bidderId,
                overall != null ? overall.name() : null,
                items
        );
    }

    private String getExtractedIdentifier(Long bidderId, DocumentType docType, String... keys) {
        if (documentRepository == null || verificationResultRepository == null) {
            return null;
        }
        return documentRepository.findFirstByBidderIdAndDocumentTypeOrderByUploadedAtDesc(bidderId, docType)
                .flatMap(doc -> verificationResultRepository.findByDocumentId(doc.getId()))
                .map(vr -> extractKeyFromJson(vr.getExtractedDataJson(), keys))
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private String extractKeyFromJson(String json, String... keys) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> data = objectMapper.readValue(json, Map.class);
            return extractFirstNonBlank(data, keys);
        } catch (Exception e) {
            log.warn("Failed to parse extractedDataJson: {}", e.getMessage());
            return null;
        }
    }

    private String extractFirstNonBlank(Map<String, Object> data, String... keys) {
        if (data == null || keys == null) return null;
        for (String key : keys) {
            Object val = data.get(key);
            if (val != null) {
                String s = String.valueOf(val).trim();
                if (!s.isBlank()) {
                    return s;
                }
            }
        }
        return null;
    }
}
