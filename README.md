# Integrated Bid Compliance Verification Platform — SIH 2026 (Smart Automation)

MVP foundation with three independent services: a React frontend, a Spring Boot
backend, and a FastAPI AI microservice.

```
/backend       Spring Boot 3 (Java 17) REST API + PostgreSQL
/frontend      React + Vite dashboard
/ai-service    FastAPI microservice (skeleton — AI model added later)
```

This milestone intentionally ships **no authentication** and **no AI model** —
see each service's README for what's stubbed and why. The goal is a clean,
demo-ready foundation to build on incrementally.

## Quick start (3 terminals)

**1. PostgreSQL** — create the database once:
```sql
CREATE DATABASE bid_compliance;
```

**2. Backend** (http://localhost:8080)
```bash
cd backend
mvn spring-boot:run
```

**3. AI service** (http://localhost:8000)
```bash
cd ai-service
python -m venv venv && source venv/bin/activate   # Windows: venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```
Optional but recommended for scanned/image documents: install Tesseract OCR
(see Prerequisites below). Without it, the service still runs fine.

**4. Frontend** (http://localhost:5173)
```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173` → mock login → Dashboard.

## Environment variables

### Backend (`backend/`)
| Variable | Default |
|---|---|
| `DB_HOST` | `localhost` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `bid_compliance` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | `postgres` |
| `JPA_DDL_AUTO` | `update` |
| `JPA_SHOW_SQL` | `true` |
| `SERVER_PORT` | `8080` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` |
| `UPLOAD_DIR` | `./uploads` |
| `UPLOAD_MAX_FILE_SIZE` | `10MB` |
| `UPLOAD_MAX_REQUEST_SIZE` | `10MB` |

### AI service (`ai-service/`)
| Variable | Default |
|---|---|
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` |

### Frontend (`frontend/`)
| Variable | Default |
|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080/api` |
| `VITE_AI_API_BASE_URL` | `http://localhost:8000/api` |

All defaults work together out of the box for local development — you only
need to change them for non-default ports, remote databases, or deployment.

## Prerequisites checklist
- **JDK 17+** and **Maven 3.9+** for the backend
- **PostgreSQL 14+** running locally, with a `bid_compliance` database created
- **Python 3.10+** for the AI service
- **Tesseract OCR** (optional, recommended) for scanned/image document
  support — `sudo apt-get install tesseract-ocr` (Ubuntu/Debian),
  `brew install tesseract` (macOS), or the Windows installer at
  https://github.com/UB-Mannheim/tesseract/wiki. Without it, scanned/image
  uploads are still handled gracefully (marked `REQUIRES_REVIEW` instead of
  auto-verified) — the app never crashes.
- **Node.js 18+** and **npm** for the frontend

## Verifying the foundation works
```bash
curl http://localhost:8080/api/health
# {"status":"UP","service":"bid-compliance-backend"}

curl http://localhost:8000/api/ai/health
# {"status":"AI service is running"}
```
Then open the frontend, log in (any Officer ID), and confirm the Dashboard
renders the four stat cards and the activity table.

## What's deliberately NOT in this milestone
- Authentication / authorization (real login, JWT, roles) — login is still a frontend-only mock
- Real government API integrations (GST/PAN/UDYAM verification) — see "AI-powered verification" below: this uses clearly-labeled MOCK/SIMULATED adapters, not real portals
- Cloud file storage — uploaded documents are stored on the local filesystem under `backend/uploads/`

These will be added incrementally in follow-up milestones once this
foundation is confirmed working end-to-end.

## Tender & Bidder Management module

- **Tenders**: `POST/GET/PUT/DELETE /api/tenders`, `GET /api/tenders/{id}`
- **Bidders**: `POST/GET/PUT/DELETE /api/bidders`, `GET /api/bidders/{id}`, `GET /api/tenders/{tenderId}/bidders`
- **Documents**: `POST /api/bidders/{bidderId}/documents` (multipart upload), `GET /api/bidders/{bidderId}/documents`
- **Dashboard stats**: `GET /api/dashboard/stats` → `{ totalTenders, totalBidders, pendingDocuments, verifiedDocuments }`

Uploaded files are validated (non-empty, PDF/PNG/JPEG only, size-limited via
`UPLOAD_MAX_FILE_SIZE`) and stored under `UPLOAD_DIR`, with the path recorded
in PostgreSQL.

The frontend has `Tenders`, `Create Tender`, `Tender Details` (inline "Add
Bidder"), and `Bidder Details` pages, all reachable from the sidebar.

## AI-powered document verification pipeline

**End-to-end flow:** upload a document on the Bidder Details page → backend
stores it → backend sends it to the AI service with the bidder's profile and
the tender's description → AI service extracts text (PyMuPDF, with OCR
fallback via Tesseract) → extracts document-type-specific fields → compares
them against the bidder profile → returns a structured result (status,
confidence, checks, issues, recommendations) → backend persists it and
updates the document's status → frontend shows it immediately, expandable
per document.

**⚠️ Government Portal Verification: DEMO / SIMULATED.** No real GST, PAN,
Udyam, EPFO or ESIC system is ever contacted. The AI service's portal checks
are deterministic mock adapters, and the frontend displays a clearly visible
"DEMO / SIMULATED" banner anywhere this data is shown.

If the AI service is down when a document is uploaded, the upload still
succeeds — the document is marked `REQUIRES_REVIEW` with the error recorded.
Use the "Verify / Re-analyze" button (or `POST /api/documents/{id}/reanalyze`)
to retry.

### New endpoints
```
GET  /api/documents/queue              # documents needing attention, any bidder
GET  /api/documents/{id}               # single document + its verification result
POST /api/documents/{id}/reanalyze     # re-run AI verification
GET  /api/compliance/bidders/{id}      # weighted compliance report for one bidder
GET  /api/compliance/tenders/{id}      # compliance reports for every bidder under a tender
POST {ai-service}/api/ai/analyze       # (internal, called by the backend) document analysis
```

### New frontend pages
- **Bidder Details** — now shows a Compliance Summary (score, risk, expandable
  per-requirement evidence) and an expandable, per-document verification
  panel (extracted data, checks, issues, recommendations, with a
  Verify/Re-analyze button).
- **Verification Queue** (`/verification-queue`) — all documents that are
  PENDING, REQUIRES_REVIEW, or FAILED, across every tender.
- **Compliance Reports** (`/compliance-reports`) — pick a tender, see every
  bidder's score/risk, drill into full evidence, print the report.

### Database changes
Two new tables (auto-created via `ddl-auto: update`):
- `verification_results` — one row per document's latest AI verification
  (status, confidence, extracted data / checks / issues / recommendations as
  JSON text columns, source, timestamp, error message if any)
- `audit_logs` — append-only trail (document uploaded, AI analysis
  started/completed/failed, compliance score generated) with timestamps

### Sample test data
See `backend/seed-documents/README.md` for three ready-to-use synthetic PDFs
(and instructions to regenerate more) covering:
- **Case A**: all documents valid → high compliance / low risk
- **Case B**: missing documents → lower score / higher risk
- **Case C**: mismatched GSTIN/company name → FAILED / REQUIRES_REVIEW

## Full test procedure

1. Start all three services (commands above). Confirm health:
   ```bash
   curl http://localhost:8080/api/health
   curl http://localhost:8000/api/ai/health
   ```
2. Log in to the frontend (any Officer ID).
3. Create a Tender with description: `GST, PAN, Udyam/MSME, Make in India, OEM authorization`.
4. Add a bidder "Meridian Infra Pvt Ltd" with PAN `ABCDE1234F`, GSTIN
   `27ABCDE1234F1Z5`, Udyam `UDYAM-MH-01-1234567` (see Case A in
   `backend/seed-documents/`).
5. On the bidder's page, upload `case_a_meridian_gst.pdf` as GST — watch it
   come back `VERIFIED` with extracted GSTIN/legal name and matching checks.
   Upload the PAN and UDYAM documents too.
6. Check the Compliance Summary panel — score should reflect the
   documents you've verified vs. the two still missing (Make in India, OEM).
7. Add a second bidder and upload `case_c_sundaram_gst_mismatch.pdf` as GST
   with a different GSTIN on file — confirm it comes back `FAILED` with a
   GSTIN mismatch issue.
8. Visit **Verification Queue** — confirm documents needing attention appear.
9. Visit **Compliance Reports**, pick your tender, confirm both bidders'
   scores/risk levels appear and expand correctly; try "Print Report".
10. Stop the AI service and upload another document — confirm the upload
    still succeeds and the document is marked `REQUIRES_REVIEW` rather than
    the app crashing. Restart the AI service and use "Verify / Re-analyze".

## Government Verification Simulator (Phase 1)

A separate, database-backed simulator (distinct from the AI service's
lighter-weight mock checks) that verifies a bidder's GSTIN/PAN/Udyam number
against seeded PostgreSQL data and flags cross-source name inconsistencies.
**No real government system is ever contacted.** Full details, demo data
(Case A/B/C), API contracts, and curl examples are in `backend/README.md`
under "Government Verification Simulator (Phase 1)". On the Bidder Details
page, look for the new "Government Verification" section and its "Verify
Government Records" button.

## SIH26100 — Full Compliance & Rule Engine (this upgrade)

This upgrade builds on the Government Verification Simulator to implement
the SIH26100 problem statement's Phase 1: **GST, PAN, Udyam, Local Content,
and Debarment**, combined by a dedicated Rule Engine into one transparent
compliance score and risk assessment, plus a Procurement Officer review
step. **The system assists the Procurement Officer; it never makes the
final procurement decision.**

### What's new
- **Debarment/blacklisting check** (`DebarmentVerificationProvider`): matches a
  bidder's PAN → GSTIN → fuzzy legal name (in that priority) against a
  synthetic debarment list. A match is always `REQUIRES_REVIEW`, never an
  automatic disqualification.
- **Local Content / Make in India** (`LocalContentEvaluator`): a genuine
  **rule**, not a certificate lookup — reads the local-content percentage
  the AI service already extracted from the uploaded document and compares
  it to the tender's own configurable threshold (`Tender.localContentThresholdPercent`,
  defaults to 50%).
- **Configurable fuzzy name matching** (`FuzzyNameMatcher`): Levenshtein +
  token-overlap blend (no external fuzzy-matching library needed). Every
  GST/PAN/Udyam name check now uses three configurable bands instead of a
  binary match: `compliance.name-match.pass-threshold` (default 90) →
  VERIFIED, `review-threshold` (default 70) → REQUIRES_REVIEW, below that →
  MISMATCH.
- **Tender-specific document requirements** (`TenderDocumentRequirement`):
  `GET/PUT /api/tenders/{id}/document-requirements` lets each tender declare
  which document types are mandatory vs optional, instead of relying only on
  keyword-parsing the tender description.
- **Rule Engine** (`RuleEngineService`): combines GST + PAN + Udyam + Local
  Content + Debarment + missing-mandatory-documents + cross-document name
  consistency into one weighted score (configurable in `application.yml`
  under `compliance.weights.*`, defaults: GST 15, PAN 15, Udyam 10, Local
  Content 20, Debarment 20, Document Completeness 10, Cross-Document Match
  10 = 100) and a LOW/MEDIUM/HIGH risk level with explicit reasons.
- **Procurement Officer Review** (`OfficerReview`): Approve / Request
  Clarification / Reject, with comments, stored with a compliance-score
  snapshot and a full audit trail entry. The UI and every API response
  carry the disclaimer: *"Final procurement decision remains with the
  authorized Procurement Officer."*
- **Phase 2 placeholders**: `EpfoProvider`, `EsicProvider`, `DpiitProvider`,
  `NsicProvider`, `OemAuthorizationProvider`, `DigiLockerProvider` all
  implement the same `GovernmentVerificationProvider` interface and honestly
  report that official integration requires credentials not available in
  this prototype — they never fabricate a successful official verification.
- **AI Bid Compliance Dashboard**: new section on the existing Dashboard
  showing Total Bidders / Compliant / Needs Review / High Risk / Missing
  Documents across every bidder, with a drill-down table (`GET
  /api/dashboard/compliance-stats`).

### New endpoints
```
GET/PUT /api/tenders/{id}/document-requirements
GET     /api/bidders/{id}/compliance     # score, risk, categories, evidence, recommendations
GET     /api/bidders/{id}/risk           # risk level + reasons only
GET     /api/bidders/{id}/audit          # full audit trail for this bidder
POST    /api/bidders/{id}/review         # record Approve / Request Clarification / Reject
GET     /api/bidders/{id}/review         # decision history
GET     /api/dashboard/compliance-stats  # dashboard-wide compliance metrics
```
(Distinct from the older `/api/compliance/bidders/{id}`, which remains — that's
the earlier AI-document-based compliance report still used by the Compliance
Reports page. The new `/api/bidders/{id}/compliance` is the full Rule Engine
combining every verification source.)

### Database changes
Three new tables: `tender_document_requirements`, `government_debarment_records`
(seeded with one synthetic debarred entity, "Risky Vendors Pvt Ltd"),
`officer_reviews`. `bidder_government_verifications` gained a `source_type`
column (`OFFICIAL_API` / `REFERENCE_DATABASE` / `RULE_BASED` /
`DOCUMENT_ONLY` / `MANUAL_REVIEW`) so provenance is always explicit — a
synthetic reference-database result can never be silently presented as an
official API result. `tenders` gained `local_content_threshold_percent`.

### Configurable thresholds (`backend/src/main/resources/application.yml`)
```yaml
compliance:
  weights:
    gst: 15
    pan: 15
    udyam: 10
    local-content: 20
    debarment: 20
    document-completeness: 10
    cross-document-match: 10
  name-match:
    pass-threshold: 90    # >= this % similarity -> VERIFIED
    review-threshold: 70  # >= this (but below pass) -> REQUIRES_REVIEW; below -> MISMATCH
  local-content:
    default-threshold-percent: 50
```
All overridable via environment variables (see the `${VAR:default}` syntax
in the file) — e.g. `COMPLIANCE_WEIGHT_DEBARMENT=25`.

### Demo scenarios (Phase 1 section 24)
Extends the existing `backend/seed-documents/` Case A/B/C with:
- **Case A** ("ABC Technologies Pvt Ltd") → upload matching GST/PAN/Udyam +
  a Make in India document with ≥50% local content → high score, LOW risk.
- **Case B** → skip uploading a mandatory document → missing-document
  warning, MEDIUM risk.
- **Case C** → the seeded cross-source-inconsistent identifiers → REQUIRES_REVIEW,
  never an automatic disqualification.
- **Case D** ("Risky Vendors Pvt Ltd", PAN `DEBARR123D`, GSTIN `24DEBARR123D1Z1`) →
  seeded on the debarment list → "Potential Debarment Match", HIGH risk.
- **Local content failure**: upload a Make in India document whose extracted
  percentage is below the tender's threshold → NON_COMPLIANT, MEDIUM risk.

### Honesty commitments (spec section 29)
1. All GST/PAN/Udyam/Debarment reference data in this build is **synthetic** —
   generated for this prototype, not sourced from any real registry.
2. It is **not** an official government database, and the UI labels every
   such result "Prototype Reference Database — Synthetic" / "DEMO / SIMULATED".
3. Production deployment would require authorized government/API integrations
   (GSTN, Udyam portal, EPFO, ESIC, DPIIT, NSIC, DigiLocker, an official
   debarment registry) with proper credentials and approval.
4. Some verification processes (EPFO, ESIC, DPIIT, NSIC, DigiLocker, OEM
   authorization) currently have **no official free API** — Phase 2
   providers honestly report `UNVERIFIABLE`/manual-review rather than
   fabricating success.
5. OCR can misread documents — a low-confidence or `REQUIRES_REVIEW` result
   should prompt manual verification, not automatic rejection.
6. AI/rule-engine recommendations are **advisory only**.
7. The Procurement Officer makes the final Approve / Clarify / Reject decision;
   the system only records it.

### Known limitations of this build
- Dashboard compliance stats recompute the rule engine for every bidder on
  every load — fine at demo scale, not paginated/cached for production.
- No authentication — `officerName` on a review is free text, not tied to a
  real user/session (matches the rest of the platform's current auth state).
- Phase 2 providers are not yet wired into the main verification flow or
  the compliance score — they exist as extensible scaffolding only.
- Cross-document consistency compares legal/enterprise names only (not
  addresses or dates) for now.
