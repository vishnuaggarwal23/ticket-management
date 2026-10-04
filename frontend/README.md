# Ticket Management — frontend

React + Next.js (App Router) + Vite (shared `src/` dev). JavaScript only.

## Prerequisites

- **Node.js 24.x** (see `.nvmrc`)
- Backend API (default **http://127.0.0.1:8080**) — `cd backend && ./mvnw spring-boot:run` with Postgres configured

## Setup

```bash
cd frontend
cp .env.example .env.local   # recommended — direct :8080 API; needed for slow local Ask/Ollama
npm install
```

## Development

```bash
npm run dev
```

Opens **http://localhost:3000** by default. With `NEXT_PUBLIC_API_BASE_URL` set (see `.env.example`), the browser calls the backend on **8080** directly. If that variable is empty, Next.js proxies `/api/*` via rewrites (`BACKEND_REWRITE_URL`); that proxy times out around **30 seconds**, which breaks **Ask** when local Ollama generation is slow.

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

## UI architecture

| Area | Location | Notes |
|------|----------|--------|
| Routes | `app/` | `/` → `/tickets`; `/tickets/new`, `/tickets/[id]`, `/ask` (**DEC-20**) |
| HTTP client | `src/api/` | Single `client.js` + `tickets.js` / `ask.js`; `{ data }`, `{ error }`, list `meta` |
| Components | `src/components/` | Screens, table, forms, ask panel, breadcrumbs, skeletons |
| Helpers | `src/lib/` | Status/priority badges, list query defaults, patch/dirty helpers |
| Styles | `app/globals.css` | Design tokens (teal/slate), no Tailwind/MUI (**Convention**) |
| Typography | `app/layout.js` | **Inter** via `next/font/google` |

### UX patterns (aligned with [`spec/ui-flow.md`](../spec/ui-flow.md))

- **Navigation** — Sticky header with **Tickets** / **Ask** and a global **New ticket** CTA; breadcrumbs on list, detail, create, and ask.
- **List** — One-click **status chips**; search form still supports **Enter** and status dropdown (**DEC-20** Apply + Enter); explicit `sort=createdAt,desc` on API calls; numbered pagination when `totalPages` ≤ 20.
- **List → detail** — Entire table row opens the ticket (click or Enter/Space), not only ID/title links.
- **Detail** — Field **Save** is separate from **status** buttons (**DEC-06** / **DEC-20**). Sticky bar when the form is dirty (Save / Discard, **⌘/Ctrl+S**). Status moves blocked until edits are saved or discarded.
- **Comments** — Thread scrolls into view after a successful post.
- **Ask** — Example questions run **one-click** (submit immediately). `/ask?prefill=…` pre-fills the textarea (e.g. from detail **Ask about this**). Grounded vs no-match styling per **DEC-11**.
- **Loading** — Route-level skeletons (`app/**/loading.js`) and an inline spinner during Ask.

## Demo walkthrough (UI — hub §8.7)

With backend on **8080** and `npm run dev` on **3000** (prefer `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080` in `.env.local`):

1. **List** — Open `/tickets`; skeleton then table or empty state.
2. **Create** — Header **New ticket** (or empty-state CTA) → title required → lands on detail with `TKT-{n}`.
3. **Validation** — New ticket with empty title → field/`error.details` message.
4. **Filter** — Click a **status chip** (one click) or search + **Search** / Enter; **Reset all** clears filters.
5. **Open ticket** — Click any list row → detail with breadcrumbs.
6. **Field save** — Edit fields → sticky **Save** (or **⌘/Ctrl+S**) → reload shows persisted values.
7. **Comment** — Post comment → appears in thread; page scrolls to comments.
8. **Status** — Use **Move to …** buttons only (no reopen from terminal states); **409** shows readable message if backend rejects.
9. **Ask (grounded)** — `/ask` → one-click example or custom question → answer + citation links to tickets.
10. **Ask (no-match)** — Obscure question → **200** honest answer, empty citations (not HTTP error styling).
11. **Ask (validation)** — Blank question → client or **400** validation message.

Full grader script: [`spec/requirements.md`](../spec/requirements.md) **§8.7** and [`spec/ui-flow.md`](../spec/ui-flow.md) **§12**.

## Review

UI changes: [`commands/review-frontend.md`](../commands/review-frontend.md). No frontend automated tests this milestone.

## Changelog (implementation notes)

**Convention:** Product behaviour remains defined by `spec/ui-flow.md`; this section records **what changed in the repo** after initial UI-L delivery (2026-10-04).

### Review-driven fixes

| Change | Files |
|--------|--------|
| Ask integration test expects `VALIDATION_ERROR` (not `BAD_REQUEST`) for unknown JSON properties on ask, matching `RestExceptionHandler` and [`spec/rag-api-contract.md`](../spec/rag-api-contract.md) **AC-RAG-API-01** | `backend/…/AskApiIT.java` (backend; see [`backend/README.md`](../backend/README.md)) |
| Removed unused `RouteStub.jsx` | deleted |
| List API sends explicit `sort=createdAt,desc`; pagination URLs include `sort` | `app/tickets/page.js`, `src/lib/ticketsListQuery.js`, `src/lib/ticketsListHref.js` |
| Pagination page-number links (≤ 20 pages) | `TicketListPagination.jsx`, `globals.css` |
| Softer Ask timeout copy (no framework name in primary message) | `AskPanel.jsx` |

### UX / visual pass (cohesive UI)

| Theme | What |
|-------|------|
| **Design system** | CSS variables (teal/slate), panels, focus rings, status/priority badge colors, responsive stacked table |
| **Typography** | Inter + monospace ticket IDs |
| **Fewer clicks** | Header **New ticket**; status filter chips; clickable rows; one-click Ask examples; detail → `/ask?prefill=` |
| **Flow** | Breadcrumbs; dirty-form sticky bar; block status until save/discard; comment scroll-into-view |
| **Loaders** | `Skeleton.jsx`, `ListPageSkeleton`, `DetailPageSkeleton`; `ask/loading.js` |
| **New components** | `Breadcrumbs.jsx`, `StatusFilterChips.jsx`, `isTicketFormDirty.js` |
| **Refactors** | `TicketTable.jsx` (client, row navigation); `SiteHeader.jsx` (active nav + CTA); expanded `TicketDetailPanel.jsx`, `AskPanel.jsx`, `TicketListToolbar.jsx` |
