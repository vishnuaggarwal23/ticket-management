# Review frontend

Cursor attaches this file via [`.cursor/commands/review-frontend.md`](../.cursor/commands/review-frontend.md) (pointer only). Edit **this** file; do not copy the body into `.cursor/commands/`.

Review the current **UI** change set (or specified paths) against **all steering artefacts** in **Inputs**: **`spec/`** (product behaviour), **`rules/`** (engineering conventions), related **`commands/`**, applicable **`skills/`**, and relevant **`docs/`**. Written code must meet **§2–§5** in addition to capability and API checks in **§1**.

**How artefacts fit together:** [`spec/ui-flow.md`](../spec/ui-flow.md) is the authoritative **product UI** spec; [`spec/architecture.md`](../spec/architecture.md) **§12** summarizes frontend modules — when **§12** and `ui-flow.md` differ on behaviour, **`ui-flow.md` wins**. Payloads, endpoints, and enums → [`spec/api-contract.md`](../spec/api-contract.md), [`spec/data-model.md`](../spec/data-model.md); status UX → [`spec/state-machine.md`](../spec/state-machine.md); ask `data` → [`spec/rag-api-contract.md`](../spec/rag-api-contract.md). **Stack, HTTP client patterns, folder conventions, and RAG display rules** → `rules/frontend.md`, `rules/api-standards.md`, `rules/rag-vector-store.md` (must **align** with specs, not replace them). Assignment boundaries and process evidence → `docs/` (see **Inputs**). Index and writing bar → [`rules/documentation.md`](../rules/documentation.md), [`skills/documentation/SKILL.md`](../skills/documentation/SKILL.md).

**Do not generate or require frontend tests.** Do not apply fixes unless the user asks.

**PDF UI capabilities to verify (when implemented):** create/list/detail/update fields, comments, keyword search, status filter, status display + transition UX, meaningful API errors, ask with citations or honest no-match (`rules/frontend.md` table). Backend still enforces the state machine — UI is guidance only.

## Inputs

- Diff / named frontend paths
- Assessment PDF ([`docs/Assessments.docx`](../docs/Assessments.docx)) only as **background** for capabilities (not stack) — restated in [`spec/requirements.md`](../spec/requirements.md)

### `rules/` (engineering conventions — required)

| Rule | Frontend review use |
|------|---------------------|
| [`rules/frontend.md`](../rules/frontend.md) | Stack (**DEC-15**), structure, API usage, capabilities table, usability, RAG display, spec-driven implementation, **do not** list |
| [`rules/api-standards.md`](../rules/api-standards.md) | `/api/v1`, `{ data }` / `{ error }` / list `meta`, PATCH, list params, ask paths, status codes |
| [`rules/rag-vector-store.md`](../rules/rag-vector-store.md) | Ask grounding themes; UI must not pretend to be the vector store or expose ingest internals |
| [`rules/documentation.md`](../rules/documentation.md) | Where artefacts live; rules/commands index; do not contradict agreed **DEC-***; AI mistake log process |

### `commands/` (workflows — use when relevant)

| Command | When during frontend review |
|---------|----------------------------|
| **This file** — [`commands/review-frontend.md`](review-frontend.md) | Checklist for the UI diff |
| [`commands/review-rag-output.md`](../commands/review-rag-output.md) | Ask UI present: citations/no-match claims must be traceable to API ids (same bar as manual ask review) |
| [`commands/review-spec.md`](../commands/review-spec.md) | UI implements behaviour with **no** matching `spec/` section — report **spec gap** (do not rewrite spec unless user asks) |
| [`commands/update-ai-error.md`](../commands/update-ai-error.md) | Not a UI code checklist — cite [`docs/ai-error.md`](../docs/ai-error.md) only if UI wrongly surfaces internal AI/debug artefacts |

### `skills/`

| Skill | Frontend review use |
|-------|---------------------|
| [`skills/documentation/SKILL.md`](../skills/documentation/SKILL.md) | When the diff includes **UI-facing docs** (e.g. `frontend/README.md`): clarity, links to `spec/ui-flow.md`, no secrets, PDF vs convention labels |

### `docs/` (human docs — boundaries and hub)

