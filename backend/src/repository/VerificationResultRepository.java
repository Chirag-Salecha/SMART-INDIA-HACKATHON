package com.sih.bidcompliance.repository;

import com.sih.bidcompliance.entity.VerificationResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationResultRepository extends JpaRepository<VerificationResult, Long> {
    Optional<VerificationResult> findByDocumentId(Long documentId);
    void deleteByDocumentId(Long documentId);
}
