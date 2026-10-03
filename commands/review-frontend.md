# Review frontend

Cursor attaches this file via [`.cursor/commands/review-frontend.md`](../.cursor/commands/review-frontend.md) (pointer only). Edit **this** file; do not copy the body into `.cursor/commands/`.

Review the current **UI** change set (or specified paths) against `rules/frontend.md`, `rules/api-standards.md`, `rules/rag-vector-store.md`, and agreed `spec/ui-flow.md` / API / RAG contracts.

**Do not generate or require frontend tests.** Do not apply fixes unless the user asks.

**PDF UI capabilities to verify (when implemented):** create/list/detail/update fields, comments, keyword search, status filter, status display + transition UX, meaningful API errors, ask with citations or honest no-match (`rules/frontend.md` table). Backend still enforces the state machine — UI is guidance only.

## Inputs

- Diff / named frontend paths
- `rules/frontend.md`
- `rules/api-standards.md` (paths, envelopes, PATCH, pagination, 409)
- [`spec/requirements.md`](../spec/requirements.md) — **AC-CORE-01…11** for UI-facing acceptance; **§4.1** flows and **§8.7** demo until `ui-flow.md`; **§10** for open UI/API shape (**OQ/DEC**)
- [`spec/architecture.md`](../spec/architecture.md) §12 (UI surfaces) when reviewing screen/API wiring
- [`spec/data-model.md`](../spec/data-model.md) (agreed id, category, resolutionNotes, create requiredness)
- `spec/ui-flow.md`, `spec/api-contract.md`, `spec/rag-api-contract.md` if the user has added them (default: **not** in repo — see [`rules/documentation.md`](../rules/documentation.md) interim map)
- Assessment PDF only as background for **capabilities** (not stack)

Mark each item **Pass** / **Fail** / **N/A**. Failures: **blocker** / **major** / **minor** with file references.

---

## 1. Validity (capabilities vs invented UI)

### Spec and scope

- [ ] Only agreed `ui-flow.md` (or user-confirmed screens). Until that file exists, use requirements **§4.1** / **§8.7** + architecture **§12** — no invented auth, agent-from-ask, extra ticket resources
- [ ] Open items not silently decided — **DEC-06, 15**, layout/router still open; **do not** invent id format or category (use [`spec/data-model.md`](../spec/data-model.md): `TKT-{n}`, category enum, `resolutionNotes`, title required on create)
- [ ] PDF capabilities present or explicitly deferred; when implemented, mappable to **`AC-CORE-01…11`** (§8): create, list, detail, update title/description/priority/assignee, comments, keyword search, status filter, status display, valid transitions, meaningful errors, ask with citations or no-match

### Stack (`rules/frontend.md`)

- [ ] React + Vite + TypeScript; **no Next.js**
- [ ] No frontend test files or new UI test dependencies
- [ ] API base via `import.meta.env.VITE_*`; no machine-specific hosts or secrets

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

## 2. Structure

- [ ] Presentation vs HTTP client vs view logic not all in one mega-file when that hurts maintenance
- [ ] Single HTTP module (or equivalent), not fetch in every component
- [ ] Types for props and API `data` payloads; `any` only if justified
- [ ] State owned by the screen that uses it; no unapproved global store library
- [ ] No unapproved CSS/router/state libraries unless the user agreed

---

## 3. Quality (no frontend tests)

- [ ] Labels, semantic HTML, keyboard-usable primary actions
- [ ] Loading, empty, and error states for list and ask
- [ ] Forms do not silently drop invalid input
- [ ] User-visible errors never include stack traces, SQL, or internals
- [ ] No secrets in source or committed env files

**Skip:** coverage, Playwright/Vitest, snapshot tests.

---

## Output

- **Findings** — severity, file, checklist item, brief fix (do not implement unless asked)
- **Spec gaps** — UI without `ui-flow.md` / contract
- **Open questions assumed** — confirm with the user
- **Frontend tests** — none required; do not request generating them
- **Ready?** yes / no

---

## Revision history

| Date | Note |
|------|------|
| 2026-10-04 | Initial frontend review command (React/Vite/TS); no frontend tests required. |
| 2026-10-04 | Aligned with `rules/frontend.md` and `spec/ui-flow.md` expectations. |
| 2026-10-04 | Governance pass with expanded specs and rules index. |
| 2026-10-04 | Added revision history section. |
| 2026-10-04 | [`spec/data-model.md`](../spec/data-model.md) inputs; id/category/resolutionNotes no longer open in UI review. |
| 2026-10-04 | Interim flows from requirements §4.1 / §8.7 when `ui-flow.md` not in repo. |
