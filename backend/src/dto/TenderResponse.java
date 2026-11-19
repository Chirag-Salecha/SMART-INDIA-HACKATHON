package com.sih.bidcompliance.dto;

import com.sih.bidcompliance.entity.TenderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API representation of a Tender. Kept separate from the entity so we never
 * serialize JPA proxies/relationships directly.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenderResponse {
    private Long id;
    private String tenderNumber;
    private String title;
    private String description;
    private String department;
    private LocalDateTime deadline;
    private TenderStatus status;
    private LocalDateTime createdAt;
    private Integer localContentThresholdPercent;
}
