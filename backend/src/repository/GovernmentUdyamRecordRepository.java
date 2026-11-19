package com.sih.bidcompliance.repository;

import com.sih.bidcompliance.entity.GovernmentUdyamRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GovernmentUdyamRecordRepository extends JpaRepository<GovernmentUdyamRecord, Long> {
    Optional<GovernmentUdyamRecord> findByUdyamNumberIgnoreCase(String udyamNumber);
    boolean existsByUdyamNumberIgnoreCase(String udyamNumber);
}
