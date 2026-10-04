# Backend implementation plan — ticket CRUD, state machine, then RAG

> **Artefact:** backend-only implementation plan (Spec-Driven Development **Plan / Tasks** step).  
> **Not** a Spec Kit feature tree under `specs/[###-feature]/`. Product source of truth remains `spec/`, `rules/`, `commands/`.  
> **Code root:** `backend/` under the repository working directory. **Do not** put Java sources at repo root or under a second application.  
> **Frontend:** out of this plan. **RAG (whole):** documented below, **blocked** until Phase C is marked complete **and** you explicitly confirm to start RAG.  
> **Status of this plan:** draft for implementation — Phase C completion and RAG start require your sign-off in chat.  
> **Date:** 2026-10-04.

---

## 0. How to use this document

- Work **top to bottom**. Do not skip a phase. Do not start a later phase until the earlier phase **Done criteria** and **Status** row are marked.
- Each phase lists **why**, **what to create**, **how it behaves**, **tests**, **acceptance IDs**, and **review**.
- Tick boxes in place when a slice is implemented, tested, and corrected.
- If a spec and a rule disagree, **stop** — do not guess. Confirm with the user (constitution / tech-lead rule).
- After material Java changes, run `graphify update .` so `graphify-out/` stays current (AST-only).

### 0.1 Workflow this plan implements

```text
Requirement (PDF + spec/) → Specification (agreed) → THIS PLAN
  → Implementation (backend/) → Testing (JUnit / Testcontainers)
  → Review (commands/review-code.md, commands/generate-tests.md)
  → Fix → (only then) RAG after user confirmation
```

**Do not** start from “build the complete application.”

### 0.2 Hard sequencing gate (mandatory)

| Gate | Meaning |
|------|---------|
| **G0** | Scaffold + configuration + Liquibase **relational** schema. |
| **G1** | Domain enums + state machine **before** services mutate `status`. |
| **G2** | Persistence + repositories (no ad-hoc status `UPDATE`). |
| **G3** | Application services + DTO mapping + validation. |
| **G4** | HTTP controllers + envelopes + error advice. |
| **G5** | Full CRUD + list/search/filter + comments **API integration** tests green. |
| **G6** | State machine **T1–T5** and **20 illegal pairs** proven at domain + service + HTTP (409, row unchanged). Persistence-after-restart proven. |
| **G7** | **Phase C complete** — you mark this plan’s Phase C status **complete**. |
| **G8** | **Your explicit confirmation in chat** to start RAG. Until then: **no** Spring AI wiring, **no** embeddings, **no** `ticket_vector_chunk` usage, **no** `/api/ai/ask`, **no** ingest pipeline code beyond an unused port interface if needed for later wiring. |

RAG sections (Phase D+) are written in full so they are not invented later. They are **not authorization to implement**.

---

## 1. Purpose, scope, and non-goals

### 1.1 Purpose

Deliver a **single Spring Boot 3 monolith** under `backend/` that:

1. Persists support tickets and comments in **PostgreSQL** (survives restart).
2. Exposes **JSON REST** under `/api/v1` for create, list, detail, PATCH fields, comments, keyword search, status filter.
3. Enforces the **backend status state machine** (legal T1–T5; reject all other pairs with **409**).
4. **After confirmation:** ingest ticket knowledge, embed into **PgVector**, and serve grounded **`POST /api/ai/ask`**.

### 1.2 In scope (this plan)

- Backend Java application only (`backend/`).
- Ticket **CRUD** (create, list, get, PATCH fields — **no DELETE**, **no PUT**).
- Comments (add + view on detail).
- Keyword `q` + status filter + pagination/sort.
- Validation, error envelope, CORS for local Vite (convention).
- State machine and transitions.
- Tests: JUnit 5, Mockito, MockMvc slice, PostgreSQL Testcontainers + Liquibase.
- RAG **design and later implementation** as Phase D–F (gated).

### 1.3 Out of scope (do not implement)

From [`spec/requirements.md`](spec/requirements.md) **§2.3 Reference** and rules:

- Frontend / React / Vite.
- Authentication, authorization, roles, multi-tenancy (**DEC-12**).
- `DELETE` ticket, `PUT` ticket, `/transition` sub-resource, bulk ops, attachments, webhooks.
- Agents, tool chaining, notifications, ask creating tickets.
- Ask `confidence` / extra `reason` fields.
- Async ingest queues, versioned embedding history, semantic chunking, rerankers, vector admin APIs.
- CI pipeline, H2, OpenAPI-as-requirement, Lombok, MapStruct, Spring Security.
- Frontend tests.

### 1.4 Labeling (assessment vs convention)

| Label | In this plan |
|-------|----------------|
| **PDF** | Assignment obligation (CRUD capabilities, SM, persistence, RAG later). |
| **Convention** | Project choice (`/api/v1`, PATCH, 409, envelopes, Boot 3, Maven Wrapper, Liquibase, Testcontainers). |
| **Agreed DEC** | Hub [`spec/requirements.md`](spec/requirements.md) §10.2 **DEC-01…19**. |

Do not describe conventions as PDF mandates in comments or README.

---

## 2. Source of truth (read these; do not duplicate blindly)

