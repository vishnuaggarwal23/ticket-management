# Testing guidelines

Cursor attaches this file via [`.cursor/rules/testing.mdc`](../.cursor/rules/testing.mdc) (pointer only). Edit **this** file; do not copy the body into the `.mdc`.

Tests prove **acceptance criteria** from `spec/`. Backend coding standards live in `rules/java-springboot.md`. HTTP envelopes, status codes, pagination, and versioned paths live in `rules/api-standards.md`. Resource field catalogs live in `spec/api-contract.md` / `spec/rag-api-contract.md` once agreed. RAG **retrieval quality** is judged per `spec/evaluation-strategy.md` + `commands/review-rag-output.md`, not golden strings here.

| Command | Use |
|---------|-----|
| `commands/generate-tests.md` | Propose then write backend tests |
| `commands/review-code.md` | Check tests exist for changed backend code |
| `commands/review-frontend.md` | UI only — **no** frontend test files |

## Assessment vs project conventions

**Assessment PDF** requires tests that prove the ticket **state machine** (legal and illegal transitions), **backend validation**, **persistence that survives restart**, and honest RAG **no-match / citation** behaviour. It does **not** mandate JUnit, Mockito, Testcontainers, H2, coverage percentages, CI, or a frontend test stack.

**This project’s approved conventions** (use these when implementing; do not describe them as PDF requirements):

- **JUnit 5** as the test runner and assertion baseline
- **Mockito** for unit-test doubles of collaborators (repositories, RAG ports, clocks)
- Backend integration tests: **PostgreSQL with Testcontainers** (PgVector available in that image/setup when the schema uses it)
- **Liquibase** migrations applied in integration tests so schema matches what the application runs
- Run tests with **Maven Wrapper** (`./mvnw test` or the equivalent wrapper goal) — do not rely on a locally installed Maven
- **No CI** in this milestone — local, repeatable test runs only
- Docker Compose is for **local infrastructure**, not a substitute for Testcontainers in the backend integration suite

Do not assume unfinalized versions (Spring Boot 3 patch, JUnit/Mockito/Testcontainers patch, PostgreSQL image tag, embedding model).

## Tech stack (tests)

| Concern | Convention |
|---------|------------|
| Runner | JUnit 5 (`org.junit.jupiter`) |
| Doubles | Mockito (`org.mockito`), typically via `spring-boot-starter-test` |
| Web slice (no DB) | Spring MVC test utilities that ship with Spring Boot (for example MockMvc) |
| Integration DB | PostgreSQL via Testcontainers; Liquibase applied |
| Build | Maven Wrapper — `./mvnw test` |
| RAG in default suite | Test doubles for embedding/generation APIs — no real Ollama or paid calls |

Do not add extra assertion libraries, BDD runners, or coverage plugins unless a spec agrees. JUnit assertions plus Mockito verification are enough.

## Layers

Keep unit vs integration vs end-to-end distinct. Do not require a particular test-class *naming* scheme or package tree, but **every implemented application layer MUST have tests** (see **Tests by application layer** below). A suite that only hits controllers or only hits services is incomplete.

| Kind | Responsibility | Typical tools |
|------|----------------|---------------|
| **Unit** | Deterministic domain and application logic in isolation (state machine, services with mocked ports, controller slice). No real database, no network, no Ollama. | JUnit 5, Mockito, Spring MVC test utilities |
| **Integration** | Real **PostgreSQL** (Testcontainers), Liquibase-applied schema, **repository** queries, and backend behaviour that depends on the database — including **HTTP APIs** that persist (create/list/get/patch/comments/transitions through `/api/v1`). | JUnit 5, Spring Boot test support (`@SpringBootTest` / `@DataJpaTest` as needed), MockMvc or `TestRestTemplate` against the running app, Testcontainers |
| **End-to-end / UI** | **Out of scope this milestone.** Do **not** generate frontend tests. Compose may back a local full stack; it must not be the only way backend integration tests can run. | Not used |

HTTP-adapter tests that exercise controllers without a database are **unit/slice-style** tests of the web layer, not PostgreSQL integration tests. Repository tests that need SQL, constraints, or JPQL **are** integration tests.

## Tests by application layer

Align with `rules/java-springboot.md` (`api`, `domain`, `service`, `persistence`, `rag`). Each implemented type in those layers needs **positive and negative** tests for its important behaviours. Do not collapse all of this into one “API happy-path” class.

### Domain / state machine

