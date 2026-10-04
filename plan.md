# Implementation plan — backend (`backend/`) + frontend (`frontend/`)

> **Artefact:** Spec-Driven Development **Plan / Tasks** for the ticket-management monorepo.  
> **Not** a Spec Kit feature tree under `specs/[###-feature]/`. Product source of truth remains `spec/`, `rules/`, `commands/`.  
> **Code roots:** `backend/` (Spring Boot) and `frontend/` (Next.js + Vite + JavaScript). **Do not** put Java at repo root or under a second backend app.  
> **Backend status:** Phases **A–F** and stack upgrade **G** complete. Live Ollama/Spring AI optional for local ask demos.  
> **Frontend status:** **UI-J complete** — continue **Part II** (§20) **UI-K** (ask page) after backend on **8080**.  
> **Date:** 2026-10-04 (frontend plan added).

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
  → Part II: frontend/ → Review (commands/review-frontend.md) → demo §8.7
```

**Do not** start from “build the complete application.”

**Part I (§6–§19):** backend only — **complete**; gates **G0–G8** apply.  
**Part II (§20):** frontend under `frontend/` — gates **FG0–FG4**; slices **UI-A…UI-L** map to [`spec/ui-flow.md`](spec/ui-flow.md) units **UI-A…UI-H** and **AC-UI-01…12**.

### 0.2 Hard sequencing gate (mandatory) — backend

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

Deliver a **single Spring Boot 4 monolith** under `backend/` that:

1. Persists support tickets and comments in **PostgreSQL** (survives restart).
2. Exposes **JSON REST** under `/api/v1` for create, list, detail, PATCH fields, comments, keyword search, status filter.
3. Enforces the **backend status state machine** (legal T1–T5; reject all other pairs with **409**).
4. **After confirmation:** ingest ticket knowledge, embed into **PgVector**, and serve grounded **`POST /api/ai/ask`**.

### 1.2 In scope (this plan)

**Part I — `backend/` (complete)**

- Backend Java application only (`backend/`).
- Ticket **CRUD** (create, list, get, PATCH fields — **no DELETE**, **no PUT**).
- Comments (add + view on detail).
- Keyword `q` + status filter + pagination/sort.
- Validation, error envelope, CORS for local Next.js (3000) and Vite (5173) (convention).
- State machine and transitions.
- Tests: JUnit 5, Mockito, MockMvc slice, PostgreSQL Testcontainers + Liquibase.
- RAG **design and later implementation** as Phase D–F (gated).

**Part II — `frontend/` (§20, not started)**

- React + Next.js (App Router) + Vite + JavaScript under `frontend/` per **DEC-15** / **DEC-20**.
- Ticket list/create/detail, search/filter, comments, status UX (guidance only), grounded ask UI.
- Manual verification + [`commands/review-frontend.md`](commands/review-frontend.md); **no** frontend test suite.

### 1.3 Out of scope (do not implement)

From [`spec/requirements.md`](spec/requirements.md) **§2.3 Reference** and rules:

- Frontend implementation (covered in **Part II**, §20 — not in Part I scope).
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
| **Convention** | Project choice (`/api/v1`, PATCH, 409, envelopes, Boot 4, Java 25, Maven Wrapper, Liquibase, Testcontainers). |
| **Agreed DEC** | Hub [`spec/requirements.md`](spec/requirements.md) §10.2 **DEC-01…20**. |

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
| UI screens, routes, **AC-UI-*** | [`spec/ui-flow.md`](spec/ui-flow.md) (**DEC-20** §4, §7–§10, §13) |
| Frontend stack, layout, client | [`rules/frontend.md`](rules/frontend.md) |
| UI diff review | [`commands/review-frontend.md`](commands/review-frontend.md) |
| Assessment PDF (capabilities) | [`docs/Assessments.docx`](docs/Assessments.docx), [`docs/assessment-brief.md`](docs/assessment-brief.md) |
| Demo walkthrough | [`spec/requirements.md`](spec/requirements.md) **§8.7**, [`spec/ui-flow.md`](spec/ui-flow.md) §12 |

**Precedence:** PDF → `spec/requirements.md` → agreed child specs → `rules/` → this plan. This plan must not invent fields, paths, or edges.

---

## 3. Implementation decisions (confirmed 2026-10-04)

| ID | Topic | Decision |
|----|--------|----------|
| **C-01** | Java root package | **`com.ticketmanagement`** — one root, never a second. |
| **C-02** | `TicketPriority` | Canonical stored/returned value is **`CRITICAL`** (fourth enum constant). Incoming JSON (create/PATCH) **`URGENT` is accepted and mapped to `CRITICAL`**. Persist and respond only `CRITICAL`. Other unknown strings still **400**. Domain/JPA enum has **no** `URGENT` constant. Mapping lives in **`dto.serde`** Jackson deserializer, not in `domain`. |
| **C-03** | Spring Boot 4 | Use the **latest production (GA) Spring Boot 4** release at implementation time — not milestone/RC. Pin in `backend/pom.xml`; see [`spec/architecture.md`](spec/architecture.md) §7.3. Do not describe the patch as a PDF requirement. |
| **C-08** | JDK | **Java 25** for compile, test, and run (`java.version` in `backend/pom.xml`). PDF exercise lists Java 21; JDK 25 is a **Convention** that satisfies PDF stack intent. |
| **C-04** | Chat / generation model (RAG) | Config via `OLLAMA_CHAT_MODEL` / `spring.ai.ollama.chat.options.model`. Example default **`llama3.2`** (Convention; not a mandated DEC). Embedding remains **`nomic-embed-text`**. |
| **C-05** | Assignee validation | **`@Size(max = 320)` only** — no `@Email` / RFC email requirement. Nullable free string. |
| **C-06** | Relational persistence | **Spring Data JPA** is the **primary** stack for `ticket` / `ticket_comment`. **Preference order:** (1) **derived** query methods (`findBy…`, `ContainingIgnoreCase`, `OrderBy…`); (2) **`JpaSpecificationExecutor`** + `Specification` for optional filters/sort/fetch (no hand-built JPQL strings or `EntityManager` repository impls); (3) **`@EntityGraph`** for fetch plans; (4) **`@Query`** only when unavoidable — today **`nextval('ticket_number_seq')`** (native). Services use repositories only — not `JdbcTemplate` for ticket I/O. **Exception:** `ticket_vector_chunk` via `rag` `VectorChunkStore` (JDBC/pgvector). |

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
  .env.example                # datasource + Ollama variable NAMES only (no in-repo Compose)
  README.md                   # run against operator-managed Postgres/pgvector + local Ollama
  src/main/java/com/ticketmanagement/
    Application.java
    advice/                   # RestExceptionHandler
    controller/               # TicketController, AiAskController
    dto/
      common/                 # DataResponse, PageResponse, ErrorResponse, …
      request/                # CreateTicketRequest, UpdateTicketRequest, AskRequest, …
      response/               # TicketDetailResponse, AskResponseData, …
      serde/                  # TicketPriorityJsonDeserializer
    domain/                   # enums + TicketStatusMachine (no Spring Web/JPA)
    entity/                   # TicketEntity, CommentEntity
    exception/                # TicketNotFoundException, IllegalTicketTransitionException, …
    repository/               # TicketRepository, CommentRepository, TicketSpecifications
    service/                  # TicketService, TicketMapper, AskService, TicketIngestionService
    util/                     # TicketId, SortParser, TicketConstraints, …
    config/
    rag/                      # ports, chunker, vector store, Ollama adapters (not controllers)
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
    advice/ controller/ dto/ domain/ entity/ exception/
    repository/ service/ util/ rag/ config/
    support/                  # Testcontainers base, fixtures (mirror production package names)
```

