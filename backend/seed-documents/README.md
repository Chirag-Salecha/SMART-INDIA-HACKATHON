# Demo Test Documents

Synthetic (non-real) PDFs for demonstrating end-to-end verification scenarios in the Bid Compliance Platform. None of these represent real people or companies.

All documents can be regenerated at any time using:
```bash
python generate_demo_pdfs.py
```

---

## Pre-seeded Government Reference Data

The Spring Boot backend seeds reference data idempotently on startup (`GovernmentDataSeeder.java`):

### TechVision Solutions Pvt Ltd
- **Company Name:** `TechVision Solutions Pvt Ltd`
- **GSTIN:** `29AABCT1234F1ZX` (State: Karnataka, Status: Active)
- **PAN:** `AABCT1234F` (Status: Active)
- **Udyam Number:** `UDYAM-KA-01-0001234` (Category: Micro, Enterprise: TechVision Solutions Pvt Ltd)

### Meridian Infra Pvt Ltd
- **Company Name:** `Meridian Infra Pvt Ltd`
- **GSTIN:** `27ABCDE1234F1Z5` (State: Maharashtra, Status: Active)
- **PAN:** `ABCDE1234F` (Status: Active)
- **Udyam Number:** `UDYAM-MH-01-1234567` (Category: Small, Enterprise: Meridian Infra Pvt Ltd)

---

## Test Scenarios & Files

### Scenario 1: Valid Verification (TechVision Solutions Pvt Ltd)
- **Bidder:** `TechVision Solutions Pvt Ltd`
- **Upload:**
  1. `techvision_valid_gst.pdf` (Document Type: GST)
  2. `techvision_valid_pan.pdf` (Document Type: PAN)
  3. `techvision_valid_udyam.pdf` (Document Type: UDYAM)
- **Data Flow:**
  - AI Service extracts `29AABCT1234F1ZX`, `AABCT1234F`, `UDYAM-KA-01-0001234`.
  - Spring Boot queries PostgreSQL reference tables `government_*_records`.
  - Reference records match bidder company name and active status.
  - Government verification status: `VERIFIED`.
  - Rule Engine awards full points (15/15 GST, 15/15 PAN, 10/10 Udyam).
  - Risk Level: `LOW`.

### Scenario 2: Entity Mismatch Detection
- **Bidder:** `TechVision Solutions Pvt Ltd`
- **Upload:** `techvision_wrong_gst.pdf` (Document Type: GST)
  - Contains GSTIN `27ABCDE1234F1Z5` and legal name `Different Entity Solutions Pvt Ltd`.
- **Expected Outcome:**
  - Spring Boot matches record belonging to `Meridian Infra Pvt Ltd`.
  - Bidder name `TechVision Solutions Pvt Ltd` mismatches record legal name.
  - Government Verification: `MISMATCH` with discrepancy explanation.
  - Rule Engine awards 0/15 points for GST.
  - Risk Level: `HIGH`.

### Scenario 3: Unregistered Record (NOT_FOUND)
- **Bidder:** `TechVision Solutions Pvt Ltd`
- **Upload:** `techvision_unknown_gst.pdf` (Document Type: GST)
  - Contains GSTIN `29XXXXX9999X1Z9` (not present in government reference DB).
- **Expected Outcome:**
  - Spring Boot queries `government_gst_records` and finds no record.
  - Government Verification: `NOT_FOUND` ("GSTIN 29XXXXX9999X1Z9 is not found in the official registry").
  - Rule Engine: 0/15 points for GST (government verification is AUTHORITATIVE and does NOT fall back to OCR).
  - Risk Level: `HIGH`.

### Scenario 4: Missing Identifier in Document
- **Bidder:** `TechVision Solutions Pvt Ltd`
- **Upload:** `techvision_missing_id.pdf` (Document Type: GST)
  - PDF contains text but no GSTIN pattern.
- **Expected Outcome:**
  - AI Service extracts no GSTIN.
  - Government Verification: `UNVERIFIABLE` ("No GSTIN found in document to verify against government registry").
  - Rule Engine: Flagged for review / 0 points.