- **Kind:** unit. Real domain types. **No Mockito** on the machine under test. No Spring, no JPA.
- **Positive:** every legal transition in `spec/state-machine.md` (`OPEN` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`; `OPEN`/`IN_PROGRESS` → `CANCELLED`, and any other agreed legal edges).
- **Negative:** every disallowed pair once the spec lists them; at minimum the assessment examples (`CLOSED` → `OPEN`, `RESOLVED` → `OPEN`, `CANCELLED` → `OPEN`). Prefer `@ParameterizedTest` over one method per edge.
- Assert the resulting status **or** the domain exception. The machine MUST NOT depend on repositories.

### Services

- **Kind:** unit. Construct the service with **Mockito** doubles for repositories, RAG ingest/ask ports, and other collaborators. No Testcontainers in these tests.
- **Positive:** create, update fields, add comment, list/search/filter, load detail, legal status transition; after successful **update** or **close**, ingest port is invoked.
- **Negative:** not found; illegal transition thrown **before** `save` of the illegal status; validation/domain failures do not call ingest; blank/invalid inputs the service is responsible for (if not only Bean Validation).
- Services MUST NOT be tested only through HTTP. Controller tests do not replace service tests.

### Controllers (HTTP mapping)

- **Kind:** web slice (MockMvc or equivalent). Mock the **service** (Mockito). Do not stand up PostgreSQL here.
- These tests are **necessary but not sufficient**. Full **API testing** (envelopes, pagination, versioned paths, persistence through HTTP) is required in **API testing** below.
- Controllers stay thin: slice tests prove mapping and `@Valid` — **not** the transition table (domain) and **not** JPQL (repository).

### Repositories (persistence)

- **Kind:** integration. **PostgreSQL via Testcontainers**, Liquibase applied. Do **not** mock `TicketRepository` / `CommentRepository` / `EntityManager` to fake SQL.
- **Positive:** insert and find by id; keyword search; filter by status; comments loaded as the query defines; updates that the repository is responsible for (field persistence, not status-machine rules).
- **Negative:** missing id → empty `Optional`; search/filter with no rows → empty collection, not an error; constraint violations the schema actually enforces (not null, unique) when those exist in Liquibase.
- Repositories MUST NOT encode or bypass the state machine. Do not write a repository test that `UPDATE`s status around the domain rules and calls that a transition test.

### RAG (`rag/` + ask controller)

- **Kind:** unit for document/chunk assembly and ask orchestration (Mockito for embed/search/generate ports); integration only for schema/persistence of derived vectors when that is under test (Testcontainers + Liquibase). Default suite still must not call real Ollama.
- **Positive:** knowledge text built from description, comments, resolution notes; ingest triggered from the service after update/close; ask returns grounded shape with cited ticket ids that appear in retrieval.
- **Negative:** empty/below-threshold retrieval → honest no-match, **no** generate-from-empty-context; citations MUST NOT include ids the model invented.

### Error handling (`@RestControllerAdvice`)

- Cover mapping onto the **error envelope** in `rules/api-standards.md` (see **API testing**). Bean Validation → 400 `VALIDATION_ERROR`; not found → 404 `NOT_FOUND`; illegal transition → 409 `ILLEGAL_TRANSITION`; unexpected → 500 `INTERNAL_ERROR` with no stack traces, SQL, or secrets.

## API testing

HTTP APIs MUST be tested as APIs — not only as Java controllers, services, or repositories. Prove `rules/api-standards.md` (and `spec/api-contract.md` / `spec/rag-api-contract.md` when those exist). Assessment capabilities (create, list, get, update fields, comments, keyword search, status filter, status transitions, ask) MUST have **positive and negative** HTTP tests.

### Two levels (both required for implemented endpoints)

| Level | How | What it proves |
|-------|-----|----------------|
| **API slice** | MockMvc (or equivalent) + Mockito for the application service | Status codes, JSON envelopes, Bean Validation, advice mapping, query-param binding, `Location` on create. No PostgreSQL. |
| **API integration** | HTTP against the Spring app + **PostgreSQL Testcontainers** + Liquibase | The same contracts **and** that rows actually persist, lists/search/filter/pagination read from the DB, illegal transitions leave the row unchanged, restart/reload via API still returns the ticket. |

Do not treat a service unit test as an API test. Do not treat a repository test as an API test. Do not skip API integration because slice tests exist.

Use JUnit 5. Prefer Spring MVC test utilities that ship with Spring Boot. Do not add RestAssured or a second HTTP client unless a spec agrees.

### Contracts to assert on every JSON response

- **Success (single):** HTTP 2xx body has `data` (object); no `error`; no list `meta`.
- **Success (list):** HTTP 200 body has `data` (array, including `[]`) and `meta` (`page`, `size`, `totalElements`, `totalPages`, `sort`). Empty search is **200** + empty `data`, not 404.
- **Create:** HTTP **201**, success envelope, `Location` header to the new resource.
- **Error:** body has `error.status`, `error.code`, `error.message`, `error.timestamp`, `error.path`; `error.status` matches the HTTP status; `details` present for `VALIDATION_ERROR`. Never stack traces, SQL, or secrets.
- **Ask no-match:** HTTP **200** + success envelope; honest no-match **inside `data`**, not the error envelope.

### Paths, methods, versioning

- Ticket APIs under `/api/v1/tickets` (and comments) as in api-standards. Assert **PATCH** for field/status updates; **PUT** is not used.
- `POST /api/ai/ask` **and** `POST /api/v1/ai/ask` accept `{"question":"..."}` and share behaviour.
- Unknown or disallowed methods/paths fail in a client-safe way (do not leak internals).

### List APIs: pagination, sort, search, filter

For `GET /api/v1/tickets`, API tests MUST cover:

| Case | Expect |
|------|--------|
| Default list | `page=0`, default `size`, `meta` populated |
| `page` / `size` | Requested page returned; `size` &lt; 1 or &gt; 100 → 400 `VALIDATION_ERROR` |
| `sort` whitelist | Allowed property applied; `meta.sort` echoes it |
| Unknown `sort` | 400 `VALIDATION_ERROR` |
| `q` keyword | Hits match; no hits → 200 empty `data` |
| `status` filter | Hits match; invalid status → 400 `VALIDATION_ERROR` |
| `q` + `status` | AND semantics |

API integration tests MUST back these with real rows in PostgreSQL.

### Endpoint matrix (positive and negative)

| HTTP capability | Positive | Negative |
|-----------------|----------|----------|
| `POST /api/v1/tickets` | 201 + `data` + `Location` | 400 validation / malformed JSON; no row written (integration) |
| `GET /api/v1/tickets` | 200 + `data`/`meta`; pagination/search/filter as above | invalid query params → 400 |
| `GET /api/v1/tickets/{id}` | 200 + `data` | 404 `NOT_FOUND` |
| `PATCH /api/v1/tickets/{id}` | 200 + updated `data` | 400 validation; 404; illegal status → **409** `ILLEGAL_TRANSITION` (row unchanged in integration) |
| `POST /api/v1/tickets/{id}/comments` | 201 + `data` | 400; 404 |
| `POST /api/ai/ask` and `/api/v1/ai/ask` | 200 + `data` (grounded + citations **or** no-match) | 400 blank/missing `question` |

Illegal transition is proven at the API with **409** and the error envelope; the domain table of edges remains a **state-machine unit** test.

### What API tests must not do

- Call real Ollama or paid models (stub embed/generate in API integration).
- Assert a single golden RAG answer string as quality proof.
- Use a running Docker Compose Postgres instead of Testcontainers for API integration.
- Invent a second envelope in tests that production does not return.

## JUnit 5

- Use JUnit Jupiter (`@Test`, `@ParameterizedTest`, `@Nested` where a flow has several cases). Do not use JUnit 4 (`org.junit.Test`, `@RunWith`).
- Name tests after **behaviour and acceptance criteria**, not implementation methods (`illegalTransitionFromClosedToOpen_isRejected`, not `testUpdate1`).
- Prefer parameterized tests for closed sets (every illegal transition, every required-field violation) instead of copy-paste methods.
- One logical assertion theme per test. Do not mix unrelated flows in a single method.
- Use `@DisplayName` only when it clarifies the criterion; names should already be readable.
- Do not depend on test execution order. Do not share mutable static state between tests.

## Mockito

- Use Mockito in **unit** tests to isolate the class under test. Mock **ports and collaborators** (repository interfaces, RAG ingest/ask ports), not the class under test, not JPA entities, and not value objects.
- Prefer constructor injection in production code so tests can pass mocks without Spring.
- `when` / `given` only what the test needs. Verify **meaningful** interactions (illegal transition never calls `save` with the illegal status; update/close triggers ingest). Do not verify every getter.
- Do not mock PostgreSQL, Liquibase, or the state-machine type itself when that type is the subject of the test — unit-test the machine with real domain objects.
- Do not use Mockito in integration tests to stub the database. Doubles for **Ollama / embedding / chat** remain allowed in the default integration suite so tests stay offline.
- Do not add PowerMock, mock static Spring internals, or mock `EntityManager` to fake persistence — that belongs in Testcontainers tests.

## Unit testing

Unit tests MUST cover important **positive and negative** paths of deterministic logic:

| Area | Positive | Negative |
|------|----------|----------|
| State machine | Legal transitions (`OPEN` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`; `OPEN`/`IN_PROGRESS` → `CANCELLED` per `spec/state-machine.md`) | Illegal transitions (assessment examples: `CLOSED` → `OPEN`, `RESOLVED` → `OPEN`, `CANCELLED` → `OPEN`, and every other disallowed pair once the spec lists them) |
| Ticket fields | Create/update with valid title, description, priority, assignee | Missing/blank/too-long fields per agreed validation; reject without persisting |
| Comments | Add comment on an allowable ticket | Reject empty comment / comment on a state the spec forbids |
| Search / filter | Keyword and status filter return matching tickets | No match → empty result, not an error; invalid status filter → client error |
| Services | Orchestration: load → apply domain rule → save; RAG refresh after update/close | Not-found; illegal transition thrown **before** save; ingest **not** called when the write fails |
| HTTP slice | Valid JSON maps to the service; success envelope | Malformed JSON; Bean Validation; see **API testing** |

