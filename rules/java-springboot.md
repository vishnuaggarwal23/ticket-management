# Java Spring Boot guidelines

Cursor attaches this file via [`.cursor/rules/java-springboot.mdc`](../.cursor/rules/java-springboot.mdc) (pointer only). Edit **this** file; do not copy the body into the `.mdc`.

Backend coding standards for the support ticket management application.

| Topic | Where |
|-------|--------|
| Business/functional modules, layering map | [`spec/architecture.md`](../spec/architecture.md) §0, §4, §8–9 |
| HTTP paths, envelopes, list params | `rules/api-standards.md`; API map [`spec/architecture.md`](../spec/architecture.md) §11 |
| Ticket/comment **field** catalogs | [`spec/data-model.md`](../spec/data-model.md) (agreed); HTTP contract [`spec/api-contract.md`](../spec/api-contract.md) (agreed) |
| State machine rules | [`spec/state-machine.md`](../spec/state-machine.md) (agreed) **§0**, §5–§6; PDF hub [`spec/requirements.md`](../spec/requirements.md) FEAT-11 |
| RAG | `rules/rag-vector-store.md`, [`spec/architecture.md`](../spec/architecture.md) §0, §14–16, [`spec/rag-ingestion.md`](../spec/rag-ingestion.md) (agreed) |
| Tests | `rules/testing.md`, [`spec/test-strategy.md`](../spec/test-strategy.md), `commands/generate-tests.md` |
| UI | `rules/frontend.md` |

## Assessment vs project conventions

**Assessment PDF** names Java 21, Spring Boot, Spring AI, PostgreSQL/H2, an embedding model, and a vector store (examples include PGVector or Chroma). It requires REST APIs, backend validation, backend-enforced ticket status rules, persistence that survives restart, and no secrets in the repository. It does **not** mandate Spring Boot 3, Maven, Liquibase, PgVector over Chroma, Ollama, a package tree, JPA mapping style, or a test database.

**This project’s approved conventions** (use these when implementing; do not describe them as PDF requirements):

- Java **21**, Spring Boot **3** (major version only — do not pin a patch)
- **Maven Wrapper** — run `./mvnw` (or `mvnw.cmd`); do not rely on a locally installed Maven
- **PostgreSQL** as the ticket system of record; **Spring Data JPA** as the primary relational access layer; **PgVector** in the same instance for embeddings; **Liquibase** for all schema
- **Spring AI** for embeddings and chat; **Ollama** as the initial provider **via configuration only**
- Jakarta APIs (`jakarta.*`), not `javax.*`

Do not assume a finalized Spring Boot patch, Spring AI version, PostgreSQL/PgVector version, embedding model, vector dimension, or starter set until those are agreed.

## Tech stack (backend)

| Concern | Convention |
|---------|------------|
| Language | Java 21 |
| App | Single Spring Boot 3 monolith (`@SpringBootApplication`) |
| HTTP | Spring Web MVC, JSON REST |
| Persistence | **Spring Data JPA** (primary) on **PostgreSQL** — derived queries first, then `JpaSpecificationExecutor`, then `@EntityGraph`; `@Query` (native/JPQL) only when unavoidable; **no** hand-built JPQL in `*CustomImpl` + `EntityManager` |
| Schema | Liquibase changelogs; Hibernate DDL is **not** the source of truth (`ddl-auto` `validate` or `none`) |
| Non-JPA I/O | **Narrow exception:** `ticket_vector_chunk` cosine search/upsert may use `JdbcTemplate` or Spring AI behind a `rag` port — not a second persistence style for tickets/comments |
| Validation | Bean Validation on API DTOs (`jakarta.validation`) |
| Config | `application.yml` + environment variables; typed `@ConfigurationProperties` for RAG |
| Time | `java.time` (`Instant` for stored timestamps); no `java.util.Date` |

Do not add MapStruct, Lombok, QueryDSL, or extra web stacks unless a spec agrees. Do not add Spring Security unless authentication is an agreed spec.

## Package structure

One root package under `src/main/java` (do not invent a second Spring Boot application). Pick one root (e.g. `com.example.tickets`) and use it consistently — **do not** commit a second package root.

Layout is **by technical role** (one package per kind of type), matching [`spec/architecture.md`](../spec/architecture.md) §9 — so you can open `controller/`, `service/`, `entity/`, etc. and see everything at a glance:

