package com.sih.bidcompliance.dto;

import com.sih.bidcompliance.entity.DocumentType;
import com.sih.bidcompliance.entity.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Row shape for the Verification Queue page - a document plus just enough
 * bidder/tender context to be useful without a second round-trip.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentQueueItemResponse {
    private Long documentId;
    private DocumentType documentType;
    private VerificationStatus verificationStatus;
    private LocalDateTime uploadedAt;
    private Long bidderId;
    private String companyName;
    private Long tenderId;
    private String tenderNumber;
}
