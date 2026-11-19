package com.sih.bidcompliance.service;

import com.sih.bidcompliance.dto.GovernmentLookupResult;
import com.sih.bidcompliance.entity.GovernmentVerificationType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** Phase 2 placeholder - see EpfoProvider for the rationale. */
@Service
public class NsicProvider implements GovernmentVerificationProvider {
    @Override
    public GovernmentVerificationType getType() {
        return GovernmentVerificationType.NSIC;
    }

    @Override
    public String getSourceName() {
        return "PLACEHOLDER_NSIC";
    }

    @Override
    public GovernmentLookupResult verify(String identifier) {
        return new GovernmentLookupResult(false, identifier, null, null, java.util.Map.of(),
                getSourceName(), LocalDateTime.now(),
                "Official NSIC integration requires authorized credentials/API access - not available in this prototype.");
    }
}
