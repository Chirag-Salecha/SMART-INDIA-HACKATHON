package com.sih.bidcompliance.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mirrors the AI service's governmentVerification block. mode is always
 * "DEMO_SIMULATED" for this build - see AI service for details. Never
 * present this as a real government lookup in the UI.
 */
@Data
@NoArgsConstructor
public class AiGovernmentVerificationDto {
    private String mode;
    private String disclaimer;
    private String source;
    private String portalStatus;
}