```
{root}/
  Application.java
  advice/           # @RestControllerAdvice (error mapping)
  controller/       # all @RestController types (tickets, ask, …)
  dto/
    common/         # envelopes: DataResponse, PageResponse, ErrorResponse, …
    request/        # inbound JSON records (@Valid)
    response/       # outbound JSON records
    serde/          # Jackson deserializers/serializers for DTOs
  domain/           # enums, state machine, pure domain rules (no Spring Web, no JPA)
  entity/           # JPA @Entity types only
  exception/        # application/domain exceptions (mapped in advice/)
  repository/       # Spring Data JPA interfaces + Specification helpers
  service/          # @Service use cases, @Component mappers; includes AskService / TicketIngestionService
  util/             # small shared helpers (TicketId, SortParser, constraints constants, …)
  rag/              # RAG ports, adapters, chunking, vector store impl (not HTTP controllers)
  config/           # @Configuration, @ConfigurationProperties, Spring AI wiring
```

**Rules:**

- **Do not** mix controllers and DTOs in one package (`api.ticket` style is deprecated).
- **Do not** put entities and repositories in one package — `entity/` vs `repository/`.
- **Do not** put exceptions in `domain/` — use `exception/` so failures are easy to find.
- HTTP stays thin: `controller/` → `service/` → `repository/` / `rag` ports.
- Tests use the **same package names** under `src/test/java` (`controller`, `service`, `repository`, …).
- Do not invent parallel trees (`web`, `dao`, `manager`). Subpackages under `dto/` and `rag/` are allowed when a group grows; avoid one-class micro-packages.

The main class stays empty of business logic.

## Layering and call flow

```
Controller → Application service → Domain (rules) / Repository (I/O)
                                      → RAG port (ingest or ask)
```

| Layer | May | Must not |
|-------|-----|----------|
| **controller** (+ **dto**) | HTTP mapping, `@Valid`, status codes; calls services | Status-machine rules, JPQL, embedding math, leaking entities |
| **advice** | Map exceptions → error envelope | Business rules |
| **domain** | Ticket status rules, domain errors, pure functions | `@RestController`, `@Entity`, `@Autowired`, JDBC |
| **service** | `@Transactional` use cases; **inject repositories** (and RAG ports), not raw SQL for tickets | HTTP types (`HttpServletRequest`); `JdbcTemplate` / `EntityManager` for ticket CRUD; persistence-entity JSON |
| **entity** / **repository** | Entities map tables; repos load/save, keyword/status queries | `JdbcTemplate` on ticket tables; ad-hoc `UPDATE status`; skip the state machine |
| **rag** | Build knowledge text, retrieve, generate from retrieved context only | Create tickets, notify, tool-chain from ask |
| **config** | Beans and property binding | Business rules |

Keep HTTP adapters thin. Repositories must not apply ad-hoc status updates.

## Coding style

- Constructor injection only. Prefer a single `final` constructor (or one compact constructor). No field/`@Autowired` injection, no setter injection.
- Readable names: `TicketService`, `TicketStatus`, `IllegalTicketTransitionException`. No opaque abbreviations (`TktSvc`, `SM`).
- Prefer `record` for API DTOs and small immutable values. Use `enum` for `TicketStatus`, `TicketPriority`, `TicketCategory` per [`spec/data-model.md`](../spec/data-model.md) §5. Do not use `String` for those in domain or persistence. `TicketPriority` has **`CRITICAL`** (not `URGENT`); accept JSON `"URGENT"` only via an **API-layer** deserializer that maps it to `CRITICAL`.
- Public application APIs must not return `null`. Use `Optional` for a missing ticket; empty `List`/`Page` for empty collections.
- Prefer `final` on injected collaborators. Name them by role (`ticketService`, `ticketRepository`), not as if they were collections (`tickets` for a single repository or service).
- Keep methods short; extract patch application, after-commit scheduling, and ingest chunk assembly into focused types or private methods when orchestration grows.
- Java 21 is fine (`record`, `switch`, text blocks for JPQL or prompts held in config/code as agreed). Do not use `sun.*` APIs.
- Log with SLF4J. Include ticket id when known. Never log secrets, passwords, API keys, raw prompts, or full model output by default.

