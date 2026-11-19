package com.sih.bidcompliance.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Deserialized response from POST {ai-service}/api/ai/analyze.
 * Field names match the AI service's camelCase JSON exactly, so no
 * @JsonProperty mapping is needed.
 */
@Data
@NoArgsConstructor
public class AiAnalysisResponse {
    private String status;
    private Double confidence;
    private String documentType;
    private Map<String, Object> extractedData;
    private List<AiCheckDto> checks;
    private List<String> issues;
    private List<String> recommendations;
    private String verificationSource;
    private AiGovernmentVerificationDto governmentVerification;
    private String rawTextPreview;
}
