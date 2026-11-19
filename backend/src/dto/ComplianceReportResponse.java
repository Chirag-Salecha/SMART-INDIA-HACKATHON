package com.sih.bidcompliance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceReportResponse {
    private Long bidderId;
    private String companyName;
    private Long tenderId;
    private String tenderNumber;
    private int complianceScore;
    private String riskLevel; // LOW | MEDIUM | HIGH
    private List<ComplianceRequirementResult> requirements;
    private LocalDateTime generatedAt;
}
