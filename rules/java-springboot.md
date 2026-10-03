# Java Spring Boot guidelines

Cursor attaches this file via [`.cursor/rules/java-springboot.mdc`](../.cursor/rules/java-springboot.mdc) (pointer only). Edit **this** file; do not copy the body into the `.mdc`.

Backend coding standards for the support ticket management application.

| Topic | Where |
|-------|--------|
| Business/functional modules, layering map | [`spec/architecture.md`](../spec/architecture.md) §4, §8–9 |
| HTTP paths, envelopes, list params | `rules/api-standards.md`; API map [`spec/architecture.md`](../spec/architecture.md) §11 |
| Ticket/comment **field** catalogs | [`spec/data-model.md`](../spec/data-model.md) (agreed); HTTP narrative in `spec/api-contract.md` when added |
| State machine rules | [`spec/state-machine.md`](../spec/state-machine.md) (draft); PDF hub [`spec/requirements.md`](../spec/requirements.md) FEAT-11 |
| RAG | `rules/rag-vector-store.md`, [`spec/architecture.md`](../spec/architecture.md) §14–16; numeric tuning in `spec/rag-ingestion.md` when added |
| Tests | `rules/testing.md`, `commands/generate-tests.md` |
| UI | `rules/frontend.md` |

## Assessment vs project conventions

**Assessment PDF** names Java 21, Spring Boot, Spring AI, PostgreSQL/H2, an embedding model, and a vector store (examples include PGVector or Chroma). It requires REST APIs, backend validation, backend-enforced ticket status rules, persistence that survives restart, and no secrets in the repository. It does **not** mandate Spring Boot 3, Maven, Liquibase, PgVector over Chroma, Ollama, a package tree, JPA mapping style, or a test database.

**This project’s approved conventions** (use these when implementing; do not describe them as PDF requirements):

- Java **21**, Spring Boot **3** (major version only — do not pin a patch)
- **Maven Wrapper** — run `./mvnw` (or `mvnw.cmd`); do not rely on a locally installed Maven
- **PostgreSQL** as the ticket system of record; **PgVector** in the same instance for embeddings; **Liquibase** for all schema
- **Spring AI** for embeddings and chat; **Ollama** as the initial provider **via configuration only**
- Jakarta APIs (`jakarta.*`), not `javax.*`

Do not assume a finalized Spring Boot patch, Spring AI version, PostgreSQL/PgVector version, embedding model, vector dimension, or starter set until those are agreed.

## Tech stack (backend)

| Concern | Convention |
|---------|------------|
| Language | Java 21 |
| App | Single Spring Boot 3 monolith (`@SpringBootApplication`) |
| HTTP | Spring Web MVC, JSON REST |
| Persistence | Spring Data JPA + PostgreSQL |
| Schema | Liquibase changelogs; Hibernate DDL is **not** the source of truth (`ddl-auto` `validate` or `none`) |
| Validation | Bean Validation on API DTOs (`jakarta.validation`) |
| Config | `application.yml` + environment variables; typed `@ConfigurationProperties` for RAG |
| Time | `java.time` (`Instant` for stored timestamps); no `java.util.Date` |

Do not add MapStruct, Lombok, QueryDSL, or extra web stacks unless a spec agrees. Do not add Spring Security unless authentication is an agreed spec.

## Package structure

One root package under `src/main/java` (do not invent a second Spring Boot application). Pick one root (e.g. `com.example.tickets`) and use it consistently — **do not** commit a second package root.

Layout is **by layer**, matching [`spec/architecture.md`](../spec/architecture.md) §9:

```
{root}/
  Application.java
  api/            # controllers, request/response records, @ControllerAdvice
  domain/         # status enum, state machine, domain exceptions (no Spring Web, no JPA)
  service/        # transactional application services; mapping DTO ↔ domain/entity
  persistence/    # JPA entities, Spring Data repositories
  rag/            # document build, chunk/embed ports, retrieval, ask orchestration
  config/         # @Configuration, @ConfigurationProperties, Spring AI wiring
```

