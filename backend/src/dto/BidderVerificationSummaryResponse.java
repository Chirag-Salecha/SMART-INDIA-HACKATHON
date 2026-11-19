package com.sih.bidcompliance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidderVerificationSummaryResponse {
    private Long bidderId;
    private String overallStatus; // null if never verified
    private List<BidderGovernmentVerificationItemResponse> items;
}
