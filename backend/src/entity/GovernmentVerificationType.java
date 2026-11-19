package com.sih.bidcompliance.entity;

/**
 * A government/institutional verification source. Deliberately distinct
 * from DocumentType - this describes an *identifier lookup source*, not an
 * uploaded document category, so it can grow to cover sources that never
 * correspond to a single uploaded document (e.g. blacklisting checks).
 *
 * Phase 1 implements GST, PAN and UDYAM. The rest are placeholders for
 * future phases so the enum (and switch statements over it) won't need to
 * be restructured later - only extended.
 */
public enum GovernmentVerificationType {
    GST,
    PAN,
    UDYAM,
    EPFO,
    ESIC,
    STARTUP_INDIA,
    NSIC,
    MCA21,
    OEM_AUTHORIZATION,
    MAKE_IN_INDIA,
    LOCAL_CONTENT,
    BLACKLISTING,
    DEBARMENT,
    DIGILOCKER,
    /** Not a single source - the result of comparing names across sources. */
    CROSS_SOURCE
}
