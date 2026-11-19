package com.sih.bidcompliance.service;

import com.sih.bidcompliance.dto.GovernmentLookupResult;
import com.sih.bidcompliance.entity.GovernmentVerificationType;

/**
 * Abstraction for a single government/institutional verification source.
 *
 * IMPORTANT: every implementation in this codebase is a SIMULATED/MOCK
 * data source backed by our own PostgreSQL tables - none of them connect
 * to a real government system. This interface exists so a real adapter
 * (calling an actual GSTN/Udyam/etc API) can be swapped in later without
 * changing any calling code - see GstVerificationProvider,
 * PanVerificationProvider, UdyamVerificationProvider for the current
 * (simulated) implementations.
 */
public interface GovernmentVerificationProvider {

    GovernmentVerificationType getType();

    /** Machine-readable source label, e.g. "SIMULATED_GSTN". */
    String getSourceName();

    /**
     * Looks up the given identifier. Never throws for a "not found" case -
     * that's represented by GovernmentLookupResult.isFound() == false.
     */
    GovernmentLookupResult verify(String identifier);
}
