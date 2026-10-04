# Review code

Cursor attaches this file via [`.cursor/commands/review-code.md`](../.cursor/commands/review-code.md) (pointer only). Edit **this** file; do not copy the body into `.cursor/commands/`.

Review the current change set (or specified paths) as a senior Java / Spring Boot tech lead.

Do **not** apply fixes unless the user asks. Confirm before editing.

**Java / Spring Boot bar:** A change that works but violates [`rules/java-springboot.md`](../rules/java-springboot.md) (coding guidelines, Spring Boot practices, naming, or packaging) is a **Fail**. Do not treat Boot 4, Maven Wrapper, Liquibase, or the package tree as PDF requirements — they are **project conventions**. For REST envelopes and list params, also use `rules/api-standards.md`. Do not invent extra libraries, layers, or naming schemes not in those files or an agreed spec.

**Quick routing**

| Diff contains | Primary command |
|---------------|----------------|
| `src/main/java`, `src/test/java`, backend resources | This file |
| `frontend/`, `src/**/*.jsx`, Next.js app | `commands/review-frontend.md` (no UI tests required) |
| Only `spec/` | `commands/review-spec.md` |

## Inputs

- Diff / named paths
- Specs: all ten PDF-listed files under `spec/` (**agreed** 2026-10-04; **DEC-01…20**): [`spec/requirements.md`](../spec/requirements.md) (**§0.5**), [`spec/architecture.md`](../spec/architecture.md), [`spec/ui-flow.md`](../spec/ui-flow.md), [`spec/data-model.md`](../spec/data-model.md), [`spec/api-contract.md`](../spec/api-contract.md), [`spec/rag-api-contract.md`](../spec/rag-api-contract.md), [`spec/state-machine.md`](../spec/state-machine.md), [`spec/rag-ingestion.md`](../spec/rag-ingestion.md), [`spec/evaluation-strategy.md`](../spec/evaluation-strategy.md), [`spec/test-strategy.md`](../spec/test-strategy.md) (each **§0** + SM §5, ask §6 where relevant); index [`rules/documentation.md`](../rules/documentation.md)
- Rules: `rules/java-springboot.md`, `rules/api-standards.md`, `rules/testing.md`, `rules/rag-vector-store.md`
- If the diff is **UI**: follow [`commands/review-frontend.md`](review-frontend.md) instead of (or in addition to) this backend checklist. Do not require frontend tests.
- Assessment PDF only as background — do not treat conventions as PDF requirements

Mark each item **Pass** / **Fail** / **N/A**. Failures need file references and severity: **blocker** / **major** / **minor**.

---

## 1. Code validity (correctness vs specs and rules)

### Spec and scope

- [ ] Implements only **agreed** specs; no **Reference** features ([`spec/requirements.md`](../spec/requirements.md) **§2.3** — auth, delete API, agents, confidence on ask, rerankers, etc.)
- [ ] Code matches **§10.2 (DEC-*)** — **DEC-01…20** agreed (hub 2026-10-04). Embedding: **DEC-09** (`nomic-embed-text`, 768). DB: **DEC-10** (Postgres + Testcontainers; no H2). Chunk/retrieval defaults: **DEC-16** via config. Ingest: **DEC-18** sync after commit. Ask limits: **DEC-17**. Frontend UX: **DEC-20**. Index: [`rules/documentation.md`](../rules/documentation.md)
- [ ] Domain status machine matches [`spec/state-machine.md`](../spec/state-machine.md) §5 (T1–T5, X1–X3, full invalid matrix)
- [ ] Illegal transitions rejected in **domain**, not only by hiding UI actions
- [ ] Assessment-invalid reopens rejected (`CLOSED`/`RESOLVED`/`CANCELLED` → `OPEN`)

### API (`rules/api-standards.md`, [`spec/api-contract.md`](../spec/api-contract.md))

- [ ] Ticket URLs under `/api/v1`; **PATCH** for field/status updates; no **PUT**
- [ ] Request/response shapes and status codes match `api-contract.md` §4–§6 (create rejects `status`, comment 201 returns `Comment`, empty PATCH → 400)
- [ ] `POST /api/ai/ask` with `{"question":"..."}` still works; `/api/v1/ai/ask` behaves the same
- [ ] Success envelope `{ "data" }`; lists also have `{ "data", "meta" }`
- [ ] Create returns **201** + `Location`
- [ ] Empty list is **200** + `data: []`, not 404
- [ ] Error envelope `{ "error": { status, code, message, details?, timestamp, path } }`; HTTP status matches `error.status`
- [ ] 400 `VALIDATION_ERROR` / `BAD_REQUEST`; 404 `NOT_FOUND`; 409 `ILLEGAL_TRANSITION`; 500 `INTERNAL_ERROR` with no internals leaked
- [ ] List query params: `page`, `size` (1–100), `sort` (whitelist), `q`, `status`
- [ ] Ask no-match is **200** + honest no-match **inside `data`**, not the error envelope
- [ ] JSON camelCase; DTOs not JPA entities; Bean Validation at the boundary

