package com.sih.bidcompliance.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tenders")
@Getter
@Setter
@NoArgsConstructor
public class Tender {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String tenderNumber;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String department;

    private LocalDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenderStatus status = TenderStatus.DRAFT;

    /**
     * Minimum required "Make in India" local content percentage for this
     * tender. Nullable - when not set, the rule engine falls back to a
     * configurable platform default (see RuleEngineService).
     */
    private Integer localContentThresholdPercent;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Entities are never serialized directly in API responses (DTOs are used
    // instead), but JsonIgnore is kept as a safety net against accidental
    // circular serialization if an entity is ever returned by mistake.
    @JsonIgnore
    @OneToMany(mappedBy = "tender", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bidder> bidders = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
