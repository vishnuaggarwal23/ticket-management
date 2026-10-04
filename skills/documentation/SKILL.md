# Documentation skill

Use this skill whenever creating or revising specs, architecture notes, API docs, RAG docs, developer-facing markdown, or syncing human summaries (`docs/assessment-brief.md`) with `spec/`, `rules/`, and `commands/`.

**Canonical index:** [`rules/documentation.md`](../../rules/documentation.md) (artefact locations, reviewer section maps, workflow). **Assessment summary:** [`docs/assessment-brief.md`](../../docs/assessment-brief.md).

## Goals

- Specs are the source of truth and must be **detailed enough to implement without guessing**.
- Documentation explains **why** (trade-offs), not only what.
- **PDF vs Convention vs Open vs Reference** — label claims. **Open** = in PDF scope, needs **DEC-*** / child spec before implementation. **Reference** = out of PDF ([`requirements.md`](../../spec/requirements.md) **§2.3**) — document only; **no** plan/tasks/code.
- Prompt history remains in SpecStory (`.specstory/history/`); index at [`docs/prompt-history.md`](../../docs/prompt-history.md).
- **Revision history** — every substantive edit to `spec/*.md`, `rules/*.md`, `commands/*.md`, `skills/**/*.md`, and `docs/assessment-brief.md` adds a dated row to that file’s revision table.

## PDF hygiene checklist (do not skip)

All items from `docs/Assessments.docx` p.1–2 must exist in-repo before implementation sign-off — see [`requirements.md`](../../spec/requirements.md) **§6.8** (concrete paths to `rules/*` and `commands/*`). Spec list: ten names under `spec/` per assignment p.2 (see requirements child-spec table). **Completeness index:** requirements **§0.4** (theme map) + **§0.5** (verbatim anchors); each child spec **§0** (file-local PDF map + BRF/FRI/IRI where applicable) — see [`rules/documentation.md`](../../rules/documentation.md) writing bar.

## Before writing

1. Confirm audience (implementer, reviewer, assessor).
2. Confirm target path (`spec/...` vs `docs/...` vs `rules/...`).
3. If requirements are unclear, **ask the user**—do not invent.
4. For codebase exploration when `graphify-out/` exists, run `graphify query` first (see `.cursor/rules/graphify.mdc`).

## Workflow (PDF)

```
Requirement → Specification → review-spec → Plan/Tasks → Implementation → generate-tests / tests → review-code / review-frontend → Fix
```

| Step | Command / rule |
|------|----------------|
| Spec quality | [`commands/review-spec.md`](../../commands/review-spec.md) |
| Backend tests ideas | [`commands/generate-tests.md`](../../commands/generate-tests.md) + [`spec/test-strategy.md`](../../spec/test-strategy.md) |
| Backend code | [`commands/review-code.md`](../../commands/review-code.md) + `rules/java-springboot.md` |
| UI code | [`commands/review-frontend.md`](../../commands/review-frontend.md) + `rules/frontend.md` |
| Ask grounding | [`commands/review-rag-output.md`](../../commands/review-rag-output.md) + `rules/rag-vector-store.md` |

## Spec set (ten files on disk — 2026-10-04)

Implement from these; **draft** unless header says **agreed**. PDF themes: [`requirements.md`](../../spec/requirements.md) **§0.4** + **§0.5**; per-file **§0** maps; summary [`assessment-brief.md`](../../docs/assessment-brief.md) **§4** and **§13**. PDF UI spec: [`ui-flow.md`](../../spec/ui-flow.md) (UI architecture summary: [`architecture.md`](../../spec/architecture.md) **§12**).

| File | Status | Primary content | §0 (PDF map / triad) |
|------|--------|-----------------|----------------------|
| [`requirements.md`](../../spec/requirements.md) | draft hub | FEAT-*, AC-CORE-*, flows A–E, OQ/DEC §10, §0.4 + **§0.5** | Hub §0.1–0.5 |
| [`architecture.md`](../../spec/architecture.md) | draft | Modules, APIs, RAG §15–16; frontend **§12**; verbatim RAG ladder | §0 |
| [`ui-flow.md`](../../spec/ui-flow.md) | draft | PDF `ui-flow`: screens, flows, **AC-UI-*** | §0 |
| [`data-model.md`](../../spec/data-model.md) | **agreed** | Entities, Liquibase §14, DTOs §10, AC-DM-* | §0 |
| [`state-machine.md`](../../spec/state-machine.md) | draft | T1–T5, X1–X3, §5.6 illegal matrix, REST ex §6.5, AC-SM-* | §0 |
| [`api-contract.md`](../../spec/api-contract.md) | draft | HTTP §2.11; ticket payloads §3–§5; ask §6 summary | §0 |
| [`rag-api-contract.md`](../../spec/rag-api-contract.md) | draft | Ask HTTP + `data`; **AC-RAG-API-*** | §0 |
| [`rag-ingestion.md`](../../spec/rag-ingestion.md) | draft | Hybrid chunking **DEC-16**; **DEC-09** embed; sync ingest **DEC-18**; re-ingest **DEC-01** | §0 |
| [`evaluation-strategy.md`](../../spec/evaluation-strategy.md) | draft | Retrieval quality eval (FEAT-22), AC-EVAL-* | §0 |
| [`test-strategy.md`](../../spec/test-strategy.md) | draft | **§5** SM determinism; **§6** ask bands; AC layer maps | §0 |

