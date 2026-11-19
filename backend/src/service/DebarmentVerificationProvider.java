package com.sih.bidcompliance.service;

import com.sih.bidcompliance.dto.DebarmentCheckResult;
import com.sih.bidcompliance.entity.GovernmentDebarmentRecord;
import com.sih.bidcompliance.repository.GovernmentDebarmentRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * SIMULATED blacklisting/debarment check, backed entirely by our own
 * `government_debarment_records` table - never a real government debarment
 * registry.
 *
 * Matching priority: PAN (strongest) > GSTIN > exact legal name > fuzzy
 * name match. A match NEVER auto-disqualifies a bidder - it is surfaced as
 * "Potential Debarment Match" for Procurement Officer review only.
 */
@Service
@RequiredArgsConstructor
public class DebarmentVerificationProvider {

    private static final int NAME_FUZZY_THRESHOLD = 85;

    private final GovernmentDebarmentRecordRepository repository;
    private final FuzzyNameMatcher fuzzyNameMatcher;

    public DebarmentCheckResult check(String pan, String gstin, String legalName) {
        if (pan != null && !pan.isBlank()) {
            Optional<GovernmentDebarmentRecord> byPan = repository.findByPanIgnoreCase(pan.trim());
            if (byPan.isPresent()) {
                return toResult(byPan.get(), "PAN", pan, 100);
            }
        }

        if (gstin != null && !gstin.isBlank()) {
            Optional<GovernmentDebarmentRecord> byGstin = repository.findByGstinIgnoreCase(gstin.trim());
            if (byGstin.isPresent()) {
                return toResult(byGstin.get(), "GSTIN", gstin, 100);
            }
        }

        if (legalName != null && !legalName.isBlank()) {
            List<GovernmentDebarmentRecord> all = repository.findAll();
            for (GovernmentDebarmentRecord record : all) {
                int similarity = fuzzyNameMatcher.similarity(legalName, record.getEntityName());
                if (similarity >= NAME_FUZZY_THRESHOLD) {
                    return toResult(record, "NAME_FUZZY", legalName, similarity);
                }
            }
        }

        return clear();
    }

    private DebarmentCheckResult toResult(GovernmentDebarmentRecord record, String matchedOn, String matchedValue, int similarity) {
        String period = (record.getDebarmentDate() != null ? record.getDebarmentDate() : "unknown")
                + " to " + (record.getExpiryDate() != null ? record.getExpiryDate() : "indefinite");

        return new DebarmentCheckResult(
                true,
                matchedOn,
                matchedValue,
                record.getEntityName(),
                record.getAuthority(),
                record.getReason(),
                period,
                record.getStatus(),
                similarity
        );
    }

    private DebarmentCheckResult clear() {
        return new DebarmentCheckResult(false, "NONE", null, null, null, null, null, "CLEAR", 0);
    }
}