Rules:

- No real database, Docker, or network. Clock and ids may be fakes if the domain needs them.
- Assert on **outcomes** (new status, exception type, arguments passed to the ingest port), not on log lines.
- Controllers stay thin: slice tests prove mapping and validation, not the transition table (that is domain unit tests).

## Integration testing (Testcontainers)

- Default and preferred integration database is **PostgreSQL via Testcontainers**, not H2 and not a developer’s already-running local Postgres.
- **H2** is not the default or preferred stand-in. Mention it only when **explicitly justified** for isolated tests where PostgreSQL-specific types, PgVector, or Liquibase-on-Postgres behaviour is **not** under test.
- Start Postgres (with PgVector when the schema needs `vector` types) in the test JVM. Point Spring DataSource at the container. Do not require `docker compose up` first.
- Apply **Liquibase** the same way the application does. Tests that ignore migrations are not proving production schema.
- Integration tests MUST cover important **positive and negative** flows that depend on storage or HTTP + DB:

| Flow | Positive | Negative |
|------|----------|----------|
| Persistence | Create, read, update fields; data still present after application-context “restart” | Unique/check constraints; missing ticket → not found |
| Status | Legal transition persisted and reloaded | Illegal transition rejected, **row unchanged**, client-usable error |
| Comments | Comment stored and returned on detail | Invalid comment not stored |
| List / search / filter | Results match keyword and status | Empty list when nothing matches |
| Validation | — | API rejects invalid bodies without writing rows |
| RAG hook (deterministic) | Update/close triggers re-ingest port (or equivalent) | Failed ticket write does not leave a successful ingest as the only side effect |
| Ask via HTTP | 200 success envelope; citations only from retrieval | No-match in `data` (still 200); 400 blank question; no fabricated ids |
| HTTP + DB (API integration) | Create/list/get/patch/comment via `/api/v1`; pagination `meta`; data survives reload | 400/404/409 envelopes; illegal PATCH leaves row unchanged; `size`/`sort` validation |

