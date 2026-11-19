package com.sih.bidcompliance.service;

import com.sih.bidcompliance.dto.BidderRequest;
import com.sih.bidcompliance.dto.BidderResponse;
import com.sih.bidcompliance.entity.Bidder;
import com.sih.bidcompliance.entity.Document;
import com.sih.bidcompliance.entity.Tender;
import com.sih.bidcompliance.exception.ResourceNotFoundException;
import com.sih.bidcompliance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BidderService {

    private final BidderRepository bidderRepository;
    private final TenderRepository tenderRepository;
    private final OfficerReviewRepository officerReviewRepository;
    private final BidderGovernmentVerificationRepository bidderGovernmentVerificationRepository;
    private final DocumentRepository documentRepository;
    private final VerificationResultRepository verificationResultRepository;

    public BidderResponse create(BidderRequest request) {
        Tender tender = tenderRepository.findById(request.getTenderId())
                .orElseThrow(() -> new ResourceNotFoundException("Tender not found with id " + request.getTenderId()));

        Bidder bidder = new Bidder();
        applyRequest(bidder, request);
        bidder.setTender(tender);

        return toResponse(bidderRepository.save(bidder));
    }

    public List<BidderResponse> findAll() {
        return bidderRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public BidderResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    public List<BidderResponse> findByTenderId(Long tenderId) {
        if (!tenderRepository.existsById(tenderId)) {
            throw new ResourceNotFoundException("Tender not found with id " + tenderId);
        }
        return bidderRepository.findByTenderId(tenderId).stream()
                .map(this::toResponse)
                .toList();
    }

    public BidderResponse update(Long id, BidderRequest request) {
        Bidder bidder = getOrThrow(id);

        if (!bidder.getTender().getId().equals(request.getTenderId())) {
            Tender tender = tenderRepository.findById(request.getTenderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tender not found with id " + request.getTenderId()));
            bidder.setTender(tender);
        }

        applyRequest(bidder, request);
        return toResponse(bidderRepository.save(bidder));
    }

    @Transactional
    public void delete(Long id) {
        Bidder bidder = getOrThrow(id);

        // 1. Delete officer reviews for this bidder
        officerReviewRepository.deleteByBidderId(id);

        // 2. Delete government verification records for this bidder
        bidderGovernmentVerificationRepository.deleteByBidderId(id);

        // 3. For each document, delete its verification result, then the documents
        List<Document> documents = documentRepository.findByBidderId(id);
        for (Document doc : documents) {
            verificationResultRepository.deleteByDocumentId(doc.getId());
        }
        documentRepository.deleteByBidderId(id);

        // 4. Delete the bidder record (government reference records are never touched)
        bidderRepository.delete(bidder);
    }

    private Bidder getOrThrow(Long id) {
        return bidderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bidder not found with id " + id));
    }

    private void applyRequest(Bidder bidder, BidderRequest request) {
        bidder.setCompanyName(request.getCompanyName());
        bidder.setPanNumber(request.getPanNumber());
        bidder.setGstin(request.getGstin());
        bidder.setUdyamNumber(request.getUdyamNumber());
        bidder.setEmail(request.getEmail());
        bidder.setPhone(request.getPhone());
    }

    private BidderResponse toResponse(Bidder bidder) {
        return new BidderResponse(
                bidder.getId(),
                bidder.getCompanyName(),
                bidder.getPanNumber(),
                bidder.getGstin(),
                bidder.getUdyamNumber(),
                bidder.getEmail(),
                bidder.getPhone(),
                bidder.getTender().getId(),
                bidder.getCreatedAt()
        );
    }
}
