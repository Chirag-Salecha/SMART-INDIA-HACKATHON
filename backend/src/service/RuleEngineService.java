package com.sih.bidcompliance.service;

import com.sih.bidcompliance.dto.*;
import com.sih.bidcompliance.entity.*;
import com.sih.bidcompliance.exception.ResourceNotFoundException;
import com.sih.bidcompliance.repository.BidderRepository;
import com.sih.bidcompliance.repository.DocumentRepository;
import com.sih.bidcompliance.repository.TenderDocumentRequirementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * The dedicated compliance/rules engine (kept entirely out of React, per
 * spec section 14): combines government-identifier verification (GST, PAN,
 * Udyam, Local Content, Debarment - see BidderGovernmentVerificationService),
 * document verification outcomes (ComplianceEngineService),
 * tender-specific missing-document detection, and cross-document name
 * consistency into one transparent, weighted compliance score and risk
 * assessment.
 *
 * IMPORTANT: the compliance score is a decision-support indicator only. It
 * never makes, or claims to make, the final procurement decision - that
 * remains with the Procurement Officer (see OfficerReviewService).
 */
@Service
public class RuleEngineService {

    private final BidderRepository bidderRepository;
    private final DocumentRepository documentRepository;
    private final TenderDocumentRequirementRepository documentRequirementRepository;
    private final BidderGovernmentVerificationService governmentVerificationService;
    private final LocalContentEvaluator localContentEvaluator;
    private final DebarmentVerificationProvider debarmentProvider;
    private final TenderRequirementParser requirementParser;
    private final AuditService auditService;

    @Autowired
    public RuleEngineService(
            BidderRepository bidderRepository,
            DocumentRepository documentRepository,
            TenderDocumentRequirementRepository documentRequirementRepository,
            BidderGovernmentVerificationService governmentVerificationService,
            LocalContentEvaluator localContentEvaluator,
            DebarmentVerificationProvider debarmentProvider,
            TenderRequirementParser requirementParser,
            AuditService auditService
    ) {
        this.bidderRepository = bidderRepository;
        this.documentRepository = documentRepository;
        this.documentRequirementRepository = documentRequirementRepository;
        this.governmentVerificationService = governmentVerificationService;
        this.localContentEvaluator = localContentEvaluator;
        this.debarmentProvider = debarmentProvider;
        this.requirementParser = requirementParser;
        this.auditService = auditService;
    }

    // Overloaded constructor for backwards compatibility with tests
    public RuleEngineService(
            BidderRepository bidderRepository,
            DocumentRepository documentRepository,
            TenderDocumentRequirementRepository documentRequirementRepository,
            BidderGovernmentVerificationService governmentVerificationService,
            ComplianceEngineService complianceEngineService,
            LocalContentEvaluator localContentEvaluator,
            DebarmentVerificationProvider debarmentProvider,
            TenderRequirementParser requirementParser,
            AuditService auditService
    ) {
        this(bidderRepository, documentRepository, documentRequirementRepository,
                governmentVerificationService, localContentEvaluator, debarmentProvider,
                requirementParser, auditService);
    }

    @Value("${compliance.weights.gst:15}") private int weightGst = 15;
    @Value("${compliance.weights.pan:15}") private int weightPan = 15;
    @Value("${compliance.weights.udyam:10}") private int weightUdyam = 10;
    @Value("${compliance.weights.local-content:20}") private int weightLocalContent = 20;
    @Value("${compliance.weights.debarment:20}") private int weightDebarment = 20;
    @Value("${compliance.weights.document-completeness:10}") private int weightDocumentCompleteness = 10;
    @Value("${compliance.weights.cross-document-match:10}") private int weightCrossDocumentMatch = 10;

