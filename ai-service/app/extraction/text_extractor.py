"""
Extracts raw text from an uploaded document.

- PDFs are read with PyMuPDF (fitz). If a PDF has little/no embedded text
  (i.e. it's a scan), each page is rasterized and passed through OCR.
- Images (PNG/JPG/JPEG) always go through OCR.
- If the OCR engine (Tesseract) isn't installed on the host machine, we
  never crash - we return a clean, explicit "OCR not available" result so
  the rest of the pipeline can degrade to REQUIRES_REVIEW instead of 500ing.
"""

import io
import logging
from dataclasses import dataclass

logger = logging.getLogger(__name__)

MIN_TEXT_LENGTH_BEFORE_OCR_FALLBACK = 20


@dataclass
class ExtractionResult:
    text: str
    method: str  # PDF_TEXT_LAYER | OCR | OCR_UNAVAILABLE | UNSUPPORTED | ERROR
    ocr_available: bool


def _ocr_available() -> bool:
    try:
        import pytesseract  # noqa: F401
        import shutil

        return shutil.which("tesseract") is not None
    except ImportError:
        return False


def _ocr_image_bytes(image_bytes: bytes) -> str:
    from PIL import Image
    import pytesseract

    image = Image.open(io.BytesIO(image_bytes))
    return pytesseract.image_to_string(image)


def _extract_from_pdf(file_bytes: bytes) -> ExtractionResult:
    import fitz  # PyMuPDF

    try:
        doc = fitz.open(stream=file_bytes, filetype="pdf")
    except Exception as exc:  # malformed PDF, etc.
        logger.warning("Failed to open PDF: %s", exc)
        return ExtractionResult(text="", method="ERROR", ocr_available=_ocr_available())

    text_parts = []
    for page in doc:
        text_parts.append(page.get_text())
    text = "\n".join(text_parts).strip()

    if len(text) >= MIN_TEXT_LENGTH_BEFORE_OCR_FALLBACK:
        doc.close()
        return ExtractionResult(text=text, method="PDF_TEXT_LAYER", ocr_available=_ocr_available())

    # Likely a scanned PDF with no text layer - fall back to OCR on each page.
    if not _ocr_available():
        doc.close()
        return ExtractionResult(text=text, method="OCR_UNAVAILABLE", ocr_available=False)

    ocr_parts = []
    for page in doc:
        pixmap = page.get_pixmap(dpi=200)
        image_bytes = pixmap.tobytes("png")
        try:
            ocr_parts.append(_ocr_image_bytes(image_bytes))
        except Exception as exc:  # pragma: no cover - defensive
            logger.warning("OCR failed on a PDF page: %s", exc)
    doc.close()

    ocr_text = "\n".join(ocr_parts).strip()
    return ExtractionResult(text=ocr_text, method="OCR", ocr_available=True)


def _extract_from_image(file_bytes: bytes) -> ExtractionResult:
    if not _ocr_available():
        return ExtractionResult(text="", method="OCR_UNAVAILABLE", ocr_available=False)

    try:
        text = _ocr_image_bytes(file_bytes)
        return ExtractionResult(text=text.strip(), method="OCR", ocr_available=True)
    except Exception as exc:  # pragma: no cover - defensive
        logger.warning("OCR failed on image: %s", exc)
        return ExtractionResult(text="", method="ERROR", ocr_available=True)


def extract_text(file_bytes: bytes, content_type: str) -> ExtractionResult:
    content_type = (content_type or "").lower()

    if content_type == "application/pdf":
        return _extract_from_pdf(file_bytes)

    if content_type in ("image/png", "image/jpeg", "image/jpg"):
        return _extract_from_image(file_bytes)

    return ExtractionResult(text="", method="UNSUPPORTED", ocr_available=_ocr_available())
