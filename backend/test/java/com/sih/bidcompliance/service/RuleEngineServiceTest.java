package com.sih.bidcompliance.service;

import com.sih.bidcompliance.dto.BidderGovernmentVerificationItemResponse;
import com.sih.bidcompliance.dto.BidderVerificationSummaryResponse;
import com.sih.bidcompliance.dto.ComplianceAssessmentResponse;
import com.sih.bidcompliance.dto.ComplianceCategoryResult;
import com.sih.bidcompliance.entity.Bidder;
import com.sih.bidcompliance.entity.Document;
import com.sih.bidcompliance.entity.DocumentType;
import com.sih.bidcompliance.entity.Tender;
import com.sih.bidcompliance.entity.VerificationStatus;
import com.sih.bidcompliance.repository.BidderRepository;
import com.sih.bidcompliance.repository.DocumentRepository;
import com.sih.bidcompliance.repository.TenderDocumentRequirementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleEngineServiceTest {

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

    private static final List<DocumentType> BASELINE_TYPES =
            List.of(DocumentType.GST, DocumentType.PAN, DocumentType.UDYAM, DocumentType.MAKE_IN_INDIA, DocumentType.OEM_AUTHORIZATION);

    @BeforeEach
    void setUp() {
        service = new RuleEngineService(
                bidderRepository, documentRepository, documentRequirementRepository,
                governmentVerificationService, complianceEngineService, localContentEvaluator,
                debarmentProvider, requirementParser, auditService
        );
    }

    private Bidder bidderWithTender() {
        Tender tender = new Tender();
        tender.setId(10L);
        tender.setDescription("GST, PAN, Udyam, Make in India, OEM Authorization");

        Bidder bidder = new Bidder();
        bidder.setId(1L);
        bidder.setCompanyName("ABC Technologies Pvt Ltd");
        bidder.setTender(tender);
        return bidder;
    }

    private Document doc(DocumentType type) {
        Document document = new Document();
        document.setDocumentType(type);
        return document;
    }

    private BidderGovernmentVerificationItemResponse item(String type, String status) {
        return new BidderGovernmentVerificationItemResponse(
                type, status, "ID", "Bidder Name", "Gov Name", status.equals("VERIFIED") ? null : "issue detail",
                "SIMULATED_SOURCE", "REFERENCE_DATABASE", LocalDateTime.now()
        );
    }

    @Test
    void fullyCompliantBidderGetsHighScoreAndLowRisk() {
        Bidder bidder = bidderWithTender();
        when(bidderRepository.findById(1L)).thenReturn(Optional.of(bidder));
        when(requirementParser.parseRequiredTypes(bidder.getTender().getDescription())).thenReturn(BASELINE_TYPES);
        when(documentRepository.findByBidderId(1L)).thenReturn(
                BASELINE_TYPES.stream().map(this::doc).toList()
        );
        when(governmentVerificationService.getLatestSummary(1L)).thenReturn(new BidderVerificationSummaryResponse(
                1L, "VERIFIED", List.of(
                        item("GST", "VERIFIED"), item("PAN", "VERIFIED"), item("UDYAM", "VERIFIED"),
                        item("MAKE_IN_INDIA", "VERIFIED"), item("BLACKLISTING", "VERIFIED")
                )
        ));

        ComplianceAssessmentResponse assessment = service.assess(1L);

        assertThat(assessment.getComplianceScore()).isEqualTo(100);
        assertThat(assessment.getRiskLevel()).isEqualTo("LOW");
        assertThat(assessment.getMissingMandatoryDocuments()).isEmpty();
    }

    @Test
    void governmentVerificationFailureTakesPrecedenceOverUploadedDocumentVerification() {
        Bidder bidder = bidderWithTender();
        when(bidderRepository.findById(1L)).thenReturn(Optional.of(bidder));
        when(requirementParser.parseRequiredTypes(bidder.getTender().getDescription())).thenReturn(BASELINE_TYPES);

        // Uploaded documents are present and OCR-verified
        Document gstDoc = doc(DocumentType.GST);
        gstDoc.setVerificationStatus(VerificationStatus.VERIFIED);
        Document panDoc = doc(DocumentType.PAN);
        panDoc.setVerificationStatus(VerificationStatus.VERIFIED);
        Document udyamDoc = doc(DocumentType.UDYAM);
        udyamDoc.setVerificationStatus(VerificationStatus.VERIFIED);
        Document miiDoc = doc(DocumentType.MAKE_IN_INDIA);
        miiDoc.setVerificationStatus(VerificationStatus.VERIFIED);
        Document oemDoc = doc(DocumentType.OEM_AUTHORIZATION);
        oemDoc.setVerificationStatus(VerificationStatus.VERIFIED);

        when(documentRepository.findByBidderId(1L)).thenReturn(
                List.of(gstDoc, panDoc, udyamDoc, miiDoc, oemDoc)
        );

        // But Government Verification returned NOT_FOUND for GST and MISMATCH for PAN
        when(governmentVerificationService.getLatestSummary(1L)).thenReturn(new BidderVerificationSummaryResponse(
                1L, "MISMATCH", List.of(
                        item("GST", "NOT_FOUND"),
                        item("PAN", "MISMATCH"),
                        item("UDYAM", "VERIFIED"),
                        item("MAKE_IN_INDIA", "VERIFIED"),
                        item("BLACKLISTING", "VERIFIED")
                )
        ));

        ComplianceAssessmentResponse assessment = service.assess(1L);

        // GST and PAN must NOT be awarded points despite documents being present
        ComplianceCategoryResult gstCategory = assessment.getCategories().stream()
                .filter(c -> "GST".equals(c.getCategory()))
                .findFirst().orElseThrow();
        assertThat(gstCategory.getStatus()).isEqualTo("NOT_FOUND");
        assertThat(gstCategory.getAchievedWeight()).isEqualTo(0.0);

        ComplianceCategoryResult panCategory = assessment.getCategories().stream()
                .filter(c -> "PAN".equals(c.getCategory()))
                .findFirst().orElseThrow();
        assertThat(panCategory.getStatus()).isEqualTo("MISMATCH");
        assertThat(panCategory.getAchievedWeight()).isEqualTo(0.0);

        // Score must be significantly lower than 100
        assertThat(assessment.getComplianceScore()).isLessThan(80);
        // Risk level must be HIGH because GST is NOT_FOUND and PAN is MISMATCH
        assertThat(assessment.getRiskLevel()).isEqualTo("HIGH");
        assertThat(assessment.getRiskReasons()).anyMatch(r -> r.contains("GST verification issue"));
        assertThat(assessment.getRiskReasons()).anyMatch(r -> r.contains("PAN verification issue"));
    }

    @Test
    void missingMandatoryDocumentIsReportedAndRaisesRisk() {
        Bidder bidder = bidderWithTender();
        when(bidderRepository.findById(1L)).thenReturn(Optional.of(bidder));
        when(requirementParser.parseRequiredTypes(bidder.getTender().getDescription())).thenReturn(BASELINE_TYPES);
        // UDYAM document not uploaded.
        when(documentRepository.findByBidderId(1L)).thenReturn(
                List.of(doc(DocumentType.GST), doc(DocumentType.PAN), doc(DocumentType.MAKE_IN_INDIA), doc(DocumentType.OEM_AUTHORIZATION))
        );
        when(governmentVerificationService.getLatestSummary(1L)).thenReturn(new BidderVerificationSummaryResponse(
                1L, "VERIFIED", List.of(
                        item("GST", "VERIFIED"), item("PAN", "VERIFIED"), item("UDYAM", "VERIFIED"),
                        item("MAKE_IN_INDIA", "VERIFIED"), item("BLACKLISTING", "VERIFIED")
                )
        ));

        ComplianceAssessmentResponse assessment = service.assess(1L);

        assertThat(assessment.getMissingMandatoryDocuments()).contains("UDYAM");
        assertThat(assessment.getRiskLevel()).isEqualTo("MEDIUM");
        assertThat(assessment.getComplianceScore()).isLessThan(100);
    }

    @Test
    void debarmentMatchProducesHighRisk() {
        Bidder bidder = bidderWithTender();
        when(bidderRepository.findById(1L)).thenReturn(Optional.of(bidder));
        when(requirementParser.parseRequiredTypes(bidder.getTender().getDescription())).thenReturn(BASELINE_TYPES);
        when(documentRepository.findByBidderId(1L)).thenReturn(BASELINE_TYPES.stream().map(this::doc).toList());
        when(governmentVerificationService.getLatestSummary(1L)).thenReturn(new BidderVerificationSummaryResponse(
                1L, "REQUIRES_REVIEW", List.of(
                        item("GST", "VERIFIED"), item("PAN", "VERIFIED"), item("UDYAM", "VERIFIED"),
                        item("MAKE_IN_INDIA", "VERIFIED"),
                        new BidderGovernmentVerificationItemResponse("BLACKLISTING", "REQUIRES_REVIEW", "ABCDE1234F",
                                "ABC Technologies Pvt Ltd", "ABC Technologies Pvt Ltd",
                                "Potential debarment match on PAN (Ministry of Petroleum & Natural Gas). Final decision remains with the Procurement Officer.",
                                "SIMULATED_DEBARMENT_REGISTRY", "REFERENCE_DATABASE", LocalDateTime.now())
                )
        ));

        ComplianceAssessmentResponse assessment = service.assess(1L);

        assertThat(assessment.getRiskLevel()).isEqualTo("HIGH");
        assertThat(assessment.getRiskReasons()).anyMatch(r -> r.contains("debarment"));
        // Never disqualifies - recommendation is advisory, not a rejection.
        assertThat(assessment.getRecommendations()).anyMatch(r -> r.contains("Procurement Officer Review Required"));
        assertThat(assessment.getRecommendations()).noneMatch(r -> r.toLowerCase().contains("automatically reject"));
    }

    @Test
    void neverVerifiedBidderGetsPendingCategoriesNotCrash() {
        Bidder bidder = bidderWithTender();
        when(bidderRepository.findById(1L)).thenReturn(Optional.of(bidder));
        when(requirementParser.parseRequiredTypes(bidder.getTender().getDescription())).thenReturn(BASELINE_TYPES);
        when(documentRepository.findByBidderId(1L)).thenReturn(List.of());
        when(governmentVerificationService.getLatestSummary(1L)).thenReturn(new BidderVerificationSummaryResponse(1L, null, List.of()));

        ComplianceAssessmentResponse assessment = service.assess(1L);

        assertThat(assessment.getCategories()).extracting("status").contains("PENDING");
    }
}
