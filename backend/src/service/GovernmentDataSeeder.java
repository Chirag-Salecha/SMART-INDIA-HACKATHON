package com.sih.bidcompliance.service;

import com.sih.bidcompliance.entity.GovernmentDebarmentRecord;
import com.sih.bidcompliance.entity.GovernmentGstRecord;
import com.sih.bidcompliance.entity.GovernmentPanRecord;
import com.sih.bidcompliance.entity.GovernmentUdyamRecord;
import com.sih.bidcompliance.repository.GovernmentDebarmentRecordRepository;
import com.sih.bidcompliance.repository.GovernmentGstRecordRepository;
import com.sih.bidcompliance.repository.GovernmentPanRecordRepository;
import com.sih.bidcompliance.repository.GovernmentUdyamRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Seeds the SIMULATED government verification tables with demo data on
 * startup, idempotently (checked by identifier, so restarts never create
 * duplicates). None of this data represents real people or companies.
 *
 * Demo scenarios:
 *  - CASE A: fully valid, cross-source-consistent bidder ("ABC Technologies Pvt Ltd")
 *  - CASE B: invalid/non-compliant records (cancelled GST, invalid PAN, inactive Udyam)
 *  - CASE C: cross-source name inconsistency (GST/PAN say one name, Udyam says another)
 *  - CASE D: debarred bidder ("Risky Vendors Pvt Ltd") - PAN and GSTIN both on the
 *    simulated debarment list, for the "Debarment Match" demo scenario
 */
@Component
@RequiredArgsConstructor
public class GovernmentDataSeeder implements ApplicationRunner {

    private final GovernmentGstRecordRepository gstRepository;
    private final GovernmentPanRecordRepository panRepository;
    private final GovernmentUdyamRecordRepository udyamRepository;
    private final GovernmentDebarmentRecordRepository debarmentRepository;

    @Override
    public void run(ApplicationArguments args) {
        seedGst();
        seedPan();
        seedUdyam();
        seedDebarment();
    }

    private void seedGst() {
        // TechVision Solutions Pvt Ltd - Prototype demo reference record
        seedGstRecord("29AABCT1234F1ZX", "TechVision Solutions Pvt Ltd", "ACTIVE", "FILED",
                LocalDate.of(2021, 1, 15), "42 Electronics City, Bengaluru, Karnataka");
        seedGstRecord("29ABCDE1234F1Z5", "ABC Technologies Pvt Ltd", "ACTIVE", "FILED",
                LocalDate.of(2019, 4, 1), "12 MG Road, Bengaluru, Karnataka");
        seedGstRecord("27CANCL0000C1Z1", "Cancel Traders Pvt Ltd", "CANCELLED", "NOT_FILED",
                LocalDate.of(2017, 6, 15), "44 Market Street, Mumbai, Maharashtra");
        seedGstRecord("07INCON1234C1Z2", "ABC Technologies Pvt Ltd", "ACTIVE", "FILED",
                LocalDate.of(2020, 1, 10), "9 Connaught Place, New Delhi");
        // CASE D: debarred entity - see seedDebarment()
        seedGstRecord("24DEBARR123D1Z1", "Risky Vendors Pvt Ltd", "ACTIVE", "FILED",
                LocalDate.of(2015, 8, 1), "5 Ring Road, Ahmedabad, Gujarat");
    }

    private void seedPan() {
        // TechVision Solutions Pvt Ltd - Prototype demo reference record
        seedPanRecord("AABCT1234F", "TechVision Solutions Pvt Ltd", "ACTIVE", "COMPLIANT", "2024-25");
        seedPanRecord("ABCDE1234F", "ABC Technologies Pvt Ltd", "VALID", "COMPLIANT", "2024-25");
        seedPanRecord("XXXXX0000X", "Invalid Traders", "INVALID", "NON_COMPLIANT", "2023-24");
        seedPanRecord("INCON1234C", "ABC Technologies Pvt Ltd", "VALID", "COMPLIANT", "2024-25");
        seedPanRecord("DEBARR123D", "Risky Vendors Pvt Ltd", "VALID", "COMPLIANT", "2024-25");
    }

