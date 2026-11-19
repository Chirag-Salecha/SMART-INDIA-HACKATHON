package com.sih.bidcompliance.service;

import com.sih.bidcompliance.dto.GovernmentLookupResult;
import com.sih.bidcompliance.entity.GovernmentVerificationType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** Phase 2 placeholder (Startup India / DPIIT recognition) - see EpfoProvider for rationale. */
@Service
public class DpiitProvider implements GovernmentVerificationProvider {
    @Override
    public GovernmentVerificationType getType() {
        return GovernmentVerificationType.STARTUP_INDIA;
    }

    @Override
    public String getSourceName() {
        return "PLACEHOLDER_DPIIT";
    }

    @Override
    public GovernmentLookupResult verify(String identifier) {
        return new GovernmentLookupResult(false, identifier, null, null, java.util.Map.of(),
                getSourceName(), LocalDateTime.now(),
                "Official DPIIT/Startup India integration requires authorized credentials/API access - not available in this prototype.");
    }
}