### Persistence and config (`rules/java-springboot.md`)

- [ ] PostgreSQL + Liquibase; Hibernate DDL is not the schema source of truth
- [ ] Entities/tables match [`spec/data-model.md`](../spec/data-model.md) (`ticket`, `ticket_comment`, `ticket_vector_chunk`); indexes per §14.5 present in changelogs
- [ ] No ad-hoc status `UPDATE` that bypasses the state machine
- [ ] `jakarta.*` not `javax.*`; constructor injection; no field `@Autowired`
- [ ] No secrets, machine-specific URLs, or hardcoded RAG model/K/threshold/chunk size in Java
- [ ] RAG provider (Ollama) only via configuration

### RAG (`rules/rag-vector-store.md`) — if ingest/ask touched

- [ ] Knowledge text only from description, comments, resolution notes
- [ ] Metadata per [`spec/data-model.md`](../spec/data-model.md) §11 (`ticketId`, `status`, `priority`, `assignee`, `category`; technical keys only as specified)
- [ ] Re-ingest per [`spec/rag-ingestion.md`](../spec/rag-ingestion.md) §10 (**DEC-01** agreed); hybrid chunker §6–§9
- [ ] Retrieve-then-generate once; no tools, ticket create, or notify from ask
- [ ] Empty/below-threshold retrieval does not call the LLM to invent an answer
- [ ] Citations are ticket IDs from **retrieval**, not model-guessed ids
- [ ] Retrieval ranking quality is **not** proved by a golden LLM string in unit tests — use [`spec/evaluation-strategy.md`](../spec/evaluation-strategy.md) §5–§8 + `commands/review-rag-output.md` when debugging ask
- [ ] Chunking from [`spec/rag-ingestion.md`](../spec/rag-ingestion.md) (**DEC-16**); `nomic-embed-text` / **768** (**DEC-09**); K/threshold/chunk sizes **not** hardcoded in Java

### Frontend

- [ ] If UI files changed, run **`commands/review-frontend.md`** (React/Next.js/Vite/JavaScript, envelopes, ask display). **No frontend tests.**

---

## 2. Java / Spring Boot standards (guidelines, practices, naming, packaging)

Apply [`rules/java-springboot.md`](../rules/java-springboot.md) in full. Cross-check layering with [`spec/architecture.md`](../spec/architecture.md) §8–9.

### Packaging strategy

- [ ] **One** Spring Boot application (`@SpringBootApplication`); main class has no business logic
- [ ] **One** Java package root under `src/main/java` (e.g. `com.ticketmanagement`); no second root, no default package
- [ ] Types are **grouped and separated** by layer **and** by logical concern — **Fail** if controllers, DTOs, entities, services, RAG, and config all sit in `{root}` or in one bag package
- [ ] Layout is **by technical role** (quick navigation): `{root}/controller`, `dto/*`, `service`, `entity`, `repository`, `exception`, `util`, `advice`, `domain`, `rag`, `config`
- [ ] All `@RestController` types live in `controller/` (not mixed with DTOs); HTTP records in `dto/common`, `dto/request`, `dto/response`; `@RestControllerAdvice` in `advice/`
- [ ] Do not mix unrelated types in one package (entity + controller, RAG port + ticket service, config beans + domain enums)
- [ ] Subpackages under `dto/` and `rag/` when a group grows; do not resurrect `api.ticket` / `persistence` combined bags; do not invent `web`, `dao`, `manager`
- [ ] Ask orchestration in `service/`; RAG adapters in `rag/`; **thin** `AiAskController` in `controller/` — not a second microservice
- [ ] JPA entities only in `entity/`; Spring Data repos (+ `Specification` helpers) only in `repository/`; domain types are not `@Entity`
- [ ] Exceptions in `exception/` (not `domain/`); small helpers in `util/`
- [ ] Ticket/comment I/O is **Spring Data JPA** (`JpaRepository`, `@Query`, `Pageable`, custom fragments) — services do not use `JdbcTemplate` / raw SQL for `ticket` / `ticket_comment` (**C-06**)
- [ ] Vector chunk storage stays behind `rag` ports (JDBC/pgvector allowed there); no duplicate DAO layer for tickets
- [ ] `@Configuration` / `@ConfigurationProperties` live in `config`; no ad-hoc bean wiring inside controllers/services
- [ ] Test sources mirror production packages under `src/test/java` (same root); IT types stay with repository/API tests per `rules/testing.md`

### Naming