- Cover **failures** as well as happy paths. A suite that only creates tickets is incomplete.
- Do not use arbitrary sleeps to “wait for the database.” Failures should be deterministic; wait only when asynchrony is real and the wait is bounded.
- Default suite: **no** real Ollama, cloud, or paid model calls.

## Coverage of important flows

Every **agreed** ticket and ask capability in `spec/` that is implemented MUST have tests for both success and failure (or explicit empty) outcomes, at the **layers that own that behaviour** (domain, service, controller, repository, RAG) **and as HTTP APIs** (slice + integration per **API testing**). Minimum themes:

1. Create ticket — valid payload; validation errors (API 201/`Location` + service + repository insert)
2. List / keyword search / filter by status / pagination / sort — hits; empty **200**; bad `q`/`status`/`size`/`sort` (API + service + **repository queries**)
3. View detail — found; 404 (API + service + repository)
4. PATCH title, description, priority, assignee — valid; not found; validation errors
5. Add comments — valid; validation / not found (API + service + repository)
6. Status transitions — legal edges and illegal edges including assessment-invalid reopen paths (**state machine unit tests** + service + API **409** + persisted result)
7. Restart / reload — stored tickets still returned by GET API
8. RAG ingest trigger on update and close (service unit; optional persistence of chunks in integration)
9. `POST /api/ai/ask` **and** `POST /api/v1/ai/ask` — 200 envelope with grounded citations **or** no-match in `data`; 400 invalid question

