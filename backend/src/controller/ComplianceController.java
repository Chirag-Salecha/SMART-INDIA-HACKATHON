package com.sih.bidcompliance.controller;

import com.sih.bidcompliance.dto.ComplianceReportResponse;
import com.sih.bidcompliance.service.ComplianceEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/compliance")
@RequiredArgsConstructor
public class ComplianceController {

    private final ComplianceEngineService complianceEngineService;

    @GetMapping("/bidders/{bidderId}")
    public ResponseEntity<ComplianceReportResponse> getForBidder(@PathVariable Long bidderId) {
        return ResponseEntity.ok(complianceEngineService.evaluateBidder(bidderId));
    }

    @GetMapping("/tenders/{tenderId}")
    public ResponseEntity<List<ComplianceReportResponse>> getForTender(@PathVariable Long tenderId) {
        return ResponseEntity.ok(complianceEngineService.evaluateTender(tenderId));
    }
}