**Layering (must hold):**

```text
controller → service → domain (rules) / repository (+ rag ports)
```

| Package | Holds | Must not |
|---------|--------|----------|
| `controller` | REST adapters | Business rules, JPQL, entities in JSON |
| `dto/*` | HTTP records + serde | JPA, transactions |
| `advice` | Exception → error envelope | Business rules |
| `domain` | Enums, state machine | Spring Web, JPA, `@Entity` |
| `exception` | Typed failures | HTTP mapping (that is `advice`) |
| `entity` | JPA mappings | Controllers, DTOs |
| `repository` | Spring Data JPA | Ad-hoc status `UPDATE`; SM bypass |
| `service` | Use cases, mappers | `HttpServletRequest`, raw SQL for tickets |
| `util` | Pure helpers | Spring stereotypes |
| `config` | Beans, properties | Business rules |
| `rag` | Ingest/retrieve adapters | Ticket HTTP, ask controllers |

---

## 5. Cross-cutting conventions (all phases)

### 5.1 Stack (**Convention** unless marked PDF)

- Java **21** (**PDF**), Jakarta (`jakarta.*`).
- Spring Boot **3**, Spring Web MVC.
- **Spring Data JPA** (**C-06**, **Convention**): primary persistence — `JpaRepository` + `JpaSpecificationExecutor`, derived methods first, `Specification` for dynamic list/search/sort/fetch; avoid JPQL/`EntityManager` custom `*Impl` classes; native `@Query` only for sequence `nextval`; Hibernate validates against Liquibase.
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
- Integration: **Testcontainers PostgreSQL** + same Liquibase; **not** the operator’s local Postgres used for `spring-boot:run`.
- Local dev: point `SPRING_DATASOURCE_*` at **your** PostgreSQL (pgvector-capable) — **no** `docker-compose` or other container definitions under `backend/`.
- Default suite: **no** live Ollama (RAG later uses doubles).
- Run: `cd backend && ./mvnw test`.
- After CRUD slices: follow [`commands/generate-tests.md`](commands/generate-tests.md) P0 order; review with [`commands/review-code.md`](commands/review-code.md).
- **Phase B (2026-10-04):** generate-tests **P0–P2 for implemented ticket CRUD** written and `./mvnw test` green. **P0 status machine, ask, and RAG ingest** remain skipped (no production code). Review-code on Phase B: **no blocking Fail**; three **minor** findings **fixed** (list `size` from `ApiProperties.pageSizeDefault`, constructor-injected `EntityManager`, SLF4J log on unexpected 500). **Do not** start Phase C until you confirm.

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
  - Map `/api/**` to `http://localhost:3000` and `http://localhost:5173`; methods `GET, POST, PATCH, OPTIONS`; **not** `*` origin as a permanent default.
- **A.5 Local database (operator-managed; not in `backend/`)**
  - **Do not** add `docker-compose.yml`, Dockerfiles, or other container orchestration under `backend/`. Postgres (with **pgvector** for Phase D+) and Ollama run in **your** existing containers or hosts.
  - Document required env names in `.env.example` (`SPRING_DATASOURCE_URL`, `USERNAME`, `PASSWORD`; later `OLLAMA_BASE_URL`, `OLLAMA_CHAT_MODEL`). Credentials **only** in env, not hardcoded in Java.
  - `./mvnw test` uses **Testcontainers** only — never the dev datasource URL.
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

**Status:** complete (B1–B5) — generate-tests P0–P2 (CRUD) green; review-code Pass (minors fixed)  
**Depends on:** Phase A complete; **C-01…C-03, C-05** agreed (this document §3)  
**Implements:** FEAT-01…10 (except SM), AC-CORE-01…11 (API), AC-API create/list/get/patch-fields/comments, AC-DM-01/02/05

Implement **B1 → B5 in order**. Do **not** PATCH `status` until Phase C (you may accept `status` on the DTO but **must not** persist it until the machine exists — cleaner: **omit status handling** until Phase C).

**Recommendation:** `UpdateTicketRequest.status` is added in **Phase C**, not B.

### 7.1 Phase B1 — Domain enums, constants, utilities (no SM table yet)

**Why:** Closed sets must be Java enums, not `String`, in domain and persistence ([`rules/java-springboot.md`](rules/java-springboot.md)).

- **Create**
  - `TicketStatus`: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`.
  - `TicketPriority`: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` only (**C-02**). **Do not** add `URGENT` to the enum.
  - JSON (`dto.serde`): custom deserializer maps `URGENT` → `CRITICAL`; serializer always writes `CRITICAL`. Test: create/PATCH with `"priority":"URGENT"` returns `"CRITICAL"` and persists `CRITICAL`. Invalid values (e.g. `P1`) still 400.
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
  - On persist, allocate `nextval('ticket_number_seq')` via repository **native `@Query`** (only allowed native) and format `TKT-{n}`. Do not use UUID for ticket PK.
