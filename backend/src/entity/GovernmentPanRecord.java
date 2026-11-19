package com.sih.bidcompliance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A row in the SIMULATED Income Tax / PAN database. Mock data only - see
 * PanVerificationProvider.
 */
@Entity
@Table(name = "government_pan_records")
@Getter
@Setter
@NoArgsConstructor
public class GovernmentPanRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String pan;

    @Column(nullable = false)
    private String legalName;

    /** e.g. VALID, INVALID */
    @Column(nullable = false)
    private String status;

    /** e.g. COMPLIANT, NON_COMPLIANT */
    private String incomeTaxComplianceStatus;

    private String assessmentYear;

    private LocalDateTime lastVerifiedAt;
}
