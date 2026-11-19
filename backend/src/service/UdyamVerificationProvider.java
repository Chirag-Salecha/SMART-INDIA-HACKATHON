package com.sih.bidcompliance.service;

import com.sih.bidcompliance.dto.GovernmentLookupResult;
import com.sih.bidcompliance.entity.GovernmentUdyamRecord;
import com.sih.bidcompliance.entity.GovernmentVerificationType;
import com.sih.bidcompliance.repository.GovernmentUdyamRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SIMULATED Udyam/MSME verification source, backed entirely by our own
 * `government_udyam_records` table. No real Udyam portal is contacted.
 */
@Service
@RequiredArgsConstructor
public class UdyamVerificationProvider implements GovernmentVerificationProvider {

    private static final String SOURCE_NAME = "SIMULATED_UDYAM";

    private final GovernmentUdyamRecordRepository repository;

    @Override
    public GovernmentVerificationType getType() {
        return GovernmentVerificationType.UDYAM;
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    @Override
    public GovernmentLookupResult verify(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return notFound(identifier, "No Udyam number was provided to verify.");
        }

        return repository.findByUdyamNumberIgnoreCase(identifier.trim())
                .map(this::toResult)
                .orElseGet(() -> notFound(identifier, "Udyam number not found in simulated verification database"));
    }

    private GovernmentLookupResult toResult(GovernmentUdyamRecord record) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("enterpriseType", record.getEnterpriseType());
        attributes.put("registrationDate", record.getRegistrationDate());
        attributes.put("registeredAddress", record.getRegisteredAddress());

        return new GovernmentLookupResult(
                true,
                record.getUdyamNumber(),
                record.getEnterpriseName(),
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
