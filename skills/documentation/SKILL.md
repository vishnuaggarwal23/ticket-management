# Documentation skill

Use this skill whenever creating or revising specs, architecture notes, API docs, RAG docs, developer-facing markdown, or syncing human summaries (`docs/assessment-brief.md`) with `spec/`, `rules/`, and `commands/`.

**Canonical index:** [`rules/documentation.md`](../../rules/documentation.md) (artefact locations, reviewer section maps, workflow). **Assessment summary:** [`docs/assessment-brief.md`](../../docs/assessment-brief.md).

## Goals

- Specs are the source of truth and must be **detailed enough to implement without guessing**.
- Documentation explains **why** (trade-offs), not only what.
- **PDF vs Convention vs Open** — label claims; open items stay in spec **Open questions** / requirements **§10.2 (DEC-*)** until the user confirms.
- Prompt history remains in SpecStory (`.specstory/history/`); index at [`docs/prompt-history.md`](../../docs/prompt-history.md).
- **Revision history** — every substantive edit to `spec/*.md`, `rules/*.md`, `commands/*.md`, `skills/**/*.md`, and `docs/assessment-brief.md` adds a dated row to that file’s revision table.

## PDF hygiene checklist (do not skip)

All items from `docs/Assessments.docx` p.1–2 must exist in-repo before implementation sign-off — see [`requirements.md`](../../spec/requirements.md) **§6.8** (concrete paths to `rules/*` and `commands/*`). Spec list: ten names under `spec/` per assignment p.2 (eight files on disk — see requirements child-spec table).

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

## Spec set (eight files on disk; PDF lists ten names — 2026-10-04)

Implement from these; **draft** unless header says **agreed**. PDF themes: [`requirements.md`](../../spec/requirements.md) **§0.4**; summary [`assessment-brief.md`](../../docs/assessment-brief.md) **§13**. PDF-only names `rag-api-contract.md` / `ui-flow.md` → [`api-contract.md`](../../spec/api-contract.md) **§6.2–§6.5** and [`architecture.md`](../../spec/architecture.md) **§12.3–§12.6**.

| File | Status | Primary content |
|------|--------|-----------------|
| [`requirements.md`](../../spec/requirements.md) | draft hub | FEAT-*, AC-CORE-*, flows A–E, OQ/DEC §10, §0.4 coverage |
| [`architecture.md`](../../spec/architecture.md) | draft | Modules, APIs, RAG §15–16; **UI** §12.3–§12.6 (PDF `ui-flow`) |
| [`data-model.md`](../../spec/data-model.md) | **agreed** | Entities, Liquibase §14, DTOs §10, AC-DM-* |
| [`state-machine.md`](../../spec/state-machine.md) | draft | T1–T5, X1–X3, §5.6 illegal matrix, AC-SM-* |
| [`api-contract.md`](../../spec/api-contract.md) | draft | HTTP §2.11; payloads §3–§6; ask **§6.2–§6.5** (**AC-RAG-API-***) |
| [`rag-ingestion.md`](../../spec/rag-ingestion.md) | draft | Hybrid chunking §6–§9, re-ingest §10, AC-RAG-ING-* |
| [`evaluation-strategy.md`](../../spec/evaluation-strategy.md) | draft | Retrieval quality eval (FEAT-22), AC-EVAL-* |
| [`test-strategy.md`](../../spec/test-strategy.md) | draft | **§5** SM determinism; **§6** ask bands; AC layer maps |

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
| [`improve-from-assessment-pdf.md`](../../commands/improve-from-assessment-pdf.md) | Assignment sync from `docs/Assessments.docx`; improve **existing** artefacts only (not `prompt-history.md`; **no new files**) |

## Spec template (minimum sections)

1. Title & status (draft / agreed / deprecated) + primary source (PDF → requirements)
2. Problem / context
3. Scope & non-goals
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
| **AC-RAG-API-*** | [`api-contract.md`](../../spec/api-contract.md) §6.2–§6.5 |
| **AC-EVAL-*** | [`evaluation-strategy.md`](../../spec/evaluation-strategy.md) |
| **AC-UI-*** | [`architecture.md`](../../spec/architecture.md) §12.3–§12.6 |
| **AC-TS-*** | [`test-strategy.md`](../../spec/test-strategy.md) §12 |

Map backend tests through [`test-strategy.md`](../../spec/test-strategy.md) first; details in `rules/testing.md`.

## RAG / architecture docs

[`spec/architecture.md`](../../spec/architecture.md) is the home for **system design** (see `rules/documentation.md` for section map). Always document and justify:

- Business vs functional modules and backend layers (§4, §8–9)
- Ticket aggregate and RAG text sources (§5.3)
- Communication (sync REST) and API map (§10–11)
- Vector store (PgVector convention) vs PostgreSQL SoR (§13–14)
- RAG ingest and ask pipeline (§15)
- Chunking **justification** (§16); **mechanics and proposed numbers** → [`rag-ingestion.md`](../../spec/rag-ingestion.md)
- Embedding model tradeoffs (§16.3); model id / dimension **Open** → **DEC-09**
- Configurable top-K and threshold (property keys in `rag-ingestion.md`; values open until agreed)
- Grounding and no-match ([`api-contract.md`](../../spec/api-contract.md) §6.2–§6.5; wording **DEC-11**)
- Retrieval quality eval ([`evaluation-strategy.md`](../../spec/evaluation-strategy.md) §3–§9; **AC-EVAL-***; test bands — [`test-strategy.md`](../../spec/test-strategy.md) **§5–§6**)

**Chunking default (draft):** hybrid paragraph/comment-boundary first, fixed-size overflow for long blocks — see `rag-ingestion.md` §9. Confirm **proposed** §9.3 numbers with the user before marking agreed.

## API and state machine docs

- HTTP envelopes and versioning: `rules/api-standards.md` — payloads/scenarios: [`api-contract.md`](../../spec/api-contract.md)
- Transitions: [`state-machine.md`](../../spec/state-machine.md) — illegal edges §5.6; PATCH `status` §6

## AI mistake log

When a meaningful AI mistake is caught (bad code or ungrounded answer), add an entry under [`docs/ai-mistakes.md`](../../docs/ai-mistakes.md) with: date, what was wrong, how it was detected, fix.

## Revision history

| Date | Note |
|------|------|
| 2026-09-24 | Initial skill: spec template, RAG/architecture guidance, ai-mistakes. |
| 2026-10-04 | Synced with seven `spec/` files, full `rules/` + `commands/` index, AC-* families, workflow, assessment-brief pointer, revision-history rule. |
| 2026-10-04 | All ten PDF `spec/` files; AC-RAG-API / AC-EVAL / AC-UI families; traceability §0.4 / assessment-brief §13. |
| 2026-10-04 | PDF hygiene checklist → requirements §6.8. |
| 2026-10-04 | `update-prompt-history` command in commands index. |
| 2026-10-04 | RAG eval pointer to [`evaluation-strategy.md`](../../spec/evaluation-strategy.md) §3–§9 in RAG docs section. |