| Doc | Frontend review use |
|-----|---------------------|
| [`docs/assessment-brief.md`](../docs/assessment-brief.md) | PDF theme hub; **§13** traceability — UI capabilities not weakened vs assignment |
| [`docs/ai-error.md`](../docs/ai-error.md) | Process evidence (**AC-CORE-23**) — **not** an in-app product screen ([`ui-flow.md`](../spec/ui-flow.md) §4.2) |
| [`docs/prompt-history.md`](../docs/prompt-history.md) | Process evidence — **not** in-app UI |
| [`docs/ai-mistakes.md`](../docs/ai-mistakes.md) | Pointer to `ai-error.md` — same as above |

### `spec/` (product behaviour — required cross-check)

Read every row below that applies to touched screens:

| Spec | Frontend review use |
|------|---------------------|
| [`spec/ui-flow.md`](../spec/ui-flow.md) | **Primary** — **§0** map; **§4** routes/nav; **§5–§6** modules + global behaviour; **§7** screen catalog; **§8** CRUD; **§9** status UX; **§10** ask/RAG; **§11** flows A–E; **§12** demo checklist; **§13** **AC-UI-01…12** |
| [`spec/requirements.md`](../spec/requirements.md) | **AC-CORE-01…11** (and ask **16–18** where ask UI exists); **§4.1** flows A–E; **§8.7** demo steps; **§2.3 Reference** (must not appear as product UI); **§10** **DEC-*** only as recorded — do not invent decisions |
| [`spec/architecture.md`](../spec/architecture.md) | **§12.1–§12.5** — functional areas, client modules, screen map, transition UX, ask panel (detail still in `ui-flow.md`) |
| [`spec/api-contract.md`](../spec/api-contract.md) | **§3** `TicketSummary` / `TicketDetail` / comment / error shapes; **§4–§5** ticket + comment HTTP; **§6** ask boundary (envelopes in `rules/api-standards.md`) |
| [`spec/data-model.md`](../spec/data-model.md) | **§5** enums (`TicketStatus`, priority **DEC-13**, category **DEC-03**); **DEC-04** `TKT-{n}`; **DEC-07** no client `status` on create; **DEC-08** search scope (title/description — not comments in `q`) |
| [`spec/state-machine.md`](../spec/state-machine.md) | **§5.1** T1–T5 legal edges; **§5.2** X1–X3 / terminal states; **§6** PATCH `status` (**DEC-06**); UI legal-target buttons must match matrix |
| [`spec/rag-api-contract.md`](../spec/rag-api-contract.md) | **§6–§7** request/response; **§13** client/UI obligations; **DEC-11** no-match **`data.answer`** |
| [`spec/rag-ingestion.md`](../spec/rag-ingestion.md) | **§10** — no ingest admin UI; freshness verified via ticket edits + ask (**ui-flow.md** §10) |
| [`spec/evaluation-strategy.md`](../spec/evaluation-strategy.md) | No eval/debug UI in app (**ui-flow.md** §4.2) |
| [`spec/test-strategy.md`](../spec/test-strategy.md) | Trace **AC-CORE-*** / **AC-UI-*** to product behaviour only — **do not** require frontend tests |

Mark each item **Pass** / **Fail** / **N/A**. Failures: **blocker** / **major** / **minor** with file references and a **steering citation** (`spec/…` §, `rules/…` section, or `docs/…` § as applicable).

---

## 1. Validity (capabilities vs invented UI)

### Spec and scope

- [ ] Only agreed [`ui-flow.md`](../spec/ui-flow.md) screens/flows (or user-confirmed deltas). Use requirements **§4.1** / **§8.7** as hub — no invented auth, agent-from-ask, extra ticket resources, delete-ticket UI
- [ ] **DEC-06** PATCH `status` UX; **DEC-15** stack; **DEC-20** routes/UX agreed; **do not** build **Reference** UI (§2.3); **do not** invent id/category (use [`spec/data-model.md`](../spec/data-model.md))
- [ ] PDF capabilities present or explicitly deferred; when implemented, mappable to **`AC-CORE-01…11`** (§8) and **`AC-UI-01…12`** ([`ui-flow.md`](../spec/ui-flow.md) §13): create, list, detail, update title/description/priority/assignee, comments, keyword search, status filter, status display, valid transitions, meaningful errors, ask with citations or no-match

### Stack (`rules/frontend.md`)

