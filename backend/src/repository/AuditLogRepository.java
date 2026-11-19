package com.sih.bidcompliance.repository;

import com.sih.bidcompliance.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByBidderIdOrderByCreatedAtDesc(Long bidderId);
    List<AuditLog> findByDocumentIdOrderByCreatedAtDesc(Long documentId);
}
