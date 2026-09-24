# Documentation skill

Use this skill whenever creating or revising specs, architecture notes, API docs, RAG docs, or developer-facing markdown in this repo.

## Goals

- Specs are the source of truth and must be **detailed enough to implement without guessing**.
- Documentation explains **why** (trade-offs), not only what.
- Prompt history remains in SpecStory (`.specstory/history/`); index at `docs/prompt-history.md`.

## Before writing

1. Confirm audience (implementer, reviewer, assessor).
2. Confirm target path (`spec/...` vs `docs/...`).
3. If requirements are unclear, **ask the user**—do not invent.

## Spec template (minimum sections)

1. Title & status (draft / agreed / deprecated)
2. Problem / context
3. Scope & non-goals
4. Requirements (functional / non-functional)
5. Acceptance criteria (testable, checklist)
6. Contracts / data / flows (as relevant)
7. Open questions
8. Revision history (short)

## RAG / architecture docs

Always document and justify:

- Chunking strategy for ticket data
- Embedding model choice (local vs cloud) and cost/latency/quality
- Configurable retrieval parameters (top-K, similarity threshold)
- Grounding rules and no-match behavior

## AI mistake log

When a meaningful AI mistake is caught (bad code or ungrounded answer), add an entry under `docs/ai-mistakes.md` with: date, what was wrong, how it was detected, fix.
