"""
Extracts structured fields from raw document text, based on document_type.

This uses pattern matching (regex + labelled-line lookup) rather than a
trained model - appropriate for a hackathon MVP where the goal is a
transparent, explainable pipeline rather than a black-box classifier.
"""

import re
from typing import Any, Dict, Optional

GSTIN_PATTERN = re.compile(r"\b\d{2}[A-Z]{5}\d{4}[A-Z]\d[Z][A-Z0-9]\b")
PAN_PATTERN = re.compile(r"\b[A-Z]{5}\d{4}[A-Z]\b")
UDYAM_PATTERN = re.compile(r"\bUDYAM-[A-Z]{2}-\d{2}-\d{7}\b")
PERCENTAGE_PATTERN = re.compile(r"\b(\d{1,3}(?:\.\d+)?)\s?%")
DATE_PATTERN = re.compile(
    r"\b(\d{1,2}[/\-.]\d{1,2}[/\-.]\d{2,4}|\d{4}[/\-.]\d{1,2}[/\-.]\d{1,2})\b"
)


def _find_labelled_value(text: str, labels: list[str]) -> Optional[str]:
    """
    Looks for a line like "Legal Name: Acme Pvt Ltd" and returns the value
    for the first matching label. Case-insensitive, tolerant of ':' or '-'.
    """
    for label in labels:
        pattern = re.compile(
            rf"{re.escape(label)}\s*[:\-]\s*(.+)", re.IGNORECASE
        )
        match = pattern.search(text)
        if match:
            value = match.group(1).strip()
            # Trim to the current line only (avoid swallowing the whole doc)
            value = value.splitlines()[0].strip()
            if value:
                return value
    return None


def _find_first(pattern: re.Pattern, text: str) -> Optional[str]:
    match = pattern.search(text)
    return match.group(0) if match else None


def extract_fields(document_type: str, text: str) -> Dict[str, Any]:
    document_type = (document_type or "OTHER").upper()
    text = text or ""

    if document_type == "GST":
        return _extract_gst(text)
    if document_type == "PAN":
        return _extract_pan(text)
    if document_type == "UDYAM":
        return _extract_udyam(text)
    if document_type == "OEM_AUTHORIZATION":
        return _extract_oem_authorization(text)
    if document_type == "MAKE_IN_INDIA":
        return _extract_make_in_india(text)

    return _extract_generic(text)


def _extract_gst(text: str) -> Dict[str, Any]:
    gstin = _find_first(GSTIN_PATTERN, text)
    legal_name = _find_labelled_value(
        text, ["Legal Name", "Trade Name", "Legal Name of Business"]
    )
    status = None
    status_match = re.search(r"\b(Active|Cancelled|Suspended)\b", text, re.IGNORECASE)
    if status_match:
        status = status_match.group(1).title()
    registration_date = _find_labelled_value(text, ["Date of Registration", "Registration Date"])
    if not registration_date:
        registration_date = _find_first(DATE_PATTERN, text)

    return {
        "gstin": gstin,
        "legalBusinessName": legal_name,
        "registrationStatus": status,
        "registrationDate": registration_date,
    }


def _extract_pan(text: str) -> Dict[str, Any]:
    pan = _find_first(PAN_PATTERN, text)
    entity_name = _find_labelled_value(text, ["Name", "Entity Name", "Applicant Name"])
    return {
        "pan": pan,
        "entityName": entity_name,
    }


def _extract_udyam(text: str) -> Dict[str, Any]:
    udyam_number = _find_first(UDYAM_PATTERN, text)
    enterprise_name = _find_labelled_value(
        text, ["Name of Enterprise", "Enterprise Name", "Udyam Registration Name"]
    )
    category = None
    category_match = re.search(r"\b(Micro|Small|Medium)\b", text, re.IGNORECASE)
    if category_match:
        category = category_match.group(1).title()

    return {
        "udyamNumber": udyam_number,
        "enterpriseName": enterprise_name,
        "enterpriseCategory": category,
    }


def _extract_oem_authorization(text: str) -> Dict[str, Any]:
    oem_name = _find_labelled_value(text, ["OEM Name", "Manufacturer", "OEM"])
    authorized_bidder = _find_labelled_value(
        text, ["Authorized Dealer", "Authorized Bidder", "Authorized Reseller", "Bidder Name"]
    )
    validity = _find_labelled_value(text, ["Valid Until", "Validity", "Valid Up To"])
    if not validity:
        validity = _find_first(DATE_PATTERN, text)

    return {
        "oemName": oem_name,
        "authorizedBidder": authorized_bidder,
        "validity": validity,
    }


def _extract_make_in_india(text: str) -> Dict[str, Any]:
    manufacturer_name = _find_labelled_value(text, ["Manufacturer", "Manufacturer Name"])
    local_content = _find_first(PERCENTAGE_PATTERN, text)
    has_declaration = bool(
        re.search(r"\bmake\s*in\s*india\b", text, re.IGNORECASE)
        or re.search(r"\blocal\s*content\b", text, re.IGNORECASE)
    )

    return {
        "manufacturerName": manufacturer_name,
        "localContentPercentage": local_content,
        "declarationPresent": has_declaration,
    }


def _extract_generic(text: str) -> Dict[str, Any]:
    """
    Fallback for INCOME_TAX, EPFO, ESIC, STARTUP_INDIA, NSIC, OTHER: we don't
    have a bespoke schema for these yet, so surface whatever recognizable IDs
    appear and a text preview for manual review.
    """
    return {
        "possiblePan": _find_first(PAN_PATTERN, text),
        "possibleGstin": _find_first(GSTIN_PATTERN, text),
        "possibleDate": _find_first(DATE_PATTERN, text),
        "textPreview": text[:300].strip() if text else None,
    }