    @Transactional
    public ComplianceAssessmentResponse assess(Long bidderId) {
        Bidder bidder = bidderRepository.findById(bidderId)
                .orElseThrow(() -> new ResourceNotFoundException("Bidder not found with id " + bidderId));
        Tender tender = bidder.getTender();

        BidderVerificationSummaryResponse govSummary = governmentVerificationService.getLatestSummary(bidderId);
        if (govSummary == null || govSummary.getItems() == null || govSummary.getItems().isEmpty()) {
            try {
                govSummary = governmentVerificationService.verifyBidder(bidderId);
            } catch (Exception ignored) {
                // Keep existing govSummary if verifyBidder fails
            }
        }

        Map<String, BidderGovernmentVerificationItemResponse> byType = new HashMap<>();
        if (govSummary != null && govSummary.getItems() != null) {
            for (BidderGovernmentVerificationItemResponse item : govSummary.getItems()) {
                byType.put(item.getVerificationType(), item);
            }
        }

        // Support alias mappings across identifier names
        addAlias(byType, "LOCAL_CONTENT", "MAKE_IN_INDIA");
        addAlias(byType, "DEBARMENT", "BLACKLISTING");
        addAlias(byType, "GST", "GSTIN", "GST_VERIFICATION");
        addAlias(byType, "PAN", "PAN_VERIFICATION");
        addAlias(byType, "UDYAM", "UDYAM_VERIFICATION", "UDYAM_CERTIFICATE");

        List<ComplianceCategoryResult> categories = new ArrayList<>();
        categories.add(resolveCategory("GST", weightGst, List.of("GST", "GSTIN", "GST_VERIFICATION"),
                DocumentType.GST, byType, bidder));
        categories.add(resolveCategory("PAN", weightPan, List.of("PAN", "PAN_VERIFICATION"),
                DocumentType.PAN, byType, bidder));
        categories.add(resolveCategory("UDYAM", weightUdyam, List.of("UDYAM", "UDYAM_VERIFICATION", "UDYAM_CERTIFICATE"),
                DocumentType.UDYAM, byType, bidder));
        categories.add(resolveLocalContent(weightLocalContent, byType, bidder, tender));
        categories.add(resolveDebarment(weightDebarment, byType, bidder));

        List<DocumentType> requiredTypes = getRequiredDocumentTypes(tender);
        Set<DocumentType> mandatoryTypes = getMandatoryDocumentTypes(tender, requiredTypes);
        Set<DocumentType> uploadedTypes = documentRepository.findByBidderId(bidderId).stream()
                .map(Document::getDocumentType)
                .collect(java.util.stream.Collectors.toSet());

        List<String> missingMandatory = mandatoryTypes.stream()
                .filter(t -> !uploadedTypes.contains(t))
                .map(Enum::name)
                .sorted()
                .toList();

        categories.add(documentCompletenessCategory(mandatoryTypes, uploadedTypes));
        categories.add(crossDocumentCategory(byType.get("CROSS_SOURCE")));

        int score = (int) Math.round(categories.stream().mapToDouble(ComplianceCategoryResult::getAchievedWeight).sum());
        score = Math.max(0, Math.min(100, score));

        List<String> riskReasons = new ArrayList<>();
        String riskLevel = computeRisk(categories, missingMandatory, riskReasons);

        List<String> recommendations = buildRecommendations(categories, missingMandatory);

        auditService.log(AuditEventType.COMPLIANCE_SCORE_GENERATED,
                tender != null ? tender.getId() : null, bidderId, null,
                "Rule engine compliance assessment generated: score=" + score + ", risk=" + riskLevel);

        return new ComplianceAssessmentResponse(
                bidderId,
                bidder.getCompanyName(),
                score,
                riskLevel,
                riskReasons,
                categories,
                missingMandatory,
                recommendations,
                LocalDateTime.now()
        );
    }

    private void addAlias(Map<String, BidderGovernmentVerificationItemResponse> map, String key1, String... otherKeys) {
        BidderGovernmentVerificationItemResponse found = map.get(key1);
        if (found == null) {
            for (String k : otherKeys) {
                if (map.containsKey(k)) {
                    found = map.get(k);
                    break;
                }
            }
        }
        if (found != null) {
            map.putIfAbsent(key1, found);
            for (String k : otherKeys) {
                map.putIfAbsent(k, found);
            }
        }
    }

    private boolean isPassStatus(String status) {
        if (status == null) return false;
        String s = status.trim().toUpperCase();
        return s.equals("VERIFIED") || s.equals("PASS") || s.equals("PASSED")
                || s.equals("CLEAR") || s.equals("SUCCESS") || s.equals("COMPLIANT")
                || s.equals("VALID") || s.equals("ACTIVE");
    }

