# Frontend guidelines

Cursor attaches this file via [`.cursor/rules/frontend.mdc`](../.cursor/rules/frontend.mdc) (pointer only). Edit **this** file; do not copy the body into the `.mdc`.

React UI for tickets and grounded Q&A. Product screens and click-by-click flows: [`spec/ui-flow.md`](../spec/ui-flow.md) (**§0** PDF ui-flow map; **AC-UI-***). Stack **agreed** **DEC-15** (React + Next.js + Vite + JavaScript; Node.js **24.x** Active LTS); UX **DEC-20** ([`ui-flow.md`](../spec/ui-flow.md) §4, §7); transition API **agreed** **DEC-06** (PATCH `status`). Out-of-PDF UI (**Reference** — do not build): [`spec/requirements.md`](../spec/requirements.md) **§2.3**. UI architecture summary: [`spec/architecture.md`](../spec/architecture.md) **§12**. Hub journeys: [`spec/requirements.md`](../spec/requirements.md) Flows A–E, **§0.5**, and demo **§8.7**. HTTP envelopes and paths: `rules/api-standards.md`. Ask grounding: `rules/rag-vector-store.md`. These rules do **not** replace those specs.

**This milestone: do not write frontend test cases** (no Vitest, Testing Library, Playwright, Cypress, or other UI test suite). Backend tests remain in `rules/testing.md`. UI review: `commands/review-frontend.md`.

| Read first | Purpose |
|------------|---------|
| [`spec/requirements.md`](../spec/requirements.md) | **AC-CORE-*** UI checklist (§8); demo walkthrough **§8.7**; journeys **§4.1** |
| [`spec/architecture.md`](../spec/architecture.md) | UI functional areas and API usage (§12); client communication (§10) |
| `rules/api-standards.md` | `/api/v1`, envelopes, PATCH, list params, ask paths |
| [`spec/api-contract.md`](../spec/api-contract.md) | Ticket/comment JSON shapes and error cases for API client validation |
| [`spec/ui-flow.md`](../spec/ui-flow.md) | Screens, CRUD flows, ask/RAG UX, **AC-UI-*** |
| [`spec/architecture.md`](../spec/architecture.md) §12 | Frontend architecture summary |
| [`spec/rag-api-contract.md`](../spec/rag-api-contract.md) | Citation / no-match fields inside ask `data` (**DEC-11** agreed) |

## Assessment vs project conventions vs open decisions

**Assessment PDF requires** (UI **capabilities**, not a frontend stack):

- Create, list, view, and update tickets (**title, description, priority, assignee**).
- Add and view comments.
- Search by keyword; filter by status.
- Show ticket **status**; users can take **valid** status transitions (backend still **enforces** the machine).
- Show **meaningful** validation and API errors.
- Ask natural-language questions over ticket history; show a **grounded** answer with **ticket ID** citations, or an honest **no relevant tickets found** (or equivalent).
- No agent UI from ask (no create-ticket / notify / tool-chain from the question box).

The PDF names React/Next.js **or equivalent**. It does **not** mandate Vite, JavaScript vs TypeScript, a CSS kit, or a frontend test tool.

**This project’s approved conventions** (not PDF mandates):

- **Node.js** **24.x** Active LTS (**Krypton**) — when a frontend package is added, pin in `.nvmrc` and `package.json` `engines` (e.g. **24.21.x**) compatible with the chosen Next.js GA release
- **No `frontend/` tree in this repo** until an explicit user request or agreed plan/tasks item authorizes UI implementation — these rules describe conventions only
- **React** + **Next.js** (App Router) + **Vite** + **JavaScript** (`.js`/`.jsx`; no TypeScript in frontend)
- **Next.js** owns routing, pages, and production build (`next dev` on **3000** by default)
- **Vite** owns fast dev/HMR and optional dev proxy for shared modules under `frontend/src/` (especially the HTTP client); default Vite dev port **5173**
- API base URL via **`NEXT_PUBLIC_*`** in the Next app and **`VITE_*`** when running shared modules under Vite dev — committed examples list **variable names only**
- Call versioned ticket APIs and envelopes from `rules/api-standards.md` (`/api/v1`, `{ data }`, `{ error }`, list `meta`)
- Preserve `POST /api/ai/ask` with `{ "question": "..." }` (also `/api/v1/ai/ask`)

**Agreed data shape — [`spec/data-model.md`](../spec/data-model.md) (shapes in API client JSDoc or inline validation):**

- Ticket **id** display/link: `TKT-{n}` strings from API (DEC-04)
- Optional **category** enum; optional **resolutionNotes** on detail/edit (DEC-03, DEC-05)
- Create: **title** required; priority/assignee/category optional; no status on create form (DEC-07, DEC-13)

