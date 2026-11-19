# Bid Compliance Backend

Spring Boot backend for the Integrated Bid Compliance Verification Platform (SIH 2026).

## Stack
- Java 17
- Spring Boot 3.3
- Spring Web, Spring Data JPA
- PostgreSQL
- Lombok
- Maven

## Package structure
```
com.sih.bidcompliance
├── controller   # REST endpoints
├── service      # business logic
├── repository   # Spring Data JPA repositories
├── entity       # JPA entities
├── dto          # request/response payloads
├── config       # app configuration (CORS, etc.)
└── exception    # global exception handling
```

## Prerequisites
- JDK 17+
- Maven 3.9+ (or use the included `mvnw` if you add one)
- PostgreSQL 14+ running locally or accessible remotely

## 1. Create the database
```sql
CREATE DATABASE bid_compliance;
```

## 2. Configure environment variables
All config is externalized via environment variables (see `src/main/resources/application.yml`).

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `bid_compliance` | Database name |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |
| `JPA_DDL_AUTO` | `update` | Hibernate schema strategy |
| `JPA_SHOW_SQL` | `true` | Log SQL statements |
| `SERVER_PORT` | `8080` | Backend port |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Frontend origin allowed by CORS |
| `UPLOAD_DIR` | `./uploads` | Local directory for stored bidder documents |
| `UPLOAD_MAX_FILE_SIZE` | `10MB` | Max upload size per file |
| `UPLOAD_MAX_REQUEST_SIZE` | `10MB` | Max multipart request size |
| `AI_SERVICE_BASE_URL` | `http://localhost:8000` | Base URL of the FastAPI ai-service |
| `AI_SERVICE_TIMEOUT_SECONDS` | `30` | Connect/read timeout for AI service calls |

You can export these in your shell, or create a `.env` and load it with your IDE / `direnv` / `export $(cat .env | xargs)`.

Defaults work out of the box for local development if Postgres is running on `localhost:5432` with user/password `postgres`/`postgres`.

## 3. Run the backend
```bash
cd backend
mvn spring-boot:run
```

Or build a jar and run it:
```bash
mvn clean package
java -jar target/bid-compliance-backend.jar
```

The API will start on `http://localhost:8080`.

## 4. Verify it's running
```bash
curl http://localhost:8080/api/health
```
Expected response:
```json
{"status":"UP","service":"bid-compliance-backend"}
```

## Notes
- Authentication is intentionally NOT implemented yet (mock login lives on the frontend for now).
- CORS is restricted to the origin(s) in `CORS_ALLOWED_ORIGINS` (defaults to the Vite dev server at `http://localhost:5173`).

## AI verification module

Every document upload (`POST /api/bidders/{bidderId}/documents`) now automatically:
1. Stores the file (as before).
2. Calls the `ai-service` (`AI_SERVICE_BASE_URL`) with the document + bidder profile + tender description.
3. Persists the structured result (`VerificationResult` entity, one per document) and updates the document's `verificationStatus`.
4. Writes audit trail entries (`AuditLog`) at each step for transparency.

**If the AI service is unreachable, the upload still succeeds** — the document is
marked `REQUIRES_REVIEW` with the error recorded, rather than the request failing.
Use `POST /api/documents/{id}/reanalyze` to retry once the AI service is back up.

### New API endpoints
```
GET    /api/documents/queue              # documents PENDING / REQUIRES_REVIEW / FAILED, across all bidders
GET    /api/documents/{id}               # single document incl. its verification result
POST   /api/documents/{id}/reanalyze     # re-run AI verification on an existing document
GET    /api/compliance/bidders/{id}      # weighted compliance report for one bidder
GET    /api/compliance/tenders/{id}      # compliance reports for every bidder under a tender
```

### Compliance engine
`ComplianceEngineService` derives required document types from the tender's
description (looking for keywords like "GST", "Udyam/MSME", "Make in India";
falls back to the five baseline requirements — GST, PAN, UDYAM, OEM
Authorization, Make in India — if nothing is recognized), then scores the
bidder using fixed weights (GST=20, PAN=20, UDYAM=15, OEM=20, Make in
India=15, any additional required types split the remaining weight). The
score is 100% derived from actual `VerificationStatus` values — no random
numbers anywhere.

### Government verification is MOCK/SIMULATED
The AI service's GST/PAN/Udyam portal checks are deterministic mock
adapters, clearly labeled `DEMO_SIMULATED` in every response. This backend
does not (and should not) claim real government database access.

