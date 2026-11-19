package com.sih.bidcompliance.service;

import com.sih.bidcompliance.entity.DocumentType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Determines which document types are mandatory for a tender by scanning
 * its free-text description for known keywords (e.g. "GST", "Udyam/MSME",
 * "Make in India"). If nothing recognizable is found, falls back to the
 * five baseline requirements called out in the platform's compliance
 * engine spec, so every tender still gets a meaningful compliance report.
 */
@Component
public class TenderRequirementParser {

    private static final List<DocumentType> DEFAULT_REQUIREMENTS = List.of(
            DocumentType.GST,
            DocumentType.PAN,
            DocumentType.UDYAM,
            DocumentType.OEM_AUTHORIZATION,
            DocumentType.MAKE_IN_INDIA
    );

    private record Keyword(Pattern pattern, DocumentType type) {}

    private static final List<Keyword> KEYWORDS = List.of(
            new Keyword(Pattern.compile("\\bGST\\b", Pattern.CASE_INSENSITIVE), DocumentType.GST),
            new Keyword(Pattern.compile("\\bPAN\\b", Pattern.CASE_INSENSITIVE), DocumentType.PAN),
            new Keyword(Pattern.compile("\\bUDYAM\\b|\\bMSME\\b", Pattern.CASE_INSENSITIVE), DocumentType.UDYAM),
            new Keyword(Pattern.compile("\\bOEM\\b|OEM\\s*Authorization|OEM\\s*Authorisation", Pattern.CASE_INSENSITIVE), DocumentType.OEM_AUTHORIZATION),
            new Keyword(Pattern.compile("Make\\s*in\\s*India", Pattern.CASE_INSENSITIVE), DocumentType.MAKE_IN_INDIA),
            new Keyword(Pattern.compile("Income\\s*Tax", Pattern.CASE_INSENSITIVE), DocumentType.INCOME_TAX),
            new Keyword(Pattern.compile("\\bEPFO\\b|Provident\\s*Fund", Pattern.CASE_INSENSITIVE), DocumentType.EPFO),
            new Keyword(Pattern.compile("\\bESIC\\b|Employee\\s*State\\s*Insurance", Pattern.CASE_INSENSITIVE), DocumentType.ESIC),
            new Keyword(Pattern.compile("Startup\\s*India", Pattern.CASE_INSENSITIVE), DocumentType.STARTUP_INDIA),
            new Keyword(Pattern.compile("\\bNSIC\\b", Pattern.CASE_INSENSITIVE), DocumentType.NSIC)
    );

    public List<DocumentType> parseRequiredTypes(String tenderDescription) {
        if (tenderDescription == null || tenderDescription.isBlank()) {
            return DEFAULT_REQUIREMENTS;
        }

        Set<DocumentType> found = new LinkedHashSet<>();
        for (Keyword keyword : KEYWORDS) {
            if (keyword.pattern().matcher(tenderDescription).find()) {
                found.add(keyword.type());
            }
        }

        return found.isEmpty() ? DEFAULT_REQUIREMENTS : List.copyOf(found);
    }
}
