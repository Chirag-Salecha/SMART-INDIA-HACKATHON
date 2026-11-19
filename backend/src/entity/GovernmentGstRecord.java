package com.sih.bidcompliance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A row in the SIMULATED GSTN database. This table represents our own
 * mock government data store - it is never populated from, or synced
 * with, any real government system. See GstVerificationProvider.
 */
@Entity
@Table(name = "government_gst_records")
@Getter
@Setter
@NoArgsConstructor
public class GovernmentGstRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String gstin;

    @Column(nullable = false)
    private String legalName;

    /** e.g. ACTIVE, CANCELLED, SUSPENDED */
    @Column(nullable = false)
    private String status;

    private LocalDate registrationDate;

    /** e.g. FILED, NOT_FILED, OVERDUE */
    private String returnFilingStatus;

    private String registeredAddress;

    private LocalDateTime lastVerifiedAt;
}