- [ ] Types: PascalCase; methods/fields/locals: camelCase; packages: all-lowercase, no underscores
- [ ] Constants and enum constants: `UPPER_SNAKE`; `TicketStatus`, `TicketPriority` (`CRITICAL`, not domain `URGENT`), `TicketCategory` are **enums**, not `String` (JSON `"URGENT"` → `CRITICAL` only in **api**)
- [ ] Names are readable and role-true: `TicketService`, `TicketRepository`, `TicketStatusMachine`, `IllegalTicketTransitionException` — not `TktSvc`, `SM`, `Helper`, `Util`, `Manager` dumping mixed concerns
- [ ] Layer suffixes: `*Controller` in `controller`; `*Service` in `service`; `*Repository` in `repository`; `*Entity` in `entity`; `*Exception` in `exception`
- [ ] Request/response types are API **records** (e.g. create/update/list DTOs) — not `*VO`, not persistence entities reused as JSON
- [ ] Spring Data method names match derived-query / `@Query` intent; no vague `getData` / `doWork`
- [ ] Config properties classes use a clear prefix (e.g. `rag.retrieval`); no unexplained one-letter type names

### Coding guidelines

- [ ] Constructor injection only; collaborators `final`; no field/`@Autowired` injection; no setter injection
- [ ] `jakarta.*` not `javax.*`; Java 25 language features are fine (`record`, `switch`, text blocks); no `sun.*`
- [ ] Public application APIs do not return `null` — `Optional` for a missing ticket; empty `List`/`Page` for empty collections
- [ ] Prefer `record` for API DTOs and small immutable values; no Lombok, MapStruct, QueryDSL, or extra web stacks unless a spec agrees
- [ ] Methods stay focused; extract helpers when a method mixes orchestration, validation, and field mapping — no god class that both mutates tickets and inlines the transition table
- [ ] Injected collaborators are named by role: `ticketService`, `ticketRepository` — not ambiguous `tickets` when the type is not a collection
- [ ] Time: `java.time` (`Instant` for stored timestamps); no `java.util.Date` / `Calendar`
- [ ] Log with SLF4J; include ticket id when known; never log secrets, passwords, API keys, raw prompts, or full model output by default
- [ ] No SQL/JPQL string concatenation of user input; bind parameters; `sort` is allowlisted
- [ ] Domain exception `getMessage()` has no SQL, stack traces, or secrets

### Spring Boot practices

- [ ] Matches agreed **functional → technical** map ([`spec/architecture.md`](../spec/architecture.md) §8–9): ticket ops in `service`/`repository`, ask in `rag/`, state machine in `domain`
- [ ] Call flow: `controller` → `service` → `domain` (rules) / `repository` (I/O) / `rag` port — no skipped-layer shortcuts
- [ ] **controller:** `@RestController` + JSON; HTTP mapping, `@Valid`/`@Validated`, status codes, DTO mapping only. No JPQL, no status-machine tables, no `@Transactional`, no leaking entities, no Spring AI/PgVector/Ollama types on the HTTP boundary
- [ ] **domain:** status rules and domain errors only; **no** Spring Web, JPA, `@Autowired`, or JDBC
- [ ] **service:** `@Transactional` on writes; `@Transactional(readOnly = true)` on reads that need a transaction; short transactions; depend on repository **interfaces** and domain types, not controllers or `HttpServletRequest`; after-commit side effects (e.g. RAG ingest) in small helpers, not inlined transaction-sync blocks
- [ ] Ticket mutations trigger RAG ingest via **service** hook/port after successful write — not from controller or repository ([`spec/architecture.md`](../spec/architecture.md) §10.2, §15.4)
- [ ] **repository:** Spring Data JPA load/save and keyword/status queries only; no ad-hoc `@Modifying` status `UPDATE`; keyword `q` searches **title and description** only
- [ ] **rag:** retrieve-then-generate from ticket knowledge only; no ticket create, notify, or tool-chain from ask
- [ ] **config:** beans and property binding only; RAG/JDBC/Ollama URLs, model ids, top-K, threshold, chunk limits in `@ConfigurationProperties` — not magic numbers in Java
- [ ] One `@RestControllerAdvice` maps to the error envelope; no per-controller error JSON; 500 bodies have no internals
- [ ] `spring.jpa.open-in-view=false`; no OSIV lazy loads in controllers; Hibernate `ddl-auto` is `validate` or `none`
- [ ] CORS is not `*` as a permanent default; no Spring Security unless an agreed spec
- [ ] Entities map 1:1 to Liquibase (`@Table`/`@Column`); no Jackson annotations on entities “to make the API work”; prefer lazy + explicit fetch over EAGER comment graphs
- [ ] Maven Wrapper (`./mvnw`) is the build path; no machine-local Maven as the documented/review assumption