    private void seedUdyam() {
        // TechVision Solutions Pvt Ltd - Prototype demo reference record
        seedUdyamRecord("UDYAM-KA-01-0001234", "TechVision Solutions Pvt Ltd", "MEDIUM", "ACTIVE",
                LocalDate.of(2021, 2, 20), "42 Electronics City, Bengaluru, Karnataka");
        seedUdyamRecord("UDYAM-KA-01-1234567", "ABC Technologies Pvt Ltd", "SMALL", "ACTIVE",
                LocalDate.of(2019, 5, 20), "12 MG Road, Bengaluru, Karnataka");
        seedUdyamRecord("UDYAM-DL-02-7654321", "Inactive Enterprises", "MICRO", "INACTIVE",
                LocalDate.of(2018, 3, 3), "77 Karol Bagh, New Delhi");
        // CASE C: same underlying bidder as the GST/PAN "07INCON1234C1Z2" /
        // "INCON1234C" records above, but the Udyam enterprise name differs -
        // this is what triggers the cross-source consistency warning.
        seedUdyamRecord("UDYAM-MH-03-1112223", "ABC Technology Limited", "SMALL", "ACTIVE",
                LocalDate.of(2020, 2, 1), "9 Connaught Place, New Delhi");
    }

    private void seedDebarment() {
        seedDebarmentRecord("DEBARR123D", "24DEBARR123D1Z1", "Risky Vendors Pvt Ltd",
                "Ministry of Petroleum & Natural Gas", "Submission of fraudulent GST documentation in a prior tender",
                LocalDate.of(2024, 1, 1), LocalDate.of(2027, 1, 1), "ACTIVE");
    }

    private void seedGstRecord(String gstin, String legalName, String status, String filingStatus,
                                LocalDate registrationDate, String address) {
        if (gstRepository.existsByGstinIgnoreCase(gstin)) {
            return;
        }
        GovernmentGstRecord record = new GovernmentGstRecord();
        record.setGstin(gstin);
        record.setLegalName(legalName);
        record.setStatus(status);
        record.setReturnFilingStatus(filingStatus);
        record.setRegistrationDate(registrationDate);
        record.setRegisteredAddress(address);
        record.setLastVerifiedAt(LocalDateTime.now());
        gstRepository.save(record);
    }

    private void seedPanRecord(String pan, String legalName, String status, String complianceStatus, String assessmentYear) {
        if (panRepository.existsByPanIgnoreCase(pan)) {
            return;
        }
        GovernmentPanRecord record = new GovernmentPanRecord();
        record.setPan(pan);
        record.setLegalName(legalName);
        record.setStatus(status);
        record.setIncomeTaxComplianceStatus(complianceStatus);
        record.setAssessmentYear(assessmentYear);
        record.setLastVerifiedAt(LocalDateTime.now());
        panRepository.save(record);
    }

    private void seedUdyamRecord(String udyamNumber, String enterpriseName, String enterpriseType, String status,
                                  LocalDate registrationDate, String address) {
        if (udyamRepository.existsByUdyamNumberIgnoreCase(udyamNumber)) {
            return;
        }
        GovernmentUdyamRecord record = new GovernmentUdyamRecord();
        record.setUdyamNumber(udyamNumber);
        record.setEnterpriseName(enterpriseName);
        record.setEnterpriseType(enterpriseType);
        record.setStatus(status);
        record.setRegistrationDate(registrationDate);
        record.setRegisteredAddress(address);
        record.setLastVerifiedAt(LocalDateTime.now());
        udyamRepository.save(record);
    }

    private void seedDebarmentRecord(String pan, String gstin, String entityName, String authority, String reason,
                                      LocalDate debarmentDate, LocalDate expiryDate, String status) {
        if (debarmentRepository.findByPanIgnoreCase(pan).isPresent()) {
            return;
        }
        GovernmentDebarmentRecord record = new GovernmentDebarmentRecord();
        record.setPan(pan);
        record.setGstin(gstin);
        record.setEntityName(entityName);
        record.setAuthority(authority);
        record.setReason(reason);
        record.setDebarmentDate(debarmentDate);
        record.setExpiryDate(expiryDate);
        record.setStatus(status);
        debarmentRepository.save(record);
    }
}
