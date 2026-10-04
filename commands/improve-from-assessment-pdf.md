# Improve from assessment (DOCX)

Cursor attaches this file via [`.cursor/commands/improve-from-assessment-pdf.md`](../.cursor/commands/improve-from-assessment-pdf.md) (pointer only). Edit **this** file; do not copy the body into `.cursor/commands/`.

Re-read [`docs/Assessments.docx`](../docs/Assessments.docx) and **improve existing documentation only** — closer assignment alignment, clearer traceability, fixed cross-links, filled gaps, and honest **Open** / **DEC-*** markers. This is a **maintenance pass**, not implementation.

## Authoritative assignment

| File | Role |
|------|------|
| [`docs/Assessments.docx`](../docs/Assessments.docx) | **Primary source** — 6-page ATL/TL assignment |

Read via IDE, or extract text with `unzip -p docs/Assessments.docx word/document.xml` (strip XML), or `python-docx` if installed.

## Hard constraints (non-negotiable)

1. **Do not create any new files** — no new paths under `spec/`, `commands/`, `rules/`, `skills/`, or `docs/`. The assignment’s ten spec names are satisfied by **ten** existing `spec/*.md` files (`ui-flow.md` → `ui-model.md` only). Do not add `spec/ui-flow.md`.
2. **In-scope directories only** — edit files that already exist under:
   - `spec/*.md`
   - `commands/*.md`
   - `rules/*.md`
   - `skills/**` (e.g. `skills/documentation/SKILL.md`)
   - `docs/*.md` **except** [`docs/prompt-history.md`](../docs/prompt-history.md) (generated index — use `commands/update-prompt-history.md` instead)
3. **Do not edit** `.cursor/**`, application source, `graphify-out/`, `.specstory/**`, or `docs/Assessments.docx` in this command (except you may *read* SpecStory for context).
4. **Do not invent product features** not supported by the assignment. Elaboration and **Convention** choices must stay labeled; in-scope gaps stay **Open** or **DEC-*** — confirm with the user before locking. Out-of-PDF topics → **Reference** in [`spec/requirements.md`](../spec/requirements.md) **§2.3** (document only; do not expand scope).
5. **Do not rewrite entire files** unless the user asked for a full rewrite. Prefer targeted edits: missing AC rows, broken links, §0.4 gaps, revision-history rows, reviewer maps.
6. **Confirm with the user** before resolving an **Open** / **DEC-*** item or changing **agreed** [`spec/data-model.md`](../spec/data-model.md) substance.

## Source priority and fallbacks

| Priority | Source | Use |
|----------|--------|-----|
| 1 | `docs/Assessments.docx` | Full assignment text |
| 2 | [`docs/assessment-brief.md`](../docs/assessment-brief.md) | Human restatement + §13 coverage table |
| 3 | [`spec/requirements.md`](../spec/requirements.md) **§0.4**, **§8**, **§10** | Assignment → repo map, AC-CORE, decision register |

If DOCX cannot be read, continue with §0.4 + `assessment-brief.md` for **targeted edits only**; in the report mark **Assessment access: fallback** and list themes that could not be re-verified. Do not fabricate page quotes.

## Ten-file `spec/` set (assignment lists ten names)

| Assignment-listed name | Repo file |
|------------------------|-----------|
| `rag-api-contract.md` | [`spec/rag-api-contract.md`](../spec/rag-api-contract.md) (**AC-RAG-API-***) |
| `ui-flow.md` | [`spec/ui-model.md`](../spec/ui-model.md) (**AC-UI-***); summary in [`spec/architecture.md`](../spec/architecture.md) **§12** |
| State machine test proof | [`spec/test-strategy.md`](../spec/test-strategy.md) **§5** (ask bands **§6**) |
| Retrieval quality eval | [`spec/evaluation-strategy.md`](../spec/evaluation-strategy.md) |

Index: [`rules/documentation.md`](../rules/documentation.md).

## Suggested edit order

Work hub-first, then steering, then satellite docs.