**Implementer choice (in scope — not blocking DEC):** screen layout, transition control widget, ask page vs panel, CSS kit, client global store — must satisfy demo [`spec/requirements.md`](../spec/requirements.md) **§8.7** / Flow B. Next.js App Router file routes replace a separate client-only router unless spec agrees otherwise.

**Reference — do not build UI for:** auth screens, delete ticket, agent actions from ask, confidence badges, vector/chunk debug consoles ([`spec/requirements.md`](../spec/requirements.md) **§2.3**).

**Ask display:** use `data.answer` + `data.citedTicketIds` per [`spec/rag-api-contract.md`](../spec/rag-api-contract.md) §7 (**DEC-11**).

## Stack

- **Runtime:** Node.js **24.x** Active LTS only for frontend tooling and `next dev` / `next build`.
- **UX (**DEC-20**):** Routes `/`, `/tickets`, `/tickets/new`, `/tickets/[id]`, `/ask`; dedicated ask page; list search Apply+Enter; pagination from `meta`; detail Save vs status buttons — see [`ui-flow.md`](../spec/ui-flow.md).
- **UI:** React function components; **JavaScript** (`.js`/`.jsx`). Prefer JSDoc on the API client for DTO shapes that match [`spec/api-contract.md`](../spec/api-contract.md); do not invent parallel field names.
- **Application shell:** **Next.js** App Router (`frontend/app/`). Use **`next.config.js`** rewrites/proxy to backend when that is simpler than a Vite-only proxy for page routes.
- **Shared modules:** **Vite** (`frontend/vite.config.js`) for developing/importing shared code under `frontend/src/` (HTTP client, presentational components). Next transpiles imports from `src/`; do not maintain a second competing SPA entry unless an agreed spec says so.
- Do not add Remix, Angular, or a second UI framework.
- Do not add Tailwind, MUI, Redux, or Zustand unless the user or an agreed spec says so.
- Do not add a frontend test runner or write `.test.jsx` / e2e UI tests.

## Structure

- Separate **presentation**, **HTTP access**, and **view logic** when that keeps files maintainable.
- **One** HTTP module (fetch or a thin wrapper) — no `fetch`/`axios` copied into every screen.
- Map API success `{ data, meta? }` and error `{ error }` in that module. Surface `error.message` and field `details` on forms. Do not parse a second ad-hoc JSON shape.
- Hold state at the screen that owns it. No required global store.
- Do **not** freeze a folder tree here. When implementation is authorized, prefer `app/` (Next routes) plus `src/` (shared JS) under the chosen frontend root (often `frontend/`).

### Suggested layout (convention when implemented — not present in repo today)

```
frontend/
  .nvmrc                  # 24 — Active LTS major
  package.json            # engines.node >=24 <25
  app/                    # Next.js App Router (pages, layouts)
  src/
    api/                  # fetch wrapper, envelope parsing
    components/           # reusable UI
    lib/                  # optional helpers
  next.config.js
  vite.config.js          # shared-module dev / proxy (port 5173)
  .env.example            # NEXT_PUBLIC_* and VITE_* names only — no secrets
```

### Environment and API base URL

When the frontend package exists, committed **`.env.example`** lists names only:

```bash
# Next.js client (browser)
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080

# Vite dev for shared src/ modules (optional local HMR)
VITE_API_BASE_URL=http://localhost:8080
```

Next.js client usage:

```javascript
const base = process.env.NEXT_PUBLIC_API_BASE_URL ?? '';
const res = await fetch(`${base}/api/v1/tickets?page=0&size=20`);
const body = await res.json();
if (!res.ok) throw new ApiError(body.error);
return body.data;
```

Shared module under Vite dev:

```javascript
const base = import.meta.env.VITE_API_BASE_URL;
const res = await fetch(`${base}/api/v1/tickets?page=0&size=20`);
```

Use **`POST /api/v1/ai/ask`** for ask (see `rules/api-standards.md`). Dev proxy options:

- **Next.js** `rewrites` in `next.config.js` to `http://localhost:8080`, or
- **Vite** `server.proxy` in `vite.config.js` when exercising `src/` without Next:

```javascript
// vite.config.js — optional; adjust target to backend
export default {
  server: {
    proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } },
  },
};
```

If proxy is used, public base URL env vars can be empty and paths stay relative `/api/v1/...`.

Backend CORS for local dev allows **`http://localhost:3000`** (Next) and **`http://localhost:5173`** (Vite) per `WebCorsConfig` — not `*` as a permanent default.

## API usage