- [ ] React + Next.js + Vite + JavaScript (**DEC-15**); Node.js **24.x** LTS; no frontend test files or new UI test dependencies
- [ ] API base via `NEXT_PUBLIC_*` (Next) and/or `VITE_*` (shared modules); no machine-specific hosts or secrets

### API client

- [ ] Calls `/api/v1` ticket routes; **PATCH** not PUT; ask `POST /api/ai/ask` and/or `/api/v1/ai/ask` with `{ "question" }`
- [ ] Parses `{ data }` / list `{ data, meta }` / `{ error }` — no second envelope
- [ ] Empty list treated as empty UI, not as failure
- [ ] 400 / 404 / 409 messages shown from `error` (field `details` when present)
- [ ] Does not treat UI disablement as the only status-machine enforcement

### Ask / RAG display

- [ ] Assistant output visually distinct from ticket text
- [ ] Citations are ticket IDs from the API, not client-invented
- [ ] No-match shown honestly; not dressed as verified facts
- [ ] No create-ticket / notify from the ask box

---

## 2. Structure and file organization

- [ ] Presentation vs HTTP client vs view logic not all in one mega-file when that hurts maintenance
- [ ] Single HTTP module (or equivalent), not `fetch` in every component
- [ ] JSDoc or clear prop validation for API `data` payloads; avoid untyped `any` unless justified
- [ ] No unapproved CSS/router/state libraries unless the user agreed
- [ ] Files grouped under logical packages/folders (e.g. Next `app/` routes, shared `src/api`, `src/components`, `src/lib`) — not a flat dump of unrelated concerns
- [ ] Route and page files stay thin; reusable UI and non-route logic live in shared modules under `src/` per `rules/frontend.md`
- [ ] Naming: components PascalCase; modules/helpers camelCase files; env and config names match stack (`NEXT_PUBLIC_*`, `VITE_*`, `next.config.js`, `vite.config.js`)

---

## 3. Quality (no frontend tests)

- [ ] Labels, semantic HTML, keyboard-usable primary actions
- [ ] Loading, empty, and error states for list and ask
- [ ] Forms do not silently drop invalid input
- [ ] User-visible errors never include stack traces, SQL, or internals
- [ ] No secrets in source or committed env files

**Skip:** coverage, Playwright/Vitest, snapshot tests.

---

## 4. Code quality and stack conventions

Review **readability**, **modularity**, **API error handling**, and **React state** against `rules/frontend.md`. Fail when code is clever but opaque, duplicated, or scatters HTTP/error logic across components.

### JavaScript

- [ ] Modern ES modules (`import`/`export`); consistent semicolon/quote style with the rest of the tree
- [ ] `const`/`let` only; no `var`; avoid mutable shared module-level state for request/UI data
- [ ] Async API work uses `async`/`await` with explicit error paths — not unhandled promise rejections
- [ ] Names describe intent (functions as verbs, data as nouns); no misleading abbreviations
- [ ] Self-explanatory control flow; comments only for non-obvious business or integration rules — not narrating obvious lines

### React

- [ ] Function components and hooks; no class components unless an existing pattern requires it
- [ ] Hooks rules respected (stable deps, no conditional hooks); derived UI computed in render or `useMemo` only when needed
- [ ] Props and local state have a clear owner; lift state only when siblings need it — no prop-drilling through many layers when a small shared helper or context (if ever approved) would match spec
- [ ] Event handlers and effects are small; side effects isolated in `useEffect` with correct dependencies
- [ ] Lists use stable `key`s; conditional render paths do not drop user input silently (see **§3**)
- [ ] No duplicate fetch-on-mount in parent and child for the same resource

### Next.js

- [ ] App Router conventions: routes under `app/`, layouts/pages colocated appropriately; `"use client"` only where client hooks or browser APIs are required
- [ ] `next.config.js` rewrites/proxy and env usage match `rules/frontend.md` — no ad-hoc second routing system
- [ ] Client-only code does not import server-only APIs; public env via `NEXT_PUBLIC_*` only for browser-safe values
- [ ] Link/navigation uses Next primitives (`Link`, `useRouter`) for in-app routes agreed in **DEC-20** / [`ui-flow.md`](../spec/ui-flow.md)

### Vite

