package com.sih.bidcompliance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Stores the outcome of an AI verification run for a single Document.
 * Each Document has at most one VerificationResult - re-analysis overwrites
 * it (the previous result is superseded, not versioned, for MVP simplicity).
 *
 * extractedDataJson / checksJson / issuesJson / recommendationsJson hold
 * JSON-serialized payloads (produced/consumed via Jackson ObjectMapper in
 * AiVerificationService) so the flexible, per-document-type shape of the AI
 * response doesn't require a rigid relational schema.
 */
@Entity
@Table(name = "verification_results")
@Getter
@Setter
@NoArgsConstructor
public class VerificationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false, unique = true)
    private Document document;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status;

    private Double confidence;

    @Column(columnDefinition = "TEXT")
    private String extractedDataJson;

    @Column(columnDefinition = "TEXT")
    private String checksJson;

    @Column(columnDefinition = "TEXT")
    private String issuesJson;

    @Column(columnDefinition = "TEXT")
    private String recommendationsJson;

    /** e.g. AI_AUTOMATED_EXTRACTION, AI_SERVICE_UNAVAILABLE */
    private String verificationSource;

    /** Populated only when verification could not complete (AI service down, etc.). */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime analyzedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.analyzedAt = LocalDateTime.now();
    }
}
