package com.sih.bidcompliance.service;

import com.sih.bidcompliance.dto.ComplianceAssessmentResponse;
import com.sih.bidcompliance.dto.ComplianceCategoryResult;
import com.sih.bidcompliance.dto.ComplianceReportResponse;
import com.sih.bidcompliance.dto.ComplianceRequirementResult;
import com.sih.bidcompliance.entity.Bidder;
import com.sih.bidcompliance.entity.Tender;
import com.sih.bidcompliance.repository.BidderRepository;
import com.sih.bidcompliance.repository.DocumentRepository;
import com.sih.bidcompliance.repository.TenderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplianceEngineServiceTest {

    @Mock private BidderRepository bidderRepository;
    @Mock private TenderRepository tenderRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private RuleEngineService ruleEngineService;

    private ComplianceEngineService complianceEngineService;

    @BeforeEach
    void setUp() {
        complianceEngineService = new ComplianceEngineService(
                bidderRepository, tenderRepository, documentRepository, ruleEngineService
        );
    }

    private Bidder createBidder(Long id, String name) {
        Tender tender = new Tender();
        tender.setId(100L);
        tender.setTenderNumber("TDR-2026-001");
        tender.setTitle("Supply of IT Infrastructure");

        Bidder bidder = new Bidder();
        bidder.setId(id);
        bidder.setCompanyName(name);
        bidder.setTender(tender);
        return bidder;
    }

    @Test
    @DisplayName("evaluateBidder delegates to RuleEngineService and returns identical score and risk")
    void evaluateBidderProducesIdenticalScoreAndRiskAsRuleEngine() {
        Bidder bidder = createBidder(1L, "TechVision Solutions Pvt Ltd");
        when(bidderRepository.findById(1L)).thenReturn(Optional.of(bidder));

        ComplianceAssessmentResponse assessment = new ComplianceAssessmentResponse(
                1L,
                "TechVision Solutions Pvt Ltd",
                100,
                "LOW",
                List.of(),
                List.of(
                        new ComplianceCategoryResult("GST", 15, 15.0, "VERIFIED", "Clean match", "SIMULATED_GSTN", "REFERENCE_DATABASE"),
                        new ComplianceCategoryResult("PAN", 15, 15.0, "VERIFIED", "Clean match", "SIMULATED_PAN", "REFERENCE_DATABASE"),
                        new ComplianceCategoryResult("UDYAM", 10, 10.0, "VERIFIED", "Clean match", "SIMULATED_UDYAM", "REFERENCE_DATABASE"),
                        new ComplianceCategoryResult("LOCAL_CONTENT", 20, 20.0, "VERIFIED", "Meets threshold", "LOCAL_CONTENT_RULE_ENGINE", "RULE_BASED"),
                        new ComplianceCategoryResult("DEBARMENT", 20, 20.0, "CLEAR", "No match", "SIMULATED_DEBARMENT_REGISTRY", "REFERENCE_DATABASE"),
                        new ComplianceCategoryResult("DOCUMENT_COMPLETENESS", 10, 10.0, "VERIFIED", "All uploaded", "TENDER_REQUIREMENTS", "RULE_BASED"),
                        new ComplianceCategoryResult("CROSS_DOCUMENT_MATCH", 10, 10.0, "VERIFIED", "Consistent names", "CROSS_SOURCE_ANALYSIS", "RULE_BASED")
                ),
                List.of(),
                List.of(),
                LocalDateTime.now()
        );

        when(ruleEngineService.assess(1L)).thenReturn(assessment);

        ComplianceReportResponse report = complianceEngineService.evaluateBidder(1L);

        // Assert score and risk are identical
        assertThat(report.getComplianceScore()).isEqualTo(assessment.getComplianceScore());
        assertThat(report.getRiskLevel()).isEqualTo(assessment.getRiskLevel());
        assertThat(report.getComplianceScore()).isEqualTo(100);
        assertThat(report.getRiskLevel()).isEqualTo("LOW");

        // Assert all 7 categories are present with matching points
        assertThat(report.getRequirements()).hasSize(7);
        ComplianceRequirementResult gstReq = report.getRequirements().stream()
                .filter(r -> "GST".equals(r.getRequirement()))
                .findFirst().orElseThrow();
        assertThat(gstReq.getStatus()).isEqualTo("COMPLIANT");
        assertThat(gstReq.getAchievedWeight()).isEqualTo(15.0);
        assertThat(gstReq.getWeight()).isEqualTo(15);
    }

    @Test
    @DisplayName("evaluateBidder reflects MISMATCH with 0 points and HIGH risk in lockstep with RuleEngine")
    void evaluateBidderReflectsMismatchWithZeroPointsAndHighRisk() {
        Bidder bidder = createBidder(2L, "TechVision Solutions Pvt Ltd");
        when(bidderRepository.findById(2L)).thenReturn(Optional.of(bidder));

        ComplianceAssessmentResponse assessment = new ComplianceAssessmentResponse(
                2L,
                "TechVision Solutions Pvt Ltd",
                85,
                "HIGH",
                List.of("GST verification issue - entity mismatch"),
                List.of(
                        new ComplianceCategoryResult("GST", 15, 0.0, "MISMATCH", "Name mismatch with registry", "SIMULATED_GSTN", "REFERENCE_DATABASE"),
                        new ComplianceCategoryResult("PAN", 15, 15.0, "VERIFIED", "Clean match", "SIMULATED_PAN", "REFERENCE_DATABASE"),
                        new ComplianceCategoryResult("UDYAM", 10, 10.0, "VERIFIED", "Clean match", "SIMULATED_UDYAM", "REFERENCE_DATABASE"),
                        new ComplianceCategoryResult("LOCAL_CONTENT", 20, 20.0, "VERIFIED", "Meets threshold", "LOCAL_CONTENT_RULE_ENGINE", "RULE_BASED"),
                        new ComplianceCategoryResult("DEBARMENT", 20, 20.0, "CLEAR", "No match", "SIMULATED_DEBARMENT_REGISTRY", "REFERENCE_DATABASE"),
                        new ComplianceCategoryResult("DOCUMENT_COMPLETENESS", 10, 10.0, "VERIFIED", "All uploaded", "TENDER_REQUIREMENTS", "RULE_BASED"),
                        new ComplianceCategoryResult("CROSS_DOCUMENT_MATCH", 10, 10.0, "VERIFIED", "Consistent names", "CROSS_SOURCE_ANALYSIS", "RULE_BASED")
                ),
                List.of(),
                List.of("Review GST registration document"),
                LocalDateTime.now()
        );

        when(ruleEngineService.assess(2L)).thenReturn(assessment);

        ComplianceReportResponse report = complianceEngineService.evaluateBidder(2L);

        // Assert score and risk match RuleEngine
        assertThat(report.getComplianceScore()).isEqualTo(85);
        assertThat(report.getRiskLevel()).isEqualTo("HIGH");

        ComplianceRequirementResult gstReq = report.getRequirements().stream()
                .filter(r -> "GST".equals(r.getRequirement()))
                .findFirst().orElseThrow();
        assertThat(gstReq.getStatus()).isEqualTo("MISMATCH");
        assertThat(gstReq.getAchievedWeight()).isEqualTo(0.0);
        assertThat(gstReq.getRisk()).isEqualTo("HIGH");
    }
}
