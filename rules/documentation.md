# Documentation skills

Cursor attaches this file via [`.cursor/rules/documentation.mdc`](../.cursor/rules/documentation.mdc) (pointer only). Edit **this** file; do not copy the body into the `.mdc`.

How to document this project and **where every artefact lives**. For step-by-step spec writing, also use `skills/documentation/SKILL.md`.

## Rules and commands index (steering)

Use these during implementation and review. **Edit `rules/` and `commands/`**; `.cursor/rules/*.mdc` and `.cursor/commands/*.md` are **pointers only**.

| Topic | Rule | Command(s) |
|-------|------|------------|
| Backend Java / Spring | `rules/java-springboot.md` | `commands/review-code.md` |
| REST envelopes, `/api/v1`, list params, agreed ticket JSON fields | `rules/api-standards.md` (+ [`spec/data-model.md`](../spec/data-model.md)) | `commands/review-code.md` |
| Tests (backend only) | `rules/testing.md` | `commands/generate-tests.md`, `commands/review-code.md` |
| RAG ingest / ask / grounding | `rules/rag-vector-store.md` | `commands/review-rag-output.md`, `commands/review-code.md` |
| React / Vite / TypeScript UI | `rules/frontend.md` | `commands/review-frontend.md` |
| Spec quality before coding | `rules/documentation.md` (architecture § map) | `commands/review-spec.md` |
| This file (docs layout) | `rules/documentation.md` | — |

Authoritative assignment: [`docs/Assessments.docx`](../docs/Assessments.docx). Extract into [`spec/requirements.md`](../spec/requirements.md). **Clones without that file** rely on `spec/` + these rules.

### When to run which command

| Situation | Command |
|-----------|---------|
| New or changed `spec/*.md` before coding | `commands/review-spec.md` |
| Starting a feature slice; need backend test ideas | `commands/generate-tests.md` |
| Backend Java / API / RAG code diff before merge | `commands/review-code.md` |
| React / Vite / TypeScript UI diff before merge | `commands/review-frontend.md` |
| Manual or demo check of an `/api/ai/ask` answer | `commands/review-rag-output.md` |
| Judging retrieval quality (right tickets in top-K?) | `commands/review-rag-output.md` → **Retrieval quality** + [`spec/evaluation-strategy.md`](../spec/evaluation-strategy.md) (draft); hub [`spec/requirements.md`](../spec/requirements.md) §2.5 / FEAT-22 |
| Caught wrong AI code or ungrounded answer | Note in `docs/ai-mistakes.md` (see below) |
| SpecStory saved new session(s); index stale | `commands/update-prompt-history.md` |
| Assignment drift; tighten docs without new files | `commands/improve-from-assessment-pdf.md` (`docs/Assessments.docx`) |

Workflow (PDF): Requirement → Specification → **review-spec** → Plan/Tasks → Implementation → **generate-tests** / write tests → **review-code** / **review-frontend** → Fix. Periodic: **improve-from-assessment-pdf** on `spec/` + steering artefacts.

## Where things live

| Artefact | Location |
|----------|----------|
| Assessment (authoritative assignment) | [`docs/Assessments.docx`](../docs/Assessments.docx); restated in [`spec/requirements.md`](../spec/requirements.md) |
| Implementable specs | `spec/` (see list below) |
| Human docs / notes | `docs/` (e.g. `docs/assessment-brief.md`, `docs/prompt-history.md`, `docs/ai-mistakes.md`) |
| Prompt / session history | `.specstory/history/` |
| Engineering rules | `rules/` → `.cursor/rules/*.mdc` pointers |
| Slash commands | `commands/` → `.cursor/commands/*.md` pointers |
| Documentation skill (templates) | `skills/documentation/SKILL.md` |

## Spec set (create before implementation)

Maintain detailed specs as work proceeds:

All PDF-listed files under `spec/` (paths relative to repo root):

