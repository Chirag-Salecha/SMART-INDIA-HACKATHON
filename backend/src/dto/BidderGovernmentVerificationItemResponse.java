package com.sih.bidcompliance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidderGovernmentVerificationItemResponse {
    private String verificationType;   // GST | PAN | UDYAM | MAKE_IN_INDIA | BLACKLISTING | CROSS_SOURCE
    private String resultStatus;       // VERIFIED | MISMATCH | NOT_FOUND | NON_COMPLIANT | REQUIRES_REVIEW
    private String identifierChecked;
    private String matchedValue;
    private String governmentValue;
    private String discrepancyDescription;
    private String source;
    private String sourceType;         // OFFICIAL_API | REFERENCE_DATABASE | RULE_BASED | DOCUMENT_ONLY | MANUAL_REVIEW
    private LocalDateTime verifiedAt;
}
