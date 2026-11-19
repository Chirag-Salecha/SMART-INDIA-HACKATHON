"""
Bid Compliance AI Service.

Exposes:
- GET  /api/ai/health   - liveness check
- POST /api/ai/analyze  - document text/field extraction + verification

All "government verification" performed here is a deterministic MOCK/
SIMULATED adapter (see app/verification/government_adapters.py). This
service never connects to any real GST/PAN/Udyam/EPFO/ESIC system.
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import os

from app.routers.analyze import router as analyze_router

app = FastAPI(
    title="Bid Compliance AI Service",
    description="AI microservice for the Integrated Bid Compliance Verification Platform",
    version="0.2.0",
)

# Allow the frontend (and backend, if it ever proxies) to call this service
# directly during development.
allowed_origins = os.getenv("CORS_ALLOWED_ORIGINS", "http://localhost:5173").split(",")

app.add_middleware(
    CORSMiddleware,
    allow_origins=allowed_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(analyze_router)


@app.get("/api/ai/health")
def health():
    return {"status": "AI service is running"}