---

## 3. Code quality (hygiene, tests, AI mistakes)

### Quality

- [ ] No `null` from public application APIs (`Optional` / empty collections)
- [ ] No SQL/JPQL string concatenation of user input; `sort` is allowlisted
- [ ] No logging of secrets, prompts, or full model output by default
- [ ] No stack traces, SQL, or Hibernate state in HTTP bodies
- [ ] Methods stay focused; no god services that both mutate tickets and inline the transition table
- [ ] `spring.jpa.open-in-view` not relied on; no OSIV lazy loads in controllers

### Tests (`rules/testing.md`, [`spec/test-strategy.md`](../spec/test-strategy.md))

- [ ] JUnit 5; Mockito for unit doubles; no JUnit 4
- [ ] **Domain** state-machine tests: legal and illegal edges (parameterized where a closed set)
- [ ] **Service** unit tests with mocked repos/RAG ports (ingest on update/close; no save on illegal transition)
- [ ] **Controller** slice tests (MockMvc); not a substitute for service/repository/API integration
- [ ] **Repository** tests: PostgreSQL **Testcontainers** + Liquibase; DB not mocked
- [ ] **API slice and API integration** both present for implemented endpoints (envelopes, 201/`Location`, pagination/`q`/`status`/`sort`, 409)
- [ ] Positive **and** negative paths for implemented flows
- [ ] Default suite does not call real Ollama/paid models
- [ ] Maven Wrapper would be used to run tests (`./mvnw test`); no CI required this milestone

### AI-generated code risks

- [ ] Types, annotations, and Spring APIs exist on the agreed stack (Boot 4, Spring AI 2.x, Jakarta, no hallucinated libraries)
- [ ] No copied copyrighted dumps; no credentials in the diff
- [ ] Meaningful AI mistakes proposed for [`docs/ai-error.md`](../docs/ai-error.md) (write via **`/update-ai-error`**; do not invent RAG fails)

---

## Output

- **Findings** — severity, file, failed checklist item, brief fix (do not implement unless asked). Include **guideline / practice / naming / packaging** failures from §2, not only spec mismatches.
- **Spec gaps** — code without spec, or spec without code
- **Open questions the code assumed** — must confirm with the user
- **Ready to merge?** yes / no — **no** if §2 has any **blocker** or **major** Fail

---

## Revision history

| Date | Note |
|------|------|
| 2026-09-24 | Initial backend code review checklist against specs and `rules/`. |
| 2026-10-04 | SDD expansion: routing table, severity labels, spec-gap and assumption reporting. |
| 2026-10-04 | Synced with expanded [`spec/requirements.md`](../spec/requirements.md), [`spec/architecture.md`](../spec/architecture.md), and aligned rules. |
| 2026-10-04 | Added revision history section. |
| 2026-10-04 | §2 checklist: **C-06** Spring Data JPA primary; vector JDBC only in `rag/`. |
| 2026-10-04 | Packaging checklist: `controller`, `dto/*`, `entity`, `repository`, `exception`, `util`, `advice`. |
| 2026-10-04 | Agreed data-model DECs vs open DECs; Liquibase index catalog §14.5 check. |
| 2026-10-04 | Three-spec interim map in `rules/documentation.md`; SM from requirements FEAT-11 until `state-machine.md`. |
| 2026-10-04 | State machine checks use [`spec/state-machine.md`](../spec/state-machine.md) (**agreed**). |
| 2026-10-04 | HTTP contract checks use [`spec/api-contract.md`](../spec/api-contract.md) (**agreed**). |
| 2026-10-04 | RAG ingest checks use [`spec/rag-ingestion.md`](../spec/rag-ingestion.md) (**agreed**); tests map [`spec/test-strategy.md`](../spec/test-strategy.md). |
| 2026-10-04 | Spec inputs: requirements **§0.5**; per-file **§0** via `rules/documentation.md`. |
| 2026-10-04 | Ask: no golden retrieval tests; eval [`spec/evaluation-strategy.md`](../spec/evaluation-strategy.md) §5–§8. |
| 2026-10-04 | **Reference** §2.3 scope gate; **DEC** register sync (**DEC-01…20** agreed). |
| 2026-10-04 | **DEC-01…20** agreed; review checklist updated. |
| 2026-10-04 | Hygiene: spec inputs list all ten child specs as **agreed** (removed stale **draft** labels). |
| 2026-10-04 | Java/Spring Boot review bar: coding guidelines, practices, naming, and packaging (`rules/java-springboot.md`). |
| 2026-10-04 | Packaging: group/split by layer and logical concern; reject a single dump package. |
| 2026-10-04 | AI mistakes: propose [`docs/ai-error.md`](../docs/ai-error.md); `/update-ai-error` writes the log. |
