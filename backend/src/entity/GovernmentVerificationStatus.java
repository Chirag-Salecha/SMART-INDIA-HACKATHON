package com.sih.bidcompliance.entity;

public enum GovernmentVerificationStatus {
    VERIFIED,
    MISMATCH,
    NOT_FOUND,
    NON_COMPLIANT,
    REQUIRES_REVIEW,
    /** Used by Phase 2 placeholder providers that have no official/synthetic data source yet. */
    UNVERIFIABLE
}
