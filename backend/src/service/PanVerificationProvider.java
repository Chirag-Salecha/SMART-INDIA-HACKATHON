package com.sih.bidcompliance.service;

import com.sih.bidcompliance.dto.GovernmentLookupResult;
import com.sih.bidcompliance.entity.GovernmentPanRecord;
import com.sih.bidcompliance.entity.GovernmentVerificationType;
import com.sih.bidcompliance.repository.GovernmentPanRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SIMULATED Income Tax / PAN verification source, backed entirely by our
 * own `government_pan_records` table. No real Income Tax system is contacted.
 */
@Service
@RequiredArgsConstructor
public class PanVerificationProvider implements GovernmentVerificationProvider {

    private static final String SOURCE_NAME = "SIMULATED_PAN";

    private final GovernmentPanRecordRepository repository;

    @Override
    public GovernmentVerificationType getType() {
        return GovernmentVerificationType.PAN;
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    @Override
    public GovernmentLookupResult verify(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return notFound(identifier, "No PAN was provided to verify.");
        }

        return repository.findByPanIgnoreCase(identifier.trim())
                .map(this::toResult)
                .orElseGet(() -> notFound(identifier, "PAN not found in simulated verification database"));
    }

    private GovernmentLookupResult toResult(GovernmentPanRecord record) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("incomeTaxComplianceStatus", record.getIncomeTaxComplianceStatus());
        attributes.put("assessmentYear", record.getAssessmentYear());

        return new GovernmentLookupResult(
                true,
                record.getPan(),
                record.getLegalName(),
                record.getStatus(),
                attributes,
                SOURCE_NAME,
                LocalDateTime.now(),
                null
        );
    }

    private GovernmentLookupResult notFound(String identifier, String message) {
        return new GovernmentLookupResult(false, identifier, null, null, Map.of(), SOURCE_NAME, LocalDateTime.now(), message);
    }
}
