package com.sih.bidcompliance.repository;

import com.sih.bidcompliance.entity.BidderGovernmentVerification;
import com.sih.bidcompliance.entity.GovernmentVerificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BidderGovernmentVerificationRepository extends JpaRepository<BidderGovernmentVerification, Long> {
    List<BidderGovernmentVerification> findByBidderIdOrderByVerifiedAtDesc(Long bidderId);
    Optional<BidderGovernmentVerification> findFirstByBidderIdAndVerificationTypeOrderByVerifiedAtDesc(
            Long bidderId, GovernmentVerificationType verificationType);
    void deleteByBidderId(Long bidderId);
}