- **`TicketRepository`** extends `JpaRepository<TicketEntity, String>` **and** `JpaSpecificationExecutor<TicketEntity>` (**C-06**)
  - Detail with comments: `findOne(Specification)` with fetch join spec (or `@EntityGraph`) — **not** JPQL `JOIN FETCH` unless spec agrees.
  - List/search: `findAll(Specification, Pageable)` — `Specification` for optional `status`, keyword `q` on **title + description** (bound parameters); priority sort via Criteria `CASE` inside the spec, not string-built JPQL.
  - Default interface methods may compose specs (`search`, `findWithCommentsById`) — still no `*CustomImpl` + `EntityManager`.
- **`CommentRepository`** extends `JpaRepository<CommentEntity, UUID>`
  - **Derived:** `findByTicket_IdOrderByCreatedAtAsc` (no `@Query`).
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

**Request records (`dto.request`)**

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
- [x] [`commands/review-code.md`](commands/review-code.md) on the Phase B diff — **Pass** after three minor Failures were fixed (see §17). No remaining Fail before Phase C.

### 7.5 Phase B5 — Persistence across “restart”

**Why:** **PDF** AC-CORE-09 / **AC-DM-07**.

- Integration test: create ticket (+ comment) against Testcontainers; start a **fresh** Spring context (nested class / second `@SpringBootTest`) **same container**; `GET` still returns the ticket.
- Do not treat “I restarted the IDE” as the only proof.

**Done B5 / Phase B**

- [x] Restart test green.
- [x] CRUD capabilities FEAT-01…10 (minus SM) proven at API integration.
- [x] generate-tests **P0–P2** for ticket CRUD (create/list/search/filter/get/PATCH/comments/restart, envelopes, 500 no leak, extra validation). **Not** T1–T5 / ask / ingest (no impl).

---

## 8. Phase C — State machine and transitions

**Status:** complete  
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

- [x] Domain parameterized tests: T1–T5 + 20 illegal pairs + X1–X3 + self-transition.
- [x] Service: illegal does not persist; legal persists.
- [x] API: 200 T1–T5; 409 + unchanged row for illegal (including PDF reopens).
- [x] PATCH without `status` still updates fields.
- [x] `./mvnw test` green.
- [x] SM placement review vs [`commands/review-code.md`](commands/review-code.md): machine in `domain` (no Spring/JPA); service invokes before persist; no `/transition`; no Fail found.
- [x] **Phase C status set to complete** (this document).

**Phase C status:** complete — RAG still requires explicit confirmation (G8).

---

## 9. Holding line — do not start RAG yet

After Phase C is **complete**:

1. Summarize evidence (test class names, `./mvnw test` result) in chat.
2. **Wait for explicit user confirmation** to start RAG (e.g. “start RAG” / “proceed with Phase D”).
3. Until that message: no Spring AI dependencies, no Ollama calls, no ask controllers, no chunker, no vector DDL applied in the running app (files may be sketched in this plan only).

**RAG start authorization:** granted 2026-10-04 (“start with the next phase”). Phase **D** implemented; **E/F** still sequential.

---

## 10. Phase D — RAG ingestion pipeline, embeddings, vector store

**Status:** complete  
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
| `VectorChunkStore` | Delete-all-for-ticket then insert (**DEC** re-ingest). **Not** Spring Data JPA — JDBC/pgvector (or Spring AI adapter) behind this port per **C-06**. Ticket loads for ingest still use `TicketRepository`. |
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

- [x] Vector schema + HNSW present (AC-DM-08 remainder).
- [x] Ingest tests green with embedding doubles.
- [x] Ingest pipeline review vs [`commands/review-code.md`](commands/review-code.md) RAG ingest items: knowledge from description/comments/resolution; metadata keys; re-ingest replace; no ask. Live Spring AI/Ollama adapter deferred to Phase E (port + doubles in default suite).

**Phase D status:** complete.

---

## 11. Phase E — Embedding job / ingest wiring from ticket service

**Status:** complete (2026-10-04)  
**Depends on:** Phase D complete  
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

- [x] Hooks on create/update/comment/status including close.
- [x] Tests prove hook **and** no ingest on failed ticket write.

---

## 12. Phase F — AI ask API (retrieve-then-generate)

**Status:** complete (2026-10-04)  
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

Thin `AiAskController` in `controller`.

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

- [x] Dual ask paths; envelopes; no-match 200; 400 validation.
- [x] Band A tests green without live LLM.
- [x] Review-code RAG ask checklist Pass (live Ollama/Spring AI still deferred; ports + doubles).
- [ ] Optional: one documented grounding review; AI mistake log if a hallucination is caught ([`docs/ai-error.md`](docs/ai-error.md) via **`/update-ai-error`**).

---

## 13. Test catalogue (CRUD + SM now; RAG later)

Follow [`commands/generate-tests.md`](commands/generate-tests.md). Map names to AC ids.

### 13.1 Phase B–C (implement with those phases)

| Flow | Positive | Negative / empty | Layers | Status (2026-10-04) |
|------|----------|------------------|--------|---------------------|
| Create | 201, `TKT-{n}`, `OPEN`, Location | blank title 400; no row; `status` on create 400 | slice + API-int + service + repo | **done** (incl. `@Size` title/description, `URGENT`→`CRITICAL`) |
| List | default page meta | size 0/101; bad sort; bad status | slice + API-int + repo | **done** (`size` 1 and 100; last page; omitted `size` uses `pageSizeDefault`) |
| Search `q` | title or description hit | no hit 200 []; comment-only miss | API-int + repo | **done** (case-insensitive) |
| Filter status | match | invalid enum 400; AND with `q` | API-int | **done** |
| Get | 200 detail + comments | 404 | slice + API-int | **done** |
| PATCH fields | partial update | 400; 404; `{}` 400 | slice + API-int | **done** |
| Comment | 201; appears on GET | blank body 400; 404 ticket | API-int | **done** (body max length on slice) |
| Restart | GET after new context | — | API-int | **done** (`TicketRestartIT`) |
| Envelopes / 500 | `error` shape | unexpected exception does not leak cause | slice + unit | **done** (`unexpectedExceptionIs500WithoutLeak`, `RestExceptionHandlerTest`) |
| T1–T5 | 200 persisted | — | domain + service + API-int | **done** |
| Illegal 20 / X1–X3 / self | 409, row unchanged | — | domain + service + API-int | **done** (full 20 at domain + API) |
| PATCH no status | fields change | — | service + API-int | **done** (AC-SM-08) |