- [ ] Shared modules under `frontend/src/` importable from Next without duplicating the HTTP client
- [ ] `vite.config.js` proxy/port (**5173**) and `import.meta.env.VITE_*` usage consistent with committed `.env.example` names — no secrets in repo
- [ ] No second competing SPA entry or duplicate build config unless an agreed spec says so

### Functions, DRY, and cleanliness

- [ ] **Smaller independent functions** — each unit does one job (parse envelope, map DTO to view model, format error for display)
- [ ] **Repeated logic extracted** to shared helpers (e.g. envelope parse, pagination from `meta`, field-level `error.details` mapping) — not copy-pasted across screens
- [ ] Components and modules stay **clean**: minimal nesting, early returns for guard cases, no dead code or commented-out blocks
- [ ] Code reads **self-explanatory** without a walkthrough; public helpers have brief JSDoc where types are not TypeScript

### API call error handling

- [ ] All ticket/comment/ask calls go through the **single HTTP module**; non-OK responses throw or return a typed `ApiError` (or equivalent) from `{ error }`
- [ ] Network failures and JSON parse failures surfaced as user-safe messages — not silent failures or raw `Failed to fetch` dumps of internals
- [ ] **400** / **404** / **409** (and other contract statuses) show `error.message`; form fields use `error.details` when present
- [ ] Loading flags cleared in `finally` (or equivalent); errors do not leave UI stuck in loading
- [ ] Empty **200** list responses remain success (empty state), not treated as errors (see **§1** API client)

### React state management

- [ ] State owned by the **screen** that uses it (`useState` / `useReducer` local to page or container) — matches `rules/frontend.md`; no unapproved global store (Redux, Zustand, etc.)
- [ ] Server data: clear pattern for fetch → loading → data | error; refetch/invalidate after successful mutations where the flow requires fresh detail/list
- [ ] Form state separate from fetched entity where edits are PATCHed; discard or reset behavior is intentional on navigation
- [ ] Ask flow state (question, loading, answer, citations, no-match) isolated — not mixed into unrelated ticket form state
- [ ] No duplicated sources of truth for the same ticket/list row across sibling components without a single fetch owner

---

## 5. Spec and steering compliance (`spec/`, `rules/`, `docs/`)

**Before marking UI work Pass**, cross-walk implemented routes/screens against **Inputs** — **`spec/`**, **`rules/`**, and applicable **`docs/`** (and **`skills/`** when UI-facing docs changed). For partial diffs, scope **§5** to touched flows but still cite the steering section that would fail if the rest of the app were missing behaviour.

### 5.0 Review procedure

- [ ] **Steering artefacts read** — list in **Output**: every `spec/*.md`, `rules/*.md`, `docs/*`, and `skills/*` file used, with **§** / section headings checked (same spirit as `commands/review-spec.md` Step 0)
- [ ] **`rules/` + `spec/` both satisfied** — conventions from `rules/frontend.md` / `api-standards.md` / `rag-vector-store.md` **and** product detail from `spec/`; flag **blocker** if code matches one but contradicts the other (escalate spec vs rules conflict to the user)
- [ ] Agreed **DEC-*** rows (**DEC-06**, **DEC-11**, **DEC-15**, **DEC-20**, etc.) reflected in UI; do not reopen **Open** items without user confirmation ([`ui-flow.md`](../spec/ui-flow.md) §14, [`rules/documentation.md`](../rules/documentation.md))

### 5.1 [`rules/`](../rules/) — conventions (explicit)

- [ ] [`rules/frontend.md`](../rules/frontend.md) — stack, single HTTP module, env vars, capabilities table, usability, RAG display, **do not** list (no frontend tests, no Reference UI, no TypeScript without DEC)
- [ ] [`rules/api-standards.md`](../rules/api-standards.md) — aligns with client calls and envelope parsing in the diff (see also **§1** API client)
- [ ] [`rules/rag-vector-store.md`](../rules/rag-vector-store.md) — ask UI does not bypass server retrieval or invent grounding; no chunk/score debug UI
- [ ] [`rules/documentation.md`](../rules/documentation.md) — no steering artefact contradictions; UI README/docs follow writing bar via [`skills/documentation/SKILL.md`](../skills/documentation/SKILL.md) when present

