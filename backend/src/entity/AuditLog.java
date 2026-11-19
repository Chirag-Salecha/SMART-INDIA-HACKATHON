package com.sih.bidcompliance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Append-only audit trail for transparency/auditability (SIH requirement).
 * Kept as a flat table with nullable FKs rather than relational associations
 * so a single entry can reference any subset of tender/bidder/document
 * without complicating the domain model.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditEventType eventType;

    @Column(columnDefinition = "TEXT")
    private String message;

    private Long tenderId;

    private Long bidderId;

    private Long documentId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