### 13.2 Phase D–F (after confirmation)

| Flow | Positive | Negative |
|------|----------|----------|
| Ingest | knowledge + hybrid chunks; replace on re-ingest | empty skip embed; failed write no ingest | **E done** (after-commit hooks + doubles). |
| Ask | 200 answer + cited ids from retrieval | 400 question; 200 no-match; no generate on empty | **F done** (Band A; no live LLM). |

---

## 14. Review, fix, and documentation hygiene

After each phase:

1. `cd backend && ./mvnw test`
2. [`commands/review-code.md`](commands/review-code.md) — Pass/Fail; **fix Failures** before the next phase.
3. Part I: do not run [`commands/review-frontend.md`](commands/review-frontend.md). Part II (§20): use **review-frontend** after each UI slice.
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
- [x] No frontend work in **Part I** (backend phases only)
- [x] Part II **§20** — `frontend/` scaffold (**UI-A**); product screens **UI-B** onward

---

## 16. Suggested implementation order (one-line index) — backend

1. **C-01, C-02, C-03, C-05, C-06 agreed** (2026-10-04). **C-04** chat model is env/config (`OLLAMA_CHAT_MODEL`, example `llama3.2`). **C-06** Spring Data JPA primary for relational persistence.
2. Phase **A** — Maven, yml, `.env.example`, Liquibase tickets, Testcontainers smoke (dev DB external).
3. Phase **B1** — enums, constants, sort whitelist.
4. Phase **B2** — entities, repositories, search SQL, repo tests.
5. Phase **B3** — DTOs, mapper, `TicketService` CRUD, service tests.
6. Phase **B4** — controllers, advice, slice + API integration.
7. Phase **B5** — restart persistence test.
8. **Phase B closeout** — generate-tests P0–P2 (CRUD) + review-code; **done 2026-10-04**.
9. Phase **C** — `TicketStatusMachine`, PATCH `status`, full SM tests; **complete 2026-10-04**.
10. **Stop.** User confirmation for RAG.
11. Phase **D** — vector schema, chunker, embed port, ingest service; **complete 2026-10-04**.
12. Phase **E** — after-commit hooks from ticket service; **complete 2026-10-04**.
13. Phase **F** — ask API both paths, Band A tests; **complete 2026-10-04**. Live models / Band B–C optional.

### 16.1 Frontend implementation order (one-line index) — Part II

Prerequisite: backend `./mvnw test` green; API on **8080**; CORS allows **3000** and **5173**.

1. **UI-A** — `frontend/` scaffold (Node 24, Next + Vite, env names, `next dev` / build).
2. **UI-B** — HTTP client + envelope/`ApiError` (no screen logic).
3. **UI-C** — App shell, layout, nav, route stubs (**DEC-20** routes).
4. **UI-D** — Ticket list (default `GET`, loading/empty).
5. **UI-E** — List search `q`, status filter, pagination from `meta` (**DEC-20** Apply + Enter).
6. **UI-F** — Create ticket form → **201** → detail (**parallel** with D/E after **UI-C** if desired).
7. **UI-G** — Ticket detail read-only + **404** handling.
8. **UI-H** — Detail field **Save** (PATCH without `status`).
9. **UI-I** — Comment composer + thread (**parallel** with **UI-H** after **UI-G**).
10. **UI-J** — Status transition buttons + **409** UX.
11. **UI-K** — `/ask` page (grounded answer, citations, no-match) — **parallel** with ticket slices after **UI-C** + **UI-B**.
12. **UI-L** — Demo script §8.7 walkthrough + [`commands/review-frontend.md`](commands/review-frontend.md) Pass.

---

## 17. Sign-off table (update in this file as work finishes)

| Phase | Status | Tests | Review-code | Notes |
|-------|--------|-------|-------------|-------|
| A Setup | complete | smoke green | n/a (scaffold) | Boot 3.5.16; Liquibase 001–003; pg_trgm only; dev Postgres/Ollama external to `backend/` |
| B1 Enums | complete | unit green | Pass (with B) | C-02: `CRITICAL`; JSON `URGENT`→`CRITICAL` |
| B2 Persistence | complete | IT green | Pass (with B) | Spring Data JPA + `JpaSpecificationExecutor`; no custom `*Impl` |
| B3 Service CRUD | complete | unit green | Pass (with B) | no status PATCH |
| B4 HTTP CRUD | complete | slice+IT green; generate-tests P0–P2 CRUD | **Pass** | list `size` default from `ApiProperties`; 500 logged, body generic; no status PATCH |
| B5 Restart | complete | IT green | Pass (with B) | DirtiesContext + same Testcontainers DB |
| B generate-tests | complete for CRUD | `./mvnw test` green | — | P0 SM now Phase C; ask/ingest still deferred |
| **C State machine** | **complete** | domain + service + slice + API 20 illegal / T1–T5 green | SM placement Pass | PATCH `status`; 409 unchanged row |
| **D Ingest / vectors** | **complete** | builder/chunker/unit + `TicketIngestionIT` doubles | ingest Pass (ask N/A) | `ticket_vector_chunk` + HNSW; no TicketService hook; no live Ollama |
| **E Ingest hooks** | **complete** | service unit + `TicketIngestionHookIT` | hook Pass | after-commit; ingest failure does not fail ticket write |
| **F Ask API** | **complete** | slice + `AskServiceTest` + `AskApiIT` Band A | ask Pass | dual paths; 200 no-match; 400 validation; stub generate |
| **G Stack upgrade (Boot 4 / Java 25 / Spring AI 2.x)** | **complete** | `./mvnw test` green (JDK 25) | pending | Boot **4.1.1**, Spring AI **2.0.1**, Jackson **3**, Testcontainers **2.x**, `spring-boot-starter-liquibase` |
| **UI-A Scaffold** | complete | `next build` / `next dev` on **3000** | n/a | Next **15.5.x**, Node **24**, `.env.example`, rewrites + Vite proxy |
| **UI-B API client** | complete | manual/curl against 8080 | n/a | `src/api/client`, `tickets`, `ask`, `types` |
| **UI-C Shell + routes** | complete | routes render | n/a | **DEC-20**, **FG2** |
| **UI-D List basic** | complete | manual | n/a | **AC-UI-02** partial |
| **UI-E List filters** | complete | manual | n/a | **AC-UI-02** |
| **UI-F Create** | complete | manual | n/a | **AC-UI-01** |
| **UI-G Detail read** | complete | manual | n/a | **AC-UI-03** |
| **UI-H Detail save** | complete | manual | n/a | **AC-UI-04** |
| **UI-I Comments** | complete | manual | n/a | **AC-UI-05** |
| **UI-J Status** | complete | manual | n/a | **AC-UI-06** |
| **UI-K Ask** | not started | manual (+ optional live RAG) | review-rag-output optional | **AC-UI-08…12** |
| **UI-L Demo + review** | not started | §8.7 script | review-frontend | **AC-UI-07**, hub **§8.7** |

