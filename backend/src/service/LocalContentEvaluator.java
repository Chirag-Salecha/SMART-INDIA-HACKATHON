package com.sih.bidcompliance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sih.bidcompliance.dto.LocalContentCheckResult;
import com.sih.bidcompliance.entity.Bidder;
import com.sih.bidcompliance.entity.Document;
import com.sih.bidcompliance.entity.DocumentType;
import com.sih.bidcompliance.entity.VerificationResult;
import com.sih.bidcompliance.repository.DocumentRepository;
import com.sih.bidcompliance.repository.VerificationResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Local Content / Make in India is deliberately implemented as a RULE, not
 * a lookup: it reads the local-content percentage or calculates it from
 * (localContentValue / totalValue) * 100 that the AI service already
 * extracted from the bidder's uploaded MAKE_IN_INDIA document, and compares
 * it against the tender-specific threshold (Tender.localContentThresholdPercent).
 */
@Service
@RequiredArgsConstructor
public class LocalContentEvaluator {

    public static final int DEFAULT_THRESHOLD_PERCENT = 50;

    private static final Pattern PERCENTAGE_PATTERN = Pattern.compile("(\\d{1,3}(?:\\.\\d+)?)");
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");

    private final DocumentRepository documentRepository;
    private final VerificationResultRepository verificationResultRepository;
    private final ObjectMapper objectMapper;

    public LocalContentCheckResult evaluate(Bidder bidder, Integer tenderThreshold) {
        int threshold = tenderThreshold != null ? tenderThreshold : DEFAULT_THRESHOLD_PERCENT;

        Optional<Document> document = documentRepository
                .findFirstByBidderIdAndDocumentTypeOrderByUploadedAtDesc(bidder.getId(), DocumentType.MAKE_IN_INDIA);

        if (document.isEmpty()) {
            List<Document> allDocs = documentRepository.findByBidderId(bidder.getId());
            document = allDocs.stream()
                    .filter(d -> d.getDocumentType() == DocumentType.MAKE_IN_INDIA
                            || (d.getOriginalFileName() != null && d.getOriginalFileName().toLowerCase().contains("local_content"))
                            || (d.getOriginalFileName() != null && d.getOriginalFileName().toLowerCase().contains("make_in_india")))
                    .findFirst();
        }

        if (document.isEmpty()) {
            return new LocalContentCheckResult(false, null, threshold, null,
                    "No Make in India / Local Content document has been uploaded.");
        }

        Optional<VerificationResult> result = verificationResultRepository.findByDocumentId(document.get().getId());
        if (result.isEmpty() || result.get().getExtractedDataJson() == null) {
            if (document.get().getVerificationStatus() == com.sih.bidcompliance.entity.VerificationStatus.VERIFIED) {
                return new LocalContentCheckResult(true, threshold, threshold, true, null);
            }
            return new LocalContentCheckResult(true, null, threshold, null,
                    "Local content document uploaded but has not yet been analyzed.");
        }

        Integer actual = extractPercentage(result.get().getExtractedDataJson());
        if (actual == null) {
            if (document.get().getVerificationStatus() == com.sih.bidcompliance.entity.VerificationStatus.VERIFIED
                    || result.get().getStatus() == com.sih.bidcompliance.entity.VerificationStatus.VERIFIED) {
                return new LocalContentCheckResult(true, threshold, threshold, true, null);
            }
            return new LocalContentCheckResult(true, null, threshold, null,
                    "Could not determine a local content percentage from the uploaded document.");
        }

        boolean passed = actual >= threshold;
        return new LocalContentCheckResult(true, actual, threshold, passed, null);
    }

    @SuppressWarnings("unchecked")
    private Integer extractPercentage(String extractedDataJson) {
        try {
            Map<String, Object> data = objectMapper.readValue(extractedDataJson, Map.class);

            // 1. Direct percentage check across known field variants
            Object raw = getAnyField(data,
                    "localContentPercentage", "local_content_percentage",
                    "percentage", "localContent", "local_content");
            if (raw != null) {
                Matcher matcher = PERCENTAGE_PATTERN.matcher(raw.toString());
                if (matcher.find()) {
                    return (int) Math.round(Double.parseDouble(matcher.group(1)));
                }
            }

            // 2. Value ratio check: localContentPercentage = (localContentValue / totalValue) * 100
            Object localValObj = getAnyField(data,
                    "localContentValue", "local_content_value",
                    "localValue", "local_value", "localAmount", "local_amount");
            Object totalValObj = getAnyField(data,
                    "totalValue", "total_value",
                    "contractValue", "contract_value", "totalAmount", "total_amount");
            if (localValObj != null && totalValObj != null) {
                Double localVal = parseNumericValue(localValObj.toString());
                Double totalVal = parseNumericValue(totalValObj.toString());
                if (localVal != null && totalVal != null && totalVal > 0) {
                    double calculated = (localVal / totalVal) * 100.0;
                    return (int) Math.round(calculated);
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private Object getAnyField(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            if (data.containsKey(key) && data.get(key) != null) {
                return data.get(key);
            }
        }
        return null;
    }

    private Double parseNumericValue(String text) {
        if (text == null) return null;
        String clean = text.replace(",", "").replace("₹", "").replace("INR", "")
                .replace("Rs.", "").replace("Rs", "").replace("$", "").trim();
        Matcher matcher = NUMERIC_PATTERN.matcher(clean);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
