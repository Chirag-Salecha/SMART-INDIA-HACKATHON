package com.sih.bidcompliance.service;

import com.sih.bidcompliance.dto.ComplianceAssessmentResponse;
import com.sih.bidcompliance.dto.OfficerReviewRequest;
import com.sih.bidcompliance.dto.OfficerReviewResponse;
import com.sih.bidcompliance.entity.AuditEventType;
import com.sih.bidcompliance.entity.Bidder;
import com.sih.bidcompliance.entity.OfficerReview;
import com.sih.bidcompliance.exception.ResourceNotFoundException;
import com.sih.bidcompliance.repository.BidderRepository;
import com.sih.bidcompliance.repository.OfficerReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Records the Procurement Officer's final decision for a bidder. This
 * service NEVER makes a procurement decision itself - it only stores the
 * human decision and appends an audit entry, per the platform's core
 * constraint (the AI/rule engine is advisory only).
 */
@Service
@RequiredArgsConstructor
public class OfficerReviewService {

    private final BidderRepository bidderRepository;
    private final OfficerReviewRepository officerReviewRepository;
    private final RuleEngineService ruleEngineService;
    private final AuditService auditService;

    @Transactional
    public OfficerReviewResponse recordDecision(Long bidderId, OfficerReviewRequest request) {
        Bidder bidder = bidderRepository.findById(bidderId)
                .orElseThrow(() -> new ResourceNotFoundException("Bidder not found with id " + bidderId));
        Long tenderId = bidder.getTender() != null ? bidder.getTender().getId() : null;

        // Capture the compliance snapshot at the moment of decision, for audit context.
        ComplianceAssessmentResponse assessment = ruleEngineService.assess(bidderId);

        OfficerReview review = new OfficerReview();
        review.setBidder(bidder);
        review.setTender(bidder.getTender());
        review.setDecision(request.getDecision());
        review.setComments(request.getComments());
        review.setOfficerName(request.getOfficerName());
        review.setComplianceScoreAtReview(assessment.getComplianceScore());
        review.setRiskLevelAtReview(assessment.getRiskLevel());

        OfficerReview saved = officerReviewRepository.save(review);

        auditService.log(AuditEventType.OFFICER_REVIEW_RECORDED, tenderId, bidderId, null,
                "Procurement Officer recorded decision: " + request.getDecision()
                        + (request.getOfficerName() != null ? " by " + request.getOfficerName() : ""));

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OfficerReviewResponse> getHistory(Long bidderId) {
        if (!bidderRepository.existsById(bidderId)) {
            throw new ResourceNotFoundException("Bidder not found with id " + bidderId);
        }
        return officerReviewRepository.findByBidderIdOrderByDecidedAtDesc(bidderId).stream()
                .map(this::toResponse)
                .toList();
    }

    private OfficerReviewResponse toResponse(OfficerReview review) {
        return new OfficerReviewResponse(
                review.getId(),
                review.getBidder().getId(),
                review.getTender() != null ? review.getTender().getId() : null,
                review.getDecision().name(),
                review.getComments(),
                review.getOfficerName(),
                review.getComplianceScoreAtReview(),
                review.getRiskLevelAtReview(),
                review.getDecidedAt()
        );
    }
}
