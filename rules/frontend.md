# Frontend guidelines

Cursor attaches this file via [`.cursor/rules/frontend.mdc`](../.cursor/rules/frontend.mdc) (pointer only). Edit **this** file; do not copy the body into the `.mdc`.

React UI for tickets and grounded Q&A. Product screens and click-by-click flows: [`spec/ui-model.md`](../spec/ui-model.md) (**§0** PDF ui-flow map; **AC-UI-***). Stack **agreed** **DEC-15** (React+Vite+TS); transition API **agreed** **DEC-06** (PATCH `status`). Out-of-PDF UI (**Reference** — do not build): [`spec/requirements.md`](../spec/requirements.md) **§2.3**. UI architecture summary: [`spec/architecture.md`](../spec/architecture.md) **§12**. Hub journeys: [`spec/requirements.md`](../spec/requirements.md) Flows A–E, **§0.5**, and demo **§8.7**. HTTP envelopes and paths: `rules/api-standards.md`. Ask grounding: `rules/rag-vector-store.md`. These rules do **not** replace those specs.

**This milestone: do not write frontend test cases** (no Vitest, Testing Library, Playwright, Cypress, or other UI test suite). Backend tests remain in `rules/testing.md`. UI review: `commands/review-frontend.md`.

| Read first | Purpose |
|------------|---------|
| [`spec/requirements.md`](../spec/requirements.md) | **AC-CORE-*** UI checklist (§8); demo walkthrough **§8.7**; journeys **§4.1** |
| [`spec/architecture.md`](../spec/architecture.md) | UI functional areas and API usage (§12); client communication (§10) |
| `rules/api-standards.md` | `/api/v1`, envelopes, PATCH, list params, ask paths |
| [`spec/api-contract.md`](../spec/api-contract.md) | Ticket/comment JSON shapes and error cases for API client types |
| [`spec/ui-model.md`](../spec/ui-model.md) | Screens, CRUD flows, ask/RAG UX, **AC-UI-*** |
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

The PDF names React/Next.js **or equivalent**. It does **not** mandate Vite, TypeScript, Next.js, a CSS kit, a router, or a frontend test tool.

**This project’s approved conventions** (not PDF mandates):

- **React** + **Vite** + **TypeScript**
- **No Next.js**
- API base URL via Vite env (`import.meta.env.VITE_*`) — committed examples list **variable names only**
- Call versioned ticket APIs and envelopes from `rules/api-standards.md` (`/api/v1`, `{ data }`, `{ error }`, list `meta`)
- Preserve `POST /api/ai/ask` with `{ "question": "..." }` (also `/api/v1/ai/ask`)

**Agreed data shape — [`spec/data-model.md`](../spec/data-model.md) (types in API client):**

- Ticket **id** display/link: `TKT-{n}` strings from API (DEC-04)
- Optional **category** enum; optional **resolutionNotes** on detail/edit (DEC-03, DEC-05)
- Create: **title** required; priority/assignee/category optional; no status on create form (DEC-07, DEC-13)

**Implementer choice (in scope — not blocking DEC):** screen layout, navigation, transition control widget, ask page vs panel, CSS kit, router, client global store — must satisfy demo [`spec/requirements.md`](../spec/requirements.md) **§8.7** / Flow B.

**Reference — do not build UI for:** auth screens, delete ticket, agent actions from ask, confidence badges, vector/chunk debug consoles ([`spec/requirements.md`](../spec/requirements.md) **§2.3**).

**Ask display:** use `data.answer` + `data.citedTicketIds` per [`spec/rag-api-contract.md`](../spec/rag-api-contract.md) §7 (**DEC-11**).

## Stack

- SPA with Vite; React function components; TypeScript for props, API types, and state.
- Avoid `any` and unsafe assertions where practical. Prefer types that match API DTO records (`data` payloads), not invented parallel models.
- Do not add Next.js, Remix, or a second UI framework.
- Do not add Tailwind, MUI, React Router, Redux, or Zustand unless the user or an agreed spec says so.
- Do not add a frontend test runner or write `.test.tsx` / e2e UI tests.

