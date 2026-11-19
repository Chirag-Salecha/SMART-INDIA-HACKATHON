package com.sih.bidcompliance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DebarmentCheckResult {
    private boolean matchFound;
    /** PAN | GSTIN | NAME_FUZZY | NONE */
    private String matchedOn;
    private String matchedIdentifierValue;
    private String entityName;
    private String authority;
    private String reason;
    private String debarmentPeriod;
    private String status;
    private int nameSimilarity;
}
