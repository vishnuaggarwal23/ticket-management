# Review code

Review the current change set (or specified paths) as a senior Java / Spring Boot tech lead.

## Inputs

- Relevant specs under `spec/` (especially `api-contract.md`, `state-machine.md`, `test-strategy.md`)
- Diff / files to review
- Project rules: Java Spring Boot, API standards, testing, RAG (if AI code touched)

## Checklist

1. **Spec alignment** — Does the code implement only agreed specs? Any scope creep?
2. **Correctness** — Logic bugs, null handling, edge cases, illegal ticket status transitions.
3. **API** — Status codes, validation, error bodies, DTO shapes vs contract.
4. **Security / hygiene** — No secrets, injection risks, over-exposure of internals.
5. **Spring style** — Thin controllers, service transactions, constructor injection.
6. **Tests** — Are acceptance criteria covered? Missing state-machine or validation tests?
7. **RAG (if applicable)** — Configurable retrieval; re-ingestion on update; grounding guards present?

## Output format

- **Findings** (severity: blocker / major / minor) with file references
- **Spec gaps** (code without spec or spec without code)
- **Suggested fixes** (brief; do not implement unless asked)
- **AI risk** — Any likely hallucinated APIs, wrong Spring Boot 3 APIs, or ungrounded assumptions

Confirm with the user before applying fixes.
