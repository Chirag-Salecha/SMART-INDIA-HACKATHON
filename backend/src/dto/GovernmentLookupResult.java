package com.sih.bidcompliance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Generic internal shape returned by any GovernmentVerificationProvider,
 * used by BidderGovernmentVerificationService for cross-source comparison
 * logic. Source-specific REST endpoints map this into their own typed
 * response DTOs (GstVerificationResponse, etc) rather than exposing this
 * shape directly, so each API's JSON contract stays exactly as specified.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GovernmentLookupResult {
    private boolean found;
    private String identifier;
    /** legalName / enterpriseName - whichever this source calls its name field. */
    private String name;
    private String status;
    /** Source-specific extra fields, e.g. returnFilingStatus, enterpriseType. */
    private Map<String, Object> attributes;
    private String source;
    private LocalDateTime verifiedAt;
    /** Populated only when found = false. */
    private String message;
}