## Structure

- Separate **presentation**, **HTTP access**, and **view logic** when that keeps files maintainable.
- **One** HTTP module (fetch or a thin wrapper) — no `fetch`/`axios` copied into every screen.
- Map API success `{ data, meta? }` and error `{ error }` in that module. Surface `error.message` and field `details` on forms. Do not parse a second ad-hoc JSON shape.
- Hold state at the screen that owns it. No required global store.
- Do **not** freeze a folder tree here. Prefer a small Vite `src/` with screens + `api` client when implementing.

### Suggested layout (convention, not mandatory)

```
frontend/                 # or repo root if monorepo — pick one and document in README
  src/
    api/                  # fetch wrapper, types for envelopes
    screens/              # route-level pages
    components/           # reusable UI
    App.tsx
  .env.example            # VITE_API_BASE_URL only — no secrets
```

### Environment and API base URL

Committed **`.env.example`** (names only):

```bash
# Backend origin for dev (Vite proxies or calls directly)
VITE_API_BASE_URL=http://localhost:8080
```

Client usage:

```typescript
const base = import.meta.env.VITE_API_BASE_URL;
const res = await fetch(`${base}/api/v1/tickets?page=0&size=20`);
const body = await res.json();
if (!res.ok) throw new ApiError(body.error);
return body.data;
```

Use **`POST /api/v1/ai/ask`** for ask (see `rules/api-standards.md`). Dev proxy in `vite.config.ts` is optional:

```typescript
server: {
  proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } },
},
```

If proxy is used, `VITE_API_BASE_URL` can be empty and paths stay relative `/api/v1/...`.

## API usage

- Tickets: `GET/POST /api/v1/tickets`, `GET/PATCH /api/v1/tickets/{id}`, `POST .../comments` per api-standards. List: `page`, `size`, `sort`, `q`, `status`.
- Updates: **PATCH**, not PUT.
- Create: expect **201** and follow `Location` or `data` as the contract defines.
- Empty list: **200** + empty `data` — show an empty state, not a fake error.
- 400 validation, 404 not found, 409 illegal transition: show `error.message` (and `details` when present). Backend is source of truth for illegal status.
- UI may disable obvious illegal transitions as **guidance only**.
- Ask: POST JSON `{ "question" }` to `/api/v1/ai/ask`. Loading + result. Citations = ticket ids from `data` (per [`spec/rag-api-contract.md`](../spec/rag-api-contract.md)). No-match: show API text honestly — e.g. “No relevant tickets found” — not a dressed-up model essay.
- Do not hardcode machine hosts. Do not commit secrets.

## Capabilities the UI must support (when `ui-model.md` agrees how)

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

- Implement behaviour from [`spec/ui-model.md`](../spec/ui-model.md) plus API/RAG contracts. For screens or flows not covered there, **stop and confirm** rather than inventing.
- Do not add auth screens unless a spec agrees.

## Do not

- Do not treat React/Vite/TypeScript as PDF mandates.
- Do not use Next.js.
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
| 2026-10-04 | API client types: draft [`spec/api-contract.md`](../spec/api-contract.md). |
| 2026-10-04 | UI flows: requirements §4.1 / §8.7 + architecture §12.3–§12.6 (consolidated PDF `ui-flow` themes). |
| 2026-10-04 | Product UI detail → [`spec/ui-model.md`](../spec/ui-model.md); architecture §12 summary. |
| 2026-10-04 | `improve-from-assessment-pdf`: PDF `ui-flow.md` → `ui-model.md` (not `spec/ui-flow.md`). |
| 2026-10-04 | Pointers to [`ui-model.md`](../spec/ui-model.md) **§0** and requirements **§0.5**. |
| 2026-10-04 | **DEC-15**/**DEC-11** agreed; **Reference** §2.3 — no UI for out-of-PDF features. |