    private boolean isReviewStatus(String status) {
        if (status == null) return false;
        String s = status.trim().toUpperCase();
        return s.equals("REQUIRES_REVIEW") || s.equals("REVIEW") || s.equals("PENDING_REVIEW") || s.equals("UNVERIFIABLE");
    }

    private ComplianceCategoryResult resolveCategory(
            String category,
            int weight,
            List<String> aliases,
            DocumentType docType,
            Map<String, BidderGovernmentVerificationItemResponse> byType,
            Bidder bidder
    ) {
        BidderGovernmentVerificationItemResponse govItem = null;
        for (String alias : aliases) {
            if (byType.containsKey(alias)) {
                govItem = byType.get(alias);
                break;
            }
        }

        // FIX 4: Government verification result is AUTHORITATIVE for this category.
        // If a government verification result exists (VERIFIED, MISMATCH, NOT_FOUND,
        // NON_COMPLIANT, REQUIRES_REVIEW), it MUST determine the category score.
        // Never fall back to document verification when government verification reported NOT_FOUND, MISMATCH, etc.
        if (govItem != null) {
            return fromGovernmentCheck(category, weight, govItem);
        }

        // Only use document-level AI verification result as a fallback when there is NO government verification result:
        Optional<Document> doc = docType != null
                ? documentRepository.findFirstByBidderIdAndDocumentTypeOrderByUploadedAtDesc(bidder.getId(), docType)
                : Optional.empty();

        if (doc.isPresent() && doc.get().getVerificationStatus() == VerificationStatus.VERIFIED) {
            return new ComplianceCategoryResult(
                    category, weight, weight, "VERIFIED",
                    category + " document verified against bidder profile.",
                    "DOCUMENT_VERIFICATION", "AI_AUTOMATED_EXTRACTION"
            );
        }

        // If document is in review
        if (doc.isPresent() && doc.get().getVerificationStatus() == VerificationStatus.REQUIRES_REVIEW) {
            return new ComplianceCategoryResult(
                    category, weight, weight * 0.5, "REQUIRES_REVIEW",
                    category + " document requires manual review.",
                    "DOCUMENT_VERIFICATION", "AI_AUTOMATED_EXTRACTION"
            );
        }

        if (doc.isPresent() && doc.get().getVerificationStatus() == VerificationStatus.FAILED) {
            return new ComplianceCategoryResult(
                    category, weight, 0, "NON_COMPLIANT",
                    category + " document failed verification.",
                    "DOCUMENT_VERIFICATION", "AI_AUTOMATED_EXTRACTION"
            );
        }

        return new ComplianceCategoryResult(
                category, weight, 0, "PENDING",
                "Not yet verified - run Government Verification or upload " + category + " document.",
                null, null
        );
    }

    private ComplianceCategoryResult resolveDebarment(
            int weight,
            Map<String, BidderGovernmentVerificationItemResponse> byType,
            Bidder bidder
    ) {
        BidderGovernmentVerificationItemResponse item = byType.get("DEBARMENT");
        if (item == null) item = byType.get("BLACKLISTING");

        if (item != null) {
            if (isPassStatus(item.getResultStatus())) {
                return new ComplianceCategoryResult(
                        "DEBARMENT", weight, weight, "CLEAR",
                        item.getDiscrepancyDescription() != null ? item.getDiscrepancyDescription() : "No debarment/blacklisting match found.",
                        item.getSource(), item.getSourceType()
                );
            }
            if (isReviewStatus(item.getResultStatus())) {
                return new ComplianceCategoryResult(
                        "DEBARMENT", weight, weight * 0.5, "REQUIRES_REVIEW",
                        item.getDiscrepancyDescription(),
                        item.getSource(), item.getSourceType()
                );
            }
            return fromGovernmentCheck("DEBARMENT", weight, item);
        }

        // Fallback: evaluate via DebarmentVerificationProvider
        DebarmentCheckResult check = debarmentProvider != null
                ? debarmentProvider.check(bidder.getPanNumber(), bidder.getGstin(), bidder.getCompanyName())
                : null;
        if (check == null || !check.isMatchFound()) {
            return new ComplianceCategoryResult(
                    "DEBARMENT", weight, weight, "CLEAR",
                    "No debarment/blacklisting match found.",
                    "SIMULATED_DEBARMENT_REGISTRY", "REFERENCE_DATABASE"
            );
        } else {
            return new ComplianceCategoryResult(
                    "DEBARMENT", weight, weight * 0.5, "REQUIRES_REVIEW",
                    "Potential debarment match on " + check.getMatchedOn() + " (" + check.getAuthority()
                            + "). Reason: " + check.getReason() + ". Period: " + check.getDebarmentPeriod()
                            + ". Final decision remains with the Procurement Officer.",
                    "SIMULATED_DEBARMENT_REGISTRY", "REFERENCE_DATABASE"
            );
        }
    }

