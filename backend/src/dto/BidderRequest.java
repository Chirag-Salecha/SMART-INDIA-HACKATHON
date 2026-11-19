package com.sih.bidcompliance.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Payload for creating or updating a Bidder.
 */
@Data
public class BidderRequest {

    @NotBlank(message = "companyName is required")
    private String companyName;

    private String panNumber;

    private String gstin;

    private String udyamNumber;

    @Email(message = "email must be a valid email address")
    private String email;

    private String phone;

    @NotNull(message = "tenderId is required")
    private Long tenderId;
}
