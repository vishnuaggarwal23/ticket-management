# Generate tests

Generate tests from **agreed specs** and acceptance criteria—not from guesswork.

## Inputs

- Specs: especially `test-strategy.md`, `state-machine.md`, `api-contract.md`, `rag-api-contract.md`, `evaluation-strategy.md`
- Existing test layout and frameworks in the repo
- Code under test (if already present)
- Testing standards: `rules/testing.md` / `.cursor/rules/testing.mdc`

## Rules

- Map test names to acceptance criteria. Given/When/Then is a **useful pattern**, not a required template.
- Cover valid and **invalid** status transitions.
- Cover validation failures and meaningful API errors.
- Backend integration tests that need a database: **PostgreSQL via Testcontainers** (Liquibase applied). Do not default to H2. Do not depend on a developer’s local Postgres or Compose.
- Run tests with **Maven Wrapper** (`./mvnw test` or equivalent). Do not rely on a machine-local Maven.
- **Deterministic** ticket/API behavior belongs in ordinary unit/integration tests. RAG **retrieval quality** and free-form generated answers belong in the evaluation approach — do not treat a golden string as sufficient RAG proof.
- For RAG **contract** cases once specified: citations + **no relevant tickets**; use test doubles for embedding/generation in the default suite. Do not call paid or developer-local Ollama unless a later evaluation spec says so. There is **no CI** in this milestone.
- Do not invent endpoints or fields absent from specs—ask first.
- Do not prescribe a frontend test framework.

## Output

1. List of proposed test cases mapped to acceptance criteria
2. Confirm with user if any criterion is ambiguous
3. Only then write test code in the correct packages

If implementation is missing, generate test stubs/interfaces that will fail until code exists—or wait for implementation if the user prefers.
