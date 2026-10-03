# Documentation skills

Cursor attaches this file via [`.cursor/rules/documentation.mdc`](../.cursor/rules/documentation.mdc) (pointer only). Edit **this** file; do not copy the body into the `.mdc`.

How to document this project and **where every artefact lives**. For step-by-step spec writing, also use `skills/documentation/SKILL.md`.

## Rules and commands index (steering)

Use these during implementation and review. **Edit `rules/` and `commands/`**; `.cursor/rules/*.mdc` and `.cursor/commands/*.md` are **pointers only**.

| Topic | Rule | Command(s) |
|-------|------|------------|
| Backend Java / Spring | `rules/java-springboot.md` | `commands/review-code.md` |
| REST envelopes, `/api/v1`, list params | `rules/api-standards.md` | `commands/review-code.md` |
| Tests (backend only) | `rules/testing.md` | `commands/generate-tests.md`, `commands/review-code.md` |
| RAG ingest / ask / grounding | `rules/rag-vector-store.md` | `commands/review-rag-output.md`, `commands/review-code.md` |
| React / Vite / TypeScript UI | `rules/frontend.md` | `commands/review-frontend.md` |
| Spec quality before coding | `rules/documentation.md` (architecture § map) | `commands/review-spec.md` |
| This file (docs layout) | `rules/documentation.md` | — |

Assessment PDF (`docs/Assessments.pdf`, may be gitignored): extract requirements into `spec/requirements.md`. **Clones without the PDF** rely on `spec/` + these rules.

### When to run which command

| Situation | Command |
|-----------|---------|
| New or changed `spec/*.md` before coding | `commands/review-spec.md` |
| Starting a feature slice; need backend test ideas | `commands/generate-tests.md` |
| Backend Java / API / RAG code diff before merge | `commands/review-code.md` |
| React / Vite / TypeScript UI diff before merge | `commands/review-frontend.md` |
| Manual or demo check of an `/api/ai/ask` answer | `commands/review-rag-output.md` |
| Judging retrieval quality (right tickets in top-K?) | `commands/review-rag-output.md` → **Retrieval quality** + `spec/evaluation-strategy.md` |
| Caught wrong AI code or ungrounded answer | Note in `docs/ai-mistakes.md` (see below) |

Workflow (PDF): Requirement → Specification → **review-spec** → Plan/Tasks → Implementation → **generate-tests** / write tests → **review-code** / **review-frontend** → Fix.

## Where things live

| Artefact | Location |
|----------|----------|
| Assessment (authoritative assignment) | `docs/Assessments.pdf` (gitignored); restated in `spec/requirements.md` |
| Implementable specs | `spec/` (see list below) |
| Human docs / notes | `docs/` (e.g. `docs/assessment-brief.md`, `docs/prompt-history.md`, `docs/ai-mistakes.md`) |
| Prompt / session history | `.specstory/history/` |
| Engineering rules | `rules/` → `.cursor/rules/*.mdc` pointers |
| Slash commands | `commands/` → `.cursor/commands/*.md` pointers |
| Documentation skill (templates) | `skills/documentation/SKILL.md` |

## Spec set (create before implementation)

Maintain detailed specs as work proceeds:

`requirements.md`, `architecture.md`, `data-model.md`, `api-contract.md`, `state-machine.md`, `rag-ingestion.md`, `rag-api-contract.md`, `evaluation-strategy.md`, `ui-flow.md`, `test-strategy.md`.

