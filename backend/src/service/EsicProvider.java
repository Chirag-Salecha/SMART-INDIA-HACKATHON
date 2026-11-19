package com.sih.bidcompliance.service;

import com.sih.bidcompliance.dto.GovernmentLookupResult;
import com.sih.bidcompliance.entity.GovernmentVerificationType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** Phase 2 placeholder - see EpfoProvider for the rationale. */
@Service
public class EsicProvider implements GovernmentVerificationProvider {
    @Override
    public GovernmentVerificationType getType() {
        return GovernmentVerificationType.ESIC;
    }

    @Override
    public String getSourceName() {
        return "PLACEHOLDER_ESIC";
    }

    @Override
    public GovernmentLookupResult verify(String identifier) {
        return new GovernmentLookupResult(false, identifier, null, null, java.util.Map.of(),
                getSourceName(), LocalDateTime.now(),
                "Official ESIC integration requires authorized credentials/API access - not available in this prototype.");
    }
}
