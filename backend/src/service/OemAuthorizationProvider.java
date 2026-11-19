package com.sih.bidcompliance.service;

import com.sih.bidcompliance.dto.GovernmentLookupResult;
import com.sih.bidcompliance.entity.GovernmentVerificationType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Phase 2 placeholder. OEM authorization has no single official API to
 * check against (each OEM issues its own letters) - for now this is
 * DOCUMENT_ONLY verification (the AI service already extracts OEM name /
 * authorized bidder / validity from the uploaded letter; see
 * app/extraction/field_extractor.py::_extract_oem_authorization). This
 * provider is a placeholder for a future OEM-portal or registry API.
 */
@Service
public class OemAuthorizationProvider implements GovernmentVerificationProvider {
    @Override
    public GovernmentVerificationType getType() {
        return GovernmentVerificationType.OEM_AUTHORIZATION;
    }

    @Override
    public String getSourceName() {
        return "PLACEHOLDER_OEM_REGISTRY";
    }

    @Override
    public GovernmentLookupResult verify(String identifier) {
        return new GovernmentLookupResult(false, identifier, null, null, java.util.Map.of(),
                getSourceName(), LocalDateTime.now(),
                "No official OEM authorization registry is integrated - relies on document-only AI extraction and manual review.");
    }
}
