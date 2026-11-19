package com.sih.bidcompliance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardComplianceStatsResponse {
    private long totalBidders;
    private long compliant;      // risk LOW and no missing mandatory docs
    private long needsReview;    // risk MEDIUM
    private long highRisk;       // risk HIGH
    private long missingDocuments; // bidders with at least one missing mandatory document
    private List<BidderComplianceSummary> bidders;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BidderComplianceSummary {
        private Long bidderId;
        private String companyName;
        private Long tenderId;
        private String tenderNumber;
        private int complianceScore;
        private String riskLevel;
        private List<String> missingMandatoryDocuments;
    }
}
