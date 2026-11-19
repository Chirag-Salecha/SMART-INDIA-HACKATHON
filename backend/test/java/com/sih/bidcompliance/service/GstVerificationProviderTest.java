package com.sih.bidcompliance.service;

import com.sih.bidcompliance.dto.GovernmentLookupResult;
import com.sih.bidcompliance.entity.GovernmentGstRecord;
import com.sih.bidcompliance.repository.GovernmentGstRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GstVerificationProviderTest {

    @Mock
    private GovernmentGstRecordRepository repository;

    @InjectMocks
    private GstVerificationProvider provider;

    @Test
    void returnsFoundResultForKnownActiveGstin() {
        GovernmentGstRecord record = new GovernmentGstRecord();
        record.setGstin("29ABCDE1234F1Z5");
        record.setLegalName("ABC Technologies Pvt Ltd");
        record.setStatus("ACTIVE");
        record.setReturnFilingStatus("FILED");

        when(repository.findByGstinIgnoreCase("29ABCDE1234F1Z5")).thenReturn(Optional.of(record));

        GovernmentLookupResult result = provider.verify("29ABCDE1234F1Z5");

        assertThat(result.isFound()).isTrue();
        assertThat(result.getName()).isEqualTo("ABC Technologies Pvt Ltd");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getAttributes()).containsEntry("returnFilingStatus", "FILED");
        assertThat(result.getSource()).isEqualTo("SIMULATED_GSTN");
    }

    @Test
    void returnsNotFoundForUnknownGstin() {
        when(repository.findByGstinIgnoreCase("00NOPE0000N1Z1")).thenReturn(Optional.empty());

        GovernmentLookupResult result = provider.verify("00NOPE0000N1Z1");

        assertThat(result.isFound()).isFalse();
        assertThat(result.getMessage()).contains("not found");
        assertThat(result.getSource()).isEqualTo("SIMULATED_GSTN");
    }

    @Test
    void returnsFoundButCancelledStatusForCancelledGstin() {
        GovernmentGstRecord record = new GovernmentGstRecord();
        record.setGstin("27CANCL0000C1Z1");
        record.setLegalName("Cancel Traders Pvt Ltd");
        record.setStatus("CANCELLED");
        record.setReturnFilingStatus("NOT_FILED");

        when(repository.findByGstinIgnoreCase("27CANCL0000C1Z1")).thenReturn(Optional.of(record));

        GovernmentLookupResult result = provider.verify("27CANCL0000C1Z1");

        // The provider itself just reports what it found - it's the
        // BidderGovernmentVerificationService's job to decide this means
        // NON_COMPLIANT. The provider must faithfully report status=CANCELLED.
        assertThat(result.isFound()).isTrue();
        assertThat(result.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void blankIdentifierIsTreatedAsNotFound() {
        GovernmentLookupResult result = provider.verify("  ");
        assertThat(result.isFound()).isFalse();
    }
}