Use subpackages only when a type set grows (`api.ticket`, `persistence.ticket`). Keep the **ask** path in `rag/` + a thin controller in `api/` — not a second microservice.

The main class stays empty of business logic.

## Layering and call flow

```
Controller (api) → Application service → Domain (rules) / Repository (I/O)
                                      → RAG port (ingest or ask)
```

| Layer | May | Must not |
|-------|-----|----------|
| **api** | HTTP mapping, `@Valid`, status codes, DTO mapping calls | Status-machine rules, JPQL, embedding math, leaking entities |
| **domain** | Ticket status rules, domain errors, pure functions | `@RestController`, `@Entity`, `@Autowired`, JDBC |
| **service** | `@Transactional` use cases, orchestrate repos + domain + RAG hooks | HTTP types (`HttpServletRequest`), persistence-entity JSON |
| **persistence** | Load/save, query by keyword/status | Transition status by ad-hoc `UPDATE`, skip the state machine |
| **rag** | Build knowledge text, retrieve, generate from retrieved context only | Create tickets, notify, tool-chain from ask |
| **config** | Beans and property binding | Business rules |

Keep HTTP adapters thin. Repositories must not apply ad-hoc status updates.

## Coding style

- Constructor injection only. Prefer a single `final` constructor (or one compact constructor). No field/`@Autowired` injection, no setter injection.
- Readable names: `TicketService`, `TicketStatus`, `IllegalTicketTransitionException`. No opaque abbreviations (`TktSvc`, `SM`).
- Prefer `record` for API DTOs and small immutable values. Use `enum` for `TicketStatus`, `TicketPriority`, `TicketCategory` per [`spec/data-model.md`](../spec/data-model.md) §5. Do not use `String` for those in domain or persistence.
- Public application APIs must not return `null`. Use `Optional` for a missing ticket; empty `List`/`Page` for empty collections.
- Prefer `final` on injected collaborators. Keep methods short; extract when a service method both mutates a ticket and implements transition tables.
- Java 21 is fine (`record`, `switch`, text blocks for JPQL or prompts held in config/code as agreed). Do not use `sun.*` APIs.
- Log with SLF4J. Include ticket id when known. Never log secrets, passwords, API keys, raw prompts, or full model output by default.

```java
// ❌ BAD — field injection, entity as JSON, null, String status
@Autowired TicketRepository repo;
public TicketEntity get(String id) { return repo.findById(id).orElse(null); }

// ✅ GOOD — constructor, Optional, domain-safe lookup
public TicketService(TicketRepository tickets) { this.tickets = tickets; }
public Optional<Ticket> findById(TicketId id) { return tickets.findById(id); }
```

## Configuration

- Externalize JDBC URL, credentials, Ollama/base URLs, model ids, RAG **top-K**, similarity threshold, and chunk size limits. No magic numbers in Java for retrieval.
- Bind RAG settings with `@ConfigurationProperties` (prefix such as `rag.retrieval`). Validate with `@Validated` on that properties class where practical.
- Committed examples list **environment variable names only** (`.env.example`). Never commit passwords, keys, or machine-specific absolute paths.
- Spring profiles: `local` / default for Docker Compose Postgres; tests use Testcontainers (see testing rules). Do not point the default test suite at a developer’s already-running database.
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

Public envelopes, pagination/sort/search query params, HTTP status mapping, PATCH-for-updates, and URI versioning (`/api/v1`) are defined in `rules/api-standards.md`. Resource field catalogs remain in `spec/api-contract.md` / `spec/rag-api-contract.md`. Implementation rules:

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

- Request and response types are **records** in `api` (or `api.dto`). They are the HTTP contract, not JPA entities.
- Put Bean Validation on **request** records to match [`spec/data-model.md`](../spec/data-model.md) §16 (e.g. `@NotBlank` on create `title`, `@Size` limits). Do not add required fields beyond that spec.
- Map explicitly in the service or a dedicated mapper type in `api`/`service`. No bidirectional JPA graphs in JSON.
- Ask response must be able to represent a grounded answer + cited ticket ids **or** honest no-match — field names wait on `spec/rag-api-contract.md`. Do not add a confidence field unless that spec does.
- Do not return persistence entities from controllers. Do not put Jackson annotations on entities to “make the API work.”