[`requirements.md`](../spec/requirements.md), [`architecture.md`](../spec/architecture.md), [`data-model.md`](../spec/data-model.md), [`state-machine.md`](../spec/state-machine.md), [`api-contract.md`](../spec/api-contract.md), [`rag-api-contract.md`](../spec/rag-api-contract.md), [`rag-ingestion.md`](../spec/rag-ingestion.md), [`evaluation-strategy.md`](../spec/evaluation-strategy.md), [`test-strategy.md`](../spec/test-strategy.md), [`ui-model.md`](../spec/ui-model.md).

**PDF filename alias:** assignment lists `ui-flow.md` → implemented as [`ui-model.md`](../spec/ui-model.md) (do not add `spec/ui-flow.md`). Ten assignment spec names → **ten** files under `spec/` (see [`requirements.md`](../spec/requirements.md) child-spec table). [`architecture.md`](../spec/architecture.md) **§12** is the UI architecture summary.

### Spec files in repo today (2026-10-04)

| Present | Status |
|---------|--------|
| [`requirements.md`](../spec/requirements.md) | draft hub |
| [`architecture.md`](../spec/architecture.md) | draft system design + **§12** UI architecture summary |
| [`data-model.md`](../spec/data-model.md) | **agreed** |
| [`state-machine.md`](../spec/state-machine.md) | **draft** |
| [`api-contract.md`](../spec/api-contract.md) | **draft** (ticket REST + ask §6 summary) |
| [`rag-api-contract.md`](../spec/rag-api-contract.md) | **draft** (authoritative ask / **AC-RAG-API-***) |
| [`rag-ingestion.md`](../spec/rag-ingestion.md) | **draft** |
| [`evaluation-strategy.md`](../spec/evaluation-strategy.md) | **draft** |
| [`test-strategy.md`](../spec/test-strategy.md) | **draft** |
| [`ui-model.md`](../spec/ui-model.md) | **draft** |

PDF theme → spec traceability: [`requirements.md`](../spec/requirements.md) **§0.4** and [`docs/assessment-brief.md`](../docs/assessment-brief.md) **§13**.

**Agreed DEC (do not contradict):** **DEC-03, 04, 05, 07, 08, 13** — register in requirements §10.2; detail in [`data-model.md`](../spec/data-model.md).

**Still Open (stop and confirm):** **DEC-01, 02, 06, 09, 10, 11, 12, 14, 15** — requirements §10.2.

| Spec | What it must nail down (so rules do not guess) |
|------|-----------------------------------------------|
| [`requirements.md`](../spec/requirements.md) | PDF hub: **FR** / **FEAT-***, **AC-CORE-*** (§8) and **AC-FEAT-*** (§4.2); **OQ-*** / **DEC-*** register and spec handoff (§10); precedence PDF → requirements → agreed specs → rules (§2.4); deterministic vs probabilistic proof (§2.5); demo script (§8.7); glossary (§12). Detail contracts live in child specs (§13). |
| [`architecture.md`](../spec/architecture.md) | System design: business vs functional modules, ticket aggregate shape, tech/deployment, **communication** (sync REST), **API map**, **PgVector** index role, RAG pipeline; **chunking** and **embedding tradeoffs** (PDF NFR-07 / AC-CORE-19). Numeric chunk/K/model → [`rag-ingestion.md`](../spec/rag-ingestion.md) |
| [`data-model.md`](../spec/data-model.md) | Entities, Liquibase tables, enums, RAG chunk metadata, DTO catalogs, **indexes §14.5**; **DEC-03/04/05/07/08/13** |
| [`api-contract.md`](../spec/api-contract.md) | Ticket/comment payloads; combined HTTP catalog incl. ask summary §6; envelopes in `rules/api-standards.md` |
| [`rag-api-contract.md`](../spec/rag-api-contract.md) | Ask HTTP + `data` JSON; grounding, citations, no-match (**AC-RAG-API-***, **DEC-11**) |
| [`state-machine.md`](../spec/state-machine.md) | Legal/illegal transitions (including skipped steps if any) |
| [`rag-ingestion.md`](../spec/rag-ingestion.md) | Chunking (hybrid paragraph + fixed overflow), ingest sources, re-ingest (**DEC-01**), property keys; **DEC-09** model/dimension still open |
| [`ui-model.md`](../spec/ui-model.md) | Screens, CRUD flows, ask/RAG UX, flows A–E, **AC-UI-***; PDF `ui-flow` themes |
| [`architecture.md`](../spec/architecture.md) §12 | Frontend architecture summary; defers screen detail to `ui-model.md` |
| [`evaluation-strategy.md`](../spec/evaluation-strategy.md) | **Retrieval quality** vs grounding; **Example** corpus + PDF Q1–Q5; procedure §6–§7; failures **F-01…F-10**; **AC-EVAL-*** |
| [`test-strategy.md`](../spec/test-strategy.md) | **§5** SM determinism; **§6** ask bands; map **AC-CORE** / **AC-SM** / **AC-API** / **AC-DM** (`rules/testing.md`) |

