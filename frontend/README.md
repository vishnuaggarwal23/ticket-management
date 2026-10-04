# Ticket Management — frontend

React + Next.js (App Router) + Vite (shared `src/` dev). JavaScript only.

## Prerequisites

- **Node.js 24.x** (see `.nvmrc`)
- Backend API on **http://localhost:8080** (`cd backend && ./mvnw spring-boot:run` with Postgres configured)

## Setup

```bash
cd frontend
cp .env.example .env.local   # optional; adjust API base URLs
npm install
```

## Development

```bash
npm run dev
```

Opens **http://localhost:3000** by default. Next.js can proxy `/api/*` to the backend via `next.config.js` rewrites.

For Vite-only HMR on future `src/` modules (port **5173**):

```bash
npx vite
```

## Production build

```bash
npm run build
npm start
```

## Environment

Committed **`.env.example`** lists variable names only — do not commit secrets or a real `.env`.

| Variable | Purpose |
|----------|---------|
| `NEXT_PUBLIC_API_BASE_URL` | Browser API base (Next.js) |
| `VITE_API_BASE_URL` | API base when using Vite dev for `src/` |
