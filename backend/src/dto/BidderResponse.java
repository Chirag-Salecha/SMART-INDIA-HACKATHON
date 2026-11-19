package com.sih.bidcompliance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API representation of a Bidder.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidderResponse {
    private Long id;
    private String companyName;
    private String panNumber;
    private String gstin;
    private String udyamNumber;
    private String email;
    private String phone;
    private Long tenderId;
    private LocalDateTime createdAt;
}