**Agreed spec:** status `agreed` (or equivalent) in the spec header, or explicit user confirmation in chat. Until then, treat as **draft** — implement only what rules already lock (envelopes, stack conventions) and stop for open payloads.

### [`spec/requirements.md`](../spec/requirements.md) structure (for reviewers)

Use this map when running `commands/review-spec.md` or tracing tests — do not duplicate the full text in rules.

| Section | Use |
|---------|-----|
| §0 | How to read the file; audience; **§0.4 PDF coverage map**; **§0.5 PDF verbatim anchors** (completeness checklist) |
| §2.4–2.7 | Precedence, deterministic vs probabilistic, status enum, anti-patterns |
| §4.1–4.3 | Flows A–E; per-feature AC; **Example** demo corpus (not mandated seed data) |
| §8, §8.7 | **AC-CORE-*** sign-off; demo / grading script |
| §9 | FR → AC → FEAT traceability |
| §10 | **OQ-*** catalogue, **DEC-*** register (agree before implementing), handoff to child specs |
| §11 | PDF wording tensions (e.g. re-ingest on close — **DEC-01**) |
| §12–§13 | Glossary; what must not live in `requirements.md` |

Closing an **OQ** requires updating the owning child spec, recording **Decision** in §10.2, and user confirmation — see §10.3.

### [`spec/architecture.md`](../spec/architecture.md) structure (for reviewers)

Use when running `commands/review-spec.md` or tracing implementation to layers — do not duplicate the full text in rules.

| Section | Use |
|---------|-----|
| §3 | Architectural principles (monolith, grounded AI, spec-driven) |
| **§0** | PDF theme map; verbatim RAG ladder; architecture-level BRF/FRI/IRI |
| §4 | **Business** modules, actors, capability map → FEAT ids |
| §5 | **Ticket** aggregate (conceptual); RAG text sources; not field catalogs |
| §6–7 | System context, stack, deployment topology |
| §8–9 | **Functional** modules and **technical** packages (`api` / `domain` / `service` / `persistence` / `rag` / `config`) |
| §10 | **Communication** — sync JSON REST, sequences, error vs ask no-match |
| §11 | API capability map (PDF ask path + Convention `/api/v1`) |
| §12 | Frontend architecture summary; screen/flow detail → [`ui-model.md`](../spec/ui-model.md) |
| §13–14 | Relational SoR vs **vector DB** (PgVector, chunk lifecycle) |
| §15–16 | RAG pipeline, knowledge docs, **chunking justification**, **embedding tradeoffs** |
| §17 | State machine placement in domain |
| §21 | **OQ** / **DEC** pointers (sync with `requirements.md` §10) |

Field-level tickets, Liquibase, and ask `data` JSON stay in child specs (§22 table). Architecture **labels** PDF vs **Convention** vs **Open**.

### [`spec/data-model.md`](../spec/data-model.md) structure (for reviewers)

| Section | Use |
|---------|-----|
| **§0** | PDF persistence map; business/functional/implementation triad |
| §4–7 | Domain overview, enums, relational entities, associations |
| §8–9 | Vector/`ticket_vector_chunk`, logical RAG pipeline types |
| §10–12 | API DTOs, `RagChunkMetadata`, embeddables |
| §13–15 | Data flows, Liquibase DDL + **§14.5 index catalog**, search SQL |
| §16–17 | Validation rules; **agreed DEC** table (sync with `requirements.md` §10.2) |
| §18 | **AC-DM-*** acceptance checks |

