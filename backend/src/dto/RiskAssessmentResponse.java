package com.sih.bidcompliance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessmentResponse {
    private Long bidderId;
    private String riskLevel;
    private List<String> reasons;
}