### 5.2 [`docs/`](../docs/) — assignment and non-product boundaries

- [ ] [`docs/assessment-brief.md`](../docs/assessment-brief.md) — UI capabilities match assignment themes (detail in `spec/requirements.md` §0.4 / §8)
- [ ] No in-app screens for prompt history, SpecStory, or AI mistake log ([`docs/prompt-history.md`](../docs/prompt-history.md), [`docs/ai-error.md`](../docs/ai-error.md) — [`ui-flow.md`](../spec/ui-flow.md) §4.2)

### 5.3 [`ui-flow.md`](../spec/ui-flow.md) — routes, screens, flows

- [ ] **§4** — Routes: `/` → `/tickets`, `/tickets`, `/tickets/new`, `/tickets/[id]` (`TKT-{n}`), `/ask`; nav to **`/ask`** from list/detail; dedicated ask page (**DEC-20**), not drawer-only v1
- [ ] **§7.1 List** — `GET` with `page`, `size`, `sort`, `q`, `status`; row fields per **TicketSummary**; search **Apply** + **Enter**; status filter; pagination from `meta.page` / `meta.totalPages`; empty vs no-match copy
- [ ] **§7.2 Create** — fields per table; **`status` forbidden** on form; **201** → navigate to new detail; **400** with field `details` (demo empty title)
- [ ] **§7.3 Detail** — full **TicketDetail** display; comments ascending; **Save changes** PATCH **without** `status`; **status** via separate legal-target buttons only (**DEC-20**); optional `category`, `resolutionNotes` (**DEC-03**, **DEC-05**)
- [ ] **§8** — CRUD mapping (create/list/detail/field PATCH/status PATCH/comment POST) matches implemented APIs
- [ ] **§9** — Legal-target button group per current state (**DEC-02**); **409** `ILLEGAL_TRANSITION` prominent; `resolutionNotes` not required before resolve (**DEC-05**)
- [ ] **§10** — Ask: `{ "question" }` only; loading; grounded **200** vs no-match **200** vs **400** blank question; no ingest jobs UI
- [ ] **§11** — Flows **A–E** reachable in UI where implemented (invalid reopen **Flow C**, no-match **Flow E**)
- [ ] **§12** — When reviewing full UI, demo checklist steps mappable to screens (cross-ref [`requirements.md`](../spec/requirements.md) **§8.7**)

### 5.4 [`ui-flow.md`](../spec/ui-flow.md) §13 — **AC-UI-01…12** (explicit)

Mark each **Pass** / **Fail** / **N/A** when the related UI exists:

- [ ] **AC-UI-01** — Create from form; success shows new **`TKT-{n}`**
- [ ] **AC-UI-02** — List with **`q`** and **`status`** filter (together or independently)
- [ ] **AC-UI-03** — Detail shows title, description, priority, assignee, status, category, resolution notes, comments
- [ ] **AC-UI-04** — Field PATCH from detail; values survive reload
- [ ] **AC-UI-05** — Add comment; appears in thread after success
- [ ] **AC-UI-06** — Status actions: legal targets only; **409** readable on illegal attempt
- [ ] **AC-UI-07** — Validation/transition errors from `error.message` / `details`; no stack traces
- [ ] **AC-UI-08** — Ask submits `{ "question" }`; loading + result visible
- [ ] **AC-UI-09** — Grounded ask shows `data.answer` + links for each **`data.citedTicketIds`**
- [ ] **AC-UI-10** — No-match **200**: honest `data.answer`, empty citations; not styled as HTTP error
- [ ] **AC-UI-11** — No create-ticket / notify / agent tools from ask UI
- [ ] **AC-UI-12** — Assistant output visually distinct from ticket fields and comments

### 5.5 [`api-contract.md`](../spec/api-contract.md) + [`data-model.md`](../spec/data-model.md)

- [ ] JSON field names and enums match **§3** / **§5** — no parallel client-only names (`URGENT` → display **CRITICAL** if sent)
- [ ] Create/update PATCH bodies only include contract-allowed fields; list query params match **§4** list operation
- [ ] Comment POST body and thread shape match **§5**; ticket id in routes uses API **`TKT-{n}`** strings (**DEC-04**)
- [ ] Client validation hints align with server rules (e.g. required **title**) but **backend remains source of truth** ([`ui-flow.md`](../spec/ui-flow.md) §6.2)

