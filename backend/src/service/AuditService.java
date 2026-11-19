package com.sih.bidcompliance.service;

import com.sih.bidcompliance.dto.AuditLogResponse;
import com.sih.bidcompliance.entity.AuditEventType;
import com.sih.bidcompliance.entity.AuditLog;
import com.sih.bidcompliance.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Append-only audit trail. Every write is fire-and-forget from the caller's
 * perspective - logging must never be allowed to break the primary flow
 * (upload, analysis, compliance scoring), so failures here are swallowed
 * after being logged to stdout rather than propagated.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(AuditEventType eventType, Long tenderId, Long bidderId, Long documentId, String message) {
        try {
            AuditLog entry = new AuditLog();
            entry.setEventType(eventType);
            entry.setTenderId(tenderId);
            entry.setBidderId(bidderId);
            entry.setDocumentId(documentId);
            entry.setMessage(message);
            auditLogRepository.save(entry);
        } catch (Exception e) {
            System.err.println("Failed to write audit log entry [" + eventType + "]: " + e.getMessage());
        }
    }

    public List<AuditLogResponse> getBidderHistory(Long bidderId) {
        return auditLogRepository.findByBidderIdOrderByCreatedAtDesc(bidderId).stream()
                .map(entry -> new AuditLogResponse(
                        entry.getId(),
                        entry.getEventType().name(),
                        entry.getMessage(),
                        entry.getTenderId(),
                        entry.getBidderId(),
                        entry.getDocumentId(),
                        entry.getCreatedAt()
                ))
                .toList();
    }
}
