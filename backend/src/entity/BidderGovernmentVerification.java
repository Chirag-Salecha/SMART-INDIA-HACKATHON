package com.sih.bidcompliance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * An auditable record of one government-identifier verification check
 * against a bidder (GST, PAN, Udyam, or a CROSS_SOURCE consistency check).
 * Rows are append-only - re-running verification inserts new rows rather
 * than overwriting, so the full history stays available for audit.
 */
@Entity
@Table(name = "bidder_government_verifications")
@Getter
@Setter
@NoArgsConstructor
public class BidderGovernmentVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id", nullable = false)
    private Bidder bidder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GovernmentVerificationType verificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GovernmentVerificationStatus resultStatus;

    /** The GSTIN/PAN/Udyam number (or other identifier) that was checked. */
    private String identifierChecked;

    /** The value from the bidder's own profile that was compared. */
    private String matchedValue;

    /** The corresponding value found in the simulated government record. */
    private String governmentValue;

    @Column(columnDefinition = "TEXT")
    private String discrepancyDescription;

    /** e.g. SIMULATED_GSTN, SIMULATED_PAN, SIMULATED_UDYAM */
    private String source;

    /**
     * How the result was obtained (official API vs our synthetic reference
     * database vs a deterministic rule vs document-only vs manual review).
     * Never let REFERENCE_DATABASE results be presented as OFFICIAL_API.
     */
    @Enumerated(EnumType.STRING)
    private VerificationSourceType sourceType;

    @Column(columnDefinition = "TEXT")
    private String evidenceDetails;

    @Column(nullable = false)
    private LocalDateTime verifiedAt;

    @PrePersist
    protected void onCreate() {
        this.verifiedAt = LocalDateTime.now();
    }
}
