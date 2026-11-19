package com.sih.bidcompliance.service;

import com.sih.bidcompliance.dto.ComplianceAssessmentResponse;
import com.sih.bidcompliance.dto.OfficerReviewRequest;
import com.sih.bidcompliance.dto.OfficerReviewResponse;
import com.sih.bidcompliance.entity.Bidder;
import com.sih.bidcompliance.entity.OfficerDecision;
import com.sih.bidcompliance.entity.OfficerReview;
import com.sih.bidcompliance.repository.BidderRepository;
import com.sih.bidcompliance.repository.OfficerReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfficerReviewServiceTest {

    @Mock private BidderRepository bidderRepository;
    @Mock private OfficerReviewRepository officerReviewRepository;
    @Mock private RuleEngineService ruleEngineService;
    @Mock private AuditService auditService;

    private OfficerReviewService service;

    @BeforeEach
    void setUp() {
        service = new OfficerReviewService(bidderRepository, officerReviewRepository, ruleEngineService, auditService);
    }

    @Test
    void decisionIsStoredWithComplianceSnapshotAndAudited() {
        Bidder bidder = new Bidder();
        bidder.setId(1L);
        bidder.setCompanyName("ABC Technologies Pvt Ltd");
        when(bidderRepository.findById(1L)).thenReturn(Optional.of(bidder));
        when(ruleEngineService.assess(1L)).thenReturn(new ComplianceAssessmentResponse(
                1L, "ABC Technologies Pvt Ltd", 87, "MEDIUM", List.of("some reason"), List.of(), List.of(), List.of(), LocalDateTime.now()
        ));
        when(officerReviewRepository.save(any(OfficerReview.class))).thenAnswer(invocation -> {
            OfficerReview r = invocation.getArgument(0);
            r.setId(99L);
            r.setDecidedAt(LocalDateTime.now());
            return r;
        });

        OfficerReviewRequest request = new OfficerReviewRequest();
        request.setDecision(OfficerDecision.APPROVE);
        request.setComments("Looks fine");
        request.setOfficerName("R. Sharma");

        OfficerReviewResponse response = service.recordDecision(1L, request);

        assertThat(response.getDecision()).isEqualTo("APPROVE");
        assertThat(response.getComplianceScoreAtReview()).isEqualTo(87);
        assertThat(response.getRiskLevelAtReview()).isEqualTo("MEDIUM");
        assertThat(response.getDisclaimer()).contains("Procurement Officer");

        ArgumentCaptor<OfficerReview> captor = ArgumentCaptor.forClass(OfficerReview.class);
        verify(officerReviewRepository).save(captor.capture());
        assertThat(captor.getValue().getDecision()).isEqualTo(OfficerDecision.APPROVE);
        assertThat(captor.getValue().getComments()).isEqualTo("Looks fine");

        verify(auditService).log(any(), any(), eq(1L), any(), contains("APPROVE"));
    }
}