    private ComplianceCategoryResult resolveLocalContent(
            int weight,
            Map<String, BidderGovernmentVerificationItemResponse> byType,
            Bidder bidder,
            Tender tender
    ) {
        BidderGovernmentVerificationItemResponse item = byType.get("LOCAL_CONTENT");
        if (item == null) item = byType.get("MAKE_IN_INDIA");

        // FIX 4: Government/Rule check is authoritative when present
        if (item != null) {
            return fromGovernmentCheck("LOCAL_CONTENT", weight, item);
        }

        // Evaluate via LocalContentEvaluator
        Integer threshold = tender != null ? tender.getLocalContentThresholdPercent() : null;
        LocalContentCheckResult check = localContentEvaluator != null
                ? localContentEvaluator.evaluate(bidder, threshold)
                : null;
        if (check != null && Boolean.TRUE.equals(check.getPassed())) {
            return new ComplianceCategoryResult(
                    "LOCAL_CONTENT", weight, weight, "VERIFIED",
                    check.getMessage() != null ? check.getMessage() : "Local content " + check.getActualPercentage() + "% meets the required " + check.getRequiredPercentage() + "%.",
                    "LOCAL_CONTENT_RULE_ENGINE", "RULE_BASED"
            );
        }
        if (check != null && Boolean.FALSE.equals(check.getPassed())) {
            return new ComplianceCategoryResult(
                    "LOCAL_CONTENT", weight, 0, "NON_COMPLIANT",
                    check.getMessage() != null ? check.getMessage() : "Local content " + check.getActualPercentage() + "% is below required " + check.getRequiredPercentage() + "%.",
                    "LOCAL_CONTENT_RULE_ENGINE", "RULE_BASED"
            );
        }

        // Check document verification
        Optional<Document> doc = documentRepository.findFirstByBidderIdAndDocumentTypeOrderByUploadedAtDesc(bidder.getId(), DocumentType.MAKE_IN_INDIA);
        if (doc.isPresent() && doc.get().getVerificationStatus() == VerificationStatus.VERIFIED) {
            return new ComplianceCategoryResult(
                    "LOCAL_CONTENT", weight, weight, "VERIFIED",
                    "Make in India / Local Content document verified against bidder profile.",
                    "LOCAL_CONTENT_RULE_ENGINE", "RULE_BASED"
            );
        }

        if (check != null && !check.isDocumentFound()) {
            return new ComplianceCategoryResult(
                    "LOCAL_CONTENT", weight, 0, "NOT_FOUND",
                    check.getMessage() != null ? check.getMessage() : "No Make in India / Local Content document has been uploaded.",
                    "LOCAL_CONTENT_RULE_ENGINE", "RULE_BASED"
            );
        }

        if (check != null) {
            return new ComplianceCategoryResult(
                    "LOCAL_CONTENT", weight, weight * 0.5, "REQUIRES_REVIEW",
                    check.getMessage() != null ? check.getMessage() : "Local content document uploaded but requires manual review.",
                    "LOCAL_CONTENT_RULE_ENGINE", "RULE_BASED"
            );
        }

        return new ComplianceCategoryResult(
                "LOCAL_CONTENT", weight, 0, "PENDING",
                "Local content verification not yet performed.",
                "LOCAL_CONTENT_RULE_ENGINE", "RULE_BASED"
        );
    }

