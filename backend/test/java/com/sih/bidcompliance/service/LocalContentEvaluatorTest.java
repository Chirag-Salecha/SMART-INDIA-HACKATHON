package com.sih.bidcompliance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sih.bidcompliance.dto.LocalContentCheckResult;
import com.sih.bidcompliance.entity.Bidder;
import com.sih.bidcompliance.entity.Document;
import com.sih.bidcompliance.entity.DocumentType;
import com.sih.bidcompliance.entity.VerificationResult;
import com.sih.bidcompliance.repository.DocumentRepository;
import com.sih.bidcompliance.repository.VerificationResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalContentEvaluatorTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private VerificationResultRepository verificationResultRepository;

    private LocalContentEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new LocalContentEvaluator(documentRepository, verificationResultRepository, new ObjectMapper());
    }

    private Bidder bidder() {
        Bidder bidder = new Bidder();
        bidder.setId(1L);
        bidder.setCompanyName("ABC Technologies Pvt Ltd");
        return bidder;
    }

    private Document document() {
        Document document = new Document();
        document.setId(10L);
        return document;
    }

    @Test
    void aboveThresholdPasses() {
        when(documentRepository.findFirstByBidderIdAndDocumentTypeOrderByUploadedAtDesc(1L, DocumentType.MAKE_IN_INDIA))
                .thenReturn(Optional.of(document()));
        VerificationResult result = new VerificationResult();
        result.setExtractedDataJson("{\"localContentPercentage\":\"62%\"}");
        when(verificationResultRepository.findByDocumentId(10L)).thenReturn(Optional.of(result));

        LocalContentCheckResult check = evaluator.evaluate(bidder(), 50);

        assertThat(check.isDocumentFound()).isTrue();
        assertThat(check.getActualPercentage()).isEqualTo(62);
        assertThat(check.getPassed()).isTrue();
    }

    @Test
    void belowThresholdFails() {
        when(documentRepository.findFirstByBidderIdAndDocumentTypeOrderByUploadedAtDesc(1L, DocumentType.MAKE_IN_INDIA))
                .thenReturn(Optional.of(document()));
        VerificationResult result = new VerificationResult();
        result.setExtractedDataJson("{\"localContentPercentage\":\"32%\"}");
        when(verificationResultRepository.findByDocumentId(10L)).thenReturn(Optional.of(result));

        LocalContentCheckResult check = evaluator.evaluate(bidder(), 50);

        assertThat(check.getActualPercentage()).isEqualTo(32);
        assertThat(check.getPassed()).isFalse();
    }

    @Test
    void missingOrUnparsableValueReturnsNullPassed() {
        when(documentRepository.findFirstByBidderIdAndDocumentTypeOrderByUploadedAtDesc(1L, DocumentType.MAKE_IN_INDIA))
                .thenReturn(Optional.of(document()));
        VerificationResult result = new VerificationResult();
        result.setExtractedDataJson("{\"localContentPercentage\":null}");
        when(verificationResultRepository.findByDocumentId(10L)).thenReturn(Optional.of(result));

        LocalContentCheckResult check = evaluator.evaluate(bidder(), 50);

        assertThat(check.getActualPercentage()).isNull();
        assertThat(check.getPassed()).isNull();
    }

    @Test
    void noDocumentUploadedIsReportedAsNotFound() {
        when(documentRepository.findFirstByBidderIdAndDocumentTypeOrderByUploadedAtDesc(1L, DocumentType.MAKE_IN_INDIA))
                .thenReturn(Optional.empty());

        LocalContentCheckResult check = evaluator.evaluate(bidder(), 50);

        assertThat(check.isDocumentFound()).isFalse();
        assertThat(check.getPassed()).isNull();
    }

    @Test
    void usesDefaultThresholdWhenTenderThresholdIsNull() {
        when(documentRepository.findFirstByBidderIdAndDocumentTypeOrderByUploadedAtDesc(1L, DocumentType.MAKE_IN_INDIA))
                .thenReturn(Optional.empty());

        LocalContentCheckResult check = evaluator.evaluate(bidder(), null);

        assertThat(check.getRequiredPercentage()).isEqualTo(LocalContentEvaluator.DEFAULT_THRESHOLD_PERCENT);
    }

    @Test
    void valueRatioCalculatesPercentageCorrectly() {
        when(documentRepository.findFirstByBidderIdAndDocumentTypeOrderByUploadedAtDesc(1L, DocumentType.MAKE_IN_INDIA))
                .thenReturn(Optional.of(document()));
        VerificationResult result = new VerificationResult();
        result.setExtractedDataJson("{\"localContentValue\":\"60,00,000\", \"totalValue\":\"1,00,00,000\"}");
        when(verificationResultRepository.findByDocumentId(10L)).thenReturn(Optional.of(result));

        LocalContentCheckResult check = evaluator.evaluate(bidder(), 50);

        assertThat(check.isDocumentFound()).isTrue();
        assertThat(check.getActualPercentage()).isEqualTo(60);
        assertThat(check.getPassed()).isTrue();
    }

    @Test
    void snakeCasePercentageKeyWorks() {
        when(documentRepository.findFirstByBidderIdAndDocumentTypeOrderByUploadedAtDesc(1L, DocumentType.MAKE_IN_INDIA))
                .thenReturn(Optional.of(document()));
        VerificationResult result = new VerificationResult();
        result.setExtractedDataJson("{\"local_content_percentage\":\"75%\"}");
        when(verificationResultRepository.findByDocumentId(10L)).thenReturn(Optional.of(result));

        LocalContentCheckResult check = evaluator.evaluate(bidder(), 50);

        assertThat(check.isDocumentFound()).isTrue();
        assertThat(check.getActualPercentage()).isEqualTo(75);
        assertThat(check.getPassed()).isTrue();
    }
}
