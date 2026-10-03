# Testing guidelines

Tests prove **acceptance criteria** from `spec/`. The assessment PDF requires state-machine integration tests, persistence across restart, backend validation, and honest RAG no-match/citation behavior. It does **not** mandate JUnit, Testcontainers, H2, coverage percentages, or a frontend test stack.

## Assessment vs project conventions

**This project’s approved testing conventions** (not PDF mandates):

- **JUnit 5**
- Backend integration tests: **PostgreSQL with Testcontainers** (PgVector available in that image/setup as required for schema that uses it)
- **Liquibase** migrations applied in integration tests so schema matches what the application runs
- Run tests with **Maven Wrapper** (`./mvnw test` or the equivalent wrapper goal) — do not rely on a locally installed Maven
- **No CI** in this milestone — local, repeatable test runs only
- Docker Compose is for **local infrastructure**, not a substitute for Testcontainers in the backend integration suite

Do not assume unfinalized versions (Spring Boot 3 patch, PostgreSQL, Testcontainers, embedding model).

## Layers

Keep responsibilities distinct. Do not require a particular test-class layout.

| Layer | Responsibility |
|-------|----------------|
| **Unit** | Deterministic domain and application logic in isolation (state machine, validation rules, pure services). No real database, no network, no Ollama. **JUnit 5**. |
| **Integration** | Real **PostgreSQL** (Testcontainers), Liquibase-applied schema, persistence, and backend behavior that depends on the database (including status transitions that must survive storage). Use Spring Boot’s test support as needed (for example `@SpringBootTest`). |
| **End-to-end** | User-visible flows across UI and API when those exist. Do **not** prescribe a frontend test framework here (none is an approved testing decision yet). Compose may back a local full stack; it must not be the only way backend integration tests can run. |

HTTP-adapter tests that exercise controllers without a database are **unit/slice-style** tests of the web layer, not PostgreSQL integration tests. Spring MVC test utilities that ship with Spring Boot may be used; extra assertion or mocking libraries are **not** required by these standards (propose them separately if needed).

## Deterministic behavior vs RAG

- **Deterministic:** ticket CRUD, comments, search/filter, input validation, status transitions (valid and invalid), and persistence. These must have stable, repeatable tests with fixed expected results.
- **Probabilistic / RAG:** retrieval quality and generated answers can vary. They need a **separate evaluation approach** (grounding, citations, no-match). Do **not** treat a single golden string as sufficient RAG “proof.” Detailed RAG evaluation criteria belong in RAG and evaluation standards — not in this file.
- Ticket **re-ingestion** after update or close is an assessment behavior to verify; how embeddings are scored is a RAG/evaluation concern.

## Backend integration tests (PostgreSQL)

- Default and preferred integration database is **PostgreSQL via Testcontainers**, not H2 and not a developer’s already-running local Postgres.
- **H2** is not the default or preferred stand-in. Mention it only when **explicitly justified** for isolated tests where PostgreSQL-specific types, PgVector, or Liquibase-on-Postgres behavior is **not** under test.
- Integration tests should apply **Liquibase** and exercise real persistence (inserts, reads after “restart” of the application context, constraints that exist in PostgreSQL).
- Cover important **failures** as well as happy paths (for example illegal status transitions, validation errors). No coverage percentage is required.

## Repeatability and isolation

- Tests must not depend on developer-specific config, secrets, a pre-existing local database, or a running Docker Compose Postgres.
- Do not call paid or developer-local **Ollama / cloud LLM** APIs from the default unit/integration suite. Use test doubles for generative calls unless a later RAG/evaluation standard says otherwise.
- Do not use arbitrary sleeps to “wait for the database.” Failures should be deterministic; wait only when asynchrony is real and the wait is bounded.
- After code changes, run the smallest relevant test set and state what was not run. There is **no CI** yet.

## Do not

- Do not invent assessment requirements or treat project test tooling as if the PDF named it.
- Do not add CI pipelines or require a hosted runner.
- Do not impose coverage %, a mandatory Given/When/Then template, or a fixed package of test classes. Mapping test names to acceptance criteria is enough.
- Do not prescribe frontend test tools (Vitest, Playwright, Testing Library, etc.) until they are approved.
- Do not define RAG scoring, golden-answer sets, or retrieval metrics here.
