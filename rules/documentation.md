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
| Spec quality before coding | — | `commands/review-spec.md` |
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
| `requirements.md` | PDF-derived FR/NFR and acceptance checklist |
| `architecture.md` | **Chunking strategy** and **embedding model tradeoffs** (PDF NFR-07); system context |
| `data-model.md` | Ticket id format, fields, comments, resolution notes, category |
| `api-contract.md` | Ticket/comment payloads **inside** `data` (envelopes are in `rules/api-standards.md`) |
| `state-machine.md` | Legal/illegal transitions (including skipped steps if any) |
| `rag-ingestion.md` | Chunking **values**, models, dimensions, ingest timing, property keys |
| `rag-api-contract.md` | Ask `data` fields: answer, citations, no-match wording |
| `evaluation-strategy.md` | How to judge **retrieval quality** (PDF: probabilistic; not unit-test golden strings) |
| `ui-flow.md` | Screens, navigation, how status transition and ask are triggered |
| `test-strategy.md` | Map acceptance criteria → backend tests (`rules/testing.md`) |

**Agreed spec:** status `agreed` (or equivalent) in the spec header, or explicit user confirmation in chat. Until then, treat as **draft** — implement only what rules already lock (envelopes, stack conventions) and stop for open payloads.

## Writing bar

- Specs are detailed enough to implement without guessing: problem, scope/non-goals, requirements, acceptance criteria, contracts, open questions, revision history.
- Prefer checklists; link acceptance criteria to backend tests or `commands/review-frontend.md` for UI.
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