No coverage **percentage** is required. Mapping test names to acceptance criteria is enough. Do not treat a single golden RAG answer string as proof of retrieval quality.

## Deterministic behaviour vs RAG

- **Deterministic:** ticket CRUD, comments, search/filter, input validation, status transitions (valid and invalid), and persistence. These MUST have stable, repeatable tests with fixed expected results.
- **Probabilistic / RAG:** retrieval ranking and free-form generated answers can vary. Evaluate grounding with `commands/review-rag-output.md`; evaluate **whether the right tickets were retrieved** with `spec/evaluation-strategy.md` and the **Retrieval quality** section of that command. Do **not** treat a single golden answer string as sufficient proof.
- Ticket **re-ingestion** after update or close is an assessment behaviour to verify in unit and/or integration tests; how embeddings are scored is a RAG/evaluation concern.

## Persistence “restart” (PDF acceptance)

Prove **data survives application restart** with an integration test, for example:

1. `@SpringBootTest` + Testcontainers Postgres + Liquibase.
2. Create a ticket via API or service; assert persisted.
3. Start a **second** test method or nested `@Nested` class that boots a **fresh** application context (same container) and `GET`s the ticket — still present with same status.

Do not rely on manual “stop the IDE and run again” as the only proof.

## Testcontainers + Liquibase (integration bootstrap)

Default pattern for backend integration tests that need PostgreSQL. Use **JUnit 5**; run with `./mvnw test`. Do not point tests at a developer’s local Postgres or Compose-only DB.

```java
@Testcontainers
@SpringBootTest
class TicketApiIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
      .withDatabaseName("tickets_test");

  @DynamicPropertySource
  static void registerDataSource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  // Liquibase runs on startup (same changelogs as the app).
  // Use MockMvc or TestRestTemplate against full context.
}
```

- Use a **PgVector-capable** image when vector schema is under test (align image tag with `spec/rag-ingestion.md` when agreed).
- `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` + the same container is fine for **repository-only** tests.
- Stub **embedding/chat** ports in ask tests — no real Ollama in the default suite (`rules/rag-vector-store.md`).

## Slice vs integration — how much to duplicate

| Concern | API slice (MockMvc, mock service) | API integration (real DB) |
|---------|-----------------------------------|---------------------------|
| Envelope JSON shape | Yes | Yes (at least one happy path per endpoint) |
| Bean Validation / 400 | Yes | Optional if covered in slice |
| 409 illegal transition + row unchanged | Optional in slice | **Required** |
| Pagination / `q` / `status` against real rows | No | **Required** |
| Repository JPQL | No | `@DataJpaTest` or full `@SpringBootTest` |

You do **not** need fifty duplicate tests; you **do** need each **risk** covered at the right layer (see **API testing**).

## Frontend acceptance (PDF) without UI tests

PDF requires ticket created from UI, meaningful errors, etc. This milestone **does not** add Vitest/Playwright. Substitute:

- Backend API integration tests for the same behaviours the UI calls.
- `commands/review-frontend.md` on UI diffs before merge.

State that substitution in `spec/test-strategy.md` when written.

## Repeatability and isolation

- Tests must not depend on developer-specific config, secrets, a pre-existing local database, or a running Docker Compose Postgres.
- Do not call paid or developer-local **Ollama / cloud LLM** APIs from the default unit/integration suite. Use Mockito (or other test doubles) for generative and embedding calls unless a later RAG/evaluation standard says otherwise.
- After code changes, run the smallest relevant test set and state what was not run. There is **no CI** yet.

## Do not

- Do not invent assessment requirements or treat project test tooling as if the PDF named it.
- Do not add CI pipelines or require a hosted runner.
- Do not impose coverage %, a mandatory Given/When/Then template, or a fixed test-class *name* pattern. Do not skip domain, service, controller, repository, or **API** tests for implemented behaviour.
- Do not generate or require frontend tests (Vitest, Playwright, Testing Library, Cypress, etc.). UI review is `commands/review-frontend.md`.
- Do not define RAG scoring, golden-answer sets, or retrieval metrics here.
- Do not mock the database in place of Testcontainers for persistence tests.
- Do not skip negative paths for implemented flows.
- Do not skip HTTP **API testing** (envelopes, pagination, `/api/v1`, 409) or treat service/repository tests as a substitute.
- Do not replace repository tests with mocked repositories, or state-machine tests with controller-only tests.
