package com.sih.bidcompliance.controller;

import com.sih.bidcompliance.dto.GstVerificationResponse;
import com.sih.bidcompliance.dto.PanVerificationResponse;
import com.sih.bidcompliance.dto.UdyamVerificationResponse;
import com.sih.bidcompliance.service.GstVerificationProvider;
import com.sih.bidcompliance.service.IdentifierFormatValidator;
import com.sih.bidcompliance.service.PanVerificationProvider;
import com.sih.bidcompliance.service.UdyamVerificationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Direct lookups against the SIMULATED government verification sources.
 * These never touch a real government system - see the *VerificationProvider
 * classes and government_gst_records / government_pan_records /
 * government_udyam_records tables (seeded by GovernmentDataSeeder).
 */
@RestController
@RequestMapping("/api/government")
@RequiredArgsConstructor
public class GovernmentVerificationController {

    private final GstVerificationProvider gstVerificationProvider;
    private final PanVerificationProvider panVerificationProvider;
    private final UdyamVerificationProvider udyamVerificationProvider;
    private final IdentifierFormatValidator formatValidator;

    @GetMapping("/gst/{gstin}")
    public ResponseEntity<GstVerificationResponse> verifyGst(@PathVariable String gstin) {
        if (!formatValidator.isValidGstin(gstin)) {
            throw new IllegalArgumentException("Invalid GSTIN format: " + gstin);
        }
        return ResponseEntity.ok(GstVerificationResponse.from(gstVerificationProvider.verify(gstin)));
    }

    @GetMapping("/pan/{pan}")
    public ResponseEntity<PanVerificationResponse> verifyPan(@PathVariable String pan) {
        if (!formatValidator.isValidPan(pan)) {
            throw new IllegalArgumentException("Invalid PAN format: " + pan);
        }
        return ResponseEntity.ok(PanVerificationResponse.from(panVerificationProvider.verify(pan)));
    }

    @GetMapping("/udyam/{udyamNumber}")
    public ResponseEntity<UdyamVerificationResponse> verifyUdyam(@PathVariable String udyamNumber) {
        if (!formatValidator.isValidUdyam(udyamNumber)) {
            throw new IllegalArgumentException("Invalid Udyam number format: " + udyamNumber);
        }
        return ResponseEntity.ok(UdyamVerificationResponse.from(udyamVerificationProvider.verify(udyamNumber)));
    }
}