| Concern | Read |
|---------|------|
| Hub FR / FEAT / AC-CORE / DEC | [`spec/requirements.md`](spec/requirements.md) |
| Layers, packages, RAG placement | [`spec/architecture.md`](spec/architecture.md) §8–9, §13–19 |
| Columns, enums, DTOs, Liquibase, validation | [`spec/data-model.md`](spec/data-model.md) |
| HTTP paths, payloads, scenarios | [`spec/api-contract.md`](spec/api-contract.md) |
| T1–T5, 20 illegal pairs, 409 | [`spec/state-machine.md`](spec/state-machine.md) |
| Ingest/chunk/embed (Phase D+) | [`spec/rag-ingestion.md`](spec/rag-ingestion.md) |
| Ask JSON (Phase F) | [`spec/rag-api-contract.md`](spec/rag-api-contract.md) |
| Test mapping | [`spec/test-strategy.md`](spec/test-strategy.md), [`rules/testing.md`](rules/testing.md) |
| Java style | [`rules/java-springboot.md`](rules/java-springboot.md) |
| Envelopes, list params | [`rules/api-standards.md`](rules/api-standards.md) |
| RAG rules (Phase D+) | [`rules/rag-vector-store.md`](rules/rag-vector-store.md) |
| Review diffs | [`commands/review-code.md`](commands/review-code.md) |
| Propose tests | [`commands/generate-tests.md`](commands/generate-tests.md) |
| Ask grounding (after RAG) | [`commands/review-rag-output.md`](commands/review-rag-output.md) |
| Retrieval quality (after RAG) | [`spec/evaluation-strategy.md`](spec/evaluation-strategy.md) |

**Precedence:** PDF → `spec/requirements.md` → agreed child specs → `rules/` → this plan. This plan must not invent fields, paths, or edges.

---

## 3. Implementation decisions (confirmed 2026-10-04)

| ID | Topic | Decision |
|----|--------|----------|
| **C-01** | Java root package | **`com.ticketmanagement`** — one root, never a second. |
| **C-02** | `TicketPriority` | Canonical stored/returned value is **`CRITICAL`** (fourth enum constant). Incoming JSON (create/PATCH) **`URGENT` is accepted and mapped to `CRITICAL`**. Persist and respond only `CRITICAL`. Other unknown strings still **400**. Domain/JPA enum has **no** `URGENT` constant. Mapping lives in the **API Jackson deserializer** (`api`), not in `domain`. |
| **C-03** | Spring Boot 3 | Use the **latest production (GA) Spring Boot 3** release at implementation time — not milestone/RC. Do not describe the patch as a PDF requirement. |
| **C-04** | Chat / generation model (RAG) | **Still deferred** until RAG start confirmation. (User “C-04 only size” applies to assignee, recorded as **C-05**.) |
| **C-05** | Assignee validation | **`@Size(max = 320)` only** — no `@Email` / RFC email requirement. Nullable free string. |

Specs/rules that still said `URGENT` as a first-class enum or `@Email` on assignee are aligned to this table.

---

## 4. Target layout (`backend/`)

All production and test code lives here.

```text
backend/
  pom.xml
  .mvn/wrapper/
  mvnw
  mvnw.cmd
  docker-compose.yml          # local PostgreSQL (+ pgvector image); not the test DB
  .env.example                # variable NAMES only
  README.md                   # how to run backend only (optional, short)
  src/main/java/com/ticketmanagement/
    Application.java
    api/
      common/                 # envelopes, page meta, error body records
      ticket/                 # TicketController, request/response records
      advice/                 # RestExceptionHandler
    domain/
      TicketStatus.java
      TicketPriority.java
      TicketCategory.java
      TicketStatusMachine.java
      IllegalTicketTransitionException.java
      TicketNotFoundException.java
    service/
      TicketService.java
      TicketMapper.java       # explicit DTO ↔ entity (no MapStruct)
    persistence/
      TicketEntity.java
      CommentEntity.java
      TicketRepository.java
      CommentRepository.java
    config/
      WebCorsConfig.java
      JacksonConfig.java      # if needed for unknown properties
      PersistenceConfig.java  # optional
    rag/                      # EMPTY until Phase D — do not populate early
  src/main/resources/
    application.yml
    application-local.yml     # optional
    db/changelog/
      db.changelog-master.yaml
      001-ticket-tables.yaml
      002-extensions.yaml     # pg_trgm in Phase 0; vector in Phase D
      003-ticket-indexes.yaml
      004-vector-chunk-table.yaml   # Phase D only
      005-vector-indexes.yaml       # Phase D only
  src/test/java/com/ticketmanagement/
    domain/
    service/
    api/
    persistence/
    support/                  # Testcontainers base, fixtures
```

**Layering (must hold):**

```text
Controller (api) → TicketService → Domain (status rules) / Repositories
```

| Layer | May | Must not |
|-------|-----|----------|
| `api` | HTTP, `@Valid`, status codes, envelopes | Transition tables, JPQL, entities in JSON |
| `domain` | Enums, SM, domain exceptions | Spring Web, JPA, `@Autowired` |
| `service` | `@Transactional` use cases, mapping | `HttpServletRequest`, leaking entities |
| `persistence` | Load/save, search/filter queries | Ad-hoc `UPDATE status`, SM bypass |
| `config` | Beans, properties | Business rules |
| `rag` | Phase D+ only | Ticket create/notify from ask |

---

## 5. Cross-cutting conventions (all phases)

### 5.1 Stack (**Convention** unless marked PDF)

- Java **21** (**PDF**), Jakarta (`jakarta.*`).
- Spring Boot **3**, Spring Web MVC, Spring Data JPA.
- Maven Wrapper only: `./mvnw` from `backend/`.
- PostgreSQL (**DEC-10**); **no H2**.
- Liquibase owns schema; `spring.jpa.hibernate.ddl-auto: validate` (or `none`).
- `spring.jpa.open-in-view: false`.
- Constructor injection; `record` DTOs; no Lombok.
- Time: `Instant` / `TIMESTAMPTZ`.
- Logging: SLF4J; ticket id when known; **never** secrets, SQL dumps, or stack traces in API bodies.

### 5.2 HTTP (**Convention**; capabilities **PDF**)

| Item | Rule |
|------|------|
| Ticket base | `/api/v1/tickets` |
| Methods | `POST` create/comment; `GET` list/detail; `PATCH` update; **no PUT/DELETE** |
| Success single | `{ "data": { … } }` |
| Success list | `{ "data": [ ], "meta": { page, size, totalElements, totalPages, sort } }` |
| Error | `{ "error": { status, code, message, details?, timestamp, path } }` |
| Create | **201** + `Location: /api/v1/tickets/{id}` |
| Empty list | **200** + `data: []` — never 404 |
| JSON | camelCase; enums uppercase |
| List query | `page` (default 0), `size` (default 20, max 100), `sort` whitelist, `q`, `status` |

