package com.sih.bidcompliance.repository;

import com.sih.bidcompliance.entity.TenderDocumentRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenderDocumentRequirementRepository extends JpaRepository<TenderDocumentRequirement, Long> {
    List<TenderDocumentRequirement> findByTenderId(Long tenderId);
    void deleteByTenderId(Long tenderId);
}
