package com.sih.bidcompliance.repository;

import com.sih.bidcompliance.entity.GovernmentDebarmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GovernmentDebarmentRecordRepository extends JpaRepository<GovernmentDebarmentRecord, Long> {
    Optional<GovernmentDebarmentRecord> findByPanIgnoreCase(String pan);
    Optional<GovernmentDebarmentRecord> findByGstinIgnoreCase(String gstin);
    List<GovernmentDebarmentRecord> findAll();
}
