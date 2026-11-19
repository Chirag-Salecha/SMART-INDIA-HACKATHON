package com.sih.bidcompliance.repository;

import com.sih.bidcompliance.entity.Tender;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenderRepository extends JpaRepository<Tender, Long> {
}