    private List<DocumentType> getRequiredDocumentTypes(Tender tender) {
        if (tender == null) return List.of();
        List<TenderDocumentRequirement> explicit = documentRequirementRepository.findByTenderId(tender.getId());
        if (!explicit.isEmpty()) {
            return explicit.stream().map(TenderDocumentRequirement::getDocumentType).toList();
        }
        return requirementParser.parseRequiredTypes(tender.getDescription());
    }

    private Set<DocumentType> getMandatoryDocumentTypes(Tender tender, List<DocumentType> requiredTypes) {
        if (tender == null) return Set.of();
        List<TenderDocumentRequirement> explicit = documentRequirementRepository.findByTenderId(tender.getId());
        if (!explicit.isEmpty()) {
            return explicit.stream()
                    .filter(TenderDocumentRequirement::isMandatory)
                    .map(TenderDocumentRequirement::getDocumentType)
                    .collect(java.util.stream.Collectors.toSet());
        }
        return new HashSet<>(requiredTypes);
    }

    private ComplianceCategoryResult fromGovernmentCheck(String category, int weight, BidderGovernmentVerificationItemResponse item) {
        if (item == null) {
            return new ComplianceCategoryResult(category, weight, 0,
                    "PENDING", "Not yet verified - run Government Verification for this bidder.", null, null);
        }

        double achieved = isPassStatus(item.getResultStatus()) ? weight
                : isReviewStatus(item.getResultStatus()) ? weight * 0.5
                : 0;

        String status = isPassStatus(item.getResultStatus())
                ? ("DEBARMENT".equals(category) ? "CLEAR" : "VERIFIED")
                : item.getResultStatus();

        return new ComplianceCategoryResult(
                category, weight, achieved, status,
                item.getDiscrepancyDescription() != null ? item.getDiscrepancyDescription() : "No discrepancies found.",
                item.getSource(), item.getSourceType()
        );
    }

    private ComplianceCategoryResult documentCompletenessCategory(Set<DocumentType> mandatoryTypes, Set<DocumentType> uploadedTypes) {
        if (mandatoryTypes.isEmpty()) {
            return new ComplianceCategoryResult("DOCUMENT_COMPLETENESS", weightDocumentCompleteness,
                    weightDocumentCompleteness, "NOT_APPLICABLE", "No document requirements configured for this tender.",
                    "TENDER_REQUIREMENTS", "RULE_BASED");
        }
        long uploadedMandatoryCount = mandatoryTypes.stream().filter(uploadedTypes::contains).count();
        double ratio = (double) uploadedMandatoryCount / mandatoryTypes.size();
        double achieved = weightDocumentCompleteness * ratio;
        String status = ratio >= 1.0 ? "VERIFIED" : ratio > 0 ? "REVIEW" : "MISSING";
        String evidence = uploadedMandatoryCount + " of " + mandatoryTypes.size() + " mandatory document(s) uploaded.";
        return new ComplianceCategoryResult("DOCUMENT_COMPLETENESS", weightDocumentCompleteness, achieved, status, evidence,
                "TENDER_REQUIREMENTS", "RULE_BASED");
    }

    private ComplianceCategoryResult crossDocumentCategory(BidderGovernmentVerificationItemResponse crossSourceItem) {
        if (crossSourceItem == null) {
            return new ComplianceCategoryResult("CROSS_DOCUMENT_MATCH", weightCrossDocumentMatch,
                    weightCrossDocumentMatch, "VERIFIED", "Bidder profile, GST, PAN and Udyam names are consistent.",
                    "CROSS_SOURCE_ANALYSIS", "RULE_BASED");
        }
        return new ComplianceCategoryResult("CROSS_DOCUMENT_MATCH", weightCrossDocumentMatch,
                weightCrossDocumentMatch * 0.5, "REVIEW", crossSourceItem.getDiscrepancyDescription(),
                "CROSS_SOURCE_ANALYSIS", "RULE_BASED");
    }