**Error codes:** `BAD_REQUEST` 400, `VALIDATION_ERROR` 400, `NOT_FOUND` 404, `ILLEGAL_TRANSITION` 409, `INTERNAL_ERROR` 500 (generic message, no internals).

### 5.3 Ticket identity and fields (agreed)

- Public id **`TKT-{n}`** from sequence `ticket_number_seq` **START 1001** (**DEC-04**). PK `VARCHAR(16)`.
- Create: **`title` required**; `description`, `assignee`, `category` optional; `priority` default **`MEDIUM`**; `description` default `''`; **`status` not on create** — server **`OPEN`** (**DEC-07**, **DEC-13**).
- **Priority (C-02):** enum `LOW` \| `MEDIUM` \| `HIGH` \| `CRITICAL`. Request body `"URGENT"` → store/return `CRITICAL`.
- **Assignee (C-05):** optional string, max 320 characters, **not** `@Email`.
- Tables: `ticket`, `ticket_comment` (Phase 0). Vector table **Phase D**.
- Keyword `q`: **title + description only**, case-insensitive contains (**DEC-08**). Comments not searched.
- Comments: UUID PK, `body` required, ordered `created_at` ASC; adding a comment bumps `ticket.updated_at`.
- Comments and field PATCH allowed on **any** status including `CLOSED` / `CANCELLED` ([`spec/api-contract.md`](spec/api-contract.md) §4.4). Only **status** is SM-gated.
- Assignee: nullable string, not a user FK.

### 5.4 Testing (every implemented layer)

- Unit: domain SM (real types, **no Mockito on the machine**); services with Mockito repos.
- Slice: MockMvc + mocked `TicketService`.
- Integration: **Testcontainers PostgreSQL** + same Liquibase; **not** Compose Postgres; **not** a developer’s local DB.
- Default suite: **no** live Ollama (RAG later uses doubles).
- Run: `cd backend && ./mvnw test`.
- After CRUD slices: follow [`commands/generate-tests.md`](commands/generate-tests.md) P0 order; review with [`commands/review-code.md`](commands/review-code.md).

### 5.5 RAG hook during Phase A–C

- **Do not** call embeddings or write vector rows.
- **Allowed:** a no-op `TicketIngestionPort` interface in `service` or `rag` with a `NoOpTicketIngestionPort` bean that logs at debug and returns immediately — **only** if it keeps later wiring small. Prefer **no `rag/` types** until Phase D.
- Ticket HTTP **must succeed** without Ollama.

---

## 6. Phase A — Application setup and configuration

**Status:** complete  

**Depends on:** C-01, C-03  
**Implements:** IR stack conventions; architecture §7, §18

### 6.1 Why this phase exists

Without a runnable Boot app, Maven Wrapper, and a Postgres-backed schema, later layers cannot be proven. This phase produces an **empty HTTP surface** (health optional) and **migrations that create ticket tables**, not business APIs yet (controllers come in Phase B4).

### 6.2 Work — ordered bullets