---

## 20. Part II — Frontend implementation (`frontend/`)

**Status:** in progress (UI-J complete)  
**Depends on:** Backend Part I **complete** (ticket + ask APIs on **8080**). For grounded ask demos, operator-managed Ollama + corpus per [`spec/evaluation-strategy.md`](spec/evaluation-strategy.md) — not required to merge individual UI slices.  
**Implements:** **FEAT-01…11**, **FEAT-15…18** (user-visible); **FR-UI-01…03**; **AC-UI-01…12**; demo [`spec/requirements.md`](spec/requirements.md) **§8.7**.  
**Read first:** [`rules/frontend.md`](rules/frontend.md), [`spec/ui-flow.md`](spec/ui-flow.md), [`spec/architecture.md`](spec/architecture.md) **§12**, [`spec/api-contract.md`](spec/api-contract.md), [`spec/rag-api-contract.md`](spec/rag-api-contract.md) **§7**, [`commands/review-frontend.md`](commands/review-frontend.md).

### 20.0 How to use Part II

- Work **UI-A → UI-L** in the order in §16.1 unless a slice explicitly allows **parallel** work (noted below).
- Each slice has **Done criteria** checkboxes; run [`commands/review-frontend.md`](commands/review-frontend.md) on the diff before marking **UI-L** complete.
- **No frontend automated tests** this milestone ([`rules/frontend.md`](rules/frontend.md), [`spec/ui-flow.md`](spec/ui-flow.md) §2.2).
- After material JS changes under `frontend/`, run `graphify update .`.
- **Do not** add auth, delete ticket, agent-from-ask, confidence UI, or vector admin consoles ([`spec/requirements.md`](spec/requirements.md) **§2.3 Reference**).

### 20.1 Frontend sequencing gates

| Gate | Meaning |
|------|---------|
| **FG0** | `frontend/` package exists; Node **24.x** LTS; `next dev` serves on **3000**; `.env.example` has **names only**. |
| **FG1** | Single HTTP module parses `{ data, meta? }` / `{ error }`; no duplicate `fetch` in screens. |
| **FG2** | **DEC-20** routes exist (stubs OK): `/` → `/tickets`, `/tickets`, `/tickets/new`, `/tickets/[id]`, `/ask`; global nav list ↔ ask. |
| **FG3** | **AC-UI-01…07** provable manually (CRUD, search/filter, comments, status, errors). |
| **FG4** | **AC-UI-08…12** + demo **§8.7** + review-frontend **Pass**. |

### 20.2 Target layout (`frontend/`)

Per [`rules/frontend.md`](rules/frontend.md) and [`spec/ui-flow.md`](spec/ui-flow.md) §5:

```text
frontend/
  .nvmrc                      # 24
  package.json                  # engines.node >=24 <25; next, react, vite (dev)
  .env.example                  # NEXT_PUBLIC_API_BASE_URL, VITE_API_BASE_URL — names only
  app/                          # Next.js App Router
    layout.js                   # shell + nav
    page.js                     # redirect / → /tickets
    tickets/
      page.js                   # list
      new/page.js               # create
      [id]/page.js              # detail
    ask/page.js                 # assistant
  src/
    api/
      client.js                 # base URL, fetch wrapper, envelopes
      tickets.js                # ticket + comment calls (optional split)
      ask.js                    # POST ask (optional split)
    components/                 # Loading, ErrorBanner, form controls, status buttons
    lib/                        # optional: format dates, status labels
  next.config.js                # rewrites/proxy to :8080 optional
  vite.config.js                # port 5173; proxy /api for shared-module HMR
```

**Stack (**DEC-15**):** React function components; **JavaScript** only (no TypeScript). Plain CSS (global or CSS modules) — **no** Tailwind/MUI/Redux/Zustand unless user agrees later.

**API base:** `NEXT_PUBLIC_API_BASE_URL` in browser; optional `VITE_API_BASE_URL` when exercising `src/` under Vite. Prefer relative `/api/v1/...` when Next rewrites proxy to backend.

### 20.3 Slice dependency graph (independence)

```text
UI-A (scaffold)
  └─ UI-B (API client)
       ├─ UI-C (shell + routes)
       │    ├─ UI-D (list basic) ── UI-E (search/filter/page)
       │    ├─ UI-F (create)          [parallel with D/E after C]
       │    ├─ UI-G (detail read) ──┬─ UI-H (field save)
       │    │                         ├─ UI-I (comments)   [parallel after G]
       │    │                         └─ UI-J (status)     [parallel after G]
       │    └─ UI-K (ask)             [parallel after C; citations need G for links]
       └─ UI-L (demo + review)       [after FG3 + FG4 slices]
```

---

### 20.4 Phase UI-A — Scaffold and tooling

**Status:** complete (2026-10-04)  
**Depends on:** none (repo may have no `frontend/` yet)  
**Maps to:** [`spec/ui-flow.md`](spec/ui-flow.md) **IR-UI-01**, **IR-UI-03**; [`rules/frontend.md`](rules/frontend.md) Stack §

