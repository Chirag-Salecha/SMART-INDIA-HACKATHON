package com.sih.bidcompliance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregate counts shown on the Dashboard overview cards.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private long totalTenders;
    private long totalBidders;
    private long pendingDocuments;
    private long verifiedDocuments;
}
