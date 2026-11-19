package com.sih.bidcompliance.dto;

import com.sih.bidcompliance.entity.TenderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Payload for creating or updating a Tender.
 */
@Data
public class TenderRequest {

    @NotBlank(message = "tenderNumber is required")
    private String tenderNumber;

    @NotBlank(message = "title is required")
    private String title;

    private String description;

    private String department;

    @NotNull(message = "deadline is required")
    private LocalDateTime deadline;

    private TenderStatus status;

    /** Minimum required Make in India local content percentage, e.g. 50. */
    private Integer localContentThresholdPercent;
}
