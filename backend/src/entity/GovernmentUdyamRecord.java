package com.sih.bidcompliance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A row in the SIMULATED Udyam/MSME database. Mock data only - see
 * UdyamVerificationProvider.
 */
@Entity
@Table(name = "government_udyam_records")
@Getter
@Setter
@NoArgsConstructor
public class GovernmentUdyamRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String udyamNumber;

    @Column(nullable = false)
    private String enterpriseName;

    /** e.g. MICRO, SMALL, MEDIUM */
    private String enterpriseType;

    /** e.g. ACTIVE, INACTIVE, CANCELLED */
    @Column(nullable = false)
    private String status;

    private LocalDate registrationDate;

    private String registeredAddress;

    private LocalDateTime lastVerifiedAt;
}
