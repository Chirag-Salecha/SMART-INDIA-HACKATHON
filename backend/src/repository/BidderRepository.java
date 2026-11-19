package com.sih.bidcompliance.repository;

import com.sih.bidcompliance.entity.Bidder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BidderRepository extends JpaRepository<Bidder, Long> {
    List<Bidder> findByTenderId(Long tenderId);
}