| Spec | What it must nail down (so rules do not guess) |
|------|-----------------------------------------------|
| `requirements.md` | PDF hub: **FR** / **FEAT-***, **AC-CORE-*** (§8) and **AC-FEAT-*** (§4.2); **OQ-*** / **DEC-*** register and spec handoff (§10); precedence PDF → requirements → agreed specs → rules (§2.4); deterministic vs probabilistic proof (§2.5); demo script (§8.7); glossary (§12). Detail contracts live in child specs (§13). |
| `architecture.md` | System design: business vs functional modules, ticket aggregate shape, tech/deployment, **communication** (sync REST), **API map**, **PgVector** index role, RAG pipeline; **chunking** and **embedding tradeoffs** (PDF NFR-07 / AC-CORE-19). Numeric chunk/K/model → `rag-ingestion.md` |
| `data-model.md` | Ticket id format, fields, comments, resolution notes, category |
| `api-contract.md` | Ticket/comment payloads **inside** `data` (envelopes are in `rules/api-standards.md`) |
| `state-machine.md` | Legal/illegal transitions (including skipped steps if any) |
| `rag-ingestion.md` | Chunking **values**, models, dimensions, ingest timing, property keys |
| `rag-api-contract.md` | Ask `data` fields: answer, citations, no-match wording |
| `evaluation-strategy.md` | How to judge **retrieval quality** (PDF: probabilistic; not unit-test golden strings) |
| `ui-flow.md` | Screens, navigation, how status transition and ask are triggered |
| `test-strategy.md` | Map acceptance criteria → backend tests (`rules/testing.md`) |

**Agreed spec:** status `agreed` (or equivalent) in the spec header, or explicit user confirmation in chat. Until then, treat as **draft** — implement only what rules already lock (envelopes, stack conventions) and stop for open payloads.

### `spec/requirements.md` structure (for reviewers)

Use this map when running `commands/review-spec.md` or tracing tests — do not duplicate the full text in rules.

| Section | Use |
|---------|-----|
| §0 | How to read the file; audience |
| §2.4–2.7 | Precedence, deterministic vs probabilistic, status enum, anti-patterns |
| §4.1–4.3 | Flows A–E; per-feature AC; **Example** demo corpus (not mandated seed data) |
| §8, §8.7 | **AC-CORE-*** sign-off; demo / grading script |
| §9 | FR → AC → FEAT traceability |
| §10 | **OQ-*** catalogue, **DEC-*** register (agree before implementing), handoff to child specs |
| §11 | PDF wording tensions (e.g. re-ingest on close — **DEC-01**) |
| §12–§13 | Glossary; what must not live in `requirements.md` |

Closing an **OQ** requires updating the owning child spec, recording **Decision** in §10.2, and user confirmation — see §10.3.

### `spec/architecture.md` structure (for reviewers)

Use when running `commands/review-spec.md` or tracing implementation to layers — do not duplicate the full text in rules.

| Section | Use |
|---------|-----|
| §3 | Architectural principles (monolith, grounded AI, spec-driven) |
| §4 | **Business** modules, actors, capability map → FEAT ids |
| §5 | **Ticket** aggregate (conceptual); RAG text sources; not field catalogs |
| §6–7 | System context, stack, deployment topology |
| §8–9 | **Functional** modules and **technical** packages (`api` / `domain` / `service` / `persistence` / `rag` / `config`) |
| §10 | **Communication** — sync JSON REST, sequences, error vs ask no-match |
| §11 | API capability map (PDF ask path + Convention `/api/v1`) |
| §12 | Frontend surfaces (Convention React+Vite+TS) |
| §13–14 | Relational SoR vs **vector DB** (PgVector, chunk lifecycle) |
| §15–16 | RAG pipeline, knowledge docs, **chunking justification**, **embedding tradeoffs** |
| §17 | State machine placement in domain |
| §21 | **OQ** / **DEC** pointers (sync with `requirements.md` §10) |

Field-level tickets, Liquibase, and ask `data` JSON stay in child specs (§22 table). Architecture **labels** PDF vs **Convention** vs **Open**.

## Writing bar

- Specs are detailed enough to implement without guessing: problem, scope/non-goals, requirements, acceptance criteria, contracts, open questions, revision history.
- Prefer checklists; link acceptance criteria to backend tests or `commands/review-frontend.md` for UI.
- Map backend tests to **`AC-CORE-*`** / **`AC-FEAT-*`** in `spec/requirements.md` (§8–§9) when proposing or reviewing tests.
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
