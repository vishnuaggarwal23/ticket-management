# Review frontend

Cursor attaches this file via [`.cursor/commands/review-frontend.md`](../.cursor/commands/review-frontend.md) (pointer only). Edit **this** file; do not copy the body into `.cursor/commands/`.

Review the current **UI** change set (or specified paths) against `rules/frontend.md`, `rules/api-standards.md`, `rules/rag-vector-store.md`, [`spec/ui-flow.md`](../spec/ui-flow.md) (**§0**, **AC-UI-***), [`spec/architecture.md`](../spec/architecture.md) §12 (summary), and API / RAG contracts. Written code must meet **§2–§4** (structure, UX quality, and stack/code conventions) in addition to capability and API checks in **§1**.

**Do not generate or require frontend tests.** Do not apply fixes unless the user asks.

**PDF UI capabilities to verify (when implemented):** create/list/detail/update fields, comments, keyword search, status filter, status display + transition UX, meaningful API errors, ask with citations or honest no-match (`rules/frontend.md` table). Backend still enforces the state machine — UI is guidance only.

## Inputs

- Diff / named frontend paths
- `rules/frontend.md`
- `rules/api-standards.md` (paths, envelopes, PATCH, pagination, 409)
- [`spec/requirements.md`](../spec/requirements.md) — **AC-CORE-01…11** for UI-facing acceptance; **§4.1** flows and **§8.7** demo; **§10** for open UI/API shape (**OQ/DEC**)
- [`spec/ui-flow.md`](../spec/ui-flow.md) — **§0** PDF ui-flow map; screens, flows, **AC-UI-01…12**; **DEC-06** agreed (PATCH `status`), **DEC-15** agreed §14
- [`spec/architecture.md`](../spec/architecture.md) §12 when reviewing module/API wiring at architecture level
- [`spec/data-model.md`](../spec/data-model.md) (agreed id, category, resolutionNotes, create requiredness)
- [`spec/rag-api-contract.md`](../spec/rag-api-contract.md) — **DEC-11** agreed; no **Reference** UI ([`spec/requirements.md`](../spec/requirements.md) **§2.3**)
- Assessment PDF only as background for **capabilities** (not stack)

Mark each item **Pass** / **Fail** / **N/A**. Failures: **blocker** / **major** / **minor** with file references.

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

## Output

- **Findings** — severity, file, checklist item, brief fix (do not implement unless asked)
- **Spec gaps** — UI without `ui-flow.md` / contract alignment
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
