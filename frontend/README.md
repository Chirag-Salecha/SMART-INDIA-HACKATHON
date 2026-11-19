# Bid Compliance Frontend

React + Vite frontend for the Integrated Bid Compliance Verification Platform (SIH 2026).

## Stack
- React 18
- Vite
- React Router
- Axios
- Plain CSS (design tokens in `src/styles/variables.css`)

## Structure
```
src/
├── api/            # axios clients for backend + AI service
├── context/        # AuthContext (mock login state)
├── components/     # reusable UI: Sidebar, Topbar, StatCard, ActivityLog, ProtectedRoute
├── pages/          # Login, Dashboard
└── styles/         # shared CSS variables
```

## Prerequisites
- Node.js 18+
- npm

## Setup
```bash
cd frontend
npm install
```

## Environment variables
Copy `.env.example` to `.env` and adjust if your backend/AI service run on different ports.

| Variable | Default | Description |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080/api` | Spring Boot backend base URL |
| `VITE_AI_API_BASE_URL` | `http://localhost:8000/api` | FastAPI AI service base URL |

## Run
```bash
npm run dev
```
App runs at `http://localhost:5173`.

## Notes
- Login is a **mock login** for this milestone: any non-empty Officer ID logs you in and stores the session in `localStorage`. No credentials are sent to the backend yet.
- Dashboard stats and activity log currently use in-memory mock data; they'll be wired to real backend endpoints in a later milestone.
- `ProtectedRoute` redirects to `/login` if no mock session exists.
