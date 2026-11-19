package com.sih.bidcompliance.dto;

import com.sih.bidcompliance.entity.DocumentType;
import com.sih.bidcompliance.entity.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API representation of an uploaded Document, including its latest AI
 * verification result if one has been computed (null while PENDING and
 * not yet analyzed, or if analysis has never run).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {
    private Long id;
    private Long bidderId;
    private DocumentType documentType;
    private String originalFileName;
    private String filePath;
    private VerificationStatus verificationStatus;
    private LocalDateTime uploadedAt;
    private VerificationResultResponse verification;
}