## Engineering rules (`rules/` — edit these; `.cursor/rules/*.mdc` are pointers only)

| Rule | Use when |
|------|----------|
| [`java-springboot.md`](../../rules/java-springboot.md) | Backend layering, packages, services, persistence |
| [`api-standards.md`](../../rules/api-standards.md) | Envelopes, `/api/v1`, pagination, 409 transitions |
| [`testing.md`](../../rules/testing.md) | JUnit, Mockito, Testcontainers, API integration |
| [`rag-vector-store.md`](../../rules/rag-vector-store.md) | Ingest, PgVector, ask grounding (not numeric invention) |
| [`frontend.md`](../../rules/frontend.md) | React + Vite + TS; no frontend tests this milestone |
| [`documentation.md`](../../rules/documentation.md) | Where artefacts live; reviewer section maps |

## Commands (`commands/` — edit these; `.cursor/commands/*.md` are pointers only)

| Command | Use when |
|---------|----------|
| [`review-spec.md`](../../commands/review-spec.md) | Before implementing from changed specs |
| [`generate-tests.md`](../../commands/generate-tests.md) | Backend test proposals from specs |
| [`review-code.md`](../../commands/review-code.md) | Java / API / RAG diffs |
| [`review-frontend.md`](../../commands/review-frontend.md) | UI diffs |
| [`review-rag-output.md`](../../commands/review-rag-output.md) | Manual ask answer / retrieval review |
| [`update-prompt-history.md`](../../commands/update-prompt-history.md) | Rebuild `docs/prompt-history.md` from `.specstory/history/` |
| [`update-ai-error.md`](../../commands/update-ai-error.md) | Append/insert chronological AI errors in [`docs/ai-error.md`](../../docs/ai-error.md) |
| [`improve-from-assessment-pdf.md`](../../commands/improve-from-assessment-pdf.md) | Assignment sync from `docs/Assessments.docx`; improve **existing** artefacts only (not `prompt-history.md` or `ai-error.md`; **no new files**) |

## Spec template (minimum sections)

0. **Document guide (§0)** — PDF coverage map for this file; optional business / functional / implementation requirement tables; TOC when long; **independent reading units** table; major **`##` headings** tagged `· unit **ID**` (hub [`requirements.md`](../../spec/requirements.md) **§0.8**)
1. Title & status (draft / agreed / deprecated) + primary source (PDF → requirements)
2. Problem / context
3. Scope & non-goals (include **Reference** §2.3 pointer when listing out-of-PDF topics)
4. Requirements (functional / non-functional) + traceability to FEAT / AC-CORE
5. Acceptance criteria (testable IDs: **AC-*** family per domain)
6. Contracts / data / flows (as relevant)
7. Open questions & decisions (DEC ids)
8. **Revision history** (dated rows)

## Acceptance ID families (for cross-linking tests and reviews)

| Prefix | Owner spec |
|--------|------------|
| **AC-CORE-*** | [`requirements.md`](../../spec/requirements.md) §8 |
| **AC-FEAT-*** | [`requirements.md`](../../spec/requirements.md) §4.2 |
| **AC-DM-*** | [`data-model.md`](../../spec/data-model.md) §18 |
| **AC-SM-*** | [`state-machine.md`](../../spec/state-machine.md) §9 |
| **AC-API-*** | [`api-contract.md`](../../spec/api-contract.md) §9 |
| **AC-RAG-ING-*** | [`rag-ingestion.md`](../../spec/rag-ingestion.md) §14 |
| **AC-RAG-API-*** | [`rag-api-contract.md`](../../spec/rag-api-contract.md) §17 |
| **AC-EVAL-*** | [`evaluation-strategy.md`](../../spec/evaluation-strategy.md) |
| **AC-UI-*** | [`ui-flow.md`](../../spec/ui-flow.md) §13 |
| **AC-TS-*** | [`test-strategy.md`](../../spec/test-strategy.md) §12 |