**Why:** Establishes the approved stack and dev ports before any product screen.

**Work**

- **UI-A.1** Create `frontend/` with `package.json`: `engines.node` `>=24 <25`; scripts `dev` (`next dev`), `build` (`next build`), `start` (`next start`); dependencies `next`, `react`, `react-dom`; devDependency `vite` for shared-module config.
- **UI-A.2** Add `.nvmrc` with `24`.
- **UI-A.3** Add `.env.example` with `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080` and `VITE_API_BASE_URL=http://localhost:8080` (no secrets).
- **UI-A.4** Minimal `next.config.js` (optional `rewrites` from `/api` → `http://localhost:8080`).
- **UI-A.5** `vite.config.js` with `server.port` **5173** and optional `proxy` for `/api` ([`rules/frontend.md`](rules/frontend.md)).
- **UI-A.6** Root `README` snippet or `frontend/README.md`: run backend on 8080, then `cd frontend && npm install && npm run dev`.

**Done criteria — UI-A**

- [x] `npm run build` succeeds under Node 24.
- [x] `npm run dev` serves on **3000**.
- [x] No committed `.env` with secrets.

---

### 20.5 Phase UI-B — HTTP client and error model

**Status:** complete (2026-10-04)  
**Depends on:** UI-A  
**Maps to:** [`spec/ui-flow.md`](spec/ui-flow.md) §5–§6, unit **UI-B**; [`rules/api-standards.md`](rules/api-standards.md)

**Why:** One place for envelopes prevents drift and satisfies review-frontend API client checks.

**Work**

- **UI-B.1** `src/api/client.js`: `getBaseUrl()` from `process.env.NEXT_PUBLIC_API_BASE_URL` (and `import.meta.env.VITE_API_BASE_URL` when used from Vite-only entry).
- **UI-B.2** `requestJson(path, options)` — parse JSON; on `!res.ok` throw `ApiError` with `error.message`, `error.details`, `status`, `code`.
- **UI-B.3** Helpers: `unwrapData(body)` → `data`; list helper returns `{ data, meta }`.
- **UI-B.4** JSDoc typedefs aligned to [`spec/api-contract.md`](spec/api-contract.md) §3 (`TicketSummary`, `TicketDetail`, `Comment`, etc.) and ask `AskResponseData` from [`spec/rag-api-contract.md`](spec/rag-api-contract.md).
- **UI-B.5** `src/api/tickets.js`: `listTickets({ page, size, sort, q, status })`, `getTicket(id)`, `createTicket(body)`, `patchTicket(id, body)`, `addComment(id, body)` — paths under `/api/v1/tickets`.
- **UI-B.6** `src/api/ask.js`: `askQuestion(question)` → `POST /api/v1/ai/ask` with `{ question }` (may also support `/api/ai/ask` if shared with backend dual path).

**Verification (manual, no test files)**

- With backend running: client `listTickets` returns array; forced 404 throws readable `ApiError`.

**Done criteria — UI-B**

- [x] No raw `fetch` in `app/` route files (only via `src/api/*`).
- [x] Empty list response does not throw.

---

### 20.6 Phase UI-C — Application shell and routes

**Status:** complete (2026-10-04)  
**Depends on:** UI-A, UI-B (routes may render placeholders without API)  
**Maps to:** [`spec/ui-flow.md`](spec/ui-flow.md) §4 **UI-A**, **DEC-20**

**Work**

- **UI-C.1** `app/layout.js`: site header with links **Tickets** (`/tickets`) and **Ask** (`/ask`); semantic `<main>`.
- **UI-C.2** `app/page.js`: redirect to `/tickets`.
- **UI-C.3** Stub pages for `/tickets`, `/tickets/new`, `/tickets/[id]`, `/ask` (title + “coming soon” OK until later slices).
- **UI-C.4** Shared `src/components/Loading.jsx`, `ErrorBanner.jsx` (props: message, optional details).

**Done criteria — UI-C**

- [x] All **DEC-20** routes reachable from nav.
- [x] **FG2** satisfied (stubs acceptable).

---

### 20.7 Phase UI-D — Ticket list (basic)

**Status:** complete (2026-10-04)  
**Depends on:** UI-C, UI-B  
**Maps to:** **AC-UI-02** (partial); [`spec/ui-flow.md`](spec/ui-flow.md) §7.1

**Work**

- **UI-D.1** `app/tickets/page.js` (or thin page + `src/components/TicketList.jsx`): `GET` default `page=0`, `size=20`.
- **UI-D.2** Table or list: `id`, `title`, `status`, `priority`, `assignee`, `updatedAt`.
- **UI-D.3** Row click → `/tickets/[id]`.
- **UI-D.4** Loading and empty states (“No tickets yet” + link to **New ticket**).
- **UI-D.5** Button **New ticket** → `/tickets/new`.

**Done criteria — UI-D**

- [x] Persisted tickets from backend appear after refresh.
- [x] **AC-UI-02** without search/filter yet.

---

### 20.8 Phase UI-E — List search, status filter, pagination

**Status:** complete (2026-10-04)  
**Depends on:** UI-D  
**Maps to:** **AC-UI-02**; **AC-CORE-07**, **08**; **DEC-20** (Apply + Enter, `meta` pagination)

**Work**

- **UI-E.1** Keyword input bound to `q`; **Apply** button and **Enter** trigger refresh (do not debounce-every-keystroke as the only mode).
- **UI-E.2** Status filter: “All” or enum value → `status` query param.
- **UI-E.3** Combine `q` and `status` in one request.
- **UI-E.4** Pagination UI from `meta.page`, `meta.totalPages`, `meta.totalElements`; prev/next.
- **UI-E.5** Filtered empty copy: “No matches for …”.

**Done criteria — UI-E**

- [x] Search hits title/description per backend **DEC-08** (comment-only text does not match).
- [x] **AC-UI-02** complete.

---

### 20.9 Phase UI-F — Create ticket

**Status:** complete (2026-10-04)  
**Depends on:** UI-C, UI-B (**parallel** with UI-D/E after UI-C)  
**Maps to:** **AC-UI-01**, **AC-UI-07** (create validation); [`spec/ui-flow.md`](spec/ui-flow.md) §7.2

