package com.sih.bidcompliance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceCategoryResult {
    private String category;       // GST | PAN | UDYAM | LOCAL_CONTENT | DEBARMENT | DOCUMENT_COMPLETENESS | CROSS_DOCUMENT_MATCH
    private int weight;
    private double achievedWeight;
    private String status;
    private String evidence;
    private String source;
    private String sourceType;
    private Double score;
    private Integer maxScore;

    public ComplianceCategoryResult(String category, int weight, double achievedWeight, String status, String evidence, String source, String sourceType) {
        this.category = category;
        this.weight = weight;
        this.achievedWeight = achievedWeight;
        this.status = status;
        this.evidence = evidence;
        this.source = source;
        this.sourceType = sourceType;
        this.score = achievedWeight;
        this.maxScore = weight;
    }

    public Double getScore() {
        return score != null ? score : achievedWeight;
    }

    public Integer getMaxScore() {
        return maxScore != null ? maxScore : weight;
    }
}
