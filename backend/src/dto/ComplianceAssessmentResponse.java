package com.sih.bidcompliance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceAssessmentResponse {
    private Long bidderId;
    private String companyName;
    private int complianceScore;
    private String riskLevel; // LOW | MEDIUM | HIGH
    private List<String> riskReasons;
    private List<ComplianceCategoryResult> categories;
    private List<String> missingMandatoryDocuments;
    private List<String> recommendations;
    private LocalDateTime generatedAt;
}