    private String computeRisk(List<ComplianceCategoryResult> categories, List<String> missingMandatory, List<String> reasons) {
        boolean high = false;
        boolean medium = false;

        Map<String, ComplianceCategoryResult> byCategory = new HashMap<>();
        for (ComplianceCategoryResult cat : categories) {
            byCategory.put(cat.getCategory(), cat);
        }

        ComplianceCategoryResult debarment = byCategory.get("DEBARMENT");
        if (debarment != null && isReviewStatus(debarment.getStatus())) {
            high = true;
            reasons.add("Potential debarment match found - " + debarment.getEvidence());
        }

        for (String type : List.of("GST", "PAN")) {
            ComplianceCategoryResult item = byCategory.get(type);
            if (item != null && ("MISMATCH".equals(item.getStatus()) || "NON_COMPLIANT".equals(item.getStatus())
                    || "FAILED".equals(item.getStatus()) || "NOT_FOUND".equals(item.getStatus()))) {
                high = true;
                reasons.add(type + " verification issue - " + item.getEvidence());
            }
        }

        if (!missingMandatory.isEmpty()) {
            medium = true;
            reasons.add("Missing mandatory document(s): " + String.join(", ", missingMandatory));
        }

        ComplianceCategoryResult udyam = byCategory.get("UDYAM");
        if (udyam != null && ("NOT_FOUND".equals(udyam.getStatus()) || isReviewStatus(udyam.getStatus()))) {
            medium = true;
            reasons.add("Udyam verification incomplete - " + udyam.getEvidence());
        }

        ComplianceCategoryResult localContent = byCategory.get("LOCAL_CONTENT");
        if (localContent != null && ("NON_COMPLIANT".equals(localContent.getStatus()) || "FAILED".equals(localContent.getStatus()))) {
            medium = true;
            reasons.add("Local content shortfall - " + localContent.getEvidence());
        }

        ComplianceCategoryResult crossSource = byCategory.get("CROSS_DOCUMENT_MATCH");
        if (crossSource != null && isReviewStatus(crossSource.getStatus())) {
            medium = true;
            reasons.add("Cross-document name inconsistency - " + crossSource.getEvidence());
        }

        if (high) return "HIGH";
        if (medium) return "MEDIUM";
        if (reasons.isEmpty()) {
            reasons.add("No significant inconsistencies found; all major verifications passed.");
        }
        return "LOW";
    }

    private List<String> buildRecommendations(List<ComplianceCategoryResult> categories, List<String> missingMandatory) {
        List<String> recommendations = new ArrayList<>();
        Map<String, ComplianceCategoryResult> byCategory = new HashMap<>();
        for (ComplianceCategoryResult cat : categories) {
            byCategory.put(cat.getCategory(), cat);
        }

        ComplianceCategoryResult debarment = byCategory.get("DEBARMENT");
        if (debarment != null && isReviewStatus(debarment.getStatus())) {
            recommendations.add("Procurement Officer Review Required: a potential debarment match was found. "
                    + "Verify the matching identifier and authority before proceeding.");
        }

        for (String type : List.of("GST", "PAN", "UDYAM")) {
            ComplianceCategoryResult item = byCategory.get(type);
            if (item != null && ("MISMATCH".equals(item.getStatus()) || "NON_COMPLIANT".equals(item.getStatus())
                    || "FAILED".equals(item.getStatus()) || "NOT_FOUND".equals(item.getStatus()))) {
                recommendations.add("Procurement Officer Review Required: " + item.getEvidence());
            }
        }

        ComplianceCategoryResult localContent = byCategory.get("LOCAL_CONTENT");
        if (localContent != null && ("NON_COMPLIANT".equals(localContent.getStatus()) || "FAILED".equals(localContent.getStatus()))) {
            recommendations.add("Flag for Procurement Officer review: " + localContent.getEvidence());
        }

        if (!missingMandatory.isEmpty()) {
            recommendations.add("Request the bidder to upload the missing mandatory document(s): "
                    + String.join(", ", missingMandatory) + ".");
        }

        ComplianceCategoryResult crossSource = byCategory.get("CROSS_DOCUMENT_MATCH");
        if (crossSource != null && isReviewStatus(crossSource.getStatus())) {
            recommendations.add("Procurement Officer Review Required: " + crossSource.getEvidence());
        }

        if (recommendations.isEmpty()) {
            recommendations.add("No issues detected. Standard Procurement Officer review is still required before approval.");
        }

        return recommendations;
    }
}