```java
// ❌ BAD — field injection, entity as JSON, null, String status
@Autowired TicketRepository repo;
public TicketEntity get(String id) { return repo.findById(id).orElse(null); }

// ✅ GOOD — constructor, Optional, domain-safe lookup
public TicketService(TicketRepository ticketRepository) { this.ticketRepository = ticketRepository; }
public Optional<Ticket> findById(TicketId id) { return ticketRepository.findById(id); }
```

## Configuration

- Externalize JDBC URL, credentials, Ollama/base URLs, model ids, RAG **top-K**, similarity threshold, and chunk size limits. No magic numbers in Java for retrieval.
- Bind RAG settings with `@ConfigurationProperties` (prefix such as `rag.retrieval`). Validate with `@Validated` on that properties class where practical.
- Committed examples list **environment variable names only** (`.env.example`). Never commit passwords, keys, or machine-specific absolute paths.
- Spring profiles: `local` / default for operator-managed Postgres (env vars); tests use Testcontainers (see testing rules). Do not point the default test suite at a developer’s already-running database.
- Set `spring.jpa.open-in-view=false`. Do not use Open Session in View to lazy-load in controllers.
- CORS for the local Vite dev server is an implementation convenience, not an assessment requirement. Example (adjust port to your Vite config):

```java
// config/WebCorsConfig.java — dev-oriented; not *
registry.addMapping("/api/**")
    .allowedOrigins("http://localhost:5173")
    .allowedMethods("GET", "POST", "PATCH", "OPTIONS")
    .allowedHeaders("*");
```

Do not enable permissive `*` CORS as a permanent default.

## API conventions (Spring)

Public envelopes, pagination/sort/search query params, HTTP status mapping, PATCH-for-updates, and URI versioning (`/api/v1`) are defined in `rules/api-standards.md`. Resource field catalogs: [`spec/api-contract.md`](../spec/api-contract.md). Ask `data` semantics: [`spec/rag-api-contract.md`](../spec/rag-api-contract.md). Implementation rules:

- `@RestController` + JSON. Ticket controllers use `/api/v1`. Preserve `POST /api/ai/ask` with a JSON body field `"question"` as named in the assessment; also map `POST /api/v1/ai/ask`.
- Ticket field updates are **PATCH**. Status changes go through the service + state machine, not a raw entity setter in the controller.
- Controllers return the success envelope (`data` / list `meta`) as DTO records or `ResponseEntity`. Use `@Valid` / `@Validated` on request bodies and relevant params.
- List endpoints accept `page`, `size`, `sort`, `q`, and `status` per api-standards.
- Do not add auth filters, actor roles, attachments, bulk ops, or webhooks unless a spec agrees.
- Do not expose Spring AI, PgVector, or Ollama types on the HTTP boundary.

## Controllers

- Thin: parse HTTP → validate DTO → call one service method → map result to response DTO.
- No `@Transactional` on controllers. No business `if (status == CLOSED)` transition tables in controllers.
- Use `@PathVariable` / `@RequestParam` as the contract specifies. Reject blank ask questions via Bean Validation, not in ad-hoc controller `if` soup when a constraint will do.

## DTOs

- Request and response types are **records** in `dto.request` / `dto.response` (shared envelopes in `dto.common`). They are the HTTP contract, not JPA entities.
- Put Bean Validation on **request** records to match [`spec/data-model.md`](../spec/data-model.md) §16 (e.g. `@NotBlank` on create `title`, `@Size` limits). Assignee: `@Size(max = 320)` only — **not** `@Email`. Do not add required fields beyond that spec.
- Map explicitly in the service or a dedicated mapper in `service`. No bidirectional JPA graphs in JSON.
- Ask response must represent grounded answer + cited ticket ids **or** honest no-match per [`spec/rag-api-contract.md`](../spec/rag-api-contract.md) (**DEC-11** agreed). Do not add **confidence** or other **Reference** ask fields ([`spec/requirements.md`](../spec/requirements.md) **§2.3**).
- Do not return persistence entities from controllers. Do not put Jackson annotations on entities to “make the API work.”

## Services

