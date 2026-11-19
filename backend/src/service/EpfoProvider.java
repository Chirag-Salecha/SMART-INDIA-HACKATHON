package com.sih.bidcompliance.service;

import com.sih.bidcompliance.dto.GovernmentLookupResult;
import com.sih.bidcompliance.entity.GovernmentVerificationType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Phase 2 placeholder. EPFO has no official free API and no synthetic
 * reference data yet - this deliberately returns "unverifiable" rather
 * than fabricating a successful official verification. Replace the body
 * of verify() with a real adapter once EPFO API access is authorized.
 */
@Service
public class EpfoProvider implements GovernmentVerificationProvider {
    @Override
    public GovernmentVerificationType getType() {
        return GovernmentVerificationType.EPFO;
    }

    @Override
    public String getSourceName() {
        return "PLACEHOLDER_EPFO";
    }

    @Override
    public GovernmentLookupResult verify(String identifier) {
        return new GovernmentLookupResult(false, identifier, null, null, java.util.Map.of(),
                getSourceName(), LocalDateTime.now(),
                "Official EPFO integration requires authorized credentials/API access - not available in this prototype.");
    }
}
