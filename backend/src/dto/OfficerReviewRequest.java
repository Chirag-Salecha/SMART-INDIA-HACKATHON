package com.sih.bidcompliance.dto;

import com.sih.bidcompliance.entity.OfficerDecision;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OfficerReviewRequest {
    @NotNull(message = "decision is required")
    private OfficerDecision decision;

    private String comments;

    private String officerName;
}
