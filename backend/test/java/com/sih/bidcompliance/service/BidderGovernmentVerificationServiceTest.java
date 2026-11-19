package com.sih.bidcompliance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sih.bidcompliance.dto.BidderGovernmentVerificationItemResponse;
import com.sih.bidcompliance.dto.BidderVerificationSummaryResponse;
import com.sih.bidcompliance.dto.DebarmentCheckResult;
import com.sih.bidcompliance.dto.GovernmentLookupResult;
import com.sih.bidcompliance.dto.LocalContentCheckResult;
import com.sih.bidcompliance.entity.*;
import com.sih.bidcompliance.repository.BidderGovernmentVerificationRepository;
import com.sih.bidcompliance.repository.BidderRepository;
import com.sih.bidcompliance.repository.DocumentRepository;
import com.sih.bidcompliance.repository.VerificationResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidderGovernmentVerificationServiceTest {

    @Mock private BidderRepository bidderRepository;
    @Mock private BidderGovernmentVerificationRepository verificationRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private VerificationResultRepository verificationResultRepository;
    @Mock private GstVerificationProvider gstProvider;
    @Mock private PanVerificationProvider panProvider;
    @Mock private UdyamVerificationProvider udyamProvider;
    @Mock private DebarmentVerificationProvider debarmentProvider;
    @Mock private LocalContentEvaluator localContentEvaluator;
    @Mock private AuditService auditService;

    private BidderGovernmentVerificationService service;

    @BeforeEach
    void setUp() {
        // Real ObjectMapper (not mocked) so JSON evidence serialization actually runs.
        service = new BidderGovernmentVerificationService(
                bidderRepository, verificationRepository, documentRepository, verificationResultRepository,
                gstProvider, panProvider, udyamProvider, debarmentProvider, localContentEvaluator,
                auditService, new ObjectMapper(), new FuzzyNameMatcher()
        );

        // Default "nothing found" stubs so every test doesn't need to repeat
        // them - individual tests override where the scenario requires it.
        lenient().when(debarmentProvider.check(any(), any(), any()))
                .thenReturn(new DebarmentCheckResult(false, "NONE", null, null, null, null, null, null, 0));
        lenient().when(localContentEvaluator.evaluate(any(), any()))
                .thenReturn(new LocalContentCheckResult(false, null, 50, null, "No Make in India / Local Content document has been uploaded."));
    }

    private Bidder bidder(String companyName, String gstin, String pan, String udyam) {
        Bidder bidder = new Bidder();
        bidder.setId(1L);
        bidder.setCompanyName(companyName);
        bidder.setGstin(gstin);
        bidder.setPanNumber(pan);
        bidder.setUdyamNumber(udyam);
        return bidder;
    }

    private void mockDocumentWithExtracted(Long bidderId, DocumentType type, Long docId, String json) {
        Document doc = new Document();
        doc.setId(docId);
        doc.setDocumentType(type);
        lenient().when(documentRepository.findFirstByBidderIdAndDocumentTypeOrderByUploadedAtDesc(bidderId, type))
                .thenReturn(Optional.of(doc));
        VerificationResult vr = new VerificationResult();
        vr.setExtractedDataJson(json);
        lenient().when(verificationResultRepository.findByDocumentId(docId))
                .thenReturn(Optional.of(vr));
    }

    private GovernmentLookupResult found(String identifier, String name, String status, String source) {
        return new GovernmentLookupResult(true, identifier, name, status, Map.of(), source, LocalDateTime.now(), null);
    }

    private GovernmentLookupResult notFound(String identifier, String source) {
        return new GovernmentLookupResult(false, identifier, null, null, Map.of(), source, LocalDateTime.now(), "not found");
    }

    private BidderGovernmentVerificationItemResponse itemOfType(BidderVerificationSummaryResponse summary, String type) {
        return summary.getItems().stream().filter(i -> i.getVerificationType().equals(type)).findFirst().orElseThrow();
    }

    @Test
    void allValidAndConsistentRecordsProduceOverallVerified() {
        Bidder bidder = bidder("ABC Technologies Pvt Ltd", "29ABCDE1234F1Z5", "ABCDE1234F", "UDYAM-KA-01-1234567");
        when(bidderRepository.findById(1L)).thenReturn(Optional.of(bidder));
        mockDocumentWithExtracted(1L, DocumentType.GST, 10L, "{\"gstin\":\"29ABCDE1234F1Z5\"}");
        mockDocumentWithExtracted(1L, DocumentType.PAN, 11L, "{\"pan\":\"ABCDE1234F\"}");
        mockDocumentWithExtracted(1L, DocumentType.UDYAM, 12L, "{\"udyamNumber\":\"UDYAM-KA-01-1234567\"}");

        when(gstProvider.verify("29ABCDE1234F1Z5")).thenReturn(found("29ABCDE1234F1Z5", "ABC Technologies Pvt Ltd", "ACTIVE", "SIMULATED_GSTN"));
        when(panProvider.verify("ABCDE1234F")).thenReturn(found("ABCDE1234F", "ABC Technologies Pvt Ltd", "VALID", "SIMULATED_PAN"));
        when(udyamProvider.verify("UDYAM-KA-01-1234567")).thenReturn(found("UDYAM-KA-01-1234567", "ABC Technologies Pvt Ltd", "ACTIVE", "SIMULATED_UDYAM"));

        BidderVerificationSummaryResponse summary = service.verifyBidder(1L);

        assertThat(itemOfType(summary, "GST").getResultStatus()).isEqualTo("VERIFIED");
        assertThat(itemOfType(summary, "PAN").getResultStatus()).isEqualTo("VERIFIED");
        assertThat(itemOfType(summary, "UDYAM").getResultStatus()).isEqualTo("VERIFIED");
        assertThat(itemOfType(summary, "BLACKLISTING").getResultStatus()).isEqualTo("VERIFIED");
        assertThat(summary.getItems()).noneMatch(i -> i.getVerificationType().equals("CROSS_SOURCE"));
    }

    @Test
    void gstNotFoundProducesNotFoundStatus() {
        Bidder bidder = bidder("Some Company", null, null, null);
        when(bidderRepository.findById(1L)).thenReturn(Optional.of(bidder));
        mockDocumentWithExtracted(1L, DocumentType.GST, 10L, "{\"gstin\":\"00NOPE0000N1Z1\"}");
        when(gstProvider.verify("00NOPE0000N1Z1")).thenReturn(notFound("00NOPE0000N1Z1", "SIMULATED_GSTN"));

        BidderVerificationSummaryResponse summary = service.verifyBidder(1L);

        assertThat(itemOfType(summary, "GST").getResultStatus()).isEqualTo("NOT_FOUND");
    }

    @Test
    void cancelledGstProducesNonCompliantStatus() {
        Bidder bidder = bidder("Cancel Traders Pvt Ltd", null, null, null);
        when(bidderRepository.findById(1L)).thenReturn(Optional.of(bidder));
        mockDocumentWithExtracted(1L, DocumentType.GST, 10L, "{\"gstin\":\"27CANCL0000C1Z1\"}");
        when(gstProvider.verify("27CANCL0000C1Z1")).thenReturn(found("27CANCL0000C1Z1", "Cancel Traders Pvt Ltd", "CANCELLED", "SIMULATED_GSTN"));

        BidderVerificationSummaryResponse summary = service.verifyBidder(1L);

        assertThat(itemOfType(summary, "GST").getResultStatus()).isEqualTo("NON_COMPLIANT");
        assertThat(summary.getOverallStatus()).isEqualTo("NON_COMPLIANT");
    }

    @Test
    void crossSourceNameInconsistencyIsFlaggedForReview() {
        Bidder bidder = bidder("ABC Technologies Pvt Ltd", null, null, null);
        when(bidderRepository.findById(1L)).thenReturn(Optional.of(bidder));
        mockDocumentWithExtracted(1L, DocumentType.GST, 10L, "{\"gstin\":\"07INCON1234C1Z2\"}");
        mockDocumentWithExtracted(1L, DocumentType.PAN, 11L, "{\"pan\":\"INCON1234C\"}");
        mockDocumentWithExtracted(1L, DocumentType.UDYAM, 12L, "{\"udyamNumber\":\"UDYAM-MH-03-1112223\"}");

        when(gstProvider.verify(any())).thenReturn(found("07INCON1234C1Z2", "ABC Technologies Pvt Ltd", "ACTIVE", "SIMULATED_GSTN"));
        when(panProvider.verify(any())).thenReturn(found("INCON1234C", "ABC Technologies Pvt Ltd", "VALID", "SIMULATED_PAN"));
        when(udyamProvider.verify(any())).thenReturn(found("UDYAM-MH-03-1112223", "ABC Technology Limited", "ACTIVE", "SIMULATED_UDYAM"));

        BidderVerificationSummaryResponse summary = service.verifyBidder(1L);

        boolean hasCrossSourceWarning = summary.getItems().stream()
                .anyMatch(i -> i.getVerificationType().equals("CROSS_SOURCE") && i.getResultStatus().equals("REQUIRES_REVIEW"));
        assertThat(hasCrossSourceWarning).isTrue();
        assertThat(summary.getOverallStatus()).isNotEqualTo("NON_COMPLIANT");
    }

    @Test
    void debarmentMatchIsFlaggedForReviewNotAutoDisqualified() {
        Bidder bidder = bidder("Risky Vendors Pvt Ltd", null, null, null);
        when(bidderRepository.findById(1L)).thenReturn(Optional.of(bidder));
        mockDocumentWithExtracted(1L, DocumentType.GST, 10L, "{\"gstin\":\"29ABCDE1234F1Z5\"}");
        mockDocumentWithExtracted(1L, DocumentType.PAN, 11L, "{\"pan\":\"ABCDE1234F\"}");

        when(gstProvider.verify(any())).thenReturn(found("29ABCDE1234F1Z5", "Risky Vendors Pvt Ltd", "ACTIVE", "SIMULATED_GSTN"));
        when(panProvider.verify(any())).thenReturn(found("ABCDE1234F", "Risky Vendors Pvt Ltd", "VALID", "SIMULATED_PAN"));
        when(debarmentProvider.check(any(), any(), any()))
                .thenReturn(new DebarmentCheckResult(true, "PAN", "ABCDE1234F", "Risky Vendors Pvt Ltd",
                        "Ministry of Petroleum & Natural Gas", "Fraudulent documentation", "2024-01-01 to 2027-01-01", "ACTIVE", 100));

        BidderVerificationSummaryResponse summary = service.verifyBidder(1L);

        BidderGovernmentVerificationItemResponse debarmentItem = itemOfType(summary, "BLACKLISTING");
        assertThat(debarmentItem.getResultStatus()).isEqualTo("REQUIRES_REVIEW");
        assertThat(debarmentItem.getDiscrepancyDescription()).contains("Procurement Officer");
    }

    @Test
    void localContentBelowThresholdIsNonCompliant() {
        Bidder bidder = bidder("ABC Technologies Pvt Ltd", null, null, null);
        when(bidderRepository.findById(1L)).thenReturn(Optional.of(bidder));
        mockDocumentWithExtracted(1L, DocumentType.GST, 10L, "{\"gstin\":\"29ABCDE1234F1Z5\"}");
        mockDocumentWithExtracted(1L, DocumentType.PAN, 11L, "{\"pan\":\"ABCDE1234F\"}");

        when(gstProvider.verify(any())).thenReturn(found("29ABCDE1234F1Z5", "ABC Technologies Pvt Ltd", "ACTIVE", "SIMULATED_GSTN"));
        when(panProvider.verify(any())).thenReturn(found("ABCDE1234F", "ABC Technologies Pvt Ltd", "VALID", "SIMULATED_PAN"));
        when(localContentEvaluator.evaluate(any(), any()))
                .thenReturn(new LocalContentCheckResult(true, 32, 50, false, null));

        BidderVerificationSummaryResponse summary = service.verifyBidder(1L);

        assertThat(itemOfType(summary, "MAKE_IN_INDIA").getResultStatus()).isEqualTo("NON_COMPLIANT");
    }

    @Test
    void verificationResultsArePersistedForEveryCheck() {
        Bidder bidder = bidder("ABC Technologies Pvt Ltd", null, null, null);
        when(bidderRepository.findById(1L)).thenReturn(Optional.of(bidder));
        mockDocumentWithExtracted(1L, DocumentType.GST, 10L, "{\"gstin\":\"29ABCDE1234F1Z5\"}");
        mockDocumentWithExtracted(1L, DocumentType.PAN, 11L, "{\"pan\":\"ABCDE1234F\"}");
        mockDocumentWithExtracted(1L, DocumentType.UDYAM, 12L, "{\"udyamNumber\":\"UDYAM-KA-01-1234567\"}");

        when(gstProvider.verify(any())).thenReturn(found("29ABCDE1234F1Z5", "ABC Technologies Pvt Ltd", "ACTIVE", "SIMULATED_GSTN"));
        when(panProvider.verify(any())).thenReturn(found("ABCDE1234F", "ABC Technologies Pvt Ltd", "VALID", "SIMULATED_PAN"));
        when(udyamProvider.verify(any())).thenReturn(found("UDYAM-KA-01-1234567", "ABC Technologies Pvt Ltd", "ACTIVE", "SIMULATED_UDYAM"));

        service.verifyBidder(1L);

        // GST + PAN + UDYAM + BLACKLISTING + MAKE_IN_INDIA = 5 rows
        verify(verificationRepository, times(5)).save(any(BidderGovernmentVerification.class));
        verify(auditService).log(any(), any(), eq(1L), any(), contains("started"));
        verify(auditService).log(any(), any(), eq(1L), any(), contains("completed"));
    }

    @Test
    void missingIdentifierRequiresReview() {
        Bidder bidder = bidder("Some Company", null, null, null);
        when(bidderRepository.findById(1L)).thenReturn(Optional.of(bidder));

        BidderVerificationSummaryResponse summary = service.verifyBidder(1L);

        assertThat(itemOfType(summary, "GST").getResultStatus()).isEqualTo("REQUIRES_REVIEW");
        assertThat(itemOfType(summary, "PAN").getResultStatus()).isEqualTo("REQUIRES_REVIEW");
        assertThat(itemOfType(summary, "UDYAM").getResultStatus()).isEqualTo("REQUIRES_REVIEW");
        verify(gstProvider, never()).verify(any());
    }

    @Test
    void verifyExtractedDocument_validGst_returnsVerified() {
        Bidder bidder = bidder("TechVision Solutions Pvt Ltd", null, null, null);
        Document doc = new Document();
        doc.setId(10L);
        doc.setOriginalFileName("gst.pdf");
        doc.setDocumentType(DocumentType.GST);

        when(gstProvider.verify("29AABCT1234F1ZX")).thenReturn(
                found("29AABCT1234F1ZX", "TechVision Solutions Pvt Ltd", "ACTIVE", "SIMULATED_GSTN")
        );

        GovernmentVerificationStatus status = service.verifyExtractedDocument(
                bidder, doc, Map.of("gstin", "29AABCT1234F1ZX")
        );

        assertThat(status).isEqualTo(GovernmentVerificationStatus.VERIFIED);
        verify(verificationRepository).save(any(BidderGovernmentVerification.class));
    }

    @Test
    void verifyExtractedDocument_mismatchGst_returnsMismatch() {
        Bidder bidder = bidder("TechVision Solutions Pvt Ltd", null, null, null);
        Document doc = new Document();
        doc.setId(10L);
        doc.setOriginalFileName("gst.pdf");
        doc.setDocumentType(DocumentType.GST);

        // GSTIN belongs to ABC Technologies Pvt Ltd
        when(gstProvider.verify("29ABCDE1234F1Z5")).thenReturn(
                found("29ABCDE1234F1Z5", "ABC Technologies Pvt Ltd", "ACTIVE", "SIMULATED_GSTN")
        );

        GovernmentVerificationStatus status = service.verifyExtractedDocument(
                bidder, doc, Map.of("gstin", "29ABCDE1234F1Z5")
        );

        assertThat(status).isEqualTo(GovernmentVerificationStatus.MISMATCH);
        verify(verificationRepository).save(any(BidderGovernmentVerification.class));
    }

    @Test
    void verifyExtractedDocument_notFoundGst_returnsNotFound() {
        Bidder bidder = bidder("TechVision Solutions Pvt Ltd", null, null, null);
        Document doc = new Document();
        doc.setId(10L);
        doc.setOriginalFileName("gst.pdf");
        doc.setDocumentType(DocumentType.GST);

        when(gstProvider.verify("29XXXXX9999X1Z9")).thenReturn(
                notFound("29XXXXX9999X1Z9", "SIMULATED_GSTN")
        );

        GovernmentVerificationStatus status = service.verifyExtractedDocument(
                bidder, doc, Map.of("gstin", "29XXXXX9999X1Z9")
        );

        assertThat(status).isEqualTo(GovernmentVerificationStatus.NOT_FOUND);
        verify(verificationRepository).save(any(BidderGovernmentVerification.class));
    }

    @Test
    void verifyExtractedDocument_missingIdentifier_returnsRequiresReview() {
        Bidder bidder = bidder("TechVision Solutions Pvt Ltd", null, null, null);
        Document doc = new Document();
        doc.setId(10L);
        doc.setOriginalFileName("gst.pdf");
        doc.setDocumentType(DocumentType.GST);

        GovernmentVerificationStatus status = service.verifyExtractedDocument(
                bidder, doc, Map.of()
        );

        assertThat(status).isEqualTo(GovernmentVerificationStatus.REQUIRES_REVIEW);
        verify(gstProvider, never()).verify(any());
    }

    @Test
    void getLatestSummaryReturnsEmptyItemsWhenNeverVerified() {
        when(bidderRepository.existsById(1L)).thenReturn(true);
        when(verificationRepository.findByBidderIdOrderByVerifiedAtDesc(1L)).thenReturn(List.of());

        BidderVerificationSummaryResponse summary = service.getLatestSummary(1L);

        assertThat(summary.getItems()).isEmpty();
        assertThat(summary.getOverallStatus()).isNull();
    }
}
