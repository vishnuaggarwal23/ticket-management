# Ticket Management — frontend

React + Next.js (App Router) + Vite (shared `src/` dev). JavaScript only.

## Prerequisites

- **Node.js 24.x** (see `.nvmrc`)
- Backend API (default **http://127.0.0.1:8080**) — `cd backend && ./mvnw spring-boot:run` with Postgres configured

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

Opens **http://localhost:3000** by default. Next.js proxies `/api/*` to the backend via `next.config.js` rewrites (`BACKEND_REWRITE_URL`).

For Vite-only HMR on `src/` modules (port **5173**):

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
| `NEXT_PUBLIC_API_BASE_URL` | Browser API base (Next.js); empty uses same-origin `/api` rewrites |
| `VITE_API_BASE_URL` | API base when using Vite dev for `src/` |
| `BACKEND_REWRITE_URL` | Next rewrite target for `/api/*` (dev default `127.0.0.1:8080`) |

## Demo walkthrough (UI — hub §8.7)

With backend on **8080** and `npm run dev` on **3000**:

1. **List** — Open `/tickets`; confirm tickets load (or empty state).
2. **Create** — **New ticket** → fill title (required) → submit → lands on detail with `TKT-{n}`.
3. **Validation** — **New ticket** → empty title → readable error (no submit).
4. **Search / filter** — Keyword + **Apply**; status filter; pagination when `meta.totalPages` > 1.
5. **Detail read** — Row opens `/tickets/TKT-…`; comments ascending; link to **Ask**.
6. **Field save** — Edit title/description → **Save changes** → reload shows persisted values.
7. **Comment** — Post comment → appears in thread.
8. **Status** — OPEN → IN_PROGRESS → RESOLVED → CLOSED via status buttons; illegal path shows **409** message (backend enforces).
9. **Ask grounded** — `/ask` → question matching ticket text → answer panel + citation links to detail.
10. **Ask no-match** — Obscure question → **200** honest answer, empty citations (not an error banner).
11. **Ask validation** — Blank question → **400** / client validation message.

Full grader script: [`spec/requirements.md`](../spec/requirements.md) **§8.7** and [`spec/ui-flow.md`](../spec/ui-flow.md) **§12**.

## Review

UI changes: [`commands/review-frontend.md`](../commands/review-frontend.md). No frontend automated tests this milestone.
