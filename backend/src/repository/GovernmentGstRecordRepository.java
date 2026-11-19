package com.sih.bidcompliance.repository;

import com.sih.bidcompliance.entity.GovernmentGstRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GovernmentGstRecordRepository extends JpaRepository<GovernmentGstRecord, Long> {
    Optional<GovernmentGstRecord> findByGstinIgnoreCase(String gstin);
    boolean existsByGstinIgnoreCase(String gstin);
}
