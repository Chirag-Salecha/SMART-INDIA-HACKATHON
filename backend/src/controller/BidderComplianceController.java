package com.sih.bidcompliance.controller;

import com.sih.bidcompliance.dto.*;
import com.sih.bidcompliance.service.AuditService;
import com.sih.bidcompliance.service.OfficerReviewService;
import com.sih.bidcompliance.service.RuleEngineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * New consolidated Rule Engine endpoints for a bidder: compliance
 * score/risk/reasons, audit trail, and Procurement Officer review.
 *
 * Distinct from the older /api/compliance/bidders/{id} (AI-document-based
 * compliance report from an earlier phase, still used by the Compliance
 * Reports page) - this is the newer engine combining GST/PAN/Udyam/Local
 * Content/Debarment/cross-document/missing-documents into one score.
 */
@RestController
@RequestMapping("/api/bidders/{bidderId}")
@RequiredArgsConstructor
public class BidderComplianceController {

    private final RuleEngineService ruleEngineService;
    private final AuditService auditService;
    private final OfficerReviewService officerReviewService;

    @GetMapping("/compliance")
    public ResponseEntity<ComplianceAssessmentResponse> getCompliance(@PathVariable Long bidderId) {
        return ResponseEntity.ok(ruleEngineService.assess(bidderId));
    }

    @GetMapping("/risk")
    public ResponseEntity<RiskAssessmentResponse> getRisk(@PathVariable Long bidderId) {
        ComplianceAssessmentResponse assessment = ruleEngineService.assess(bidderId);
        return ResponseEntity.ok(new RiskAssessmentResponse(bidderId, assessment.getRiskLevel(), assessment.getRiskReasons()));
    }

    @GetMapping("/audit")
    public ResponseEntity<List<AuditLogResponse>> getAudit(@PathVariable Long bidderId) {
        return ResponseEntity.ok(auditService.getBidderHistory(bidderId));
    }

    @PostMapping("/review")
    public ResponseEntity<OfficerReviewResponse> submitReview(
            @PathVariable Long bidderId, @Valid @RequestBody OfficerReviewRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(officerReviewService.recordDecision(bidderId, request));
    }

    @GetMapping("/review")
    public ResponseEntity<List<OfficerReviewResponse>> getReviewHistory(@PathVariable Long bidderId) {
        return ResponseEntity.ok(officerReviewService.getHistory(bidderId));
    }
}
