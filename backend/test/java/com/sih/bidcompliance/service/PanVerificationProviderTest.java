package com.sih.bidcompliance.service;

import com.sih.bidcompliance.dto.GovernmentLookupResult;
import com.sih.bidcompliance.entity.GovernmentPanRecord;
import com.sih.bidcompliance.repository.GovernmentPanRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PanVerificationProviderTest {

    @Mock
    private GovernmentPanRecordRepository repository;

    @InjectMocks
    private PanVerificationProvider provider;

    @Test
    void returnsFoundResultForValidPan() {
        GovernmentPanRecord record = new GovernmentPanRecord();
        record.setPan("ABCDE1234F");
        record.setLegalName("ABC Technologies Pvt Ltd");
        record.setStatus("VALID");
        record.setIncomeTaxComplianceStatus("COMPLIANT");

        when(repository.findByPanIgnoreCase("ABCDE1234F")).thenReturn(Optional.of(record));

        GovernmentLookupResult result = provider.verify("ABCDE1234F");

        assertThat(result.isFound()).isTrue();
        assertThat(result.getName()).isEqualTo("ABC Technologies Pvt Ltd");
        assertThat(result.getAttributes()).containsEntry("incomeTaxComplianceStatus", "COMPLIANT");
    }

    @Test
    void returnsFoundButInvalidStatusForInvalidPan() {
        GovernmentPanRecord record = new GovernmentPanRecord();
        record.setPan("XXXXX0000X");
        record.setLegalName("Invalid Traders");
        record.setStatus("INVALID");
        record.setIncomeTaxComplianceStatus("NON_COMPLIANT");

        when(repository.findByPanIgnoreCase("XXXXX0000X")).thenReturn(Optional.of(record));

        GovernmentLookupResult result = provider.verify("XXXXX0000X");

        assertThat(result.isFound()).isTrue();
        assertThat(result.getStatus()).isEqualTo("INVALID");
    }

    @Test
    void returnsNotFoundForUnknownPan() {
        when(repository.findByPanIgnoreCase("ZZZZZ0000Z")).thenReturn(Optional.empty());

        GovernmentLookupResult result = provider.verify("ZZZZZ0000Z");

        assertThat(result.isFound()).isFalse();
        assertThat(result.getSource()).isEqualTo("SIMULATED_PAN");
    }
}
