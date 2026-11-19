package com.sih.bidcompliance.service;

import com.sih.bidcompliance.dto.GovernmentLookupResult;
import com.sih.bidcompliance.entity.GovernmentGstRecord;
import com.sih.bidcompliance.entity.GovernmentVerificationType;
import com.sih.bidcompliance.repository.GovernmentGstRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SIMULATED GSTN verification source, backed entirely by our own
 * `government_gst_records` table. No real GST portal is contacted.
 */
@Service
@RequiredArgsConstructor
public class GstVerificationProvider implements GovernmentVerificationProvider {

    private static final String SOURCE_NAME = "SIMULATED_GSTN";

    private final GovernmentGstRecordRepository repository;

    @Override
    public GovernmentVerificationType getType() {
        return GovernmentVerificationType.GST;
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    @Override
    public GovernmentLookupResult verify(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return notFound(identifier, "No GSTIN was provided to verify.");
        }

        return repository.findByGstinIgnoreCase(identifier.trim())
                .map(this::toResult)
                .orElseGet(() -> notFound(identifier, "GSTIN not found in simulated verification database"));
    }

    private GovernmentLookupResult toResult(GovernmentGstRecord record) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("returnFilingStatus", record.getReturnFilingStatus());
        attributes.put("registrationDate", record.getRegistrationDate());
        attributes.put("registeredAddress", record.getRegisteredAddress());

        return new GovernmentLookupResult(
                true,
                record.getGstin(),
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
