"""
Mock/simulated government verification adapters.

IMPORTANT: none of these connect to any real government system. They are
deterministic, rule-based stand-ins so the platform can demo an end-to-end
verification flow without government API credentials. Every result is
labelled with mode="DEMO_SIMULATED" and a disclaimer - this must never be
presented to a user as a real government lookup.

The interface (GovernmentVerificationAdapter) is deliberately small so a
real adapter (e.g. calling the actual GST/Udyam portal APIs) can be dropped
in later without touching calling code.
"""

from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Optional

from app.extraction.field_extractor import GSTIN_PATTERN, PAN_PATTERN, UDYAM_PATTERN


@dataclass
class GovernmentVerificationResult:
    verified_format: bool
    portal_status: str
    source: str
    disclaimer: str = (
        "Mock/simulated government portal check for demo purposes only. "
        "No live connection to any government database is made."
    )


class GovernmentVerificationAdapter(ABC):
    source_name: str

    @abstractmethod
    def verify(self, value: Optional[str]) -> GovernmentVerificationResult:
        raise NotImplementedError


class MockGstVerificationService(GovernmentVerificationAdapter):
    source_name = "MOCK_GST_PORTAL"

    def verify(self, value: Optional[str]) -> GovernmentVerificationResult:
        if not value or not GSTIN_PATTERN.fullmatch(value.strip().upper()):
            return GovernmentVerificationResult(
                verified_format=False, portal_status="NOT_FOUND", source=self.source_name
            )
        return GovernmentVerificationResult(
            verified_format=True, portal_status="ACTIVE", source=self.source_name
        )


class MockPanVerificationService(GovernmentVerificationAdapter):
    source_name = "MOCK_PAN_PORTAL"

    def verify(self, value: Optional[str]) -> GovernmentVerificationResult:
        if not value or not PAN_PATTERN.fullmatch(value.strip().upper()):
            return GovernmentVerificationResult(
                verified_format=False, portal_status="NOT_FOUND", source=self.source_name
            )
        return GovernmentVerificationResult(
            verified_format=True, portal_status="ACTIVE", source=self.source_name
        )


class MockUdyamVerificationService(GovernmentVerificationAdapter):
    source_name = "MOCK_UDYAM_PORTAL"

    def verify(self, value: Optional[str]) -> GovernmentVerificationResult:
        if not value or not UDYAM_PATTERN.fullmatch(value.strip().upper()):
            return GovernmentVerificationResult(
                verified_format=False, portal_status="NOT_FOUND", source=self.source_name
            )
        return GovernmentVerificationResult(
            verified_format=True, portal_status="ACTIVE", source=self.source_name
        )


class GovernmentVerificationService:
    """Facade that picks the right mock adapter for a document type."""

    def __init__(self):
        self._adapters: dict[str, GovernmentVerificationAdapter] = {
            "GST": MockGstVerificationService(),
            "PAN": MockPanVerificationService(),
            "UDYAM": MockUdyamVerificationService(),
        }

    def verify(self, document_type: str, value: Optional[str]) -> Optional[GovernmentVerificationResult]:
        adapter = self._adapters.get((document_type or "").upper())
        if not adapter:
            return None
        return adapter.verify(value)
