package com.sih.bidcompliance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfficerReviewResponse {
    private Long id;
    private Long bidderId;
    private Long tenderId;
    private String decision;
    private String comments;
    private String officerName;
    private Integer complianceScoreAtReview;
    private String riskLevelAtReview;
    private LocalDateTime decidedAt;
    /** Always present, per spec: the system never claims to make the final decision. */
    private final String disclaimer = "Final procurement decision remains with the authorized Procurement Officer.";
}
