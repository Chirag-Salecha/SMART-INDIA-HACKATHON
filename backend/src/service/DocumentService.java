package com.sih.bidcompliance.service;

import com.sih.bidcompliance.dto.DocumentQueueItemResponse;
import com.sih.bidcompliance.dto.DocumentResponse;
import com.sih.bidcompliance.entity.*;
import com.sih.bidcompliance.exception.ResourceNotFoundException;
import com.sih.bidcompliance.repository.BidderRepository;
import com.sih.bidcompliance.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final BidderRepository bidderRepository;
    private final FileStorageService fileStorageService;
    private final AiVerificationService aiVerificationService;
    private final AuditService auditService;

    /**
     * Stores the uploaded file, then immediately runs AI verification on it.
     * The upload itself always succeeds even if AI verification fails or the
     * AI service is unreachable - see AiVerificationService for the
     * degradation behaviour.
     */
    @Transactional
    public DocumentResponse upload(Long bidderId, DocumentType documentType, MultipartFile file) {
        Bidder bidder = bidderRepository.findById(bidderId)
                .orElseThrow(() -> new ResourceNotFoundException("Bidder not found with id " + bidderId));

        String storedPath = fileStorageService.store(file);

        Document document = new Document();
        document.setBidder(bidder);
        document.setDocumentType(documentType);
        document.setOriginalFileName(file.getOriginalFilename());
        document.setFilePath(storedPath);
        document.setVerificationStatus(VerificationStatus.PENDING);
        document = documentRepository.save(document);

        Long tenderId = bidder.getTender() != null ? bidder.getTender().getId() : null;
        auditService.log(AuditEventType.DOCUMENT_UPLOADED, tenderId, bidderId, document.getId(),
                "Document uploaded: " + document.getOriginalFileName() + " (" + documentType + ")");

        String tenderRequirements = bidder.getTender() != null ? bidder.getTender().getDescription() : null;
        VerificationStatus resultStatus = aiVerificationService.analyzeAndStore(document, bidder, tenderRequirements, file);

        document.setVerificationStatus(resultStatus);
        document = documentRepository.save(document);

        return toResponse(document);
    }

    /**
     * Re-runs AI verification on an already-uploaded document (the
     * "Verify / Analyze" action). Re-reads the file from disk since the
     * original MultipartFile is no longer available.
     */
    @Transactional
    public DocumentResponse reanalyze(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id " + documentId));
        Bidder bidder = document.getBidder();

        MultipartFile file = fileStorageService.readAsMultipartFile(document.getFilePath(), document.getOriginalFileName());
        String tenderRequirements = bidder.getTender() != null ? bidder.getTender().getDescription() : null;

        VerificationStatus resultStatus = aiVerificationService.analyzeAndStore(document, bidder, tenderRequirements, file);
        document.setVerificationStatus(resultStatus);
        document = documentRepository.save(document);

        return toResponse(document);
    }

    public List<DocumentResponse> findByBidderId(Long bidderId) {
        if (!bidderRepository.existsById(bidderId)) {
            throw new ResourceNotFoundException("Bidder not found with id " + bidderId);
        }
        return documentRepository.findByBidderId(bidderId).stream()
                .map(this::toResponse)
                .toList();
    }

    public DocumentResponse findById(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id " + documentId));
        return toResponse(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentQueueItemResponse> getQueue() {
        List<Document> documents = documentRepository.findByVerificationStatusIn(
                List.of(VerificationStatus.PENDING, VerificationStatus.REQUIRES_REVIEW, VerificationStatus.FAILED)
        );

        return documents.stream()
                .map(doc -> new DocumentQueueItemResponse(
                        doc.getId(),
                        doc.getDocumentType(),
                        doc.getVerificationStatus(),
                        doc.getUploadedAt(),
                        doc.getBidder().getId(),
                        doc.getBidder().getCompanyName(),
                        doc.getBidder().getTender().getId(),
                        doc.getBidder().getTender().getTenderNumber()
                ))
                .toList();
    }

    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getBidder().getId(),
                document.getDocumentType(),
                document.getOriginalFileName(),
                document.getFilePath(),
                document.getVerificationStatus(),
                document.getUploadedAt(),
                aiVerificationService.getResultResponse(document.getId())
        );
    }
}