### 5.6 [`state-machine.md`](../spec/state-machine.md) + [`architecture.md`](../spec/architecture.md) §12.4

- [ ] UI offers only **T1–T5** targets for current status (e.g. `OPEN` → `IN_PROGRESS`, `CANCELLED`); terminal **`CLOSED`** / **`CANCELLED`** — no false “reopen” success path
- [ ] **409** message pattern visible to user (illegal e.g. **`CLOSED` → `OPEN`** — **Flow C** / **X1**)
- [ ] Backend enforcement assumed — UI disable/hide is guidance only (**§1** API client)

### 5.7 [`rag-api-contract.md`](../spec/rag-api-contract.md) + ask review bar

- [ ] **§13** obligations: `{ "question" }` only; parse `{ data }` / `{ error }`; link citations to detail; honest no-match at **200**; no ticket creation from answer
- [ ] **§7** — UI uses `data.answer` + `data.citedTicketIds`; does not invent ids or show server-internal fields (**§14**)
- [ ] Question length / **400** for blank question per contract (**DEC-17** / [`architecture.md`](../spec/architecture.md) §12.5)
- [ ] When ask UI is in scope, grounding display meets the same traceability bar as [`commands/review-rag-output.md`](../commands/review-rag-output.md) (cited ids from API response, honest no-match)

### 5.8 [`requirements.md`](../spec/requirements.md) — journeys and Reference

- [ ] **§4.1** flows **A–E** — implemented UI steps match [`ui-flow.md`](../spec/ui-flow.md) §11 mapping
- [ ] **§2.3 Reference** — no auth, delete ticket, agent-from-ask, confidence badges, vector/chunk debug consoles
- [ ] **AC-CORE-01…11** (and ask **16–18** when ask present) — satisfied via **AC-UI-*** / screen behaviour, not backend-only proof

---

## Output

- **Steering artefacts read** — `spec/`, `rules/`, `docs/`, `skills/` paths and section ids used (see **§5.0**)
- **Findings** — severity, file, checklist item (**§1–§5**), steering citation (`spec/`, `rules/`, or `docs/`), brief fix (do not implement unless asked)
- **Spec gaps** — UI behaviour without matching `spec/` **or** contradiction with `rules/`; unresolved spec vs rules conflict → confirm with user
- **Related commands** — note if [`commands/review-rag-output.md`](../commands/review-rag-output.md) or [`commands/review-spec.md`](../commands/review-spec.md) should run next
- **Open questions assumed** — confirm with the user
- **Frontend tests** — none required; do not request generating them
- **Ready?** yes / no

---

## Revision history

| Date | Note |
|------|------|
| 2026-10-04 | Initial frontend review command (React/Next/Vite/JS); no frontend tests required. |
| 2026-10-04 | Aligned with `rules/frontend.md` and architecture §12 UI expectations. |
| 2026-10-04 | Governance pass with expanded specs and rules index. |
| 2026-10-04 | Added revision history section. |
| 2026-10-04 | [`spec/data-model.md`](../spec/data-model.md) inputs; id/category/resolutionNotes no longer open in UI review. |
| 2026-10-04 | UI flows: architecture §12.3–§12.6 + requirements §4.1 / §8.7. |
| 2026-10-04 | Primary UI spec: [`ui-flow.md`](../spec/ui-flow.md); **AC-UI-*** checklist. |
| 2026-10-04 | Inputs reference [`ui-flow.md`](../spec/ui-flow.md) **§0** PDF map. |
| 2026-10-04 | **Reference** §2.3; **DEC-11**/**DEC-15** agreed. |
| 2026-10-04 | **DEC-15** stack: React + Next.js + Vite + JavaScript; Node 24 LTS. |
| 2026-10-04 | **§4** code quality: JS/React/Next/Vite conventions, DRY helpers, API errors, React state; **§2** file grouping. |
| 2026-10-04 | **§5** full `spec/` traceability: inputs table, **AC-UI-01…12**, ui-flow §4–§12, api/data-model/state-machine/rag contracts. |
| 2026-10-04 | **Inputs** + **§5**: include `rules/`, `commands/`, `skills/`, `docs/` with `spec/` (integrated steering, not spec-only). |