**Work**

- **UI-F.1** Form fields: `title` (required), `description`, `priority` select (`LOW`|`MEDIUM`|`HIGH`|`CRITICAL`), `assignee`, optional `category` — **no `status` field**.
- **UI-F.2** Submit → `POST /api/v1/tickets`; on **201** navigate to `/tickets/{data.id}` (**DEC-20**).
- **UI-F.3** **400** → show `error.message` and map `details[]` to fields; preserve user input.
- **UI-F.4** Cancel → `/tickets` without submit.

**Done criteria — UI-F**

- [x] New `TKT-{n}` visible on detail without DB tools.
- [x] Empty title shows readable validation (**demo §8.7** step 8 pattern).

---

### 20.10 Phase UI-G — Ticket detail (read)

**Status:** complete (2026-10-04)  
**Depends on:** UI-C, UI-B  
**Maps to:** **AC-UI-03**; [`spec/ui-flow.md`](spec/ui-flow.md) §7.3

**Work**

- **UI-G.1** Load `GET /api/v1/tickets/{id}` from route param (`TKT-1001` style).
- **UI-G.2** Display all detail fields + comments ascending by `createdAt`.
- **UI-G.3** **404** → “Ticket not found” + link to list.
- **UI-G.4** Link to `/ask` in detail footer (**DEC-20**).

**Done criteria — UI-G**

- [x] **AC-UI-03**; citation deep-links from ask (UI-K) can land here.

---

### 20.11 Phase UI-H — Detail field update (Save)

**Status:** complete (2026-10-04)  
**Depends on:** UI-G  
**Maps to:** **AC-UI-04**; **DEC-20** (Save separate from status)

**Work**

- **UI-H.1** Editable: `title`, `description`, `priority`, `assignee`, `category`, `resolutionNotes`.
- **UI-H.2** **Save changes** sends `PATCH` with **only changed** fields — **omit `status`**.
- **UI-H.3** Success: update view from response `data` or refetch GET.
- **UI-H.4** **400** field errors; reload shows persisted values (**AC-CORE-09** with backend restart demo).

**Done criteria — UI-H**

- [x] **AC-UI-04** after browser reload.

---

### 20.12 Phase UI-I — Comments

**Status:** complete (2026-10-04)  
**Depends on:** UI-G (**parallel** with UI-H after UI-G)  
**Maps to:** **AC-UI-05**

**Work**

- **UI-I.1** Comment thread (read-only list from detail).
- **UI-I.2** Composer: `body` required; `POST .../comments`.
- **UI-I.3** On **201**, append or refetch; show **400** on blank body.

**Done criteria — UI-I**

- [x] **AC-UI-05**.

---

### 20.13 Phase UI-J — Status transitions

**Status:** complete (2026-10-04)  
**Depends on:** UI-G (read status); UI-H recommended so field save is stable  
**Maps to:** **AC-UI-06**; [`spec/state-machine.md`](spec/state-machine.md); [`spec/ui-flow.md`](spec/ui-flow.md) §9

**Work**

- **UI-J.1** Button group: legal targets only per current `status` (§9.1 matrix); hide/disable for `CLOSED` / `CANCELLED`.
- **UI-J.2** Each action → `PATCH` with `{ "status": "<target>" }` only (or status-only body).
- **UI-J.3** **409** `ILLEGAL_TRANSITION` → prominent message; refetch ticket so UI matches server.
- **UI-J.4** Optional: offer illegal target for demo — backend must still reject (**AC-CORE-11**).

**Done criteria — UI-J**

- [x] Happy path OPEN → IN_PROGRESS → RESOLVED → CLOSED in UI.
- [x] **409** readable on illegal attempt.

---

### 20.14 Phase UI-K — Ask / assistant page

**Status:** not started  
**Depends on:** UI-C, UI-B (**parallel** with ticket phases after UI-C); UI-G for citation links  
**Maps to:** **AC-UI-08…12**; [`spec/rag-api-contract.md`](spec/rag-api-contract.md) **DEC-11**

**Work**

- **UI-K.1** `/ask`: textarea + submit; disable while loading.
- **UI-K.2** Success **200**: show `data.answer` in a visually distinct **assistant** panel (**AC-UI-12**).
- **UI-K.3** Render `data.citedTicketIds` as links to `/tickets/{id}` (**AC-UI-09**).
- **UI-K.4** No-match **200**: show honest `data.answer` (e.g. “No relevant tickets found.”); empty citations — not an error banner (**AC-UI-10**).
- **UI-K.5** **400** validation on blank/oversized question.
- **UI-K.6** No buttons for create-ticket, notify, or tools (**AC-UI-11**).

**Done criteria — UI-K**

- [ ] Band A backend tests already green; UI manual check with stub or live RAG.
- [ ] Optional: one [`commands/review-rag-output.md`](commands/review-rag-output.md) pass when Ollama corpus available.

---

### 20.15 Phase UI-L — Demo hardening and review

**Status:** not started  
**Depends on:** UI-E, UI-F, UI-H, UI-I, UI-J, UI-K (FG3 + FG4)  
**Maps to:** [`spec/requirements.md`](spec/requirements.md) **§8.7**; [`spec/ui-flow.md`](spec/ui-flow.md) §11–§12

**Work**

- **UI-L.1** Walk demo script steps that touch UI (create, list/search, detail update, comment, transitions, restart persistence, ask with citations, no-match, validation errors).
- **UI-L.2** Run [`commands/review-frontend.md`](commands/review-frontend.md) on full `frontend/` diff — fix **blocker** / **major** Failures.
- **UI-L.3** Cross-check **AC-UI-01…12** table in [`spec/ui-flow.md`](spec/ui-flow.md) §13.
- **UI-L.4** `graphify update .` after frontend files stabilize.

**Done criteria — UI-L**

- [ ] **FG4**; review-frontend **Pass**.
- [ ] Demo **§8.7** UI steps repeatable without Postman.

---

### 20.16 Frontend acceptance map

