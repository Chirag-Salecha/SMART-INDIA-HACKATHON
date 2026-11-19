package com.sih.bidcompliance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * The Procurement Officer's final review decision for a bidder on a
 * tender. This is a human decision the system records - it is never made
 * or implied by the AI/rule engine. Append-only: re-reviewing inserts a
 * new row so the full decision history is auditable.
 */
@Entity
@Table(name = "officer_reviews")
@Getter
@Setter
@NoArgsConstructor
public class OfficerReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id", nullable = false)
    private Bidder bidder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tender_id")
    private Tender tender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OfficerDecision decision;

    @Column(columnDefinition = "TEXT")
    private String comments;

    /** Free-text officer/user identifier - no auth system exists yet (see project notes). */
    private String officerName;

    /** Compliance score/risk level at the moment of this decision, for audit context. */
    private Integer complianceScoreAtReview;
    private String riskLevelAtReview;

    @Column(nullable = false, updatable = false)
    private LocalDateTime decidedAt;

    @PrePersist
    protected void onCreate() {
        this.decidedAt = LocalDateTime.now();
    }
}