### [`spec/state-machine.md`](../spec/state-machine.md) structure (for reviewers)

| Section | Use |
|---------|-----|
| **§0** | PDF state-machine map; BRF/FRI/IRI; REST examples §6.5 |
| §3–4 | States, terminal behaviour, diagrams |
| §5.1–5.2 | **PDF** valid T1–T5 and forbidden X1–X3 |
| §5.3–5.7 | **DEC-02** default (A); full valid/invalid matrix; §5.6 illegal register; PATCH `status` rules §6.1.1 |
| §6 | PATCH + **409** `ILLEGAL_TRANSITION` (**DEC-06** interim) |
| §8–9 | Domain placement; **AC-SM-*** tests |
| §10 | Open **DEC-02**, **DEC-06**; agreed **DEC-07** pointer |

### [`spec/api-contract.md`](../spec/api-contract.md) structure (for reviewers)

| Section | Use |
|---------|-----|
| **§0** | PDF REST capabilities → endpoints; BRF/FRI/IRI |
| §2 | Envelopes; **§2.8** URI catalog; **§2.11** endpoint catalog; **§2.9** headers; **§2.10** error examples |
| §3 | JSON resource models (ticket, comment, writes) |
| §4–5 | Endpoints with full HTTP + cURL + scenario tables |
| §6 | Ask URIs; **§6.2–§6.5** summary (detail → [`rag-api-contract.md`](../spec/rag-api-contract.md)) |
| §7 | Demo end-to-end URI sequence |
| §8–9 | REST summary; **AC-API-*** |
| §10 | **DEC-06**, **DEC-11**, **DEC-14** |

### [`spec/rag-api-contract.md`](../spec/rag-api-contract.md) structure (for reviewers)

| Section | Use |
|---------|-----|
| **§0** | PDF ask themes; BRF/FRI/IRI |
| §4–§6 | Endpoints, envelopes, pipeline boundary |
| §7 | `AskRequest` / `AskResponseData` inside success `data` |
| §8–§9 | Grounding, citations, no-match (**DEC-11**) |
| §10–§11 | Guardrails, non-agentic rules |
| §17 | **AC-RAG-API-01…05** (authoritative owner) |

Combined ticket + ask URI catalog remains in [`api-contract.md`](../spec/api-contract.md) §2.11 and §6.

### [`spec/rag-ingestion.md`](../spec/rag-ingestion.md) structure (for reviewers)

| Section | Use |
|---------|-----|
| **§0** | Ingest PDF map; BRF/FRI/IRI; examples (`assembledText`, `application.yml`) |
| §4–§5 | Ingest sources (description, comments, resolution); what is **not** text (transition history) |
| §6–§8 | Paragraph vs fixed-size comparison |
| §9 | **Hybrid** recommendation; **proposed** `max-chars` / `min-chars` / `overlap` (confirm to agree) |
| §10–§11 | Re-ingest triggers (**DEC-01** interim); delete-and-replace storage |
| §12 | Embedding / PgVector (**DEC-09** open) |
| §14 | **AC-RAG-ING-*** tests |

### [`spec/evaluation-strategy.md`](../spec/evaluation-strategy.md) structure (for reviewers)

| Section | Use |
|---------|-----|
| **§0** | Probabilistic-testing PDF map; eval BRF/FRI/IRI |
| §3–§4 | Grounding vs **retrieval quality**; what/how/why; deterministic vs probabilistic proof |
| §5 | **Example** eval corpus + five **PDF** questions → expected ticket ids (aligns with [`requirements.md`](../spec/requirements.md) §4.3) |
| §5.3 | Worked retrieval verdict examples |
| §6–§7 | Eval procedure; obtaining retrieved set (logs/harness) |
| §8 | Failure taxonomy **F-01…F-10** and detection |
| §9 | Eval log fields |
| §10 | **AC-EVAL-01…05** |
| §12 | Open **DEC-09**, metadata-filter **OQ** |

