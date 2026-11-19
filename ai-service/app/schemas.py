"""
Pydantic models for the /api/ai/analyze endpoint.

JSON field names use camelCase (via aliases) so the response maps directly
onto the Spring Boot AiAnalysisResponse DTO without extra @JsonProperty
plumbing on the Java side.
"""

from typing import Any, Dict, List, Optional
from pydantic import BaseModel, ConfigDict, Field


class CamelModel(BaseModel):
    model_config = ConfigDict(populate_by_name=True)


class CheckResult(CamelModel):
    field: str
    status: str  # MATCH | MISMATCH | MISSING_INFORMATION | INFO
    expected: Optional[str] = None
    actual: Optional[str] = None
    evidence: str


class GovernmentVerificationInfo(CamelModel):
    mode: str = "DEMO_SIMULATED"
    disclaimer: str = (
        "This is a mock/simulated government portal check for demo purposes. "
        "No live connection to GST, PAN, Udyam, EPFO or ESIC systems is made."
    )
    source: Optional[str] = None
    portal_status: Optional[str] = Field(default=None, alias="portalStatus")


class AnalyzeResponse(CamelModel):
    status: str  # VERIFIED | FAILED | REQUIRES_REVIEW
    confidence: float
    document_type: str = Field(alias="documentType")
    extracted_data: Dict[str, Any] = Field(default_factory=dict, alias="extractedData")
    checks: List[CheckResult] = Field(default_factory=list)
    issues: List[str] = Field(default_factory=list)
    recommendations: List[str] = Field(default_factory=list)
    verification_source: str = Field(alias="verificationSource")
    government_verification: GovernmentVerificationInfo = Field(
        default_factory=GovernmentVerificationInfo, alias="governmentVerification"
    )
    raw_text_preview: Optional[str] = Field(default=None, alias="rawTextPreview")
