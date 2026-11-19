package com.sih.bidcompliance.service;

import com.sih.bidcompliance.dto.*;
import com.sih.bidcompliance.entity.*;
import com.sih.bidcompliance.repository.BidderRepository;
import com.sih.bidcompliance.repository.DocumentRepository;
import com.sih.bidcompliance.repository.TenderDocumentRequirementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Regression tests for fully compliant Case A bidder.
 * Verifies that actual verification results (GST, PAN, Udyam, Local Content,
 * Debarment, Document Completeness, Cross-Document Match) correctly produce
 * a 100/100 compliance score and LOW risk assessment.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CaseAComplianceIntegrationTest {

    @Mock private BidderRepository bidderRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private TenderDocumentRequirementRepository documentRequirementRepository;
    @Mock private BidderGovernmentVerificationService governmentVerificationService;
    @Mock private ComplianceEngineService complianceEngineService;
    @Mock private LocalContentEvaluator localContentEvaluator;
    @Mock private DebarmentVerificationProvider debarmentProvider;
    @Mock private TenderRequirementParser requirementParser;
    @Mock private AuditService auditService;

    private RuleEngineService service;

    private static final List<DocumentType> CASE_A_TYPES = List.of(
            DocumentType.GST, DocumentType.PAN, DocumentType.UDYAM,
            DocumentType.MAKE_IN_INDIA, DocumentType.OEM_AUTHORIZATION
    );

    @BeforeEach
    void setUp() {
        service = new RuleEngineService(
                bidderRepository, documentRepository, documentRequirementRepository,
                governmentVerificationService, complianceEngineService, localContentEvaluator,
                debarmentProvider, requirementParser, auditService
        );
    }

    private Bidder createCaseABidder() {
        Tender tender = new Tender();
        tender.setId(10L);
        tender.setDescription("GST, PAN, Udyam, Make in India, OEM Authorization");
        tender.setLocalContentThresholdPercent(50);

        Bidder bidder = new Bidder();
        bidder.setId(4L);
        bidder.setCompanyName("Meridian Infra Pvt Ltd");
        bidder.setGstin("27ABCDE1234F1Z5");
        bidder.setPanNumber("ABCDE1234F");
        bidder.setUdyamNumber("UDYAM-MH-01-1234567");
        bidder.setTender(tender);
        return bidder;
    }

    private Document createVerifiedDocument(DocumentType type) {
        Document doc = new Document();
        doc.setDocumentType(type);
        doc.setVerificationStatus(VerificationStatus.VERIFIED);
        return doc;
    }

    @Test
    @DisplayName("Case A bidder with successful AI document verification scores 100/100 with LOW risk")
    void caseABidderWithDocumentVerificationScores100AndLowRisk() {
        Bidder bidder = createCaseABidder();
        when(bidderRepository.findById(4L)).thenReturn(Optional.of(bidder));
        when(requirementParser.parseRequiredTypes(bidder.getTender().getDescription())).thenReturn(CASE_A_TYPES);

        // Uploaded documents in DocumentRepository
        List<Document> uploadedDocs = CASE_A_TYPES.stream().map(this::createVerifiedDocument).toList();
        when(documentRepository.findByBidderId(4L)).thenReturn(uploadedDocs);

        for (DocumentType type : List.of(DocumentType.GST, DocumentType.PAN, DocumentType.UDYAM, DocumentType.MAKE_IN_INDIA)) {
            when(documentRepository.findFirstByBidderIdAndDocumentTypeOrderByUploadedAtDesc(4L, type))
                    .thenReturn(Optional.of(createVerifiedDocument(type)));
        }

        // Government verification summary is initially empty
        when(governmentVerificationService.getLatestSummary(4L)).thenReturn(
                new BidderVerificationSummaryResponse(4L, null, List.of())
        );

        // Debarment provider verifies bidder is clean (no match found)
        when(debarmentProvider.check("ABCDE1234F", "27ABCDE1234F1Z5", "Meridian Infra Pvt Ltd")).thenReturn(
                new DebarmentCheckResult(false, "NONE", null, null, null, null, null, "CLEAR", 0)
        );

        // Local content evaluator calculates percentage from document values: 65% meets 50% threshold
        when(localContentEvaluator.evaluate(any(Bidder.class), eq(50))).thenReturn(
                new LocalContentCheckResult(true, 65, 50, true, "Local content 65% meets required 50%.")
        );

        // Old compliance service report with COMPLIANT results
        when(complianceEngineService.evaluateBidder(4L)).thenReturn(
                new ComplianceReportResponse(4L, "Meridian Infra Pvt Ltd", 10L, "T-100", 100, "LOW", List.of(
                        new ComplianceRequirementResult("GST", "COMPLIANT", "GST document verified against bidder profile.", "LOW", null, 20, 1L),
                        new ComplianceRequirementResult("PAN", "COMPLIANT", "PAN document verified against bidder profile.", "LOW", null, 20, 2L),
                        new ComplianceRequirementResult("UDYAM", "COMPLIANT", "UDYAM document verified against bidder profile.", "LOW", null, 15, 3L),
                        new ComplianceRequirementResult("MAKE_IN_INDIA", "COMPLIANT", "Make in India document verified against bidder profile.", "LOW", null, 15, 4L),
                        new ComplianceRequirementResult("OEM_AUTHORIZATION", "COMPLIANT", "OEM authorization verified.", "LOW", null, 20, 5L)
                ), LocalDateTime.now())
        );

        ComplianceAssessmentResponse response = service.assess(4L);

        // Verify total score and risk level
        assertThat(response.getComplianceScore()).isEqualTo(100);
        assertThat(response.getRiskLevel()).isEqualTo("LOW");
        assertThat(response.getMissingMandatoryDocuments()).isEmpty();

        // Verify each individual category score matches configured weights
        ComplianceCategoryResult gstCat = findCategory(response, "GST");
        assertThat(gstCat.getAchievedWeight()).isEqualTo(15.0);
        assertThat(gstCat.getStatus()).isEqualTo("VERIFIED");

        ComplianceCategoryResult panCat = findCategory(response, "PAN");
        assertThat(panCat.getAchievedWeight()).isEqualTo(15.0);
        assertThat(panCat.getStatus()).isEqualTo("VERIFIED");

        ComplianceCategoryResult udyamCat = findCategory(response, "UDYAM");
        assertThat(udyamCat.getAchievedWeight()).isEqualTo(10.0);
        assertThat(udyamCat.getStatus()).isEqualTo("VERIFIED");

        ComplianceCategoryResult localContentCat = findCategory(response, "LOCAL_CONTENT");
        assertThat(localContentCat.getAchievedWeight()).isEqualTo(20.0);
        assertThat(localContentCat.getStatus()).isEqualTo("VERIFIED");

        ComplianceCategoryResult debarmentCat = findCategory(response, "DEBARMENT");
        assertThat(debarmentCat.getAchievedWeight()).isEqualTo(20.0);
        assertThat(debarmentCat.getStatus()).isEqualTo("CLEAR");

        ComplianceCategoryResult docCompletenessCat = findCategory(response, "DOCUMENT_COMPLETENESS");
        assertThat(docCompletenessCat.getAchievedWeight()).isEqualTo(10.0);
        assertThat(docCompletenessCat.getStatus()).isEqualTo("VERIFIED");

        ComplianceCategoryResult crossDocCat = findCategory(response, "CROSS_DOCUMENT_MATCH");
        assertThat(crossDocCat.getAchievedWeight()).isEqualTo(10.0);
        assertThat(crossDocCat.getStatus()).isEqualTo("VERIFIED");
    }

    @Test
    @DisplayName("Case A bidder with simulated government verification (PASS/CLEAR) scores 100/100")
    void caseABidderWithGovernmentVerificationScores100() {
        Bidder bidder = createCaseABidder();
        when(bidderRepository.findById(4L)).thenReturn(Optional.of(bidder));
        when(requirementParser.parseRequiredTypes(bidder.getTender().getDescription())).thenReturn(CASE_A_TYPES);

        List<Document> uploadedDocs = CASE_A_TYPES.stream().map(this::createVerifiedDocument).toList();
        when(documentRepository.findByBidderId(4L)).thenReturn(uploadedDocs);

        // Government verification summary with PASS/CLEAR/VERIFIED items
        when(governmentVerificationService.getLatestSummary(4L)).thenReturn(
                new BidderVerificationSummaryResponse(4L, "VERIFIED", List.of(
                        new BidderGovernmentVerificationItemResponse("GST", "PASS", "27ABCDE1234F1Z5", "Meridian Infra Pvt Ltd", "Meridian Infra Pvt Ltd", null, "SIMULATED_GSTN", "REFERENCE_DATABASE", LocalDateTime.now()),
                        new BidderGovernmentVerificationItemResponse("PAN", "VALID", "ABCDE1234F", "Meridian Infra Pvt Ltd", "Meridian Infra Pvt Ltd", null, "SIMULATED_PAN", "REFERENCE_DATABASE", LocalDateTime.now()),
                        new BidderGovernmentVerificationItemResponse("UDYAM", "VERIFIED", "UDYAM-MH-01-1234567", "Meridian Infra Pvt Ltd", "Meridian Infra Pvt Ltd", null, "SIMULATED_UDYAM", "REFERENCE_DATABASE", LocalDateTime.now()),
                        new BidderGovernmentVerificationItemResponse("MAKE_IN_INDIA", "PASS", null, "Meridian Infra Pvt Ltd", "65%", "Local content meets threshold.", "LOCAL_CONTENT_RULE_ENGINE", "RULE_BASED", LocalDateTime.now()),
                        new BidderGovernmentVerificationItemResponse("BLACKLISTING", "CLEAR", null, "Meridian Infra Pvt Ltd", null, "No debarment record found.", "SIMULATED_DEBARMENT_REGISTRY", "REFERENCE_DATABASE", LocalDateTime.now())
                ))
        );

        ComplianceAssessmentResponse response = service.assess(4L);

        assertThat(response.getComplianceScore()).isEqualTo(100);
        assertThat(response.getRiskLevel()).isEqualTo("LOW");
        assertThat(findCategory(response, "GST").getAchievedWeight()).isEqualTo(15.0);
        assertThat(findCategory(response, "PAN").getAchievedWeight()).isEqualTo(15.0);
        assertThat(findCategory(response, "UDYAM").getAchievedWeight()).isEqualTo(10.0);
        assertThat(findCategory(response, "LOCAL_CONTENT").getAchievedWeight()).isEqualTo(20.0);
        assertThat(findCategory(response, "DEBARMENT").getAchievedWeight()).isEqualTo(20.0);
        assertThat(findCategory(response, "DOCUMENT_COMPLETENESS").getAchievedWeight()).isEqualTo(10.0);
        assertThat(findCategory(response, "CROSS_DOCUMENT_MATCH").getAchievedWeight()).isEqualTo(10.0);
    }

    private ComplianceCategoryResult findCategory(ComplianceAssessmentResponse response, String category) {
        return response.getCategories().stream()
                .filter(c -> c.getCategory().equals(category))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Category not found: " + category));
    }
}
