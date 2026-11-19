package com.sih.bidcompliance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A tender-specific document requirement (e.g. "GST Certificate -
 * Mandatory", "Udyam Certificate - Optional"). Replaces relying solely on
 * free-text keyword parsing of the tender description for missing-document
 * detection - a tender can now declare its requirements explicitly.
 */
@Entity
@Table(name = "tender_document_requirements",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tender_id", "document_type"}))
@Getter
@Setter
@NoArgsConstructor
public class TenderDocumentRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tender_id", nullable = false)
    private Tender tender;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    @Column(nullable = false)
    private boolean mandatory = true;
}
