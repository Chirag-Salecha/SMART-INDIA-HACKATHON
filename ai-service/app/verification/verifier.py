"""
Compares AI-extracted document fields against the bidder profile supplied
in the request, and produces the structured verification result: checks,
issues, recommendations, overall status and a confidence score.

All logic here is deterministic (string/pattern comparisons) - there is no
randomness anywhere in this pipeline, by design.
"""

import re
from typing import Any, Dict, List, Optional

from app.schemas import CheckResult, GovernmentVerificationInfo
from app.verification.government_adapters import GovernmentVerificationService

_gov_service = GovernmentVerificationService()

# Which identifier field each document type is expected to carry.
IDENTITY_FIELD_MAP = {
    "GST": ("gstin", "GSTIN"),
    "PAN": ("pan", "PAN"),
    "UDYAM": ("udyamNumber", "Udyam Number"),
}


def _normalize(value: Optional[str]) -> str:
    return re.sub(r"\s+", "", (value or "")).upper()


def _name_matches(extracted_name: Optional[str], expected_name: Optional[str]) -> bool:
    if not extracted_name or not expected_name:
        return False
    extracted_tokens = set(re.findall(r"[A-Za-z]+", extracted_name.upper()))
    expected_tokens = set(re.findall(r"[A-Za-z]+", expected_name.upper()))
    if not expected_tokens:
        return False
    overlap = extracted_tokens & expected_tokens
    # Consider it a match if most of the expected company name's words appear.
    return len(overlap) >= max(1, int(len(expected_tokens) * 0.6))


