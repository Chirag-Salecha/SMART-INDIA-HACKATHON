package com.sih.bidcompliance.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class GstVerificationResponse {
    private boolean found;
    private String gstin;
    private String legalName;
    private String status;
    private String returnFilingStatus;
    private String source;
    private LocalDateTime verifiedAt;
    private String message;

    public static GstVerificationResponse from(GovernmentLookupResult result) {
        GstVerificationResponse response = new GstVerificationResponse();
        response.setFound(result.isFound());
        response.setGstin(result.getIdentifier());
        response.setSource(result.getSource());
        response.setVerifiedAt(result.getVerifiedAt());
        if (result.isFound()) {
            response.setLegalName(result.getName());
            response.setStatus(result.getStatus());
            Object filingStatus = result.getAttributes() != null ? result.getAttributes().get("returnFilingStatus") : null;
            response.setReturnFilingStatus(filingStatus != null ? filingStatus.toString() : null);
        } else {
            response.setMessage(result.getMessage());
        }
        return response;
    }
}
