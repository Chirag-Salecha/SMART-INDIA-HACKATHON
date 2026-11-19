package com.sih.bidcompliance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocalContentCheckResult {
    private boolean documentFound;
    private Integer actualPercentage;
    private int requiredPercentage;
    /** null when actualPercentage could not be determined */
    private Boolean passed;
    private String message;
}