- Tickets: `GET/POST /api/v1/tickets`, `GET/PATCH /api/v1/tickets/{id}`, `POST .../comments` per api-standards. List: `page`, `size`, `sort`, `q`, `status`.
- Updates: **PATCH**, not PUT.
- Create: expect **201** and follow `Location` or `data` as the contract defines.
- Empty list: **200** + empty `data` — show an empty state, not a fake error.
- 400 validation, 404 not found, 409 illegal transition: show `error.message` (and `details` when present). Backend is source of truth for illegal status.
- UI may disable obvious illegal transitions as **guidance only**.
- Ask: POST JSON `{ "question" }` to `/api/v1/ai/ask`. Loading + result. Citations = ticket ids from `data` (per [`spec/rag-api-contract.md`](../spec/rag-api-contract.md)). No-match: show API text honestly — e.g. “No relevant tickets found” — not a dressed-up model essay.
- Do not hardcode machine hosts. Do not commit secrets.

## Capabilities the UI must support (when `ui-flow.md` agrees how)

| Capability | Notes |
|------------|--------|
| Create | Title, description, priority, assignee |
| List | Search keyword, filter status, pagination from `meta` if the screen lists pages |
| Detail | Status, fields, comments |
| Update | Same fields; PATCH |
| Transition | Valid moves only as guidance; show 409 if backend rejects |
| Comments | Add and view |
| Errors | Meaningful, user-safe; never stack traces or SQL |
| Ask | Grounded answer + ticket id citations **or** no relevant tickets found |

## Usability

- Semantic HTML; labels on inputs; keyboard-usable controls.
- Loading, empty, and error states for list and ask.
- Do not discard form input silently on validation failure.
- Do not prescribe theme or branding.

## RAG display

- Distinguish assistant text from ticket description/comments.
- Show cited ticket IDs from the API; do not invent ids in the client.
- Honest no-match. No confidence badges unless a spec adds them.

## Testing (skipped)

- **Do not generate frontend unit, component, or e2e tests** in this milestone.
- Keep the HTTP client separable from JSX so tests could be added later if approved.
- `/generate-tests` and `rules/testing.md` apply to the **backend** only.

## Spec-driven implementation

- Implement behaviour from [`spec/ui-flow.md`](../spec/ui-flow.md) plus API/RAG contracts. For screens or flows not covered there, **stop and confirm** rather than inventing.
- Do not add auth screens unless a spec agrees.

## Do not

- Do not create, scaffold, or commit a `frontend/` directory (or other UI implementation) unless the user or an agreed spec/plan explicitly requests it.
- Do not treat React/Next.js/Vite/JavaScript as PDF mandates.
- Do not reintroduce TypeScript in the frontend without an agreed spec change.
- Do not write frontend test files or pick a UI test framework.
- Do not duplicate the state machine as the only enforcement.
- Do not invent API fields, citation JSON, or agent actions from the ask box.

---

## Revision history

| Date | Note |
|------|------|
| 2026-10-03 | Initial React + Vite + TypeScript UI conventions and ticket/ask display rules. |
| 2026-10-04 | SDD expansion: PDF vs convention; links to `spec/ui-flow.md` and API client patterns. |
| 2026-10-04 | Synced with expanded [`spec/requirements.md`](../spec/requirements.md) and [`spec/architecture.md`](../spec/architecture.md) §12. |
| 2026-10-04 | Added revision history section. |
| 2026-10-04 | Ticket id, category, resolutionNotes, create requiredness from agreed [`spec/data-model.md`](../spec/data-model.md). |
| 2026-10-04 | API client shapes: [`spec/api-contract.md`](../spec/api-contract.md) (**agreed**). |
| 2026-10-04 | UI flows: requirements §4.1 / §8.7 + architecture §12.3–§12.6 (consolidated PDF `ui-flow` themes). |
| 2026-10-04 | Product UI detail → [`spec/ui-flow.md`](../spec/ui-flow.md); architecture §12 summary. |
| 2026-10-04 | `improve-from-assessment-pdf`: PDF UI spec is [`spec/ui-flow.md`](../spec/ui-flow.md). |
| 2026-10-04 | Pointers to [`ui-flow.md`](../spec/ui-flow.md) **§0** and requirements **§0.5**. |
| 2026-10-04 | **DEC-15**/**DEC-11** agreed; **Reference** §2.3 — no UI for out-of-PDF features. |
| 2026-10-04 | **DEC-15** revised: **React + Next.js + Vite + JavaScript**; Node.js **24.x** Active LTS; Next App Router + shared `src/` via Vite; CORS **3000** + **5173**. |
| 2026-10-04 | Removed mistaken `frontend/` scaffold; stack rules are documentation-only until UI implementation is requested. |
| 2026-10-04 | Hygiene: **DEC-20** UX; stale **draft** spec pointers removed from steering cross-links. |
