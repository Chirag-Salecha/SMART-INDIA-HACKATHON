package com.sih.bidcompliance.repository;

import com.sih.bidcompliance.entity.GovernmentPanRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GovernmentPanRecordRepository extends JpaRepository<GovernmentPanRecord, Long> {
    Optional<GovernmentPanRecord> findByPanIgnoreCase(String pan);
    boolean existsByPanIgnoreCase(String pan);
}