## Government Verification Simulator (Phase 1)

A standalone, database-backed simulator for GST/PAN/Udyam verification -
**entirely separate from any real government system**. This is the
foundation for the eventual multi-source compliance engine; the interface
(`GovernmentVerificationProvider`) is designed so EPFO, ESIC, Startup India,
NSIC, MCA21, blacklisting, and DigiLocker checks can be added later as new
implementations without touching existing code.

### New tables (seeded automatically on startup)
- `government_gst_records` (gstin unique, legalName, status, registrationDate, returnFilingStatus, registeredAddress, lastVerifiedAt)
- `government_pan_records` (pan unique, legalName, status, incomeTaxComplianceStatus, assessmentYear, lastVerifiedAt)
- `government_udyam_records` (udyamNumber unique, enterpriseName, enterpriseType, status, registrationDate, registeredAddress, lastVerifiedAt)
- `bidder_government_verifications` — auditable, append-only record of every check run against a bidder (type, result status, identifier checked, matched value, government value, discrepancy description, source, evidence, timestamp)

`GovernmentDataSeeder` populates demo records idempotently on every startup
(checked by identifier, so restarts never duplicate rows) — see "Demo data"
below for exactly what's seeded.

### Direct lookup APIs (raw simulator data)
```
GET /api/government/gst/{gstin}
GET /api/government/pan/{pan}
GET /api/government/udyam/{udyamNumber}
```
Malformed identifiers return `400`. Valid-format identifiers with no
matching record return `200` with `"found": false`.

### Bidder-level verification APIs
```
POST /api/bidders/{bidderId}/government-verification/verify   # runs verification now
GET  /api/bidders/{bidderId}/government-verification           # latest known result, no re-run
```
`POST .../verify` queries all three sources for the bidder's GSTIN/PAN/Udyam
number, compares each against the bidder's profile name, runs a
cross-source name-consistency check across GST/PAN/Udyam, persists every
result (audit trail), and returns a consolidated summary with an
`overallStatus` of `VERIFIED`, `MISMATCH`, `NOT_FOUND`, `NON_COMPLIANT`, or
`REQUIRES_REVIEW`. A cross-source name mismatch alone never resolves to
`NON_COMPLIANT` — it's always `REQUIRES_REVIEW`, per the "don't
auto-disqualify" requirement.

### Demo data (Case A / B / C)
| Case | GSTIN | PAN | Udyam | Demonstrates |
|---|---|---|---|---|
| A — valid | `29ABCDE1234F1Z5` (ACTIVE) | `ABCDE1234F` (VALID) | `UDYAM-KA-01-1234567` (ACTIVE) | All VERIFIED, names consistent |
| B — invalid | `27CANCL0000C1Z1` (CANCELLED) | `XXXXX0000X` (INVALID) | `UDYAM-DL-02-7654321` (INACTIVE) | NON_COMPLIANT per source |
| C — inconsistent | `07INCON1234C1Z2` → "ABC Technologies Pvt Ltd" | `INCON1234C` → "ABC Technologies Pvt Ltd" | `UDYAM-MH-03-1112223` → "ABC Technology Limited" | Cross-source name mismatch → REQUIRES_REVIEW |

To demo: create a bidder with company name "ABC Technologies Pvt Ltd" and
the Case A identifiers above, then click "Verify Government Records" on the
Bidder Details page (or `POST` the endpoint directly).

### Sample curl requests
```bash
# Direct lookups
curl http://localhost:8080/api/government/gst/29ABCDE1234F1Z5
curl http://localhost:8080/api/government/pan/ABCDE1234F
curl http://localhost:8080/api/government/udyam/UDYAM-KA-01-1234567

# Not found (valid format, no matching record)
curl http://localhost:8080/api/government/gst/29ZZZZZ9999Z1Z1

# Invalid format (400)
curl http://localhost:8080/api/government/gst/not-a-real-gstin

# Bidder-level verification (replace 1 with a real bidder id)
curl -X POST http://localhost:8080/api/bidders/1/government-verification/verify
curl http://localhost:8080/api/bidders/1/government-verification
```

### Tests
`src/test/java/com/sih/bidcompliance/service/` contains unit tests (JUnit 5
+ Mockito, no database required) covering: valid/not-found/cancelled GST,
valid/invalid PAN, valid/not-found Udyam, cross-source name mismatch,
successful full bidder verification, and result persistence. Run with:
```bash
mvn test
```
