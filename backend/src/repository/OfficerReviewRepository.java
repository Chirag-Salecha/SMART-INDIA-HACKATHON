package com.sih.bidcompliance.repository;

import com.sih.bidcompliance.entity.OfficerReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfficerReviewRepository extends JpaRepository<OfficerReview, Long> {
    List<OfficerReview> findByBidderIdOrderByDecidedAtDesc(Long bidderId);
    void deleteByBidderId(Long bidderId);
    void deleteByTenderId(Long tenderId);
}
