package com.sih.bidcompliance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private String eventType;
    private String message;
    private Long tenderId;
    private Long bidderId;
    private Long documentId;
    private LocalDateTime createdAt;
}
