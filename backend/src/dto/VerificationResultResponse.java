package com.sih.bidcompliance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * API representation of a stored VerificationResult, with the JSON columns
 * deserialized back into structured objects for the frontend.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResultResponse {
    private String status;
    private Double confidence;
    private Map<String, Object> extractedData;
    private List<AiCheckDto> checks;
    private List<String> issues;
    private List<String> recommendations;
    private String verificationSource;
    private AiGovernmentVerificationDto governmentVerification;
    private String errorMessage;
    private LocalDateTime analyzedAt;
}
