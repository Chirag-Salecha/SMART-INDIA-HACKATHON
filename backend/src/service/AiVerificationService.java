package com.sih.bidcompliance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sih.bidcompliance.dto.*;
import com.sih.bidcompliance.entity.*;
import com.sih.bidcompliance.repository.VerificationResultRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Orchestrates AI-powered document verification: calls the AI service via
 * AiServiceClient, persists the structured result, and updates the parent
 * Document's verification status.
 *
 * If the AI service is unreachable or errors out, this NEVER throws back to
 * the caller (DocumentService) - the document upload must still succeed.
 * Instead the document is left/set to REQUIRES_REVIEW with a recorded
 * error message, so a procurement officer knows manual attention is needed.
 */
@Service
@RequiredArgsConstructor
public class AiVerificationService {

    private static final Logger log = LoggerFactory.getLogger(AiVerificationService.class);

    private final AiServiceClient aiServiceClient;
    private final VerificationResultRepository verificationResultRepository;
    private final BidderGovernmentVerificationService governmentVerificationService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    /**
     * Runs AI analysis for the given document, queries the authoritative
     * PostgreSQL government reference database with the extracted data,
     * and persists the structured outcome.
     * Returns the resulting VerificationStatus so the caller can update the
     * Document entity itself.
     */
    public VerificationStatus analyzeAndStore(Document document, Bidder bidder, String tenderRequirements, MultipartFile file) {
        Long tenderId = bidder.getTender() != null ? bidder.getTender().getId() : null;

        auditService.log(AuditEventType.AI_ANALYSIS_STARTED, tenderId, bidder.getId(), document.getId(),
                "AI analysis started for document " + document.getOriginalFileName());

        try {
            AiAnalysisResponse response = aiServiceClient.analyze(
                    file,
                    document.getDocumentType().name(),
                    bidder.getCompanyName(),
                    tenderRequirements
            );

            // FIX 3: Trigger authoritative government reference verification using AI-extracted data
            GovernmentVerificationStatus govStatus = null;
            if (governmentVerificationService != null) {
                try {
                    govStatus = governmentVerificationService.verifyExtractedDocument(
                            bidder, document, response.getExtractedData()
                    );
                } catch (Exception govEx) {
                    log.warn("Government reference check failed for document {}: {}", document.getId(), govEx.getMessage());
                }
            }

            // Map authoritative verification status:
            // Reference database verification takes precedence over raw AI extraction.
            VerificationStatus status;
            if (govStatus != null) {
                status = switch (govStatus) {
                    case VERIFIED -> VerificationStatus.VERIFIED;
                    case MISMATCH, NON_COMPLIANT -> VerificationStatus.FAILED;
                    case NOT_FOUND, REQUIRES_REVIEW, UNVERIFIABLE -> VerificationStatus.REQUIRES_REVIEW;
                };
            } else {
                status = parseStatus(response.getStatus());
            }

            saveResult(document, status, response.getConfidence(), response.getExtractedData(),
                    response.getChecks(), response.getIssues(), response.getRecommendations(),
                    response.getVerificationSource(), null);

            auditService.log(AuditEventType.AI_ANALYSIS_COMPLETED, tenderId, bidder.getId(), document.getId(),
                    "AI analysis completed with status " + status + (govStatus != null ? " (government verification: " + govStatus + ")" : ""));

            return status;
        } catch (Exception e) {
            log.warn("AI service call failed for document {}: {}", document.getId(), e.getMessage());

            saveResult(document, VerificationStatus.REQUIRES_REVIEW, null, null, null,
                    List.of("Automated verification could not be completed: the AI service is unavailable or returned an error."),
                    List.of("Retry analysis once the AI service is back online, or verify this document manually."),
                    "AI_SERVICE_UNAVAILABLE", e.getMessage());

            auditService.log(AuditEventType.AI_ANALYSIS_FAILED, tenderId, bidder.getId(), document.getId(),
                    "AI analysis failed: " + e.getMessage());

            return VerificationStatus.REQUIRES_REVIEW;
        }
    }

    public VerificationResultResponse getResultResponse(Long documentId) {
        return verificationResultRepository.findByDocumentId(documentId)
                .map(this::toResponse)
                .orElse(null);
    }

    private void saveResult(
            Document document,
            VerificationStatus status,
            Double confidence,
            Map<String, Object> extractedData,
            List<AiCheckDto> checks,
            List<String> issues,
            List<String> recommendations,
            String verificationSource,
            String errorMessage
    ) {
        VerificationResult result = verificationResultRepository.findByDocumentId(document.getId())
                .orElseGet(VerificationResult::new);

        result.setDocument(document);
        result.setStatus(status);
        result.setConfidence(confidence);
        result.setExtractedDataJson(toJson(extractedData));
        result.setChecksJson(toJson(checks));
        result.setIssuesJson(toJson(issues));
        result.setRecommendationsJson(toJson(recommendations));
        result.setVerificationSource(verificationSource);
        result.setErrorMessage(errorMessage);

        verificationResultRepository.save(result);
    }

    private VerificationResultResponse toResponse(VerificationResult result) {
        AiGovernmentVerificationDto governmentInfo = null;
        Map<String, Object> extractedData = fromJson(result.getExtractedDataJson(), Map.class);
        List<AiCheckDto> checks = fromJsonList(result.getChecksJson(), AiCheckDto.class);
        List<String> issues = fromJsonList(result.getIssuesJson(), String.class);
        List<String> recommendations = fromJsonList(result.getRecommendationsJson(), String.class);

        return new VerificationResultResponse(
                result.getStatus() != null ? result.getStatus().name() : null,
                result.getConfidence(),
                extractedData,
                checks,
                issues,
                recommendations,
                result.getVerificationSource(),
                governmentInfo,
                result.getErrorMessage(),
                result.getAnalyzedAt()
        );
    }

    private VerificationStatus parseStatus(String aiStatus) {
        if (aiStatus == null) {
            return VerificationStatus.REQUIRES_REVIEW;
        }
        try {
            return VerificationStatus.valueOf(aiStatus.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown status '{}' returned by AI service, defaulting to REQUIRES_REVIEW", aiStatus);
            return VerificationStatus.REQUIRES_REVIEW;
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Failed to serialize value to JSON for storage: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T fromJson(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("Failed to deserialize stored JSON: {}", e.getMessage());
            return null;
        }
    }

    private <T> List<T> fromJsonList(String json, Class<T> elementType) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            com.fasterxml.jackson.databind.JavaType listType =
                    objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
            return objectMapper.readValue(json, listType);
        } catch (Exception e) {
            log.warn("Failed to deserialize stored JSON list: {}", e.getMessage());
            return null;
        }
    }
}
