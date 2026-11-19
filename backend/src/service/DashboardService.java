package com.sih.bidcompliance.service;

import com.sih.bidcompliance.dto.ComplianceAssessmentResponse;
import com.sih.bidcompliance.dto.DashboardComplianceStatsResponse;
import com.sih.bidcompliance.dto.DashboardStatsResponse;
import com.sih.bidcompliance.entity.Bidder;
import com.sih.bidcompliance.entity.VerificationStatus;
import com.sih.bidcompliance.repository.BidderRepository;
import com.sih.bidcompliance.repository.DocumentRepository;
import com.sih.bidcompliance.repository.TenderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TenderRepository tenderRepository;
    private final BidderRepository bidderRepository;
    private final DocumentRepository documentRepository;
    private final RuleEngineService ruleEngineService;

    public DashboardStatsResponse getStats() {
        long totalTenders = tenderRepository.count();
        long totalBidders = bidderRepository.count();
        long pendingDocuments = documentRepository.countByVerificationStatus(VerificationStatus.PENDING);
        long verifiedDocuments = documentRepository.countByVerificationStatus(VerificationStatus.VERIFIED);

        return new DashboardStatsResponse(totalTenders, totalBidders, pendingDocuments, verifiedDocuments);
    }

    /**
     * Computes the AI Bid Compliance Dashboard metrics by running the rule
     * engine assessment for every bidder. Fine at demo scale (a handful of
     * bidders); a production build would cache/paginate this rather than
     * recomputing on every dashboard load.
     */
    @Transactional
    public DashboardComplianceStatsResponse getComplianceStats() {
        List<Bidder> bidders = bidderRepository.findAll();

        long compliant = 0, needsReview = 0, highRisk = 0, missingDocs = 0;
        List<DashboardComplianceStatsResponse.BidderComplianceSummary> summaries = new java.util.ArrayList<>();

        for (Bidder bidder : bidders) {
            ComplianceAssessmentResponse assessment = ruleEngineService.assess(bidder.getId());

            switch (assessment.getRiskLevel()) {
                case "LOW" -> compliant++;
                case "MEDIUM" -> needsReview++;
                case "HIGH" -> highRisk++;
                default -> { }
            }
            if (!assessment.getMissingMandatoryDocuments().isEmpty()) {
                missingDocs++;
            }

            summaries.add(new DashboardComplianceStatsResponse.BidderComplianceSummary(
                    bidder.getId(),
                    bidder.getCompanyName(),
                    bidder.getTender() != null ? bidder.getTender().getId() : null,
                    bidder.getTender() != null ? bidder.getTender().getTenderNumber() : null,
                    assessment.getComplianceScore(),
                    assessment.getRiskLevel(),
                    assessment.getMissingMandatoryDocuments()
            ));
        }

        return new DashboardComplianceStatsResponse(
                bidders.size(), compliant, needsReview, highRisk, missingDocs, summaries
        );
    }
}