Procedure in the field: `commands/review-rag-output.md` (**Retrieval quality** — defers detail here).

### [`spec/ui-model.md`](../spec/ui-model.md) structure (for reviewers)

| Section | Use |
|---------|-----|
| **§0** | PDF `ui-flow` capability map; UI BRF/FRI/IRI |
| §4–§5 | Information architecture, app structure |
| §7–§8 | Screen catalog; CRUD flows |
| §9 | Status transition UX (**DEC-06** interim) |
| §10 | Ask / RAG user-visible surfaces |
| §11–§12 | Flows A–E UI mapping; demo §8.7 checklist |
| §13 | **AC-UI-01…12** |
| §14 | Open **DEC-06**, **11**, **15** |

### [`spec/test-strategy.md`](../spec/test-strategy.md) structure (for reviewers)

| Section | Use |
|---------|-----|
| **§0** | PDF proof map (SM integration, ask bands, no UI tests) |
| §5 | **State machine deterministic logic** — T1–T5, X1–X3, 20 illegal pairs, layers, FEAT-21 / AC-CORE-12…14 |
| §6 | **Retrieval quality + AI output** — Bands A (JUnit) / B (grounding) / C (probabilistic eval); AC-CORE-16…18 |
| §7 | **AC-CORE** → test layers (summary) |
| §8 | **AC-SM-01…08** quick map (detail in §5) |
| §9 | **AC-API-01…09** (§2.11 catalog) |
| §11 | UI acceptance substitute (no frontend tests); manual demo → [`ui-model.md`](../spec/ui-model.md) §12 |
| §13 | **AC-TS-01…06** (this spec’s own acceptance) |

## Writing bar

- Specs are detailed enough to implement without guessing: problem, scope/non-goals, requirements, acceptance criteria, contracts, open questions, revision history.
- **§0 document guide (standard):** each `spec/*.md` (except where hub-only extras apply) should include **§0** with at least a **PDF coverage map** for that file’s themes and, where useful, **business / functional / implementation** requirement tables (BRF/FRI/IRI ids). Hub completeness index: [`requirements.md`](../spec/requirements.md) **§0.5**. **Independent reading units** table (unit ids e.g. **HUB-***, **SM-***, **ING-***, **ASK-***, **API-***, **DM-***, **ARCH-***, **UI-***, **TS-***, **EVAL-***) — small sections readable alone; prerequisites column lists **this file** only. **Major `##` headings** suffix the unit id(s): ` · unit **SM-A**` or ` · units **SM-B**…**SM-E**` (see hub [`requirements.md`](../spec/requirements.md) **§0.8**). Rules and commands **point** to spec §0 — they do not duplicate spec bodies.
- **`rules/*.md` and `commands/*.md`** — keep a short **Revision history** table at the end when content changes (same `Date | Note` format as `spec/`).
- Prefer checklists; link acceptance criteria to backend tests or `commands/review-frontend.md` for UI.
- Map backend tests via [`spec/test-strategy.md`](../spec/test-strategy.md) and **`AC-CORE-*`** / **`AC-FEAT-*`** in [`spec/requirements.md`](../spec/requirements.md) (§8–§9).
- Distinguish **PDF requirement** vs **project convention** (see any `rules/*.md` “Assessment vs conventions” section).
- Do not invent features the PDF does not support.

## AI mistake log (PDF delivery artefact)

The assessment requires **at least one meaningful mistake** caught (wrong code **or** ungrounded RAG answer), documented to show AI is not blindly trusted.

When that happens, add an entry to **`docs/ai-mistakes.md`** (create the file on first entry):

```markdown
## YYYY-MM-DD — short title

- **What was wrong:** …
- **How detected:** review command / test / manual check …
- **Fix:** …
```

Review commands may **propose** an entry; confirm with the user before writing.

## Token optimisation (PDF)

- Use **graphify** for codebase exploration when `graphify-out/` exists (see `.cursor/rules/graphify.mdc`).
- Keep stable instructions in **always-apply** rule pointers so prompt caching can apply to repeated sessions.
- Optional: Caveman, codebase-memory MCP — use if installed; not required by this repo.

