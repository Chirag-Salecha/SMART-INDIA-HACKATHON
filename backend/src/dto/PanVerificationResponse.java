package com.sih.bidcompliance.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class PanVerificationResponse {
    private boolean found;
    private String pan;
    private String legalName;
    private String status;
    private String incomeTaxComplianceStatus;
    private String source;
    private LocalDateTime verifiedAt;
    private String message;

    public static PanVerificationResponse from(GovernmentLookupResult result) {
        PanVerificationResponse response = new PanVerificationResponse();
        response.setFound(result.isFound());
        response.setPan(result.getIdentifier());
        response.setSource(result.getSource());
        response.setVerifiedAt(result.getVerifiedAt());
        if (result.isFound()) {
            response.setLegalName(result.getName());
            response.setStatus(result.getStatus());
            Object complianceStatus = result.getAttributes() != null ? result.getAttributes().get("incomeTaxComplianceStatus") : null;
            response.setIncomeTaxComplianceStatus(complianceStatus != null ? complianceStatus.toString() : null);
        } else {
            response.setMessage(result.getMessage());
        }
        return response;
    }
}