1. **`spec/requirements.md`** — §0.4 coverage map, FEAT / AC-CORE completeness vs p.6 checklist, §8.7 demo steps, §10 DEC register, child-spec table (ten files; `ui-flow` → `ui-model` alias).
2. **`spec/architecture.md`**, **`ui-model.md`**, **`api-contract.md`**, **`rag-api-contract.md`**, **`data-model.md`**, **`state-machine.md`**, **`rag-ingestion.md`**, **`evaluation-strategy.md`**, **`test-strategy.md`** — assignment themes in the right sections; cross-links; acceptance IDs; revision history.
3. **`rules/*.md`** — opening **PDF vs Convention vs Open**; pointers to spec **§0** and section maps (no duplicate bodies); reviewer maps in `documentation.md`.
4. **`commands/*.md`** — inputs/spec paths match the ten-file set; **§0.4–§0.5** / per-spec **§0** in review commands; no dead links to `spec/ui-flow.md`.
5. **`skills/documentation/SKILL.md`** — spec set table, §0 template, AC-ID owners aligned with `rules/documentation.md`.
6. **`docs/assessment-brief.md`** — §4 spec table, **§13** traceability, **§14** index; match repo reality; do **not** grant new scope beyond the assignment.

Optional cross-check (read-only): run themes from `commands/review-spec.md` checklist on changed specs before finishing.

## Per-file improvement checklist

For each file you touch, aim for:

- [ ] **Assessment fidelity** — requirement text traceable to `Assessments.docx` or explicitly **Convention** / **Open**
- [ ] **Traceability** — FEAT / AC-CORE / AC-* IDs referenced where the assignment expects testable proof
- [ ] **Cross-links** — relative links to sibling specs and `rules/*`; no dead `spec/ui-flow.md` links (use `ui-model.md`); steering docs index spec **§0** / requirements **§0.5** without duplicating bodies
- [ ] **Consistency** — same DEC ids, enum names, and API paths as [`data-model.md`](../spec/data-model.md) (agreed) and `rules/api-standards.md`
- [ ] **Revision history** — dated row describing what changed and why (if the file already has a revision section)
- [ ] **Heading unit tags** — major `##` sections suffix ids from that file’s §0.3 independent reading units table (hub **§0.8**)
- [ ] **No scope creep** — hygiene (prompt history, mistake log) documented as paths/process, not new product features

## Outputs (required)

Produce a short report:

1. **Assessment access** — DOCX read OK / fallback used / blocked
2. **Files improved** — path list with one-line summary each
3. **Files in scope but unchanged** — and why (already aligned)
4. **Gaps still open** — map to **DEC-*** or new **OQ** rows in `requirements.md` §10 (edit only if user agrees)
5. **Blocked** — anything that would have required a **new file** (state what was folded instead, or defer)

Do not start backend or frontend implementation in this command.

---

## Revision history

| Date | Note |
|------|------|
| 2026-10-04 | Initial command: PDF-driven edit-only pass over `spec/`, `rules/`, `commands/`, `skills/`, `docs/` (excl. `prompt-history.md`). |
| 2026-10-04 | Fallback: proceed with brief + §0.4 when PDF text extraction unavailable; report limitations. |
| 2026-10-04 | Single authoritative file: `docs/Assessments.docx`. |
| 2026-10-04 | Nine-file `spec/` set; PDF `ui-flow` → [`ui-model.md`](../spec/ui-model.md). |
| 2026-10-04 | Ten-file `spec/` set incl. [`rag-api-contract.md`](../spec/rag-api-contract.md); only `ui-flow.md` filename alias remains. |
| 2026-10-04 | Steering pass: `rules/`, `commands/`, `skills/`, `assessment-brief.md` index spec **§0** + requirements **§0.5**. |
| 2026-10-04 | Per-file checklist: **`##` heading unit suffix** per hub §0.8. |
| 2026-10-04 | Pass note: hub **DEC-11** / **OQ-05** owner is `rag-api-contract.md` (not `api-contract` §6.3). |
| 2026-10-04 | **Reference** §2.3: out-of-PDF topics document-only in steering pass. |
