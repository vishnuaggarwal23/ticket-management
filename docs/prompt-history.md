# Prompt history

Prompt and session history for this project is maintained with **SpecStory** (**PDF**: every prompt saved; Cursor / VS Code via SpecStory extension).

**Kiro:** If you use Kiro instead of Cursor, the PDF expects a small skill that records every prompt to a file — implement outside this repo or mirror the same `.specstory/history/` convention.

## Canonical location

- Directory: [`.specstory/history/`](../.specstory/history/)
- Keep SpecStory enabled for Cursor sessions on this repo so every prompt/session can be saved.

## Repository expectations

- `.specstory/history/` is tracked in git (see root `.gitignore` exceptions).
- Other SpecStory tool-local files (cli cache, statistics, etc.) remain ignored.
- Do not delete history files as a shortcut; if a file must be removed, confirm with the team first.
- **This index:** When SpecStory adds or **extends** a session file under `.specstory/history/`, run **`/update-prompt-history`** ([`commands/update-prompt-history.md`](../commands/update-prompt-history.md)) to sync the [Chronological session index](#chronological-session-index-cursor) below—add rows, **update existing rows** when the transcript changed, remove rows for deleted files, renumber and resort by UTC.

## Chronological session index (Cursor)

SpecStory export files, oldest first. Full transcripts (user + agent) are in the linked paths.

| # | Session (UTC) | Description | Transcript |
|---|---------------|-------------|------------|
| 1 | 2026-09-24 05:40:01 | Early repo bootstrap: create `docs`, `rules`, `commands`, `spec`, `.specstory/history`; add stack-aware `.gitignore`; initial graphify and assessment-driven setup (no application code). | [`.specstory/history/2026-09-24_05-40-01Z-project-directory-creation.md`](../.specstory/history/2026-09-24_05-40-01Z-project-directory-creation.md) |
| 2 | 2026-09-24 07:11:47 | Automated graphify subagent pass: extract a knowledge-graph JSON fragment from Cursor command/rule pointer files. | [`.specstory/history/2026-09-24_07-11-47Z-graphify-semantic-chunk-1.md`](../.specstory/history/2026-09-24_07-11-47Z-graphify-semantic-chunk-1.md) |
| 3 | 2026-09-24 17:21:45 | Review `spec/requirements.md` and `docs/assessment-brief.md` against the PDF; apply feedback (e.g. re-ingest wording, FR-11 state-machine clarity); commits. | [`.specstory/history/2026-09-24_17-21-45Z-assessment-requirements-review.md`](../.specstory/history/2026-09-24_17-21-45Z-assessment-requirements-review.md) |
| 4 | 2026-09-24 17:36:50 | Continue architecture spec: cross-review `architecture.md` against repo artefacts; refine and commit. | [`.specstory/history/2026-09-24_17-36-50Z-architecture-md-specification.md`](../.specstory/history/2026-09-24_17-36-50Z-architecture-md-specification.md) |
| 5 | 2026-09-24 17:36:50 | Start architecture step: draft `spec/architecture.md` from existing rules, docs, and specs—detailed system design only, no implementation. | [`.specstory/history/2026-09-24_17-36-50Z-lets-go-to-the.md`](../.specstory/history/2026-09-24_17-36-50Z-lets-go-to-the.md) |
| 6 | 2026-10-03 15:47:01 | Read-only audit of the working tree vs `Assessments.docx` (layout, Spec Kit, stack, gaps); detailed report in chat. | [`.specstory/history/2026-10-03_15-47-01Z-working-directory-audit.md`](../.specstory/history/2026-10-03_15-47-01Z-working-directory-audit.md) |
| 7 | 2026-10-03 16:32:55 | `/speckit-constitution`: replace placeholder with a project-specific constitution aligned to assessment workflow and existing specs/rules. | [`.specstory/history/2026-10-03_16-32-55Z-spec-kit-constitution.md`](../.specstory/history/2026-10-03_16-32-55Z-spec-kit-constitution.md) |
| 8 | 2026-10-03 17:04:09 | Expand `rules/java-springboot.md` (Boot 3, Java 21, PostgreSQL, layering); pointer `.mdc` pattern; align package tree as agreed convention in architecture. | [`.specstory/history/2026-10-03_17-04-09Z-java-spring-boot-rules.md`](../.specstory/history/2026-10-03_17-04-09Z-java-spring-boot-rules.md) |
| 9 | 2026-10-03 17:18:27 | Expand `rules/testing.md` (JUnit, Mockito, Testcontainers, unit/integration, positive/negative flows); PDF gap analysis across rules/commands. | [`.specstory/history/2026-10-03_17-18-27Z-testing-rules-for-cursor.md`](../.specstory/history/2026-10-03_17-18-27Z-testing-rules-for-cursor.md) |
| 10 | 2026-10-03 19:07:36 | Major `requirements.md` expansion from PDF (FR/FEAT, flows, AC, implementation sections); deepen examples; sync `rules/` and `commands/`. | [`.specstory/history/2026-10-03_19-07-36Z-requirements-specification-update.md`](../.specstory/history/2026-10-03_19-07-36Z-requirements-specification-update.md) |
| 11 | 2026-10-03 19:26:20 | Deep update to `spec/architecture.md` (modules, RAG, APIs, tech); follow-on edits to `rules/` and `commands/` for consistency. | [`.specstory/history/2026-10-03_19-26-20Z-architecture-specification-update.md`](../.specstory/history/2026-10-03_19-26-20Z-architecture-specification-update.md) |
| 12 | 2026-10-03 19:36:57 | Add revision-history sections to all files under `rules/` and `commands/`. | [`.specstory/history/2026-10-03_19-36-57Z-revision-history-for-rules.md`](../.specstory/history/2026-10-03_19-36-57Z-revision-history-for-rules.md) |
| 13 | 2026-10-03 19:43:17 | Create agreed `spec/data-model.md` (entities, RAG chunks, Liquibase, indexes); update rules/commands links and cross-references. | [`.specstory/history/2026-10-03_19-43-17Z-data-model-specification.md`](../.specstory/history/2026-10-03_19-43-17Z-data-model-specification.md) |
| 14 | 2026-10-03 20:00:04 | Re-check existing `spec/` files against PDF for gaps and clarity; user constraint: modify existing files only (no new spec files in that pass). | [`.specstory/history/2026-10-03_20-00-04Z-assessment-spec-completeness-review.md`](../.specstory/history/2026-10-03_20-00-04Z-assessment-spec-completeness-review.md) |
| 15 | 2026-10-03 20:10:18 | Add `state-machine.md` and `api-contract.md` (transitions, HTTP payloads, URIs); expand api-contract examples; commits. | [`.specstory/history/2026-10-03_20-10-18Z-state-machine-specification.md`](../.specstory/history/2026-10-03_20-10-18Z-state-machine-specification.md) |
| 16 | 2026-10-03 20:28:56 | Refresh `docs/assessment-brief.md` from PDF and current repo state; graphify run on the project. | [`.specstory/history/2026-10-03_20-28-56Z-assessment-brief-update.md`](../.specstory/history/2026-10-03_20-28-56Z-assessment-brief-update.md) |
| 17 | 2026-10-03 20:40:54 | State-machine/api-contract depth; `rag-ingestion.md` and test-strategy; rules/commands link fixes; assessment-brief/skills sync; PDF coverage and `prompt-history.md` index (edit-only passes). | [`.specstory/history/2026-10-03_20-40-54Z-specification-document-updates.md`](../.specstory/history/2026-10-03_20-40-54Z-specification-document-updates.md) |
| 18 | 2026-10-04 06:59:04 | Added `spec/evaluation-strategy.md` (retrieval quality, failure taxonomy F-01–F10, AC-EVAL, PDF example questions); synced cross-links in `spec/`, `rules/`, and `commands/`. | [`.specstory/history/2026-10-04_06-59-04Z-evaluation-strategy-document.md`](../.specstory/history/2026-10-04_06-59-04Z-evaluation-strategy-document.md) |
| 19 | 2026-10-04 07:40:06 | Added `spec/ui-model.md` (screens, CRUD, ask/RAG UX, **AC-UI-***); cross-linked nine-file spec set; ran `improve-from-assessment-pdf`; refreshed `docs/prompt-history.md`. | [`.specstory/history/2026-10-04_07-40-06Z-ui-model-specification.md`](../.specstory/history/2026-10-04_07-40-06Z-ui-model-specification.md) |

## Related

- Assessment hygiene also expects this index file at `docs/prompt-history.md`.
- PDF themes and spec coverage: [`spec/requirements.md`](../spec/requirements.md) §0.4 and [`docs/assessment-brief.md`](assessment-brief.md) §13.
- Human assessment summary: [`docs/assessment-brief.md`](assessment-brief.md).
- Significant AI mistakes caught during development are logged in `docs/ai-mistakes.md` (create when the first entry exists — **AC-CORE-23**).

## Revision history

| Date | Note |
|------|------|
| 2026-10-04 | Linked assessment brief §13 / requirements §0.4, AC-CORE-23 pointer. |
| 2026-10-04 | PDF Cursor/VS Code (SpecStory) and Kiro prompt-recording note. |
| 2026-10-04 | Chronological index of 17 Cursor sessions under `.specstory/history/`. |
| 2026-10-04 | Short per-session descriptions in the chronological table. |
| 2026-10-04 | Documented `commands/update-prompt-history.md` to regenerate this index. |
| 2026-10-04 | Index sync: command updates existing rows when `.specstory/history/` transcripts change. |
| 2026-10-04 | Synced chronological index from `.specstory/history/` (19 sessions; added 2, updated 2, removed 0). |
