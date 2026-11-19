# Bid Compliance AI Service

FastAPI microservice implementing the core AI-powered document verification
pipeline for the Integrated Bid Compliance Verification Platform (SIH 2026).

## What it does

`POST /api/ai/analyze` accepts a bidder document (PDF/PNG/JPEG) plus the
bidder's profile data, and returns a structured verification result:

1. **Text extraction** — PyMuPDF reads the PDF text layer directly. If a PDF
   has little/no text (a scan) or the upload is an image, it falls back to
   OCR (Tesseract via pytesseract).
2. **Field extraction** — pattern/label-based extraction of the fields
   relevant to the document type (GSTIN, PAN, Udyam number, OEM details,
   Make in India declaration, etc).
3. **Verification** — extracted fields are compared against the bidder
   profile supplied in the request (MATCH / MISMATCH / MISSING_INFORMATION),
   plus a **mock/simulated** government portal format check.
4. **Result** — an overall status (`VERIFIED` / `FAILED` / `REQUIRES_REVIEW`),
   a confidence score, the extracted data, per-field checks with evidence,
   issues, and recommendations. All deterministic — no randomness.

### ⚠️ Government verification is MOCK/SIMULATED

`app/verification/government_adapters.py` implements deterministic,
rule-based stand-ins for GST/PAN/Udyam portal checks. **This service never
connects to any real government system.** Every result is tagged
`"mode": "DEMO_SIMULATED"` with a disclaimer — never present this as a real
government lookup. Swapping in real government APIs later only requires
implementing `GovernmentVerificationAdapter` for the real endpoint.

### OCR availability

If Tesseract isn't installed on the host, the service does **not** crash —
scanned/image documents get a clean `REQUIRES_REVIEW` result explaining that
OCR is unavailable, so procurement officers know manual review is needed.

## Prerequisites
- Python 3.10+
- pip
- **Tesseract OCR binary** (optional but recommended, for scanned documents
  and images):
  - Ubuntu/Debian: `sudo apt-get install tesseract-ocr`
  - macOS: `brew install tesseract`
  - Windows: install from https://github.com/UB-Mannheim/tesseract/wiki
    and ensure `tesseract.exe` is on your PATH. If you skip this, the
    service still runs fine — image/scanned-PDF uploads will just be
    flagged `REQUIRES_REVIEW` instead of auto-verified.

## Setup
```bash
cd ai-service
python -m venv venv
source venv/bin/activate      # Windows: venv\Scripts\activate
pip install -r requirements.txt
```

## Environment variables
| Variable | Default | Description |
|---|---|---|
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated list of allowed origins |

Copy `.env.example` to `.env` and adjust as needed, or export the variables directly.

## Run
```bash
uvicorn app.main:app --reload --port 8000
```

## Verify
```bash
curl http://localhost:8000/api/ai/health
# {"status": "AI service is running"}

curl -X POST http://localhost:8000/api/ai/analyze \
  -F "file=@/path/to/some.pdf;type=application/pdf" \
  -F "document_type=GST" \
  -F "company_name=Acme Pvt Ltd" \
  -F "gstin=27ABCDE1234F1Z5"
```

Interactive API docs: `http://localhost:8000/docs`

## Project layout
```
app/
├── main.py                          FastAPI app + CORS + router registration
├── schemas.py                       Pydantic request/response models
├── extraction/
│   ├── text_extractor.py            PDF text layer + OCR fallback
│   └── field_extractor.py           Per-document-type field extraction (regex)
├── verification/
│   ├── government_adapters.py       Mock GST/PAN/Udyam portal adapters
│   └── verifier.py                  Compares extracted data vs bidder profile
└── routers/
    └── analyze.py                   POST /api/ai/analyze
```
