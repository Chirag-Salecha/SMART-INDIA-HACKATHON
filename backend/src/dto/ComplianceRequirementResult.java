package com.sih.bidcompliance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One row of a bidder's compliance report: a single tender requirement
 * (e.g. GST, PAN) and how the bidder's documents stack up against it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceRequirementResult {
    private String requirement;
    private String status;       // COMPLIANT | MISSING | REQUIRES_REVIEW | NON_COMPLIANT
    private String evidence;
    private String risk;         // LOW | MEDIUM | HIGH
    private String recommendation;
    private int weight;
    private Long documentId;
    private double achievedWeight;
    private Double score;
    private Integer maxScore;

    public ComplianceRequirementResult(String requirement, String status, String evidence, String risk, String recommendation, int weight, Long documentId) {
        this.requirement = requirement;
        this.status = status;
        this.evidence = evidence;
        this.risk = risk;
        this.recommendation = recommendation;
        this.weight = weight;
        this.documentId = documentId;
        this.achievedWeight = "COMPLIANT".equals(status) || "VERIFIED".equals(status) || "CLEAR".equals(status)
                ? weight
                : "REQUIRES_REVIEW".equals(status) ? weight * 0.5 : 0;
        this.score = this.achievedWeight;
        this.maxScore = weight;
    }

    public Double getScore() {
        return score != null ? score : achievedWeight;
    }

    public Integer getMaxScore() {
        return maxScore != null ? maxScore : weight;
    }
}
