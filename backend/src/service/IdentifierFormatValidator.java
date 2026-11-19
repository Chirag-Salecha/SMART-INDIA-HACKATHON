package com.sih.bidcompliance.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Basic structural validation for government identifiers, so malformed
 * input is rejected with a clear 400 before we even attempt a lookup.
 * Mirrors the patterns used by the AI service's field extractor.
 */
@Component
public class IdentifierFormatValidator {

    private static final Pattern GSTIN_PATTERN =
            Pattern.compile("\\d{2}[A-Z]{5}\\d{4}[A-Z]\\d[Z][A-Z0-9]");
    private static final Pattern PAN_PATTERN =
            Pattern.compile("[A-Z]{5}\\d{4}[A-Z]");
    private static final Pattern UDYAM_PATTERN =
            Pattern.compile("UDYAM-[A-Z]{2}-\\d{2}-\\d{7}");

    public boolean isValidGstin(String value) {
        return value != null && GSTIN_PATTERN.matcher(value.trim().toUpperCase()).matches();
    }

    public boolean isValidPan(String value) {
        return value != null && PAN_PATTERN.matcher(value.trim().toUpperCase()).matches();
    }

    public boolean isValidUdyam(String value) {
        return value != null && UDYAM_PATTERN.matcher(value.trim().toUpperCase()).matches();
    }
}