def build_verification_result(
    document_type: str,
    extracted_data: Dict[str, Any],
    raw_text: str,
    extraction_method: str,
    company_name: Optional[str] = None,
    pan: Optional[str] = None,
    gstin: Optional[str] = None,
    udyam_number: Optional[str] = None,
) -> Dict[str, Any]:
    checks: List[CheckResult] = []
    issues: List[str] = []
    recommendations: List[str] = []

    # 1. Extraction quality check - if we couldn't read the document at all,
    # everything downstream is unreliable, so short-circuit to REQUIRES_REVIEW.
    if extraction_method == "OCR_UNAVAILABLE":
        issues.append(
            "OCR is not available on this server, so text could not be extracted "
            "from this scanned/image document. Manual review is required."
        )
        recommendations.append(
            "Install an OCR engine (Tesseract) on the AI service host, or manually "
            "review this document."
        )
        return _finalize(
            status="REQUIRES_REVIEW",
            confidence=0.2,
            document_type=document_type,
            extracted_data=extracted_data,
            checks=checks,
            issues=issues,
            recommendations=recommendations,
            government_info=GovernmentVerificationInfo(),
            raw_text=raw_text,
        )

    if extraction_method in ("UNSUPPORTED", "ERROR") or not raw_text.strip():
        issues.append("Could not extract readable text from this document.")
        recommendations.append("Re-upload a clearer copy of this document for automated verification.")
        return _finalize(
            status="REQUIRES_REVIEW",
            confidence=0.2,
            document_type=document_type,
            extracted_data=extracted_data,
            checks=checks,
            issues=issues,
            recommendations=recommendations,
            government_info=GovernmentVerificationInfo(),
            raw_text=raw_text,
        )

    # 2. Identity field check (GSTIN / PAN / Udyam number) for the types that have one.
    critical_mismatch = False
    gov_info = GovernmentVerificationInfo()

    identity_spec = IDENTITY_FIELD_MAP.get(document_type.upper())
    if identity_spec:
        extracted_key, label = identity_spec
        extracted_value = extracted_data.get(extracted_key)

        if not extracted_value:
            checks.append(CheckResult(
                field=label, status="MISSING_INFORMATION", expected=None, actual=None,
                evidence=f"Could not locate a {label} on the uploaded document.",
            ))
            issues.append(f"{label} could not be found on the uploaded document.")
            recommendations.append(f"Re-upload a clearer copy showing the {label} clearly.")
        else:
            checks.append(CheckResult(
                field=label, status="INFO", expected=None, actual=extracted_value,
                evidence=f"Document {label} extracted as {extracted_value}.",
            ))

        # 3. Mock government portal format/status check for this identifier.
        gov_result = _gov_service.verify(document_type, extracted_value)
        if gov_result:
            gov_info = GovernmentVerificationInfo(
                source=gov_result.source,
                portalStatus=gov_result.portal_status,
            )
            checks.append(CheckResult(
                field=f"{label} (Government Portal)",
                status="INFO" if gov_result.verified_format else "MISSING_INFORMATION",
                expected=None,
                actual=gov_result.portal_status,
                evidence=(
                    f"[DEMO / SIMULATED] {gov_result.source} reports status "
                    f"'{gov_result.portal_status}' for this {label}. This is a mock "
                    f"check, not a real government database lookup."
                ),
            ))

    # 4. Company name presence check.
    company_field_candidates = [
        extracted_data.get("legalBusinessName"),
        extracted_data.get("entityName"),
        extracted_data.get("enterpriseName"),
        extracted_data.get("authorizedBidder"),
        extracted_data.get("manufacturerName"),
    ]
    extracted_name = next((c for c in company_field_candidates if c), None)

    name_found_in_text = bool(
        company_name and re.search(re.escape(company_name.split()[0]), raw_text, re.IGNORECASE)
    ) if company_name else False

    if extracted_name and company_name:
        if _name_matches(extracted_name, company_name):
            checks.append(CheckResult(
                field="Company Name", status="MATCH", expected=company_name, actual=extracted_name,
                evidence="Company name on the document matches the bidder profile.",
            ))
        else:
            checks.append(CheckResult(
                field="Company Name", status="MISMATCH", expected=company_name, actual=extracted_name,
                evidence="Company name on the document does not clearly match the bidder profile.",
            ))
            issues.append("Company name on the document does not clearly match the bidder profile.")
            recommendations.append("Verify this document was submitted by the correct bidder.")
    elif name_found_in_text:
        checks.append(CheckResult(
            field="Company Name", status="MATCH", expected=company_name, actual=company_name,
            evidence="The bidder's company name was found in the document text.",
        ))
    elif company_name:
        checks.append(CheckResult(
            field="Company Name", status="MISSING_INFORMATION", expected=company_name, actual=None,
            evidence="Could not confirm the bidder's company name appears on this document.",
        ))
        issues.append("Could not confirm the bidder's company name on this document.")
        recommendations.append("Manually confirm this document belongs to the bidder.")

    # 5. Overall status.
    if critical_mismatch:
        status = "FAILED"
    elif any(c.status == "MISSING_INFORMATION" for c in checks):
        status = "REQUIRES_REVIEW"
    elif any(c.status == "MISMATCH" for c in checks):
        status = "REQUIRES_REVIEW"
    else:
        status = "VERIFIED"

    confidence = _compute_confidence(checks, extraction_method)

    return _finalize(
        status=status,
        confidence=confidence,
        document_type=document_type,
        extracted_data=extracted_data,
        checks=checks,
        issues=issues,
        recommendations=recommendations,
        government_info=gov_info,
        raw_text=raw_text,
    )


def _compute_confidence(checks: List[CheckResult], extraction_method: str) -> float:
    if not checks:
        return 0.4

    weights = {"MATCH": 1.0, "INFO": 0.8, "MISSING_INFORMATION": 0.3, "MISMATCH": 0.0}
    scores = [weights.get(c.status, 0.5) for c in checks]
    base = sum(scores) / len(scores)

    # OCR text is noisier than a native PDF text layer - shave confidence slightly.
    if extraction_method == "OCR":
        base *= 0.9

    return round(min(max(base, 0.0), 1.0), 2)


def _finalize(
    status: str,
    confidence: float,
    document_type: str,
    extracted_data: Dict[str, Any],
    checks: List[CheckResult],
    issues: List[str],
    recommendations: List[str],
    government_info: GovernmentVerificationInfo,
    raw_text: str,
) -> Dict[str, Any]:
    return {
        "status": status,
        "confidence": confidence,
        "document_type": document_type,
        "extracted_data": extracted_data,
        "checks": checks,
        "issues": issues,
        "recommendations": recommendations,
        "verification_source": "AI_AUTOMATED_EXTRACTION",
        "government_verification": government_info,
        "raw_text_preview": raw_text[:300].strip() if raw_text else None,
    }
