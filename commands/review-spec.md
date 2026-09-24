# Review spec

Review specifications under `spec/` (or a named spec file) before implementation.

## Inputs

- Target spec file(s)
- `docs/Assessments.pdf` requirements only as background—specs must stand alone
- Related specs (requirements, architecture, api-contract, state-machine, rag-*, test-strategy, ui-flow)

## Checklist

1. **Completeness** — Problem, scope/non-goals, requirements, acceptance criteria, open questions.
2. **Testability** — Every acceptance criterion can become a test.
3. **Consistency** — No contradictions across specs (especially status transitions vs API).
4. **RAG readiness** — Ingestion fields/metadata, re-ingest rules, grounding, citations, no-match behavior, configurable top-K/threshold.
5. **Non-goals** — Explicitly out of scope (e.g. autonomous agent actions).
6. **Detail bar** — An implementer should not need to invent endpoints, fields, or transitions.

## Output format

- **Gaps / ambiguities** (must resolve before coding)
- **Contradictions**
- **Suggested edits** (do not rewrite the whole spec unless asked)
- **Ready for implementation?** yes / no — if no, list blocking questions for the user
