# Java Spring Boot guidelines

Cursor attaches this file via [`.cursor/rules/java-springboot.mdc`](../.cursor/rules/java-springboot.mdc) (pointer only). Edit **this** file; do not copy the body into the `.mdc`.

Backend coding standards for the support ticket management application. **Public paths, payloads, and error JSON** live in `spec/api-contract.md` / `spec/rag-api-contract.md` and `rules/api-standards.md` once agreed. **Tests** live in `rules/testing.md`. **RAG pipeline detail** lives in `rules/rag-vector-store.md` and `spec/rag-ingestion.md`.

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

One root package under `src/main/java` (do not invent a second Spring Boot application). Layout is **by layer**, matching `spec/architecture.md`:

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
- Prefer `record` for API DTOs and small immutable values. Prefer `enum` for ticket **status** (and other closed sets once `data-model.md` agrees). Do not use `String` for status in domain or persistence.
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
- CORS for the local Vite origin is an implementation convenience, not an assessment requirement. Do not enable permissive `*` CORS as a permanent default.

## API conventions (Spring)

Contracts (paths, PUT vs PATCH, pagination names, error body) are **not** frozen here. Implementation rules:

- `@RestController` + JSON. Class-level `@RequestMapping` under `/api` when the contract uses that prefix. Preserve `POST /api/ai/ask` with a JSON body field `"question"` as named in the assessment.
- One update style for ticket fields, chosen in `spec/api-contract.md`. Status changes go through the service + state machine, not a raw entity setter in the controller.
- Controllers return DTO records or `ResponseEntity<DTO>`. Use `@Valid` / `@Validated` on request bodies and relevant params.
- Keyword search and status filter are **capabilities** on list/search endpoints; query-parameter names wait on the API spec.
- Do not version URLs (`/v1`) unless a spec agrees. Do not add auth filters, actor roles, attachments, bulk ops, or webhooks unless a spec agrees.
- Do not expose Spring AI, PgVector, or Ollama types on the HTTP boundary.

## Controllers

- Thin: parse HTTP → validate DTO → call one service method → map result to response DTO.
- No `@Transactional` on controllers. No business `if (status == CLOSED)` transition tables in controllers.
- Use `@PathVariable` / `@RequestParam` as the contract specifies. Reject blank ask questions via Bean Validation, not in ad-hoc controller `if` soup when a constraint will do.

## DTOs

- Request and response types are **records** in `api` (or `api.dto`). They are the HTTP contract, not JPA entities.
- Put Bean Validation on **request** records (`@NotBlank`, `@Size`, `@NotNull`, etc.) to match agreed field rules. Do not invent required fields the data-model/API spec has not agreed.
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

- `TicketStatus` (or equivalent) is an enum. Transition rules are a dedicated type (e.g. `TicketStatusMachine`) with no Spring imports.
- Invalid examples from the assessment (`CLOSED` → `OPEN`, `RESOLVED` → `OPEN`, `CANCELLED` → `OPEN`) must be rejected here.
- Domain exceptions are unchecked and meaningful (`IllegalTicketTransitionException`, not-found). They must not include SQL, stack traces, or secrets in `getMessage()`.

## Repositories

- Spring Data JPA interfaces in `persistence`. Naming: `TicketRepository`, `CommentRepository`.
- Methods express queries (derived names or `@Query` with **parameters**). Never concatenate user input into JPQL/SQL.
- Keyword search and filter-by-status belong here as queries the service calls — not as business-rule methods that change status.
- No `@Modifying` query that sets `status` except through the same path as the state machine (prefer loading the entity and letting the service apply a legal transition).
- Do not expose `TicketRepository.save` from a controller.

## Entities

- JPA entities live only in `persistence`. They map **1:1 with Liquibase** tables/columns. Explicit `@Table` / `@Column` names matching the changelog; do not let undocumented Hibernate naming be the schema.
- Identity, associations (ticket ↔ comments), and column nullability follow `spec/data-model.md` once agreed. Do not invent `category`, resolution-notes shape, or id format here.
- No state-machine tables inside entity setters. A setter that blindly does `this.status = next` is incorrect if it skips domain rules.
- Prefer `Instant` + timezone-safe mapping. Do not use EAGER graphs that load the entire comment history unless the use case needs it; lazy + explicit fetch in queries is preferred.
- Vector/chunk tables are persistence of **derived** RAG data, not the ticket source of truth. Schema for `vector` columns must match the agreed embedding dimension **once that model is agreed**.

## Error handling

- `@RestControllerAdvice` in `api` maps:
  - Bean Validation → client input error (field-level messages where practical)
  - not found → client-safe not found
  - illegal transition → client-usable message (HTTP **code** is defined in `api-contract.md`, not here)
  - unexpected failure → generic 500 **without** stack traces, SQL, or internals
- Do not return `null` bodies to mean not found. Do not leak `ConstraintViolation` SQL or Hibernate entity state.
- Keep one error envelope once `api-contract.md` agrees it; until then, still stay consistent and client-safe — do not invent a second ad-hoc JSON per controller.

## Database (PostgreSQL)

- PostgreSQL holds tickets (and comments). PgVector holds embeddings. Both schemas are Liquibase-only; no ad-hoc `psql` DDL as source of truth.
- Changing an entity field, constraint, or vector column **without** a matching changelog (or the reverse) is incorrect.
- H2 is **not** the default app or integration database. Integration tests: PostgreSQL via Testcontainers with Liquibase applied (`rules/testing.md`).

## Do not

- Do not start from “build the complete application” — implement the current agreed spec/plan only.
- Do not pin unfinalized versions (Boot patch, Spring AI, Postgres, PgVector, embedding model) as if decided.
- Do not hardcode Ollama URLs, model names, top-K, or similarity thresholds in services.
- Do not invent authentication, `/v1`, OpenAPI-as-requirement, or extra ticket resources.
- Do not implement agents or side effects from `/api/ai/ask`.
- Do not treat this file as a substitute for `spec/` field catalogs or HTTP contracts.
