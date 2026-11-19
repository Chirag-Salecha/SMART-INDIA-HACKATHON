package com.sih.bidcompliance.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class UdyamVerificationResponse {
    private boolean found;
    private String udyamNumber;
    private String enterpriseName;
    private String enterpriseType;
    private String status;
    private String source;
    private LocalDateTime verifiedAt;
    private String message;

    public static UdyamVerificationResponse from(GovernmentLookupResult result) {
        UdyamVerificationResponse response = new UdyamVerificationResponse();
        response.setFound(result.isFound());
        response.setUdyamNumber(result.getIdentifier());
        response.setSource(result.getSource());
        response.setVerifiedAt(result.getVerifiedAt());
        if (result.isFound()) {
            response.setEnterpriseName(result.getName());
            response.setStatus(result.getStatus());
            Object enterpriseType = result.getAttributes() != null ? result.getAttributes().get("enterpriseType") : null;
            response.setEnterpriseType(enterpriseType != null ? enterpriseType.toString() : null);
        } else {
            response.setMessage(result.getMessage());
        }
        return response;
    }
}
