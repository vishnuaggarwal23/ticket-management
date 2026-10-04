# Review frontend

Cursor attaches this file via [`.cursor/commands/review-frontend.md`](../.cursor/commands/review-frontend.md) (pointer only). Edit **this** file; do not copy the body into `.cursor/commands/`.

Review the current **UI** change set (or specified paths) against `rules/frontend.md`, `rules/api-standards.md`, `rules/rag-vector-store.md`, [`spec/ui-model.md`](../spec/ui-model.md) (**AC-UI-***), [`spec/architecture.md`](../spec/architecture.md) §12 (summary), and API / RAG contracts.

**Do not generate or require frontend tests.** Do not apply fixes unless the user asks.

**PDF UI capabilities to verify (when implemented):** create/list/detail/update fields, comments, keyword search, status filter, status display + transition UX, meaningful API errors, ask with citations or honest no-match (`rules/frontend.md` table). Backend still enforces the state machine — UI is guidance only.

## Inputs

- Diff / named frontend paths
- `rules/frontend.md`
- `rules/api-standards.md` (paths, envelopes, PATCH, pagination, 409)
- [`spec/requirements.md`](../spec/requirements.md) — **AC-CORE-01…11** for UI-facing acceptance; **§4.1** flows and **§8.7** demo; **§10** for open UI/API shape (**OQ/DEC**)
- [`spec/ui-model.md`](../spec/ui-model.md) — screens, flows, **AC-UI-01…12**; open **DEC-06**, **DEC-15** §14
- [`spec/architecture.md`](../spec/architecture.md) §12 when reviewing module/API wiring at architecture level
- [`spec/data-model.md`](../spec/data-model.md) (agreed id, category, resolutionNotes, create requiredness)
- Draft [`spec/api-contract.md`](../spec/api-contract.md) §6.2–§6.5 — open **DEC-11** per [`spec/requirements.md`](../spec/requirements.md) §10.2
- Assessment PDF only as background for **capabilities** (not stack)

Mark each item **Pass** / **Fail** / **N/A**. Failures: **blocker** / **major** / **minor** with file references.

---

## 1. Validity (capabilities vs invented UI)

### Spec and scope

- [ ] Only agreed [`ui-model.md`](../spec/ui-model.md) screens/flows (or user-confirmed deltas). Use requirements **§4.1** / **§8.7** as hub — no invented auth, agent-from-ask, extra ticket resources, delete-ticket UI
- [ ] Open items not silently decided — **DEC-06, 15**, layout/router still open; **do not** invent id format or category (use [`spec/data-model.md`](../spec/data-model.md): `TKT-{n}`, category enum, `resolutionNotes`, title required on create)
- [ ] PDF capabilities present or explicitly deferred; when implemented, mappable to **`AC-CORE-01…11`** (§8) and **`AC-UI-01…12`** ([`ui-model.md`](../spec/ui-model.md) §13): create, list, detail, update title/description/priority/assignee, comments, keyword search, status filter, status display, valid transitions, meaningful errors, ask with citations or no-match

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
- **Spec gaps** — UI without `ui-model.md` / contract alignment
- **Open questions assumed** — confirm with the user
- **Frontend tests** — none required; do not request generating them
- **Ready?** yes / no

---

## Revision history

| Date | Note |
|------|------|
| 2026-10-04 | Initial frontend review command (React/Vite/TS); no frontend tests required. |
| 2026-10-04 | Aligned with `rules/frontend.md` and architecture §12 UI expectations. |
| 2026-10-04 | Governance pass with expanded specs and rules index. |
| 2026-10-04 | Added revision history section. |
| 2026-10-04 | [`spec/data-model.md`](../spec/data-model.md) inputs; id/category/resolutionNotes no longer open in UI review. |
| 2026-10-04 | UI flows: architecture §12.3–§12.6 + requirements §4.1 / §8.7. |
| 2026-10-04 | Primary UI spec: [`ui-model.md`](../spec/ui-model.md); **AC-UI-*** checklist. |