| Slice | Primary **AC-UI** | Hub **AC-CORE** (UI-facing) |
|-------|-------------------|-----------------------------|
| UI-F | 01 | 01, 10 |
| UI-E | 02 | 02, 07, 08 |
| UI-G | 03 | 03 |
| UI-H | 04 | 04, 05, 09 |
| UI-I | 05 | 06 |
| UI-J | 06 | 11, 12, 13 |
| UI-B…C | 07 | 10, 11 |
| UI-K | 08–12 | 16, 17, 18 |

### 20.17 Frontend explicit non-implement list

- [ ] No TypeScript in `frontend/`
- [ ] No Vitest/Playwright/Cypress or `*.test.jsx`
- [ ] No auth/login screens
- [ ] No delete-ticket UI
- [ ] No `PUT` / `DELETE` ticket calls
- [ ] No ask-side agent actions
- [ ] No confidence badges or vector debug UI
- [ ] No Tailwind/MUI/Redux/Zustand unless user agrees
- [ ] No hardcoded `localhost:8080` in source (use env or Next rewrites)

### 20.18 Frontend review hygiene

After each slice **UI-D** onward:

1. Manual smoke against running backend.
2. For merge-ready UI: [`commands/review-frontend.md`](commands/review-frontend.md).
3. Do **not** run [`commands/generate-tests.md`](commands/generate-tests.md) for frontend files.

---

## 19. Stack upgrade — Boot 4, Java 25, Spring AI 2.x

**Status (2026-10-04):** Project conventions, `backend/pom.xml`, and architecture §7.3 pins are updated. **Do not** treat the backend as merge-ready until implementation tasks below complete and `./mvnw test` is green on JDK 25.

| Step | Scope | Acceptance |
|------|--------|------------|
| G-0 | Specs, `rules/*`, commands, `plan.md`, `backend/pom.xml` | Done |
| G-1 | Resolve compile/test failures from Boot 4 / Spring Framework 7 / Spring AI 2 / Jackson 3 / Testcontainers 2 API changes | `./mvnw clean test` green on **JDK 25** |
| G-2 | Re-run `commands/review-code.md` on touched backend files | Pending (user) |
| G-3 | Update this sign-off row **G** to **complete** | Done |

**Pinned versions:** Spring Boot **4.1.1**, Java **25**, Spring AI BOM **2.0.1** — see [`spec/architecture.md`](spec/architecture.md) §7.3.

---

## 18. Revision history

| Date | Note |
|------|------|
| 2026-10-04 | Initial backend-only plan: CRUD + SM first; RAG gated on Phase C complete and user confirmation; code under `backend/`. |
| 2026-10-04 | Confirmed **C-01** `com.ticketmanagement`; **C-02** `CRITICAL` + inbound `URGENT` mapped; **C-03** latest Boot 3 GA; **C-05** assignee `@Size(max=320)` only. **C-04** chat model still deferred. |
| 2026-10-04 | Phase **A** implemented: `backend/` Spring Boot **3.5.16**, Maven Wrapper, Liquibase ticket tables + relational indexes, Testcontainers smoke. |
| 2026-10-04 | Phase **B1** implemented: domain enums/constants/sort parser; API Jackson maps `URGENT`→`CRITICAL`. |
| 2026-10-04 | Phase **B2** implemented: JPA ticket/comment entities, sequence ids, parameterized search, Testcontainers repository tests. |
| 2026-10-04 | Phase **B3–B4** implemented: ticket CRUD service/DTOs, REST `/api/v1/tickets`, envelopes, MockMvc slice + API integration tests (no status PATCH). |
| 2026-10-04 | Phase **B5** implemented: create ticket+comment, fresh Spring context, same Postgres container, GET still returns data. |
| 2026-10-04 | AI error log [`docs/ai-error.md`](docs/ai-error.md); `/update-ai-error`. |
| 2026-10-04 | Phase **B** closeout: generate-tests **P0–P2** for ticket CRUD (SM/ask/RAG skipped). Review-code **Pass** after fixing list page-size default, `EntityManager` constructor injection, and unexpected-500 logging. Phase **C** still not started. |
| 2026-10-04 | Phase **C** implemented: `TicketStatusMachine` T1–T5 and 20 illegal pairs; PATCH `status` 200/409; `./mvnw test` green. RAG not started. |
| 2026-10-04 | Phase **D** implemented: PgVector `ticket_vector_chunk` + HNSW; hybrid chunker; ingest service with embedding doubles. TicketService hooks and ask API not started. |
| 2026-10-04 | Phases **E** and **F** implemented: after-commit ingest from `TicketService`; `POST /api/ai/ask` and `/api/v1/ai/ask`; Band A tests with embedding/generation doubles. Live Ollama still not wired. |
| 2026-10-04 | Spring AI **1.1.4** Ollama adapters wired (`EmbeddingPort` / `GenerationPort`). Tests keep `spring.ai.model.*=none` + doubles. No Ollama Docker. *(Superseded by Spring AI **2.0.1** + Boot **4.1.1** in stack upgrade G-0; code migration G-1 pending.)* |
| 2026-10-04 | Plan: **no** in-repo Docker/Compose under `backend/` — dev Postgres/pgvector and Ollama are operator-managed; tests remain Testcontainers-only. |
| 2026-10-04 | **C-06:** Spring Data JPA primary for relational ticket persistence; vector chunk store remains `rag` port + JDBC exception. |
| 2026-10-04 | **C-06** tightened: derived queries → `Specification` → `@EntityGraph` → minimal native; removed custom repository `EntityManager` JPQL pattern from target implementation. |
| 2026-10-04 | Backend packages: `controller`, `dto/*`, `entity`, `repository`, `exception`, `util`, `advice`, `service`, `rag`, `config` (replaced `api/` + `persistence/` split). |
| 2026-10-04 | **Stack upgrade (G):** Backend migrated to Boot **4.1.1**, JDK **25**, Spring AI **2.0.1**, Jackson **3**, Testcontainers **2.x**; `./mvnw clean test` green with `JAVA_HOME` on JDK 25. |
| 2026-10-04 | **Part II (§20):** Frontend plan under `frontend/` — phases **UI-A…UI-L**, gates **FG0–FG4**, aligned to **DEC-15**/**DEC-20**, **AC-UI-01…12**, `rules/frontend.md`, `commands/review-frontend.md`, assessment PDF capabilities. |