- **A.1 Maven / Boot**
  - Generate Spring Boot **3** (latest **GA / production** release at implementation — **C-03**) + Java **21** project **into `backend/`**.
  - Dependencies (CRUD phase): `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `liquibase-core`, PostgreSQL driver, `spring-boot-starter-test`, Testcontainers (`junit-jupiter`, `postgresql`).
  - **Do not** add Spring AI, Spring Security, Actuator-as-product, or OpenAPI starters unless you later confirm.
  - Check in **Maven Wrapper** (`mvnw`, `.mvn/wrapper`).
- **A.2 Main class**
  - `com.ticketmanagement.Application` with `@SpringBootApplication` only — no business logic.
- **A.3 `application.yml`**
  - Datasource via env: `SPRING_DATASOURCE_URL`, `USERNAME`, `PASSWORD` (names in `.env.example`; **no real secrets**).
  - `spring.jpa.open-in-view: false`
  - `spring.jpa.hibernate.ddl-auto: validate`
  - `spring.jackson.default-property-inclusion` as needed; timestamps ISO-8601 UTC.
  - Server port **8080** (matches api-contract examples) unless you confirm otherwise.
  - List defaults: document `app.api.page-size-default: 20`, `page-size-max: 100` as `@ConfigurationProperties` (not magic numbers in controllers).
- **A.4 CORS (`config/WebCorsConfig`)**
  - Map `/api/**` to `http://localhost:5173`; methods `GET, POST, PATCH, OPTIONS`; **not** `*` origin as a permanent default.
- **A.5 Docker Compose (local only)**
  - PostgreSQL image suitable for later pgvector (e.g. `pgvector/pgvector:pg16`) so Phase D does not change the product DB.
  - Expose 5432; credentials **only** in Compose / env, not hardcoded in Java.
  - Compose is **not** used by `./mvnw test`.
- **A.6 Liquibase Phase A (relational only)**
  - Master changelog includes **001**, **002 `pg_trgm` only** (defer `vector` extension to Phase D if you want a clean CRUD schema — **or** install `vector` early with unused extension; prefer **defer vector table**).
  - **001-ticket-tables.yaml**
    - Sequence `ticket_number_seq START WITH 1001 INCREMENT BY 1`.
    - Table `ticket` columns per [`spec/data-model.md`](spec/data-model.md) §14.2.
    - Table `ticket_comment` per §14.3; FK `ON DELETE CASCADE`.
  - **003-ticket-indexes.yaml** — **exact names** from §14.5:
    - `idx_ticket_status`, `idx_ticket_created_at`, `idx_ticket_updated_at`, `idx_ticket_priority`, `idx_ticket_status_created_at`
    - `idx_ticket_title_trgm`, `idx_ticket_description_trgm` (`gin_trgm_ops`)
    - `idx_ticket_comment_ticket_created` `(ticket_id, created_at ASC)`
  - Do **not** create `ticket_vector_chunk` yet.
- **A.7 Test bootstrap**
  - Abstract `@Testcontainers` + `@DynamicPropertySource` pointing at a Postgres container.
  - One smoke test: context loads + Liquibase applied (query `ticket` table exists).
- **A.8 `.gitignore`**
  - `backend/target/` already covered by repo gitignore; do not commit `.env`.

### 6.3 Tests this phase

- **Positive:** application context starts with Testcontainers; changelogs apply; sequence exists.
- **Negative:** none required beyond failed context if URL wrong (manual).
- **AC:** **AC-DM-08** partial (relational indexes exist). Full vector indexes = Phase D.

### 6.4 Done criteria — Phase A

- [x] `cd backend && ./mvnw test` green (smoke).
- [x] No secrets in git.
- [x] Tables `ticket`, `ticket_comment` + agreed relational indexes present on Testcontainers DB.
- [x] `rag/` package empty.

---

## 7. Phase B — Ticket CRUD (no status transitions yet)

**Status:** complete (B1–B5)  
**Depends on:** Phase A complete; **C-01…C-03, C-05** agreed (this document §3)  
**Implements:** FEAT-01…10 (except SM), AC-CORE-01…11 (API), AC-API create/list/get/patch-fields/comments, AC-DM-01/02/05

Implement **B1 → B5 in order**. Do **not** PATCH `status` until Phase C (you may accept `status` on the DTO but **must not** persist it until the machine exists — cleaner: **omit status handling** until Phase C).

**Recommendation:** `UpdateTicketRequest.status` is added in **Phase C**, not B.

### 7.1 Phase B1 — Domain enums, constants, utilities (no SM table yet)

**Why:** Closed sets must be Java enums, not `String`, in domain and persistence ([`rules/java-springboot.md`](rules/java-springboot.md)).

- **Create**
  - `TicketStatus`: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`.
  - `TicketPriority`: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` only (**C-02**). **Do not** add `URGENT` to the enum.
  - `api` JSON: custom deserializer maps `URGENT` → `CRITICAL`; serializer always writes `CRITICAL`. Test: create/PATCH with `"priority":"URGENT"` returns `"CRITICAL"` and persists `CRITICAL`. Invalid values (e.g. `P1`) still 400.
  - `TicketCategory`: `PAYMENTS`, `SHIPMENT`, `BILLING`, `LOGIN`, `OTHER`.
  - Constants class or dedicated types: `TicketId` formatter `TKT-%d`; max lengths 500 / 100_000 / 320 / 50_000 / 100_000 from data-model §16.
  - `SortWhitelist`: `createdAt`, `updatedAt`, `priority`, `status`.
- **Do not** yet implement `TicketStatusMachine`.
- **Tests:** enum round-trip `valueOf` / invalid name throws; sort whitelist rejects unknown property (can live with a small `SortParser` unit test).

**Done B1**

- [x] Enums match **C-02** (`CRITICAL` only in domain).
- [x] No Spring imports in `domain` enums.

### 7.2 Phase B2 — Persistence entities and repositories

**Why:** PostgreSQL is the system of record. Entities map **1:1** to Liquibase; Hibernate does not invent columns.

- **`TicketEntity`**
  - `@Table(name = "ticket")`; explicit `@Column` names (`resolution_notes`, `created_at`, …).
  - `@Enumerated(STRING)` for status/priority/category.
  - `@OneToMany` comments **LAZY**, `mappedBy`, cascade + orphanRemoval per data-model §7.4.
  - **No** setter that is the state machine. Status field is a column; legality is domain (Phase C).
- **`CommentEntity`**
  - Table `ticket_comment`; UUID id; `@ManyToOne` ticket; `body`; `created_at`.
- **ID generation**
  - On persist, allocate `nextval('ticket_number_seq')` (native query or `@Query`) and format `TKT-{n}`. Do not use UUID for ticket PK.
- **`TicketRepository`**
  - `Optional<TicketEntity> findById(String id)`
  - Search/filter: parameterized `@Query` — `status = :status` AND (`title ILIKE :q OR description ILIKE :q`); **never** concatenate user input into JPQL.
  - `Page<TicketEntity>` with `Pageable`.
  - Priority sort: map enum order in query/`ORDER BY CASE` so `LOW < MEDIUM < HIGH < CRITICAL` ([`spec/data-model.md`](spec/data-model.md) §5.2).
- **`CommentRepository`**
  - Find by ticket ordered by `createdAt` ASC — or fetch join on detail query.
- **Must not:** `@Modifying` status updates.

**Tests (integration, Testcontainers)**

- **Positive:** insert ticket; find by `TKT-1001` (first id); comments ordered; `q` hits title; `q` hits description; `status` filter; `q` AND `status`; page `meta` counts.
- **Negative / empty:** unknown id → empty Optional; `q` only in comment body → **not** returned (**DEC-08**); no rows → empty page not error.
- **AC:** **AC-DM-01**, **AC-DM-02**, **AC-DM-05**.

**Done B2**

- [x] Entities match changelog.
- [x] Repository tests green without HTTP.

### 7.3 Phase B3 — DTOs, mapping, validation, application service (CRUD only)

**Why:** HTTP must not see JPA graphs. Bean Validation matches data-model §16. Services own transactions.

**Request records (`api`)**

- `CreateTicketRequest`: `title` `@NotBlank` `@Size(max=500)`; `description` `@Size(max=100000)` optional; `priority` optional; `assignee` optional `@Size(max=320)` **only** (**C-05**, no `@Email`); `category` optional. **No `status`.**
- `UpdateTicketRequest` (Phase B): optional `title`, `description`, `priority`, `assignee`, `category`, `resolutionNotes` — **no `status` yet**.
- `CreateCommentRequest`: `body` `@NotBlank` `@Size(max=50000)`.
- Jackson: unknown properties on ticket DTOs — follow api-contract (ask later is fail-unknown **DEC-17**). For tickets, reject unknown if the contract says so; if unspecified, prefer fail-on-unknown for consistency — **confirm if api-contract is silent**; default **fail on unknown** for write DTOs to avoid silent drops.

**Response records**

- `TicketSummaryResponse` — list fields per data-model §10.2 (no comments, no description required on list).
- `TicketDetailResponse` — summary + `description`, `resolutionNotes`, `comments`.
- `CommentResponse` — `id`, `body`, `createdAt`.
- Envelope records: `DataResponse<T>`, `PageResponse<T>` with `Meta`.

**`TicketService` use cases (write methods `@Transactional`; reads `readOnly = true`)**

- `create` — defaults `OPEN`, `MEDIUM`, empty description; allocate id; persist; return detail.
- `getById` — or throw `TicketNotFoundException`.
- `list(page, size, sort, q, status)` — validate size 1–100; parse sort whitelist; AND filters.
- `updateFields` — merge **non-null** PATCH fields; bump `updatedAt`; empty body `{}` → validation error **400** (api-contract §4.4) — treat “no updatable field present” as `VALIDATION_ERROR`.
- `addComment` — 404 if ticket missing; persist comment; bump parent `updatedAt`.
- **Do not** change `status` in this phase.

**Mapper:** explicit methods in `TicketMapper` (or package-private mapper). No Jackson on entities.

**Tests (service unit, Mockito repositories)**

- **Positive:** create defaults; patch one field; add comment; list delegates with filters.
- **Negative:** not found; blank title never `save`; empty PATCH does not `save`.

**Done B3**

- [x] Service unit tests green.
- [x] Public APIs do not return `null` (use Optional / exceptions / empty lists).

### 7.4 Phase B4 — Controllers, query validation, error handling

**Why:** Assessment requires REST + meaningful errors. Envelopes are **Convention** but locked in rules + api-contract.

- **`TicketController`** `@RequestMapping("/api/v1/tickets")`
  - `POST /` → 201 + `Location` + `{ data: TicketDetail }`
  - `GET /` → 200 `{ data, meta }`
  - `GET /{id}` → 200 `{ data }`
  - `PATCH /{id}` → 200 `{ data }`
  - `POST /{id}/comments` → 201 + `Location` to comment URL per api-contract §5.1
- Thin: `@Valid` body → one service call → wrap envelope.
- **No** `@Transactional` on controller.
- Query params: `@Min`/`@Max` on `size` or a dedicated `TicketListQuery` `@Validated` object.
- Invalid `status` / `priority` enum query or body → 400 `VALIDATION_ERROR` **before** domain.

**`RestExceptionHandler` `@RestControllerAdvice`**

| Exception | HTTP | `error.code` |
|-----------|------|----------------|
| `MethodArgumentNotValidException` / `ConstraintViolationException` | 400 | `VALIDATION_ERROR` (field `details`) |
| `HttpMessageNotReadableException` (malformed JSON / bad enum) | 400 | `BAD_REQUEST` or `VALIDATION_ERROR` per api-contract examples |
| `TicketNotFoundException` | 404 | `NOT_FOUND` |
| `IllegalTicketTransitionException` | 409 | `ILLEGAL_TRANSITION` (wire now; unused until Phase C) |
| any other | 500 | `INTERNAL_ERROR` — message `An unexpected error occurred.` |

Include `timestamp` UTC, `path`, `status` matching HTTP. **No** stack traces, SQL, Hibernate state.

**Slice tests (MockMvc, mock service)**

- Create 201 + Location + envelope.
- Get 200; get unknown 404 envelope.
- List 200 meta; `size=0` and `size=101` → 400; unknown `sort` → 400; invalid `status` → 400.
- Malformed JSON → 400.
- Create/PATCH `"priority":"URGENT"` → 201/200 with `"priority":"CRITICAL"` persisted (**C-02**).
- Assignee longer than 320 → 400; non-email string within 320 → accepted (**C-05**).
- Empty list still 200.

**API integration tests (HTTP + Testcontainers)** — **required**, not optional:

- Create persists; GET returns same id/status `OPEN`.
- Validation 400 writes **no row**.
- List pagination; `q`; `status`; AND; empty search 200 `[]`.
- PATCH title/description/priority/assignee/category/resolutionNotes.
- Comment 201; detail includes comment in order.
- Unknown id 404.

**Done B4**

- [x] Slice + integration tests green for endpoints 1–5 **except status PATCH**.
- [x] [`commands/review-code.md`](commands/review-code.md) on the diff — fix Failures before Phase C.

### 7.5 Phase B5 — Persistence across “restart”

**Why:** **PDF** AC-CORE-09 / **AC-DM-07**.

- Integration test: create ticket (+ comment) against Testcontainers; start a **fresh** Spring context (nested class / second `@SpringBootTest`) **same container**; `GET` still returns the ticket.
- Do not treat “I restarted the IDE” as the only proof.

**Done B5 / Phase B**

- [x] Restart test green.
- [x] CRUD capabilities FEAT-01…10 (minus SM) proven at API integration.

---

## 8. Phase C — State machine and transitions

**Status:** not started  
**Depends on:** Phase B complete  
**Implements:** FEAT-11, AC-CORE-12…14, AC-SM-01…08, AC-API-04/08/09, [`spec/state-machine.md`](spec/state-machine.md)

**Do not start RAG in this phase.** Illegal transition tests must be green before G7.

### 8.1 Why this phase exists

The assessment grades a **backend-enforced** machine. UI hiding buttons is not enough. Persistence must not save an illegal `status`.

### 8.2 Domain machine (`TicketStatusMachine`)

- **No Spring, no JPA.**
- Legal edges **only** (**DEC-02 A**):

| ID | From | To |
|----|------|-----|
| T1 | `OPEN` | `IN_PROGRESS` |
| T2 | `IN_PROGRESS` | `RESOLVED` |
| T3 | `RESOLVED` | `CLOSED` |
| T4 | `OPEN` | `CANCELLED` |
| T5 | `IN_PROGRESS` | `CANCELLED` |

- **All other 20 directed pairs are illegal**, including self-transitions and skipped hops (`OPEN`→`RESOLVED`, `IN_PROGRESS`→`CLOSED`, `RESOLVED`→`CANCELLED`, all outbound from `CLOSED`/`CANCELLED`).
- PDF examples X1–X3: `CLOSED`/`RESOLVED`/`CANCELLED` → `OPEN`.
- API: `assertTransitionAllowed(current, target)` throws `IllegalTicketTransitionException` with message pattern **`Cannot transition from {FROM} to {TO}`**.
- Create does **not** call the machine; server sets `OPEN`.

**Domain unit tests (parameterized)**

- **AC-SM-01:** T1–T5 succeed (boolean or no throw).
- **AC-SM-02:** X1–X3 throw.
- **AC-SM-06:** all **20** illegal pairs throw.
- **AC-SM-07:** self-transition throw.
- Machine has **no** repository mock.

### 8.3 Service orchestration

- Add `status` to `UpdateTicketRequest` (optional).
- PATCH with `status`:
  - Unknown enum → 400 (controller/Jackson) — machine not invoked.
  - Ticket missing → 404 — machine not invoked.
  - `status` present → **machine first**; on throw **do not `save`** and **do not apply other fields** (whole operation fails — state-machine.md §6.4).
  - Legal → persist new status, bump `updatedAt`.
- PATCH **without** `status` but with other fields: machine **not** invoked (**AC-SM-08**).
- PATCH `status` equal to current → **409** (self-transition).
- PATCH `{}` → 400 (already Phase B).
- Same PATCH may include `resolutionNotes` with T2 (`IN_PROGRESS`→`RESOLVED`) — notes optional (**DEC-05**).

**Service unit tests**

- Legal T1 calls `save` with new status.
- Illegal: verify **never** `save` with illegal status (Mockito).
- Field-only PATCH does not call machine (spy or inspect).

### 8.4 HTTP + integration

- **200** T1–T5; `data.status` equals target (**AC-API-09**).
- **409** envelope `ILLEGAL_TRANSITION`; `error.message` names from/to; **row unchanged** (**AC-API-04**, **AC-SM** integration).
- Parameterize HTTP tests for 20 illegal pairs **or** cover X1–X3 + a sample of skipped hops at HTTP and the full 20 at domain (test-strategy §5: **full 20 at domain; illegal at API with 409 + unchanged row** — include X1–X3 and representative skipped hops at API; prefer full 20 at API integration if cheap).

**Do not** add `POST /api/v1/tickets/{id}/transition`.

### 8.5 Review and correction

- Run `./mvnw test`.
- Run [`commands/review-code.md`](commands/review-code.md) focusing on SM placement (domain vs controller).
- Fix until legal/illegal behaviour matches the matrix — **then mark Phase C complete below**.

### 8.6 Phase C done criteria (G6 / G7)

- [ ] Domain parameterized tests: T1–T5 + 20 illegal pairs + X1–X3 + self-transition.
- [ ] Service: illegal does not persist; legal persists.
- [ ] API: 200 T1–T5; 409 + unchanged row for illegal (including PDF reopens).
- [ ] PATCH without `status` still updates fields.
- [ ] `./mvnw test` green.
- [ ] Code review Failures for SM fixed.
- [ ] **Phase C status set to complete** (this document).

**Phase C status:** not started — **change to `complete` only after the checklist above is true in the repo.**

---

## 9. Holding line — do not start RAG yet

After Phase C is **complete**:

1. Summarize evidence (test class names, `./mvnw test` result) in chat.
2. **Wait for explicit user confirmation** to start RAG (e.g. “start RAG” / “proceed with Phase D”).
3. Until that message: no Spring AI dependencies, no Ollama calls, no ask controllers, no chunker, no vector DDL applied in the running app (files may be sketched in this plan only).

**RAG start authorization:** not granted (user confirmation pending).

---

## 10. Phase D — RAG ingestion pipeline, embeddings, vector store

**Status:** blocked (after Phase C complete **and** user confirmation)  
**Implements:** FEAT-12…14, AC-CORE-15/19/20, AC-RAG-ING-*, DEC-01, DEC-09, DEC-16, DEC-18  
**Read first:** [`spec/rag-ingestion.md`](spec/rag-ingestion.md), [`spec/architecture.md`](spec/architecture.md) §14–16, [`rules/rag-vector-store.md`](rules/rag-vector-store.md)

### 10.1 Why (for later)

Ask cannot be honest without a derived index of **description, comments, resolution notes** plus metadata. Vectors are **rebuildable**; tickets remain SoR.

### 10.2 Liquibase (when unblocked)

- **002:** add `CREATE EXTENSION IF NOT EXISTS vector` (if deferred).
- **004-vector-chunk-table.yaml** — `ticket_vector_chunk` per data-model §14.4: `vector(768)`, JSONB `metadata`, unique `(ticket_id, chunk_index)`, FK cascade.
- **005-vector-indexes.yaml** — `idx_ticket_vector_chunk_embedding_hnsw` `USING hnsw (embedding vector_cosine_ops)`.
- Hibernate still `validate`. Prefer Liquibase over Spring AI auto-DDL as source of truth; if Spring AI table names differ, **changelog must match the adapter** (data-model §8.3) — confirm at implementation if names drift.

### 10.3 Configuration (no magic numbers in Java)

`@ConfigurationProperties(prefix = "rag")` — **DEC-16** defaults:

```yaml
rag:
  chunking:
    strategy: HYBRID_PARAGRAPH_THEN_FIXED
    max-chars: 800
    min-chars: 120
    overlap-chars: 80
  retrieval:
    top-k: 8
    similarity-threshold: 0.72
    distance-metric: COSINE
  embedding:
    provider: ollama
    model: nomic-embed-text
    dimensions: 768
spring.ai.ollama.embedding.options.model: nomic-embed-text
```

Ollama **base URL** from env only. Chat model id = **C-04**.

### 10.4 `rag/` types (when unblocked)

| Type | Role |
|------|------|
| `KnowledgeDocument` / builder | Assemble text per data-model §9.1 template (title header **Convention**; PDF sources = description, comments, resolution). |
| `RagChunkMetadata` | PDF keys `ticketId`, `status`, `priority`, `assignee`, `category` + technical `chunkIndex`, `ingestedAt`. |
| `TicketChunker` | Hybrid: section + comment-atomic blocks; overflow fixed-size + overlap; min-chars merge **within section only**, never across comments. |
| `EmbeddingPort` | Spring AI embedding; **same model** as ask query. |
| `VectorChunkStore` | Delete-all-for-ticket then insert (**DEC** re-ingest). |
| `TicketIngestionService` | Orchestrate load → assemble → chunk → embed → write. |

**Triggers after successful DB commit** (**DEC-18** sync): create; field update; comment add; **any status change including close** (**DEC-01 B**, **DEC-18** status-only re-ingest).

**Empty content:** no embed call, no chunk rows (after delete on re-ingest).

**Failure:** ticket HTTP already committed; ingest failure **must not** roll back the ticket; **structured error log**; recovery on next mutation / optional rebuild. Do **not** return 500 on ticket PATCH solely because Ollama is down if spec says mutation succeeds — follow rag-ingestion §10.2 exactly when implementing.

**Do not ingest:** transition history as narrative (status lives in **metadata**).

### 10.5 Tests (deterministic; mock embed)

- Builder includes description, each comment, resolution; excludes non-sources.
- Chunker: comment boundaries preserved; overflow splits; empty skip.
- Re-ingest replaces rows (integration with stub embeddings or fake vectors if schema requires `vector(768)` — use doubles; **no live Ollama** in default suite).
- Service: ingest port called after successful update/close; **not** called when write fails (if port exists).

**AC-RAG-ING-*** and **AC-DM-03/04**.

### 10.6 Done criteria — Phase D

- [ ] Vector schema + HNSW present (AC-DM-08 remainder).
- [ ] Ingest tests green with embedding doubles.
- [ ] Review-code RAG checklist Pass for ingest (ask still later).

---

## 11. Phase E — Embedding job / ingest wiring from ticket service

**Status:** blocked (after Phase D)  
**Note:** Spec is **synchronous after commit**, not a separate batch worker. **Do not** add Kafka/queues (**Reference**).

### 11.1 Work

- Wire `TicketService` (and comment path) to `TicketIngestionService` **after commit** (`TransactionSynchronization` `afterCommit` or equivalent so ingest failure cannot roll back the ticket txn).
- Status-only PATCH still re-ingests so metadata `status` (e.g. `CLOSED`) refreshes.
- Optional: maintenance method **rebuild all** for operators — **not** a public API unless you confirm (architecture §14.3 optional rebuild; **no** vector admin REST in v1).

### 11.2 Tests

- Integration: update description → stub store received new content for that `ticketId`.
- Close → metadata `CLOSED`.
- Failed embed → ticket row still updated; log/error visibility per spec.

### 11.3 Done criteria — Phase E

- [ ] Hooks on create/update/comment/status including close.
- [ ] Tests prove hook **and** no ingest on failed ticket write.

---

## 12. Phase F — AI ask API (retrieve-then-generate)

**Status:** blocked (after Phase E)  
**Implements:** FEAT-15…18, AC-CORE-16…18, AC-RAG-API-01…07, DEC-11, DEC-17, DEC-19  
**Read:** [`spec/rag-api-contract.md`](spec/rag-api-contract.md)

### 12.1 HTTP

- **`POST /api/ai/ask`** (**PDF**, required) **and** `POST /api/v1/ai/ask` — **same handler**.
- Body: **only** `question`; `@NotBlank`; max **2000**; **unknown JSON properties → 400** (**DEC-17**).
- Success **200** `{ "data": { "answer": "…", "citedTicketIds": ["TKT-…"] } }`.
- No-match: still **200**; `answer` honest phrase e.g. **“No relevant tickets found.”**; `citedTicketIds: []` (**DEC-11**). **Not** the error envelope.
- Blank question → 400 `VALIDATION_ERROR`.
- **Do not** expose prompts, chunks, model names, top-K, URLs, vectors.

### 12.2 Ask orchestration (`AskService` in `rag/`)

1. Embed question (**same** `nomic-embed-text`).
2. Similarity search top-K from config; drop below similarity threshold (**cosine**).
3. **DEC-19:** **no** metadata pre-filter on ask.
4. If no remaining hits → **no LLM call**; return no-match.
5. Else: generate **only** from retrieved excerpts + ticket ids; citations = ids from **retrieval**, deduped, **relevance order** (**DEC-17**). Strip model-guessed ids.
6. **Non-agentic:** no ticket writes, no tools, no notify.

Thin `AiAskController` in `api`.

### 12.3 Tests — Band A only in `./mvnw test` ([`spec/test-strategy.md`](spec/test-strategy.md) §6.2)

- Both paths 200 grounded shape with **stubbed** retrieval/citations.
- Empty retrieval → 200 no-match; generate port **not** called.
- 400 blank / missing / too long / unknown property.
- Citations subset of retrieval ids.

**Do not** assert a golden LLM paragraph as quality.

### 12.4 After code is correct (manual / eval — not a reason to skip Band A)

- [`commands/review-rag-output.md`](commands/review-rag-output.md) — grounding.
- [`spec/evaluation-strategy.md`](spec/evaluation-strategy.md) §5–§8 — PDF Q1–Q5 retrieval quality (**F-01…F-10**). Needs live models + corpus — **separate from default Maven suite**.

### 12.5 Done criteria — Phase F

- [ ] Dual ask paths; envelopes; no-match 200; 400 validation.
- [ ] Band A tests green without live LLM.
- [ ] Review-code RAG ask checklist Pass.
- [ ] Optional: one documented grounding review; AI mistake log if a hallucination is caught ([`docs/ai-error.md`](docs/ai-error.md) via **`/update-ai-error`**).

---

## 13. Test catalogue (CRUD + SM now; RAG later)

Follow [`commands/generate-tests.md`](commands/generate-tests.md). Map names to AC ids.

### 13.1 Phase B–C (implement with those phases)

| Flow | Positive | Negative / empty | Layers |
|------|----------|------------------|--------|
| Create | 201, `TKT-{n}`, `OPEN`, Location | blank title 400; no row; `status` on create 400 | slice + API-int + service + repo |
| List | default page meta | size 0/101; bad sort; bad status | slice + API-int + repo |
| Search `q` | title or description hit | no hit 200 []; comment-only miss | API-int + repo |
| Filter status | match | invalid enum 400; AND with `q` | API-int |
| Get | 200 detail + comments | 404 | slice + API-int |
| PATCH fields | partial update | 400; 404; `{}` 400 | slice + API-int |
| Comment | 201; appears on GET | blank body 400; 404 ticket | API-int |
| Restart | GET after new context | — | API-int |
| T1–T5 | 200 persisted | — | domain + service + API-int |
| Illegal 20 / X1–X3 / self | 409, row unchanged | — | domain + service + API-int |
| PATCH no status | fields change | — | service + API-int |

### 13.2 Phase D–F (after confirmation)

| Flow | Positive | Negative |
|------|----------|----------|
| Ingest | knowledge + hybrid chunks; replace on re-ingest | empty skip embed; failed write no ingest |
| Ask | 200 answer + cited ids from retrieval | 400 question; 200 no-match; no generate on empty |

---

## 14. Review, fix, and documentation hygiene

After each phase:

1. `cd backend && ./mvnw test`
2. [`commands/review-code.md`](commands/review-code.md) — Pass/Fail; **fix Failures** before the next phase.
3. Do not run [`commands/review-frontend.md`](commands/review-frontend.md) for this plan.
4. Do not treat [`commands/review-spec.md`](commands/review-spec.md) as a substitute for code review (specs already agreed).
5. If AI generates illegal transitions or entity JSON: correct code; log [`docs/ai-error.md`](docs/ai-error.md) with **`/update-ai-error`**.
6. `graphify update .` after Java file changes.

---

## 15. Explicit non-implement list (checklist)

- [ ] No second Spring Boot module outside `backend/`
- [ ] No H2
- [ ] No Spring Security
- [ ] No PUT/DELETE ticket
- [ ] No `/transition` URL
- [ ] No auth headers
- [ ] No ask until G8
- [ ] No hardcoded Ollama URL / top-K / threshold / chunk size in Java
- [ ] No agents / tools from ask
- [ ] No confidence field
- [ ] No frontend work in this plan

---

## 16. Suggested implementation order (one-line index)

1. **C-01, C-02, C-03, C-05 agreed** (2026-10-04). **C-04** (chat model) remains for RAG.
2. Phase **A** — Maven, yml, Compose, Liquibase tickets, Testcontainers smoke.
3. Phase **B1** — enums, constants, sort whitelist.
4. Phase **B2** — entities, repositories, search SQL, repo tests.
5. Phase **B3** — DTOs, mapper, `TicketService` CRUD, service tests.
6. Phase **B4** — controllers, advice, slice + API integration.
7. Phase **B5** — restart persistence test.
8. Phase **C** — `TicketStatusMachine`, PATCH `status`, full SM tests, review, **mark complete**.
9. **Stop.** User confirmation.
10. Phase **D** — vector schema, chunker, embed port, ingest service.
11. Phase **E** — after-commit hooks from ticket service.
12. Phase **F** — ask API both paths, Band A tests, grounding review.

---

## 17. Sign-off table (update in this file as work finishes)

| Phase | Status | Tests | Review-code | Notes |
|-------|--------|-------|-------------|-------|
| A Setup | complete | smoke green | | Boot 3.5.16; Liquibase 001–003; pg_trgm only |
| B1 Enums | complete | unit green | | C-02: `CRITICAL`; JSON `URGENT`→`CRITICAL` |
| B2 Persistence | complete | IT green | | entities + search/filter; no HTTP |
| B3 Service CRUD | complete | unit green | | no status PATCH |
| B4 HTTP CRUD | complete | slice+IT green | self-review | no status PATCH |
| B5 Restart | complete | IT green | | DirtiesContext + same Testcontainers DB |
| **C State machine** | **not started** | | | **RAG blocked until `complete` + chat confirm** |
| D Ingest / vectors | blocked | | | |
| E Ingest hooks | blocked | | | |
| F Ask API | blocked | | | |

---

## 18. Revision history

| Date | Note |
|------|------|
| 2026-10-04 | Initial backend-only plan: CRUD + SM first; RAG gated on Phase C complete and user confirmation; code under `backend/`. |
| 2026-10-04 | Confirmed **C-01** `com.ticketmanagement`; **C-02** `CRITICAL` + inbound `URGENT` mapped; **C-03** latest Boot 3 GA; **C-05** assignee `@Size(max=320)` only. **C-04** chat model still deferred. |
| 2026-10-04 | Phase **A** implemented: `backend/` Spring Boot **3.5.16**, Maven Wrapper, Compose `pgvector/pgvector:pg16`, Liquibase ticket tables + relational indexes, Testcontainers smoke. |
| 2026-10-04 | Phase **B1** implemented: domain enums/constants/sort parser; API Jackson maps `URGENT`→`CRITICAL`. |
| 2026-10-04 | Phase **B2** implemented: JPA ticket/comment entities, sequence ids, parameterized search, Testcontainers repository tests. |
| 2026-10-04 | Phase **B3–B4** implemented: ticket CRUD service/DTOs, REST `/api/v1/tickets`, envelopes, MockMvc slice + API integration tests (no status PATCH). |
| 2026-10-04 | Phase **B5** implemented: create ticket+comment, fresh Spring context, same Postgres container, GET still returns data. |
| 2026-10-04 | AI error log [`docs/ai-error.md`](docs/ai-error.md); `/update-ai-error`. |
