package com.sih.bidcompliance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * A row in the SIMULATED blacklisting/debarment reference list. Entirely
 * synthetic - never sourced from, or synced with, any real government
 * debarment registry. See DebarmentVerificationProvider.
 */
@Entity
@Table(name = "government_debarment_records")
@Getter
@Setter
@NoArgsConstructor
public class GovernmentDebarmentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String pan;

    private String gstin;

    @Column(nullable = false)
    private String entityName;

    @Column(nullable = false)
    private String authority;

    private String reason;

    private LocalDate debarmentDate;

    private LocalDate expiryDate;

    /** e.g. ACTIVE, EXPIRED */
    @Column(nullable = false)
    private String status;
}