## Skill usage

When writing or revising specs or `docs/`, follow `skills/documentation/SKILL.md`.

---

## Revision history

| Date | Note |
|------|------|
| 2026-09-24 | Initial documentation layout, spec set index, rules/commands steering, and writing bar. |
| 2026-10-03 | Stack and workflow alignment; graphify and token-optimisation notes. |
| 2026-10-04 | Expanded `requirements.md` / `architecture.md` reviewer maps; OQ/DEC and AC traceability for tests. |
| 2026-10-04 | Governance pass with expanded [`spec/requirements.md`](../spec/requirements.md) and [`spec/architecture.md`](../spec/architecture.md). |
| 2026-10-04 | Added revision history section; documented history expectation for `rules/` and `commands/`. |
| 2026-10-04 | `data-model.md` reviewer map; spec index updated for agreed data model. |
| 2026-10-04 | Reviewer map: index catalog §14.5 in `data-model.md`. |
| 2026-10-04 | Rules/commands synced with agreed data model (fields, indexes, DEC split). |
| 2026-10-04 | Markdown links to existing `spec/requirements.md`, `architecture.md`, `data-model.md` in all `rules/` and `commands/` files. |
| 2026-10-04 | Three-spec repo: interim source map, §0.4 pointer, agreed vs open **DEC** list for rules/commands. |
| 2026-10-04 | Added [`state-machine.md`](../spec/state-machine.md) (draft); reviewer map; interim map no longer substitutes for SM. |
| 2026-10-04 | Added [`api-contract.md`](../spec/api-contract.md) (draft); reviewer map; removed API interim row. |
| 2026-10-04 | Linked [`rag-ingestion.md`](../spec/rag-ingestion.md), [`test-strategy.md`](../spec/test-strategy.md); reviewer § maps; spec set links. |
| 2026-10-04 | [`skills/documentation/SKILL.md`](../skills/documentation/SKILL.md) expanded; [`docs/assessment-brief.md`](../docs/assessment-brief.md) synced to full spec/rules/commands set. |
| 2026-10-04 | `api-contract.md` reviewer map: URI catalog, HTTP examples, §7 demo sequence. |
| 2026-10-04 | All ten PDF `spec/` files present; removed interim missing-spec map; PDF traceability via requirements §0.4 + assessment-brief §13. |
| 2026-10-04 | Added `commands/update-prompt-history.md` for SpecStory index maintenance. |
| 2026-10-04 | Reviewer map for [`evaluation-strategy.md`](../spec/evaluation-strategy.md); test-strategy §4.1 pointer. |
| 2026-10-04 | [`test-strategy.md`](../spec/test-strategy.md) reviewer map: §5 state machine, §6 retrieval/AI bands. |
| 2026-10-04 | Eight-file `spec/` index; `rag-api-contract` / `ui-flow` → `api-contract` §6, `architecture` §12. |
| 2026-10-04 | Added [`ui-model.md`](../spec/ui-model.md); PDF `ui-flow` detail + **AC-UI-***; `architecture` §12 summary only. |
| 2026-10-04 | `improve-from-assessment-pdf` pass: §6.8 spec-list row in requirements; assessment-brief §13 consolidation. |
| 2026-10-04 | Ten-file `spec/` set; [`rag-api-contract.md`](../spec/rag-api-contract.md) reviewer map; **AC-RAG-API-*** owner. |
| 2026-10-04 | Added `commands/improve-from-assessment-pdf.md` (edit-only assessment sync pass). |
| 2026-10-04 | Authoritative assignment path: `docs/Assessments.docx`. |
| 2026-10-04 | Reviewer maps: **§0** on all ten specs; writing bar §0 pattern; requirements **§0.5** pointer. |
| 2026-10-04 | Writing bar: independent reading unit ids (**HUB-***, **SM-***, …) for chunked spec reading. |
| 2026-10-04 | Writing bar: **`##` heading unit suffix** convention (`· unit **ID**`); hub §0.8. |