Map backend tests through [`test-strategy.md`](../../spec/test-strategy.md) first; details in `rules/testing.md`.

## RAG / architecture docs

[`spec/architecture.md`](../../spec/architecture.md) is the home for **system design** (see `rules/documentation.md` for section map). Always document and justify:

- Business vs functional modules and backend layers (§4, §8–9)
- Ticket aggregate and RAG text sources (§5.3)
- Communication (sync REST) and API map (§10–11)
- Vector store (PgVector convention) vs PostgreSQL SoR (§13–14)
- RAG ingest and ask pipeline (§15)
- Chunking **justification** (§16); mechanics + **agreed defaults** → [`rag-ingestion.md`](../../spec/rag-ingestion.md) (**DEC-16**)
- Embedding: **DEC-09** — Ollama `nomic-embed-text`, **768** dims; tradeoffs in architecture §16.3
- Configurable top-K/threshold/chunk sizes — **DEC-16** defaults in `rag-ingestion.md` §9.3, §12
- Ask: **DEC-11** no-match; **DEC-17** request limits and citation order ([`rag-api-contract.md`](../../spec/rag-api-contract.md))
- Ingest: **DEC-18** synchronous after DB commit; failure visibility §10.2
- Eval: **DEC-19** — no metadata pre-filter on ask in v1
- Do not spec or implement **Reference** RAG/UI features (§2.3): confidence scores, async ingest job queues, metadata ask filters, etc.
- Retrieval quality eval ([`evaluation-strategy.md`](../../spec/evaluation-strategy.md) §3–§9; **AC-EVAL-***; test bands — [`test-strategy.md`](../../spec/test-strategy.md) **§5–§6**)

**Chunking default (agreed DEC-16):** hybrid paragraph/comment-boundary first, fixed-size overflow for long blocks — `rag-ingestion.md` §9.3 (800/120/80, top-k 8, threshold 0.72, cosine).

## API and state machine docs

- HTTP envelopes and versioning: `rules/api-standards.md` — payloads/scenarios: [`api-contract.md`](../../spec/api-contract.md)
- Transitions: [`state-machine.md`](../../spec/state-machine.md) — illegal edges §5.6; PATCH `status` §6

## AI mistake log

When a meaningful AI mistake is caught (bad code or ungrounded answer), run [`commands/update-ai-error.md`](../../commands/update-ai-error.md) so [`docs/ai-error.md`](../../docs/ai-error.md) gets a chronological entry: id, when, kind, what was wrong, how detected, **how resolved**. [`docs/ai-mistakes.md`](../../docs/ai-mistakes.md) is a pointer for **AC-CORE-23**.

## Revision history

| Date | Note |
|------|------|
| 2026-09-24 | Initial skill: spec template, RAG/architecture guidance, ai-mistakes. |
| 2026-10-04 | Synced with seven `spec/` files, full `rules/` + `commands/` index, AC-* families, workflow, assessment-brief pointer, revision-history rule. |
| 2026-10-04 | PDF lists ten spec names; nine files on disk; AC-RAG-API / AC-EVAL / AC-UI families; §0.4 / assessment-brief §13. |
| 2026-10-04 | PDF hygiene checklist → requirements §6.8. |
| 2026-10-04 | `update-prompt-history` command in commands index. |
| 2026-10-04 | RAG eval pointer to [`evaluation-strategy.md`](../../spec/evaluation-strategy.md) §3–§9 in RAG docs section. |
| 2026-10-04 | Nine-file spec set; **AC-UI-*** owner → [`ui-flow.md`](../../spec/ui-flow.md). |
| 2026-10-04 | Ten-file spec set; **AC-RAG-API-*** → [`rag-api-contract.md`](../../spec/rag-api-contract.md). |
| 2026-10-04 | `improve-from-assessment-pdf`: hygiene checklist ten names / nine files wording. |
| 2026-10-04 | Spec §0 pattern, requirements §0.5, updated spec-set table; aligned with `rules/documentation.md` reviewer maps. |
| 2026-10-04 | Spec template: **`##` heading unit suffix**; search `· unit **` to jump chunks. |
| 2026-10-04 | **Reference** label + §2.3; rules/commands sync for out-of-PDF scope. |
| 2026-10-04 | **DEC-01…19** agreed; RAG/ingest/ask pointers updated; chunk defaults **DEC-16**. |
| 2026-10-04 | AI error log [`docs/ai-error.md`](../../docs/ai-error.md); command `update-ai-error`. |
