package com.sih.bidcompliance.service;

import com.sih.bidcompliance.dto.GovernmentLookupResult;
import com.sih.bidcompliance.entity.GovernmentUdyamRecord;
import com.sih.bidcompliance.repository.GovernmentUdyamRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UdyamVerificationProviderTest {

    @Mock
    private GovernmentUdyamRecordRepository repository;

    @InjectMocks
    private UdyamVerificationProvider provider;

    @Test
    void returnsFoundResultForActiveUdyam() {
        GovernmentUdyamRecord record = new GovernmentUdyamRecord();
        record.setUdyamNumber("UDYAM-KA-01-1234567");
        record.setEnterpriseName("ABC Technologies Pvt Ltd");
        record.setStatus("ACTIVE");
        record.setEnterpriseType("SMALL");

        when(repository.findByUdyamNumberIgnoreCase("UDYAM-KA-01-1234567")).thenReturn(Optional.of(record));

        GovernmentLookupResult result = provider.verify("UDYAM-KA-01-1234567");

        assertThat(result.isFound()).isTrue();
        assertThat(result.getName()).isEqualTo("ABC Technologies Pvt Ltd");
        assertThat(result.getAttributes()).containsEntry("enterpriseType", "SMALL");
    }

    @Test
    void returnsNotFoundForUnknownUdyamNumber() {
        when(repository.findByUdyamNumberIgnoreCase("UDYAM-XX-99-0000000")).thenReturn(Optional.empty());

        GovernmentLookupResult result = provider.verify("UDYAM-XX-99-0000000");

        assertThat(result.isFound()).isFalse();
        assertThat(result.getSource()).isEqualTo("SIMULATED_UDYAM");
    }
}
