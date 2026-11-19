package com.sih.bidcompliance.service;

import com.sih.bidcompliance.dto.ComplianceAssessmentResponse;
import com.sih.bidcompliance.dto.ComplianceCategoryResult;
import com.sih.bidcompliance.dto.ComplianceReportResponse;
import com.sih.bidcompliance.dto.ComplianceRequirementResult;
import com.sih.bidcompliance.entity.*;
import com.sih.bidcompliance.exception.ResourceNotFoundException;
import com.sih.bidcompliance.repository.BidderRepository;
import com.sih.bidcompliance.repository.DocumentRepository;
import com.sih.bidcompliance.repository.TenderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Compliance report service: delegates to the authoritative RuleEngineService
 * to guarantee that all compliance assessments across the platform (API /api/compliance/*
 * and /api/bidders/{id}/compliance) share the exact same score, risk level, and rules.
 */
@Service
@RequiredArgsConstructor
public class ComplianceEngineService {

    public static final Map<DocumentType, Integer> BASE_WEIGHTS = Map.of(
            DocumentType.GST, 15,
            DocumentType.PAN, 15,
            DocumentType.UDYAM, 10,
            DocumentType.MAKE_IN_INDIA, 20,
            DocumentType.OEM_AUTHORIZATION, 10
    );

    private final BidderRepository bidderRepository;
    private final TenderRepository tenderRepository;
    private final DocumentRepository documentRepository;
    private final RuleEngineService ruleEngineService;

    @Transactional
    public ComplianceReportResponse evaluateBidder(Long bidderId) {
        Bidder bidder = bidderRepository.findById(bidderId)
                .orElseThrow(() -> new ResourceNotFoundException("Bidder not found with id " + bidderId));
        return evaluate(bidder);
    }

    @Transactional
    public List<ComplianceReportResponse> evaluateTender(Long tenderId) {
        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new ResourceNotFoundException("Tender not found with id " + tenderId));
        return bidderRepository.findByTenderId(tenderId).stream()
                .map(this::evaluate)
                .toList();
    }

    public ComplianceReportResponse evaluate(Bidder bidder) {
        // ONE authoritative source of truth for compliance evaluation
        ComplianceAssessmentResponse assessment = ruleEngineService.assess(bidder.getId());
        Tender tender = bidder.getTender();

        List<ComplianceRequirementResult> requirements = assessment.getCategories().stream()
                .map(cat -> toRequirementResult(bidder, cat))
                .toList();

        return new ComplianceReportResponse(
                bidder.getId(),
                bidder.getCompanyName(),
                tender != null ? tender.getId() : null,
                tender != null ? tender.getTenderNumber() : null,
                assessment.getComplianceScore(),
                assessment.getRiskLevel(),
                requirements,
                assessment.getGeneratedAt() != null ? assessment.getGeneratedAt() : LocalDateTime.now()
        );
    }

    private ComplianceRequirementResult toRequirementResult(Bidder bidder, ComplianceCategoryResult cat) {
        String status = mapStatus(cat.getStatus());
        String risk = mapRisk(cat.getStatus());
        Long docId = findDocumentIdForCategory(bidder.getId(), cat.getCategory());

        return new ComplianceRequirementResult(
                cat.getCategory(),
                status,
                cat.getEvidence() != null ? cat.getEvidence() : "No evidence available.",
                risk,
                "NON_COMPLIANT".equals(status) || "MISSING".equals(status) || "NOT_FOUND".equals(status) || "MISMATCH".equals(status)
                        ? "Ensure valid document or reference data is provided for " + cat.getCategory() + "."
                        : null,
                cat.getWeight(),
                docId,
                cat.getAchievedWeight(),
                cat.getAchievedWeight(),
                cat.getWeight()
        );
    }

    private Long findDocumentIdForCategory(Long bidderId, String category) {
        if (bidderId == null || category == null) return null;
        try {
            DocumentType type = switch (category) {
                case "GST" -> DocumentType.GST;
                case "PAN" -> DocumentType.PAN;
                case "UDYAM" -> DocumentType.UDYAM;
                case "LOCAL_CONTENT", "MAKE_IN_INDIA" -> DocumentType.MAKE_IN_INDIA;
                case "OEM_AUTHORIZATION" -> DocumentType.OEM_AUTHORIZATION;
                default -> null;
            };
            if (type != null) {
                Optional<Document> doc = documentRepository
                        .findFirstByBidderIdAndDocumentTypeOrderByUploadedAtDesc(bidderId, type);
                if (doc.isPresent()) {
                    return doc.get().getId();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String mapStatus(String catStatus) {
        if (catStatus == null) return "MISSING";
        String s = catStatus.trim().toUpperCase();
        return switch (s) {
            case "VERIFIED", "CLEAR", "PASS", "PASSED", "COMPLIANT", "VALID", "ACTIVE", "NOT_APPLICABLE" -> "COMPLIANT";
            case "NOT_FOUND" -> "NOT_FOUND";
            case "MISMATCH" -> "MISMATCH";
            case "FAILED", "NON_COMPLIANT" -> "NON_COMPLIANT";
            case "MISSING" -> "MISSING";
            case "REQUIRES_REVIEW", "REVIEW", "PENDING_REVIEW", "UNVERIFIABLE", "PENDING" -> "REQUIRES_REVIEW";
            default -> s;
        };
    }

    private String mapRisk(String catStatus) {
        if (catStatus == null) return "HIGH";
        String s = catStatus.trim().toUpperCase();
        return switch (s) {
            case "VERIFIED", "CLEAR", "PASS", "PASSED", "COMPLIANT", "VALID", "ACTIVE", "NOT_APPLICABLE" -> "LOW";
            case "REQUIRES_REVIEW", "REVIEW", "PENDING_REVIEW", "UNVERIFIABLE", "PENDING" -> "MEDIUM";
            default -> "HIGH";
        };
    }
}