- Application services own use cases: create, update fields, add comment, list/search/filter, load detail, **request a status transition**, trigger RAG refresh after update/close per [`spec/rag-ingestion.md`](../spec/rag-ingestion.md) §10.
- Annotate **write** methods `@Transactional`; read-only queries `@Transactional(readOnly = true)` when they need a transaction. Keep transactions short.
- Delegate legality of `OPEN` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`, `OPEN`/`IN_PROGRESS` → `CANCELLED`, and all other moves to the **domain state machine**. On illegal move, throw a domain exception; do not save the illegal status.
- After successful **update** or **close**, call the RAG ingestion port so embeddings do not go stale. Do not embed inside the repository.
- Services depend on repository **interfaces** and domain types, not on controllers.

## Domain (state machine)

Authoritative transition table: **[`spec/state-machine.md`](../spec/state-machine.md)** — legal edges **T1–T5**, forbidden reopen **X1–X3**, and full invalid matrix under **DEC-02 (A)** (agreed). Do not allow skipped hops (e.g. `OPEN` → `RESOLVED`).

- `TicketStatus` is an enum. Transition rules live in a dedicated type (e.g. `TicketStatusMachine`) with **no** Spring imports.
- Invalid transitions throw a domain exception; service must not persist the illegal status.
- Domain exceptions are unchecked and meaningful (`IllegalTicketTransitionException`, not-found). They must not include SQL, stack traces, or secrets in `getMessage()`.

## Repositories (Spring Data JPA — use extensively)

Ticket and comment **read/write paths go through Spring Data JPA** unless a spec documents a narrow exception (vector similarity is **not** ticket persistence — see `rag/` + `rules/rag-vector-store.md`).

**Preference order (C-06):**

1. **Derived** query methods (`findByStatus`, `findByTicket_IdOrderByCreatedAtAsc`, `ContainingIgnoreCase`, …).
2. **`JpaSpecificationExecutor`** + reusable `Specification` types for optional filters, sort (including enum order), and fetch joins — not string-concatenated JPQL in a `*CustomImpl`.
3. **`@EntityGraph`** on repository methods when a fetch plan is static.
4. **`@Query`** (JPQL or native) **only when** derived/spec APIs cannot express the access (e.g. `SELECT nextval('ticket_number_seq')`).

- Repository interfaces in `repository`, extending `JpaRepository<Entity, Id>` and `JpaSpecificationExecutor` when dynamic queries are needed. Naming: `TicketRepository`, `CommentRepository`.
- **Do not** add `TicketRepositoryCustomImpl`-style classes that build JPQL with `EntityManager.createQuery`.
- Services call repository interfaces; they do **not** embed JPQL/SQL strings or inject `JdbcTemplate` for `ticket` / `ticket_comment`.
- Methods express queries with **parameters**. Never concatenate user input into JPQL/SQL.
- Keyword search and filter-by-status belong here as queries the service calls — not as business-rule methods that change status.
- Keyword `q` searches **title and description** only (DEC-08; [`spec/data-model.md`](../spec/data-model.md) §15.2; `rules/api-standards.md`).
- No `@Modifying` query that sets `status` except through the same path as the state machine (prefer loading the entity and letting the service apply a legal transition).
- Do not expose `TicketRepository.save` from a controller.

## Entities

- JPA entities live only in `entity`. They map **1:1 with Liquibase** tables/columns. Explicit `@Table` / `@Column` names matching the changelog; do not let undocumented Hibernate naming be the schema.
- Tables: `ticket`, `ticket_comment`, `ticket_vector_chunk` ([`spec/data-model.md`](../spec/data-model.md) §6, §8, §14). Ticket PK `id` is `VARCHAR` `TKT-{n}`; comment PK `UUID`; `resolution_notes` on `ticket`.
- Identity, associations, and nullability follow [`spec/data-model.md`](../spec/data-model.md). All indexes in §14.5 must appear in Liquibase (`003-ticket-indexes`, `005-vector-indexes`, extensions in `002`).
- No state-machine tables inside entity setters. A setter that blindly does `this.status = next` is incorrect if it skips domain rules.
- Prefer `Instant` + timezone-safe mapping. Do not use EAGER graphs that load the entire comment history unless the use case needs it; lazy + explicit fetch in queries is preferred.
- **`ticket` and `ticket_comment` are JPA-mapped entities.** Do not add parallel DAOs or JDBC repositories for the same tables.
- `ticket_vector_chunk` is **derived** RAG storage. It may stay off the JPA entity graph (e.g. `JdbcVectorChunkStore` in `rag/`) when `vector` operators are easier in SQL; Liquibase still owns the table ([`spec/data-model.md`](../spec/data-model.md) §8).

## Error handling

- `@RestControllerAdvice` in `advice` maps `exception` types onto the **error envelope** in `rules/api-standards.md`:
  - Bean Validation → 400 `VALIDATION_ERROR` (field-level `details`)
  - not found → 404 `NOT_FOUND`
  - illegal transition → 409 `ILLEGAL_TRANSITION`
  - unexpected failure → 500 `INTERNAL_ERROR` **without** stack traces, SQL, or internals
- Do not return `null` bodies to mean not found. Do not leak `ConstraintViolation` SQL or Hibernate entity state.
- Do not invent a second error JSON per controller.

## Liquibase (schema source of truth)

- Changelogs under `src/main/resources/db/changelog/` (order per [`spec/data-model.md`](../spec/data-model.md) §14: tables → extensions `pg_trgm`/`vector` → relational indexes → vector table → HNSW).
- Every entity/column/index change needs a changeset. Index names are fixed in [`spec/data-model.md`](../spec/data-model.md) §14.5 — do not rename without a spec revision.
- Hibernate `ddl-auto`: `validate` or `none`. Integration tests apply the **same** changelogs via Testcontainers (`rules/testing.md`).

## Database (PostgreSQL)

- PostgreSQL holds tickets (and comments). PgVector holds embeddings. Both schemas are Liquibase-only; no ad-hoc `psql` DDL as source of truth.
- Changing an entity field, constraint, or vector column **without** a matching changelog (or the reverse) is incorrect.
- H2 is **not** the default app or integration database. Integration tests: PostgreSQL via Testcontainers with Liquibase applied (`rules/testing.md`).

## Do not

- Do not start from “build the complete application” — implement the current agreed spec/plan only.
- Do not pin unfinalized versions (Boot patch, Spring AI, Postgres, PgVector, embedding model) as if decided.
- Do not hardcode Ollama URLs, model names, top-K, or similarity thresholds in services.
- Do not invent authentication, OpenAPI-as-requirement, or extra ticket resources. URI versioning follows `rules/api-standards.md`.
- Do not implement agents or side effects from `/api/ai/ask`.
- Do not treat this file as a substitute for `spec/` field catalogs or HTTP contracts.

---

## Revision history

| Date | Note |
|------|------|
| 2026-09-24 | Initial Java 21 / Spring Boot 3 layering, packages, and backend coding conventions. |
| 2026-10-03 | Stack alignment: PostgreSQL, Liquibase, Maven Wrapper, Spring AI provider configuration. |
| 2026-10-03 | Recorded agreed backend package layout (`api` / `domain` / `service` / `persistence` / `rag` / `config`). |
| 2026-10-04 | SDD expansion: assessment vs convention; defer open payloads to agreed specs. |
| 2026-10-04 | Synced with [`spec/architecture.md`](../spec/architecture.md) §8–9 technical components. |
| 2026-10-04 | Added revision history section. |
| 2026-10-04 | Entities, enums, tables, and Liquibase index catalog aligned with agreed [`spec/data-model.md`](../spec/data-model.md). |
| 2026-10-04 | State machine: requirements FEAT-11 + [`state-machine.md`](../spec/state-machine.md); **DEC-06** PATCH agreed. |
| 2026-10-04 | **DEC-10:** PostgreSQL + Testcontainers; **DEC-09:** Ollama embed model per `rag-ingestion.md` §12. |
| 2026-10-04 | Domain SM defers to draft [`spec/state-machine.md`](../spec/state-machine.md); removed duplicate edge table. |
| 2026-10-04 | HTTP payloads: draft [`spec/api-contract.md`](../spec/api-contract.md). |
| 2026-10-04 | Pointers to child spec **§0** maps (SM, RAG, architecture); steering sync with `rules/documentation.md`. |
| 2026-10-04 | **DEC-02** agreed; **Reference** ask fields per requirements §2.3. |
| 2026-10-04 | `TicketPriority` **CRITICAL**; JSON `URGENT` mapped in `api`; assignee `@Size(max=320)`. |
| 2026-10-04 | Packaging: group by layer then logical concern; do not dump all types in one package. |
| 2026-10-04 | **Spring Data JPA primary** for tickets/comments; custom repository fragments; vector chunk I/O exception in `rag/`. |
| 2026-10-04 | **C-06** preference ladder: derived → `Specification` → `@EntityGraph` → minimal `@Query`; no `*CustomImpl` JPQL. |
| 2026-10-04 | Package layout: `controller`, `dto/*`, `entity`, `repository`, `exception`, `util`, `advice`, `service`, `rag`, `config`. |
