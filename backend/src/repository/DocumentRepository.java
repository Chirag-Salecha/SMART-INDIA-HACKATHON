package com.sih.bidcompliance.repository;

import com.sih.bidcompliance.entity.Document;
import com.sih.bidcompliance.entity.DocumentType;
import com.sih.bidcompliance.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByBidderId(Long bidderId);
    long countByVerificationStatus(VerificationStatus verificationStatus);
    List<Document> findByVerificationStatusIn(List<VerificationStatus> statuses);
    Optional<Document> findFirstByBidderIdAndDocumentTypeOrderByUploadedAtDesc(Long bidderId, DocumentType documentType);
    void deleteByBidderId(Long bidderId);
}
