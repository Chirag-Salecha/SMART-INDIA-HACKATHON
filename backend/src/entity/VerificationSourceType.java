package com.sih.bidcompliance.entity;

/**
 * How a verification result was obtained. Distinct from the *source name*
 * (e.g. "SIMULATED_GSTN") already stored on BidderGovernmentVerification -
 * this classifies the verification *method* itself, so the UI/API can
 * uniformly disclose provenance regardless of which specific provider ran.
 */
public enum VerificationSourceType {
    OFFICIAL_API,
    REFERENCE_DATABASE,
    RULE_BASED,
    DOCUMENT_ONLY,
    MANUAL_REVIEW
}
