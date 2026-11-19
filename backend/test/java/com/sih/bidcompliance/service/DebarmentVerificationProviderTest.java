package com.sih.bidcompliance.service;

import com.sih.bidcompliance.dto.DebarmentCheckResult;
import com.sih.bidcompliance.entity.GovernmentDebarmentRecord;
import com.sih.bidcompliance.repository.GovernmentDebarmentRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DebarmentVerificationProviderTest {

    @Mock private GovernmentDebarmentRecordRepository repository;

    private DebarmentVerificationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DebarmentVerificationProvider(repository, new FuzzyNameMatcher());
    }

    private GovernmentDebarmentRecord record(String pan, String gstin, String name) {
        GovernmentDebarmentRecord record = new GovernmentDebarmentRecord();
        record.setPan(pan);
        record.setGstin(gstin);
        record.setEntityName(name);
        record.setAuthority("Ministry of Petroleum & Natural Gas");
        record.setReason("Fraudulent documentation");
        record.setDebarmentDate(LocalDate.of(2024, 1, 1));
        record.setExpiryDate(LocalDate.of(2027, 1, 1));
        record.setStatus("ACTIVE");
        return record;
    }

    @Test
    void exactPanMatchIsHighestPriority() {
        when(repository.findByPanIgnoreCase("BADPN0000B")).thenReturn(Optional.of(record("BADPN0000B", "00XXXXX0000X1Z1", "Bad Actor Pvt Ltd")));

        DebarmentCheckResult result = provider.check("BADPN0000B", "29ABCDE1234F1Z5", "Some Other Name");

        assertThat(result.isMatchFound()).isTrue();
        assertThat(result.getMatchedOn()).isEqualTo("PAN");
    }

    @Test
    void gstinMatchIsUsedWhenPanDoesNotMatch() {
        lenient().when(repository.findByPanIgnoreCase("CLEANPAN01C")).thenReturn(Optional.empty());
        when(repository.findByGstinIgnoreCase("00XXXXX0000X1Z1")).thenReturn(Optional.of(record(null, "00XXXXX0000X1Z1", "Bad Actor Pvt Ltd")));

        DebarmentCheckResult result = provider.check("CLEANPAN01C", "00XXXXX0000X1Z1", "Some Company");

        assertThat(result.isMatchFound()).isTrue();
        assertThat(result.getMatchedOn()).isEqualTo("GSTIN");
    }

    @Test
    void nameOnlyFuzzyMatchIsFlaggedForManualReview() {
        lenient().when(repository.findByPanIgnoreCase("CLEANPAN01C")).thenReturn(Optional.empty());
        lenient().when(repository.findByGstinIgnoreCase("29ABCDE1234F1Z5")).thenReturn(Optional.empty());
        when(repository.findAll()).thenReturn(List.of(record(null, null, "Bad Actor Private Limited")));

        DebarmentCheckResult result = provider.check("CLEANPAN01C", "29ABCDE1234F1Z5", "Bad Actor Pvt Ltd");

        assertThat(result.isMatchFound()).isTrue();
        assertThat(result.getMatchedOn()).isEqualTo("NAME_FUZZY");
    }

    @Test
    void noMatchReturnsClear() {
        lenient().when(repository.findByPanIgnoreCase("CLEANPAN01C")).thenReturn(Optional.empty());
        lenient().when(repository.findByGstinIgnoreCase("29ABCDE1234F1Z5")).thenReturn(Optional.empty());
        when(repository.findAll()).thenReturn(List.of(record(null, null, "Totally Different Corp")));

        DebarmentCheckResult result = provider.check("CLEANPAN01C", "29ABCDE1234F1Z5", "ABC Technologies Pvt Ltd");

        assertThat(result.isMatchFound()).isFalse();
        assertThat(result.getMatchedOn()).isEqualTo("NONE");
    }
}
