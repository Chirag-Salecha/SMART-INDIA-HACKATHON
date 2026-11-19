package com.sih.bidcompliance.controller;

import com.sih.bidcompliance.dto.BidderVerificationSummaryResponse;
import com.sih.bidcompliance.service.BidderGovernmentVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bidders/{bidderId}/government-verification")
@RequiredArgsConstructor
public class BidderGovernmentVerificationController {

    private final BidderGovernmentVerificationService verificationService;

    /** "Verify Government Records" action - runs verification now. */
    @PostMapping("/verify")
    public ResponseEntity<BidderVerificationSummaryResponse> verify(@PathVariable Long bidderId) {
        return ResponseEntity.ok(verificationService.verifyBidder(bidderId));
    }

    /** Latest known verification state, without re-running checks. */
    @GetMapping
    public ResponseEntity<BidderVerificationSummaryResponse> getLatest(@PathVariable Long bidderId) {
        return ResponseEntity.ok(verificationService.getLatestSummary(bidderId));
    }
}
