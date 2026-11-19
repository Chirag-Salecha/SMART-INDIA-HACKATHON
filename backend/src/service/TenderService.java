package com.sih.bidcompliance.service;

import com.sih.bidcompliance.dto.TenderDocumentRequirementDto;
import com.sih.bidcompliance.dto.TenderRequest;
import com.sih.bidcompliance.dto.TenderResponse;
import com.sih.bidcompliance.entity.Bidder;
import com.sih.bidcompliance.entity.Tender;
import com.sih.bidcompliance.entity.TenderDocumentRequirement;
import com.sih.bidcompliance.entity.TenderStatus;
import com.sih.bidcompliance.exception.ResourceNotFoundException;
import com.sih.bidcompliance.repository.BidderRepository;
import com.sih.bidcompliance.repository.OfficerReviewRepository;
import com.sih.bidcompliance.repository.TenderDocumentRequirementRepository;
import com.sih.bidcompliance.repository.TenderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TenderService {

    private final TenderRepository tenderRepository;
    private final TenderDocumentRequirementRepository documentRequirementRepository;
    private final BidderRepository bidderRepository;
    private final BidderService bidderService;
    private final OfficerReviewRepository officerReviewRepository;

    public TenderResponse create(TenderRequest request) {
        Tender tender = new Tender();
        tender.setStatus(TenderStatus.DRAFT);
        applyRequest(tender, request);
        Tender saved = tenderRepository.save(tender);
        return toResponse(saved);
    }

    public List<TenderResponse> findAll() {
        return tenderRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public TenderResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    public TenderResponse update(Long id, TenderRequest request) {
        Tender tender = getOrThrow(id);
        applyRequest(tender, request);
        return toResponse(tenderRepository.save(tender));
    }

    @Transactional
    public void delete(Long id) {
        Tender tender = getOrThrow(id);

        // 1. Delete document requirements for this tender
        documentRequirementRepository.deleteByTenderId(id);

        // 2. Delete officer reviews for this tender
        officerReviewRepository.deleteByTenderId(id);

        // 3. Delete all bidders belonging to this tender safely (which cascades their reviews, gov verifications, verifications, docs)
        List<Bidder> bidders = bidderRepository.findByTenderId(id);
        for (Bidder bidder : bidders) {
            bidderService.delete(bidder.getId());
        }

        // 4. Delete the tender record
        tenderRepository.delete(tender);
    }

    @Transactional(readOnly = true)
    public List<TenderDocumentRequirementDto> getDocumentRequirements(Long tenderId) {
        if (!tenderRepository.existsById(tenderId)) {
            throw new ResourceNotFoundException("Tender not found with id " + tenderId);
        }
        return documentRequirementRepository.findByTenderId(tenderId).stream()
                .map(r -> new TenderDocumentRequirementDto(r.getDocumentType(), r.isMandatory()))
                .toList();
    }

    /** Replaces the full set of document requirements for a tender. */
    @Transactional
    public List<TenderDocumentRequirementDto> setDocumentRequirements(Long tenderId, List<TenderDocumentRequirementDto> requirements) {
        Tender tender = getOrThrow(tenderId);
        documentRequirementRepository.deleteByTenderId(tenderId);

        List<TenderDocumentRequirement> saved = requirements.stream()
                .map(dto -> {
                    TenderDocumentRequirement requirement = new TenderDocumentRequirement();
                    requirement.setTender(tender);
                    requirement.setDocumentType(dto.getDocumentType());
                    requirement.setMandatory(dto.isMandatory());
                    return documentRequirementRepository.save(requirement);
                })
                .toList();

        return saved.stream()
                .map(r -> new TenderDocumentRequirementDto(r.getDocumentType(), r.isMandatory()))
                .toList();
    }

    private Tender getOrThrow(Long id) {
        return tenderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tender not found with id " + id));
    }

    private void applyRequest(Tender tender, TenderRequest request) {
        tender.setTenderNumber(request.getTenderNumber());
        tender.setTitle(request.getTitle());
        tender.setDescription(request.getDescription());
        tender.setDepartment(request.getDepartment());
        tender.setDeadline(request.getDeadline());
        if (request.getStatus() != null) {
            tender.setStatus(request.getStatus());
        }
        if (request.getLocalContentThresholdPercent() != null) {
            tender.setLocalContentThresholdPercent(request.getLocalContentThresholdPercent());
        }
    }

    private TenderResponse toResponse(Tender tender) {
        return new TenderResponse(
                tender.getId(),
                tender.getTenderNumber(),
                tender.getTitle(),
                tender.getDescription(),
                tender.getDepartment(),
                tender.getDeadline(),
                tender.getStatus(),
                tender.getCreatedAt(),
                tender.getLocalContentThresholdPercent()
        );
    }
}