## Services

- Application services own use cases: create, update fields, add comment, list/search/filter, load detail, **request a status transition**, trigger RAG refresh after update/close per `spec/rag-ingestion.md`.
- Annotate **write** methods `@Transactional`; read-only queries `@Transactional(readOnly = true)` when they need a transaction. Keep transactions short.
- Delegate legality of `OPEN` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`, `OPEN`/`IN_PROGRESS` → `CANCELLED`, and all other moves to the **domain state machine**. On illegal move, throw a domain exception; do not save the illegal status.
- After successful **update** or **close**, call the RAG ingestion port so embeddings do not go stale. Do not embed inside the repository.
- Services depend on repository **interfaces** and domain types, not on controllers.

## Domain (state machine)

Authoritative transition table: **[`spec/state-machine.md`](../spec/state-machine.md)** — legal edges **T1–T5**, forbidden reopen **X1–X3**, and full invalid matrix under default **DEC-02 (A)**. Do not allow skipped hops (e.g. `OPEN` → `RESOLVED`) unless **DEC-02** is agreed to option (B) and the spec is updated.

- `TicketStatus` is an enum. Transition rules live in a dedicated type (e.g. `TicketStatusMachine`) with **no** Spring imports.
- Invalid transitions throw a domain exception; service must not persist the illegal status.
- Domain exceptions are unchecked and meaningful (`IllegalTicketTransitionException`, not-found). They must not include SQL, stack traces, or secrets in `getMessage()`.

## Repositories

- Spring Data JPA interfaces in `persistence`. Naming: `TicketRepository`, `CommentRepository`.
- Methods express queries (derived names or `@Query` with **parameters**). Never concatenate user input into JPQL/SQL.
- Keyword search and filter-by-status belong here as queries the service calls — not as business-rule methods that change status.
- Keyword `q` searches **title and description** only (DEC-08; [`spec/data-model.md`](../spec/data-model.md) §15.2; `rules/api-standards.md`).
- No `@Modifying` query that sets `status` except through the same path as the state machine (prefer loading the entity and letting the service apply a legal transition).
- Do not expose `TicketRepository.save` from a controller.

## Entities

- JPA entities live only in `persistence`. They map **1:1 with Liquibase** tables/columns. Explicit `@Table` / `@Column` names matching the changelog; do not let undocumented Hibernate naming be the schema.
- Tables: `ticket`, `ticket_comment`, `ticket_vector_chunk` ([`spec/data-model.md`](../spec/data-model.md) §6, §8, §14). Ticket PK `id` is `VARCHAR` `TKT-{n}`; comment PK `UUID`; `resolution_notes` on `ticket`.
- Identity, associations, and nullability follow [`spec/data-model.md`](../spec/data-model.md). All indexes in §14.5 must appear in Liquibase (`003-ticket-indexes`, `005-vector-indexes`, extensions in `002`).
- No state-machine tables inside entity setters. A setter that blindly does `this.status = next` is incorrect if it skips domain rules.
- Prefer `Instant` + timezone-safe mapping. Do not use EAGER graphs that load the entire comment history unless the use case needs it; lazy + explicit fetch in queries is preferred.
- Vector/chunk tables are persistence of **derived** RAG data, not the ticket source of truth. Schema for `vector` columns must match the agreed embedding dimension **once that model is agreed**.

## Error handling

- `@RestControllerAdvice` in `api` maps exceptions onto the **error envelope** in `rules/api-standards.md`:
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
| 2026-10-04 | State machine interim: requirements FEAT-11; child specs only when user adds files. |
| 2026-10-04 | Domain SM defers to draft [`spec/state-machine.md`](../spec/state-machine.md); removed duplicate edge table. |
