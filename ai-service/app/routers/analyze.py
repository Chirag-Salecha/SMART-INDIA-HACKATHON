"""
POST /api/ai/analyze - the core document analysis + verification endpoint.
"""

from fastapi import APIRouter, File, Form, HTTPException, UploadFile

from app.extraction.field_extractor import extract_fields
from app.extraction.text_extractor import extract_text
from app.schemas import AnalyzeResponse
from app.verification.verifier import build_verification_result

router = APIRouter(prefix="/api/ai", tags=["ai"])

ALLOWED_CONTENT_TYPES = {"application/pdf", "image/png", "image/jpeg", "image/jpg"}
MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024  # 10MB, matches the backend's multipart limit


@router.post("/analyze", response_model=AnalyzeResponse, response_model_by_alias=True)
async def analyze_document(
    file: UploadFile = File(...),
    document_type: str = Form(...),
    company_name: str = Form(...),
    pan: str | None = Form(default=None),
    gstin: str | None = Form(default=None),
    udyam_number: str | None = Form(default=None),
    tender_requirements: str | None = Form(default=None),
):
    if file.content_type not in ALLOWED_CONTENT_TYPES:
        raise HTTPException(status_code=400, detail="Only PDF, PNG and JPEG files are supported")

    file_bytes = await file.read()
    if not file_bytes:
        raise HTTPException(status_code=400, detail="Uploaded file is empty")
    if len(file_bytes) > MAX_FILE_SIZE_BYTES:
        raise HTTPException(status_code=400, detail="Uploaded file exceeds the maximum allowed size")

    extraction = extract_text(file_bytes, file.content_type)
    extracted_data = extract_fields(document_type, extraction.text)

    result = build_verification_result(
        document_type=document_type,
        extracted_data=extracted_data,
        raw_text=extraction.text,
        extraction_method=extraction.method,
        company_name=company_name,
    )

    return AnalyzeResponse(**result)
