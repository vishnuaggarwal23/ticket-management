# Architecture

> **Status:** draft — design for implementation; field-level contracts live in sibling specs.  
> **Requirements hub:** [`requirements.md`](requirements.md) (PDF-derived acceptance and FEAT catalogue).  
> **Assessment source:** `docs/Assessments.pdf` (via [`docs/assessment-brief.md`](../docs/assessment-brief.md)).  
> **Audience:** implementers, reviewers, assessors.

**Label legend** (same as `requirements.md`)

| Label | Meaning |
|-------|---------|
| **PDF** | Required or named in the assessment PDF. |
| **Convention** | Project choice in `rules/*` or this spec; not a PDF mandate. |
| **Open** | Underspecified; resolve in a child spec after confirmation. |
| **Example** | Illustrative only (e.g. `TKT-1001`). |

This document describes **system shape**: business capabilities, ticket and RAG structure, technology layout, APIs, communication, and components. It **does not** replace [`data-model.md`](data-model.md), [`api-contract.md`](api-contract.md), [`state-machine.md`](state-machine.md), [`rag-ingestion.md`](rag-ingestion.md), [`rag-api-contract.md`](rag-api-contract.md), [`ui-flow.md`](ui-flow.md), [`test-strategy.md`](test-strategy.md), or [`evaluation-strategy.md`](evaluation-strategy.md).

---

## Table of contents

1. [Problem and context](#1-problem-and-context)  
2. [Scope and non-goals](#2-scope-and-non-goals)  
3. [Architectural principles](#3-architectural-principles)  
4. [Business architecture](#4-business-architecture)  
5. [Ticket structure (conceptual)](#5-ticket-structure-conceptual)  
6. [System context](#6-system-context)  
7. [Technology structure](#7-technology-structure)  
8. [Functional modules and components](#8-functional-modules-and-components)  
9. [Technical components and layering](#9-technical-components-and-layering)  
10. [Communication architecture](#10-communication-architecture)  
11. [API architecture](#11-api-architecture)  
12. [Frontend architecture](#12-frontend-architecture)  
13. [Data and persistence](#13-data-and-persistence)  
14. [Vector database architecture](#14-vector-database-architecture)  
15. [RAG architecture](#15-rag-architecture)  
16. [Knowledge, chunking, and embeddings](#16-knowledge-chunking-and-embeddings)  
17. [Status state machine (placement)](#17-status-state-machine-placement)  
18. [Configuration](#18-configuration)  
19. [Errors and cross-cutting concerns](#19-errors-and-cross-cutting-concerns)  
20. [Security and deployment](#20-security-and-deployment)  
21. [Open questions and decisions](#21-open-questions-and-decisions)  
22. [Related specifications](#22-related-specifications)  
23. [Acceptance mapping](#23-acceptance-mapping)  

---

## 1. Problem and context

The system is an **AI-powered support ticket management** application (**PDF**): conventional ticket operations (create, list, detail, update core fields, comments, keyword search, status filter, persistence, validation) plus **RAG-based** natural-language Q&A over ticket history.

Two engineering regimes coexist:

| Regime | Examples | Proof style |
|--------|----------|-------------|
| **Deterministic** | CRUD, validation, status state machine | Repeatable tests; exact outcomes |
| **Probabilistic** | Similarity ranking, answer phrasing | Retrieval eval + grounding review; not one golden LLM string |

The assistant is **retrieve-then-generate** only (**PDF**): grounded answers with **ticket ID citations**, or an **honest no-match**—not an autonomous agent.

---

## 2. Scope and non-goals

### 2.1 In scope (architecture must support)

| Area | Requirement summary | Requirements trace |
|------|---------------------|-------------------|
| Tickets | CRUD, comments, keyword search, status filter, DB persistence | FEAT-01…08, FR-01…08 |
| Validation and errors | Backend validation; meaningful UI errors | FEAT-09…10, FR-09…10 |
| State machine | Backend-enforced transitions; invalid rejected | FEAT-11, FR-11…12 |
| RAG ingest | Description, comments, resolution notes → knowledge → chunk → embed → vector store | FEAT-12…14, FR-13…14 |
| RAG ask | `POST /api/ai/ask`; citations or no-match | FEAT-15…18, FR-15…18 |
| Retrieval tuning | Configurable top-K and similarity threshold | FEAT-19, FR-18 |
| Documentation | Chunking and embedding **justified** here (**PDF** NFR-07) | FEAT-20, AC-CORE-19 |

### 2.2 Non-goals

- **Autonomous agent** from the ask endpoint: no ticket creation, notifications, or tool chaining (**PDF** §2.2).
- **Authentication / authorization** — not in the PDF (**Open** → OQ-06, DEC-12).
- Multi-tenancy, attachments, email/Slack, workflow beyond the defined status machine — unless added to agreed requirements later.

---

## 3. Architectural principles

1. **Spec-driven** — behaviour follows agreed `spec/`; ambiguous product rules are confirmed before code (see `requirements.md` §10).
2. **Thin edges, rich domain** — HTTP adapters validate and map; ticket rules and the state machine live in **domain/services**, not controllers or the UI (**PDF**).
3. **Single persistence truth for tickets** — PostgreSQL is the **system of record**; the vector index is a **derived** search/RAG index, refreshed when tickets change (**PDF** freshness).
4. **Grounded AI** — generation for support Q&A uses **retrieved ticket context only**; no silent fallback to general world knowledge (**PDF**).
5. **Monolith, simple topology** — one deployable Spring Boot backend and one React SPA over REST (**Convention**); avoid microservices unless requirements change.
6. **PDF vs convention** — document both; do not present project envelopes or `/api/v1` as PDF mandates (`rules/api-standards.md`).

---

## 4. Business architecture

Business architecture describes **who** uses the system, **what** business capabilities exist, and how they group—without prescribing HTTP paths or Java packages.

### 4.1 Actors

| Actor | Role | Primary capabilities |
|-------|------|----------------------|
| **Support agent** | Day-to-day ticket work | Create/update tickets, comment, search, filter, transition status |
| **Support lead / researcher** | Historical insight | Same as agent plus **Ask** over ticket corpus |
| **Operator / engineer** | Run and configure | Deploy app, configure DB and RAG parameters (no secrets in repo) |
| **Assessor / reviewer** | Process evidence | Spec set, tests, grounding review, documented AI mistakes (**PDF** process) |

No role-based access control is required by the PDF (**Open**).

### 4.2 Business capabilities (capability map)

Capabilities align with `requirements.md` §4 feature catalogue:

```mermaid
flowchart TB
  subgraph ticket_ops [Ticket operations PDF]
    C[Create and persist tickets]
    L[List and view detail]
    U[Update metadata and assignee]
    CM[Add comments timeline]
    S[Keyword search]
    F[Filter by status]
    SM[Lifecycle status transitions]
  end

  subgraph knowledge [Knowledge and AI PDF]
    IN[Ingest ticket narrative to index]
    RF[Refresh index on change]
    ASK[Natural language ask]
    GR[Grounded answers with citations]
    NM[Honest no-match]
  end

  subgraph quality [Quality and process PDF]
    VAL[Validate inputs]
    ERR[Meaningful errors to users]
    TST[State machine integration tests]
    DOC[Document chunking and embedding]
  end

  C --> IN
  U --> RF
  CM --> RF
  SM --> RF
  IN --> ASK
  RF --> ASK
  ASK --> GR
  ASK --> NM
  SM --> VAL
  C --> VAL
  VAL --> ERR
```

### 4.3 Business modules (logical)

Business modules are **cohesive responsibility areas** for planning and traceability. They may map to one or more technical components (§9).

| Business module | Purpose | Key behaviours | Features |
|-----------------|---------|----------------|----------|
| **Ticket registry** | Authoritative record of support work | Create, read, update fields, list | FEAT-01…04, 08 |
| **Collaboration timeline** | Threaded agent notes on a ticket | Add and view comments | FEAT-05 |
| **Work discovery** | Find tickets in a queue | Keyword search, status filter | FEAT-06…07 |
| **Lifecycle governance** | Legal status progression | Allow T1–T5; reject X1–X3 | FEAT-11 |
| **Knowledge indexing** | Make ticket text searchable for AI | Build docs, chunk, embed, metadata | FEAT-12…13 |
| **Index freshness** | Avoid stale answers | Re-ingest on update/close (**PDF** p.5; **DEC-01** for close-only) | FEAT-14 |
| **Assisted research** | Q&A over history | Ask API, retrieval, generation, citations | FEAT-15…18 |
| **Operational quality** | Trust and assessability | Validation, errors, configurable retrieval, docs | FEAT-09…10, 19…23 |

**Business rule integrity** (from `requirements.md` §3.2):

- Status changes follow the published lifecycle; illegal moves are **refused** by the backend.
- The assistant does **not** invent ticket facts when retrieval does not support an answer.
- Operational knowledge in the vector index must **not go stale** when tickets change (**PDF**); execution timing is **Open** → `rag-ingestion.md`.

---

## 5. Ticket structure (conceptual)

Field catalogs, enums, and Liquibase tables belong in **`data-model.md`**. This section defines the **architectural shape** of a ticket for design discussions.

### 5.1 Ticket aggregate (logical)

A **ticket** is the primary aggregate root for support work (**PDF**). Conceptually it comprises:

| Part | Description | PDF / Open |
|------|-------------|------------|
| **Identity** | Stable id used in UI, API, and RAG citations | **Open** (OQ-01, **Example** `TKT-1001`) |
| **Core metadata** | Title, description, priority, assignee | **PDF** (update list) |
| **Lifecycle** | Status enum and transition history | **PDF** state machine |
| **Timeline** | Ordered comments (agent notes) | **PDF** |
| **Resolution narrative** | Text capturing how the issue was resolved | **PDF** ingestion source; field shape **Open** (OQ-10) |
| **Taxonomy** | Category (for metadata and filters) | **PDF** metadata key; source **Open** (OQ-03) |
| **Audit** | Created/updated timestamps | **Convention** for sorting (`rules/api-standards.md`) |

```mermaid
erDiagram
  TICKET ||--o{ COMMENT : has
  TICKET {
    string id
    string title
    string description
    string priority
    string assignee
    string status
    string category
    string resolutionNotes
    instant createdAt
    instant updatedAt
  }
  COMMENT {
    string id
    string body
    instant createdAt
  }
```

*Diagram is conceptual; column names and optionality are **Open** until `data-model.md` is agreed.*

### 5.2 Status lifecycle (summary)

Canonical states (**PDF**): `OPEN` | `IN_PROGRESS` | `RESOLVED` | `CLOSED` | `CANCELLED`.

Allowed edges (**PDF**): T1–T5 in `requirements.md` §4.2 FEAT-11. Forbidden reopen examples: `CLOSED`/`RESOLVED`/`CANCELLED` → `OPEN`.

Full matrix, skipped hops (**DEC-02**), and transition API shape (**DEC-06**) → [`state-machine.md`](state-machine.md).

### 5.3 Text sources for RAG (ticket → knowledge)

Only these ticket-owned texts feed the knowledge pipeline (**PDF** FR-13):

| Source | When included | Notes |
|--------|---------------|-------|
| Description | Always when present | Primary problem statement |
| Comments | Each comment body | Timeline context |
| Resolution notes | When field exists / populated | **Open** shape (OQ-10) |

Title and metadata (`status`, `priority`, `assignee`, `category`) are attached as **chunk metadata** for filtering and citation display, not necessarily embedded as standalone documents unless `rag-ingestion.md` agrees.

### 5.4 Ticket operations vs derived index

| Store | Role | Mutability |
|-------|------|------------|
| **PostgreSQL ticket rows** | System of record | Updated only via ticket services + validation + state machine |
| **Vector chunks** | Derived search index | Rebuilt/refreshed by ingestion; rebuildable from PostgreSQL if lost |

---

## 6. System context

```mermaid
flowchart LR
  subgraph clients [Clients]
    UI[Web UI React SPA]
  end

  subgraph backend [Spring Boot monolith]
    API[REST API layer]
    TM[Ticket application services]
    SM[Status state machine]
    RAG[RAG ingest and ask]
  end

  subgraph data [Data stores]
    PG[(PostgreSQL tickets and comments)]
    VS[(PgVector embeddings)]
  end

  subgraph external [External configurable]
    LLM[Chat completion model]
    EMB[Embedding model]
  end

  UI -->|HTTPS JSON| API
  API --> TM
  API --> RAG
  TM --> SM
  TM --> PG
  RAG --> PG
  RAG --> VS
  RAG --> EMB
  RAG --> LLM
```

**Runtime roles**

- **Web UI** — ticket screens, search/filter, status actions, AI ask panel; talks only to backend REST (**PDF** implied).
- **Spring Boot application** — ticket APIs, validation, state machine, ingestion hooks, `POST /api/ai/ask` (**PDF**).
- **PostgreSQL** — durable relational data (**PDF**).
- **PgVector** — embeddings and metadata in the same DB instance (**Convention**; PDF examples PGVector or Chroma).
- **Embedding and chat models** — via **Spring AI** (**PDF**); provider **Convention**: Ollama initially via config.

---

## 7. Technology structure

### 7.1 Stack summary

| Layer | Choice | Label |
|-------|--------|-------|
| Language | Java 21 | **PDF** |
| Backend | Spring Boot 3 | **Convention** (PDF names Spring Boot, not major version) |
| Build | Maven Wrapper (`./mvnw`) | **Convention** |
| AI | Spring AI (embed, vector store, chat) | **PDF** |
| Ticket DB | PostgreSQL + Liquibase | **Convention** (PDF allows PostgreSQL/H2) |
| Vector search | PgVector extension, same instance | **Convention** (PDF examples PGVector/Chroma) |
| Models (initial) | Ollama via Spring AI properties | **Convention**; model **ids** **Open** → `rag-ingestion.md` |
| API | REST, JSON | **PDF** |
| Frontend | React + Vite + TypeScript | **Convention** (PDF: React/Next or equivalent) |
| Tests | JUnit 5, Mockito, PostgreSQL Testcontainers | **Convention** (`rules/testing.md`) |

### 7.2 Deployment topology (logical)

```text
┌─────────────────┐     ┌──────────────────────────────┐
│  Static SPA     │     │  Spring Boot JVM (monolith)   │
│  (Vite build)   │────▶│  api / service / domain /     │
│  dev: Vite proxy│     │  persistence / rag / config   │
└─────────────────┘     └───────────┬──────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    ▼                               ▼
            ┌───────────────┐               ┌───────────────┐
            │ PostgreSQL    │               │ Ollama (dev)  │
            │ + pgvector    │               │ or cloud APIs │
            └───────────────┘               └───────────────┘
```

**Convention:** Docker Compose may run PostgreSQL (+ optional Ollama) locally; not mandated by PDF.

### 7.3 Technology dependencies (allowed direction)

```text
Frontend  →  REST  →  api  →  service  →  domain
                              ↓           ↓
                         persistence   rag  →  Spring AI  →  models
                              ↓           ↓
                         PostgreSQL   PgVector (same DB)
```

No message broker, no separate RAG microservice, no BFF unless a future spec adds one.

---

## 8. Functional modules and components

**Functional modules** are implementable slices of behaviour (often map 1:1 to application services). **Components** are the main parts inside each module.

### 8.1 Ticket management module

| Component | Responsibility |
|-----------|----------------|
| **Ticket command handler** | Create ticket, update allowed fields |
| **Ticket query handler** | Get by id, paginated list |
| **Comment handler** | Append comment to ticket |
| **State transition coordinator** | Invoke state machine on status change requests |
| **Validation adapter** | Enforce Bean Validation + domain rules at boundary |

**Triggers RAG:** successful updates and comments enqueue or run **ingestion** (§15.4; timing **Open**).

### 8.2 Discovery module

| Component | Responsibility |
|-----------|----------------|
| **Keyword search** | SQL/JPQL (or agreed) search over searchable fields (**Open** OQ-14) |
| **Status filter** | Restrict list by `status` query param (**Convention** `rules/api-standards.md`) |

Distinct from **vector similarity search** (RAG only).

### 8.3 RAG module

| Component | Responsibility |
|-----------|----------------|
| **Knowledge document builder** | Assemble ticket text + metadata into ingestible documents |
| **Chunking service** | Split documents per agreed strategy (§16) |
| **Embedding port** | Call Spring AI embedding model |
| **Vector index writer** | Upsert/delete chunks in PgVector |
| **Ingestion orchestrator** | Run pipeline on create/update/close triggers |
| **Retrieval service** | Embed question, top-K, threshold filter |
| **Ask orchestrator** | Build prompt, call chat model, map citations / no-match |
| **RAG configuration** | top-K, threshold, chunk limits via properties |

### 8.4 Cross-cutting functional components

| Component | Responsibility |
|-----------|----------------|
| **API error mapper** | `@ControllerAdvice` → stable error envelope (**Convention**) |
| **Transaction boundaries** | Ticket mutations atomic; ingestion may be after-commit (**Open**) |

---

## 9. Technical components and layering

Technical layout follows **`rules/java-springboot.md`** (**Convention**).

### 9.1 Package structure

```
{root}/
  Application.java
  api/            Controllers, request/response DTOs, @ControllerAdvice
  domain/         Ticket status enum, state machine, domain exceptions
  service/        Transactional application services
  persistence/    JPA entities, Spring Data repositories
  rag/            Knowledge build, chunk/embed, retrieval, ask
  config/         Spring configuration, RAG @ConfigurationProperties, Spring AI beans
```

### 9.2 Layer responsibilities

| Layer | Responsibility |
|-------|----------------|
| **api** | HTTP mapping, `@Valid`, status codes; **no** business rules |
| **domain** | State machine, illegal transition errors; **no** Spring Web/JPA |
| **service** | Use cases, transactions, orchestrate repos + domain + RAG hooks |
| **persistence** | Load/save; **no** bypass of state machine for status |
| **rag** | Ingest and ask; **no** ticket side effects on ask path |
| **config** | Beans and property binding only |

### 9.3 Mapping: business module → technical homes

| Business module (§4.3) | Primary packages |
|------------------------|------------------|
| Ticket registry | `service`, `persistence`, `api` |
| Collaboration timeline | `service`, `persistence`, `api` |
| Work discovery | `service`, `persistence`, `api` |
| Lifecycle governance | `domain`, `service` |
| Knowledge indexing / freshness | `rag`, `service` (hooks) |
| Assisted research | `rag`, `api` (`AiAskController` or equivalent) |

---

## 10. Communication architecture

### 10.1 Style

| Aspect | Choice |
|--------|--------|
| Integration pattern | **Synchronous request/response** over HTTP |
| Payload format | **JSON** (`application/json`) |
| Client | React SPA → backend REST only (**Convention**: no BFF) |
| Real-time | **None** required (no WebSocket/SSE in PDF) |
| Inter-service | **N/A** (monolith) |

### 10.2 Communication flows

**Ticket mutation flow**

```mermaid
sequenceDiagram
  participant UI as Frontend
  participant API as REST controller
  participant SVC as Ticket service
  participant DOM as State machine
  participant DB as PostgreSQL
  participant RAG as Ingestion hook

  UI->>API: PATCH /api/v1/tickets/{id}
  API->>SVC: validated command
  SVC->>DOM: validate status if changed
  DOM-->>SVC: ok or domain error
  SVC->>DB: persist
  SVC->>RAG: schedule/re-run ingest
  API-->>UI: 200 success envelope or 409 ILLEGAL_TRANSITION
```

**Ask flow** — see `requirements.md` Flow B; matches §15.6.

### 10.3 Error propagation to UI

- API returns **error envelope** for failures (**Convention**); UI displays `error.message` and field `details` (**PDF** meaningful errors).
- Ask **no-match** is HTTP **200** with honest message inside success `data` (**Convention** `rules/api-standards.md`) — not an error envelope.

### 10.4 CORS and local dev

**Convention:** allow Vite dev origin for `/api/**`; not an assessment requirement.

### 10.5 What is not communicated

- Internal prompts, chunk text, embedding vectors, model names, and retrieval scores are **not** exposed on public APIs (**Convention**).
- No agent-to-agent or async event bus between ticket and RAG modules in the baseline design.

---

## 11. API architecture

### 11.1 API surfaces

| Surface | Purpose | PDF / Convention |
|---------|---------|------------------|
| **Ticket REST** | CRUD, comments, search, filter, status via PATCH | Capabilities **PDF**; paths **Convention** `/api/v1/tickets` |
| **Ask REST** | Natural-language Q&A | **`POST /api/ai/ask`** **PDF**; alias **`POST /api/v1/ai/ask`** **Convention** |

Detailed paths, bodies, and field catalogs → [`api-contract.md`](api-contract.md), [`rag-api-contract.md`](rag-api-contract.md). Envelopes and status codes → `rules/api-standards.md`.

### 11.2 Ticket API capability map

| Capability | Method and path (Convention) | Assessment capability |
|------------|------------------------------|------------------------|
| Create | `POST /api/v1/tickets` | Create ticket |
| List + search + filter | `GET /api/v1/tickets?page&size&sort&q&status` | List, keyword search, status filter |
| Detail | `GET /api/v1/tickets/{id}` | View details (comments embedded) |
| Update fields / status | `PATCH /api/v1/tickets/{id}` | Update title, description, priority, assignee; status transitions |
| Add comment | `POST /api/v1/tickets/{id}/comments` | Add comments |

**DELETE** and **PUT** are not used unless a future spec adds them (**Convention**).

### 11.3 Ask API

**Request (**PDF**):**

```json
{
  "question": "What caused previous payment failures?"
}
```

**Behaviour:**

- Valid question → retrieve → generate → **200** + success envelope + answer and cited ticket ids (**PDF** outcomes).
- No relevant retrieval → **200** + honest no-match in `data` (**PDF**); do **not** call LLM on empty context (§15.5).
- Invalid/missing question → **400** `VALIDATION_ERROR` (**Convention**).
- **No** side effects (create ticket, notify) (**PDF**).

Response field names inside `data` → **Open** (OQ-05, `rag-api-contract.md`).

### 11.4 Versioning

- Ticket APIs: URI version **`/api/v1`** (**Convention**).
- Ask: keep **`/api/ai/ask`** for assessment compatibility; mirror under v1 (**Convention**).

### 11.5 Success and error envelopes

All ticket endpoints use shared **`{ "data": ... }`** and list **`meta`** pagination (**Convention**). Errors use **`{ "error": { status, code, message, details, timestamp, path } }`**.

Illegal status transition → **409** `ILLEGAL_TRANSITION` (**Convention**, not PDF-mandated status).

---

## 12. Frontend architecture

**Convention:** React + Vite + TypeScript (`rules/frontend.md`). Screens → [`ui-flow.md`](ui-flow.md).

### 12.1 UI functional areas

| Area | APIs used |
|------|-----------|
| Ticket list | `GET /api/v1/tickets` with `q`, `status`, pagination |
| Ticket detail | `GET /api/v1/tickets/{id}` |
| Create / edit | `POST` / `PATCH` |
| Comments | `POST .../comments` |
| Status actions | `PATCH` with `status` (valid targets only; server enforces) |
| AI assistant | `POST /api/v1/ai/ask` (or PDF path) |

### 12.2 Client structure (logical)

| Module | Role |
|--------|------|
| **API client** | Base URL from `import.meta.env`; typed fetch wrappers |
| **Ticket pages** | List, detail, forms |
| **Ask panel** | Question input, answer, citation list, no-match state |
| **Error display** | Map API error envelope to user-visible messages |

No secrets in the frontend bundle; LLM credentials stay server-side (**PDF** NFR-06).

---

## 13. Data and persistence

### 13.1 Relational system of record

- **Tickets** and **comments** in **PostgreSQL** via Spring Data JPA (**PDF**).
- Schema changes via **Liquibase** (**Convention**); Hibernate `ddl-auto` validate/none.
- **FR-08 / AC-CORE-09:** data survives restart.

Identifiers, required fields, resolution notes → **`data-model.md`** (**Open**).

### 13.2 Synchronization with vector index

After successful ticket **update**, **comment add**, or **close** (per agreed **DEC-01**), run **re-ingestion** for that ticket so ask retrieval sees current text and metadata (**PDF** FR-14).

Execution model (inline, `@TransactionalEventListener`, async job) → **`rag-ingestion.md`** (**Open**).

### 13.3 Testing datastores

**Convention:** integration tests use PostgreSQL Testcontainers with Liquibase; not H2-by-default (`rules/testing.md`).

---

## 14. Vector database architecture

### 14.1 Product choice

**Convention:** **PgVector** on the **same PostgreSQL instance** as ticket tables (`rules/rag-vector-store.md`). PDF lists PGVector or Chroma as examples—not a mandated product.

**Rationale (architecture):**

- One operational database to backup and migrate.
- Spring AI PgVector store support aligns with Spring Boot monolith.
- Ticket authority remains relational; vectors are disposable and rebuildable.

### 14.2 Logical contents

Each **indexed unit** is a **chunk** of ticket knowledge with:

| Element | Description |
|---------|-------------|
| **Embedding vector** | Fixed dimension per chosen model (**Open** → `rag-ingestion.md`) |
| **Chunk text** | Text segment passed to LLM at ask time (or reconstructable reference) |
| **Metadata** | **PDF** keys: `ticketId`, `status`, `priority`, `assignee`, `category` |
| **Technical keys** | Chunk id, optional ingest version — **Open** in `data-model.md` / `rag-ingestion.md` |

### 14.3 Operations

| Operation | When |
|-----------|------|
| **Insert / upsert** | After ingestion pipeline for a ticket |
| **Delete / replace** | On re-ingest: remove stale chunks for that ticket (**Open** strategy: delete-all-for-ticket vs versioned) |
| **Similarity search** | On each ask: query embedding nearest neighbours with top-K |
| **Rebuild** | Optional admin/repair: re-run ingestion for all tickets from PostgreSQL |

### 14.4 Index and distance metric

Vector index type (IVFFlat, HNSW, etc.) and distance metric (cosine vs inner product) → **Open**; must align with **similarity threshold** semantics in `rag-ingestion.md`.

### 14.5 Schema ownership

Vector tables and `pgvector` extension are versioned in **Liquibase** alongside ticket tables (**Convention**). Physical table names → `data-model.md` when agreed.

### 14.6 Consistency model

| Property | Guarantee |
|----------|-----------|
| Ticket read-your-writes | Relational DB transactional |
| Search index | **Eventually consistent** with ticket DB if ingestion is async (**Open**); must converge after re-ingest |
| Ask after update | Acceptance expects retrieval can reflect new text (**AC-CORE-20**) |

---

## 15. RAG architecture

### 15.1 Pipeline overview (**PDF**)

```text
Tickets → knowledge documents → chunk → embeddings → vector store
  → user question → similarity search → relevant ticket context
  → LLM + context only → grounded answer + ticket sources
```

Single **retrieve-then-generate** path—no agent loop.

```mermaid
flowchart LR
  subgraph ingest [Ingestion path]
    T[Ticket row] --> KD[Knowledge document builder]
    KD --> CH[Chunker]
    CH --> EM[Embedding model]
    EM --> VS[(PgVector)]
  end

  subgraph ask [Ask path]
    Q[Question] --> QE[Query embedding]
    QE --> SS[Similarity search top-K threshold]
    VS --> SS
    SS -->|hits| PR[Prompt assembler]
    SS -->|no hits| NM[No-match response]
    PR --> LLM[Chat model]
    LLM --> AN[Answer plus citations]
  end
```

### 15.2 Boundaries and guardrails

| Rule | Source |
|------|--------|
| Ingest description, comments, resolution notes | **PDF** |
| Metadata on chunks | **PDF** |
| Re-ingest on update/close | **PDF** p.5; confirm close-only (**DEC-01**) |
| Configurable top-K and threshold | **PDF** |
| No LLM call when no chunk passes threshold | **Convention** + grounding |
| No tools / side effects on ask | **PDF** |
| Do not expose prompts or vectors on API | **Convention** |

### 15.3 Knowledge document construction

**Builder** (in `rag/`):

1. Load ticket + comments (+ resolution) from PostgreSQL.
2. Format human-readable sections (title, description, labeled comments, resolution).
3. Attach metadata snapshot at ingest time (`status`, `priority`, etc.).
4. Pass text to chunker.

**Open:** one composite document per ticket per ingest vs multiple documents → default recommendation: **one composite document per ticket per ingest version**, then chunk (§16).

### 15.4 Ingestion triggers

| Event | Ingest? | Notes |
|-------|---------|-------|
| Ticket created | Yes (**PDF** implied by pipeline) | Initial index |
| Field update | Yes (**PDF** updated) | |
| Comment added | Yes (ticket updated) | |
| Status → closed | Yes per p.5 (**DEC-01** vs p.6 wording) | Metadata must show `CLOSED` |

Hook placement: ticket **service** after successful commit; exact mechanism → `rag-ingestion.md`.

### 15.5 Retrieval

1. Embed **question** (same model as ingest).
2. Vector **similarity search** with **top-K** (config).
3. Drop hits below **similarity threshold** (config).
4. If none remain → return **no relevant tickets** without LLM (**PDF** grounding).
5. Else pass chunks + ticket ids into prompt.

**Optional** metadata pre-filter (e.g. high-priority only) is **not** PDF-required; needed for some illustrative questions (**Example** in requirements §4.3) — **Open** for product phase 2.

### 15.6 Generation

- Spring AI **chat client** with system instructions: answer only from excerpts; cite ticket ids; admit insufficiency.
- Map to response DTO per `rag-api-contract.md`.
- Review grounding with `commands/review-rag-output.md` (**PDF** process).

### 15.7 RAG testing and evaluation

- **Deterministic:** contract tests for validation, no-match shape, cited ids exist in DB.
- **Probabilistic:** `evaluation-strategy.md` + sample questions from PDF/requirements §4.3 — not single golden answer strings.

---

## 16. Knowledge, chunking, and embeddings

This section satisfies **PDF** acceptance **AC-CORE-19** / **FEAT-20**: documented **justification** for chunking and embedding choices. Numeric parameters and model ids → **`rag-ingestion.md`** when agreed.

### 16.1 Knowledge representation

| Concept | Definition |
|---------|------------|
| **Knowledge document** | Intermediate text assembly for one ticket at one ingest point (**PDF** glossary) |
| **Chunk** | Embedding unit derived from that document |
| **Embedding** | Dense vector stored in PgVector |

Knowledge is **derived**; authoritative text always remains in PostgreSQL.

### 16.2 Chunking strategy (justified default)

The PDF requires documenting approach; it does **not** mandate an algorithm.

**Proposed primary strategy (**Convention** pending `rag-ingestion.md` agreement): **paragraph and comment-boundary splitting**, with secondary sentence splits for oversized blocks.

| Strategy | Fit for ticket data | Role |
|----------|---------------------|------|
| **Paragraph / comment boundaries** | Descriptions and comments are naturally block-sized | **Primary** |
| **Fixed-size** | Caps very long blocks | **Secondary** when block exceeds max chars |
| **Semantic chunking** | Long essays | **Not required** initially; cost/complexity vs short tickets |

**Proposed mechanics:**

1. Split knowledge text on blank lines and **between comments** (preserve comment attribution in text).
2. If a block exceeds configurable **max-chars** (token proxy), split on sentence boundaries.
3. Optionally merge tiny adjacent blocks up to **min-chars** to avoid noise embeddings.

**Why this fits ticket data:** support threads are short-to-medium, structured as message sequences; boundary-aware splitting improves retrieval of “one comment” facts without semantic chunker infrastructure; meets **NFR-07** narrative.

**Alternatives rejected for v1 default:**

- **Fixed-size only** — risks splitting mid-comment and mixing unrelated sentences.
- **Semantic-only** — higher cost; marginal benefit until eval shows retrieval gaps.

### 16.3 Embedding model (tradeoffs)

Integration via **Spring AI**; provider swappable in config.

| Option | Cost | Latency | Quality | CI / secrets |
|--------|------|---------|---------|--------------|
| **Local** (e.g. Ollama) | No per-token cloud bill | Hardware-dependent | Adequate for many ticket texts | No API keys (**PDF** NFR-06 friendly) |
| **Cloud** hosted API | Per-token | Often stable low | Strong general retrieval | Keys via env only |

**Architecture constraints:**

- **Same embedding model** at ingest and query (or full reindex on change).
- Vector **dimension** fixed per model — schema must match (**Open**).
- **Initial provider:** Ollama (**Convention**); **model id** not fixed here.

**Final model selection and dimension** → `rag-ingestion.md` with pointer back to this justification.

### 16.4 Configuration slots (names illustrative)

```yaml
rag:
  retrieval:
    top-k: # Open numeric — FR-18
    similarity-threshold: # Open numeric — FR-18
  chunking:
    max-chars: # Open — rag-ingestion.md
    min-chars: # Open
spring.ai:
  # embedding and chat model ids — Open
```

Use `@ConfigurationProperties` — no magic numbers in Java (**PDF** intent for top-K/threshold).

---

## 17. Status state machine (placement)

States and transitions: [`state-machine.md`](state-machine.md) + `requirements.md` FEAT-11.

**Enforcement architecture:**

- State machine module in **`domain`** — pure rules.
- Invoked only from **ticket application services** on status change.
- Repositories do not expose unguarded status updates.
- Illegal transition → domain error → **409** `ILLEGAL_TRANSITION` (**Convention**).

**Open:** skipped hops (**DEC-02**); transition API (**DEC-06**); initial status on create (**DEC-07**).

---

## 18. Configuration

Externalized configuration (**PDF** NFR-06: no secrets in git).

| Area | Examples | Notes |
|------|----------|--------|
| Data source | JDBC URL, credentials | PostgreSQL |
| Spring AI | Base URLs, API keys, model ids | Server-side only |
| RAG retrieval | top-K, similarity threshold | **PDF** FR-18 |
| RAG chunking | max/min chunk size | §16.2 |
| Frontend | `VITE_*` API base URL | Names only in committed examples |

Profiles: `application.yml` + env; `.env.example` lists variable **names** only.

---

## 19. Errors and cross-cutting concerns

Align with `rules/api-standards.md` (**Convention**).

| Situation | HTTP (Convention) | UI |
|-----------|-------------------|-----|
| Validation | 400 `VALIDATION_ERROR` | Field messages |
| Not found | 404 `NOT_FOUND` | Clear message |
| Illegal transition | 409 `ILLEGAL_TRANSITION` | Server message (**PDF**) |
| Server fault | 500 generic | No stack trace |
| Ask no-match | 200 success `data` | Honest message (**PDF**) |

**Logging:** correlation-friendly; no secrets or full prompts by default.

---

## 20. Security and deployment

- **Auth:** not required by PDF — architecture neither mandates nor forbids Spring Security (**Open** DEC-12).
- **Secrets:** DB and model credentials via environment only (**PDF**).
- **Deployment:** one JVM + PostgreSQL; optional Ollama on dev host; static SPA or Vite dev server.

---

## 21. Open questions and decisions

Do not implement ambiguous behaviour until resolved in specs + `requirements.md` §10.

| ID | Topic | Owning spec |
|----|-------|-------------|
| OQ-01 / DEC-04 | Ticket id format | `data-model.md` |
| OQ-02, OQ-03, OQ-10 / DEC-03, DEC-05, DEC-13 | Fields, category, resolution notes | `data-model.md` |
| OQ-04 / DEC-14 | REST details | `api-contract.md` |
| OQ-05 / DEC-11 | Ask response schema | `rag-api-contract.md` |
| OQ-06 / DEC-12 | Authentication | This file if in scope |
| OQ-07 / DEC-09 | Store + embedding product | `rag-ingestion.md` + this file |
| OQ-08 / DEC-10 | DB roles in test vs prod | `test-strategy.md` |
| OQ-11, OQ-12 / DEC-02, DEC-06, DEC-07 | Transitions API and skipped hops | `state-machine.md`, `api-contract.md` |
| OQ-14 / DEC-08 | Keyword search scope | `api-contract.md` |
| OQ-15 / DEC-01 | Re-ingest on close only | `rag-ingestion.md` |
| — | Ingest sync vs async | `rag-ingestion.md` |
| — | Metadata-filtered retrieval | Future spec / eval |

---

## 22. Related specifications

| Spec | Contents |
|------|----------|
| [`requirements.md`](requirements.md) | PDF traceability, FEAT/AC, flows, OQ/DEC |
| [`data-model.md`](data-model.md) | Entities, fields, Liquibase |
| [`api-contract.md`](api-contract.md) | Ticket/comment REST contracts |
| [`state-machine.md`](state-machine.md) | Transitions, errors |
| [`rag-ingestion.md`](rag-ingestion.md) | Chunk numbers, models, re-ingest execution |
| [`rag-api-contract.md`](rag-api-contract.md) | Ask `data` fields, no-match |
| [`ui-flow.md`](ui-flow.md) | Screens and journeys |
| [`test-strategy.md`](test-strategy.md) | Layered tests |
| [`evaluation-strategy.md`](evaluation-strategy.md) | Retrieval quality |
| `rules/api-standards.md` | Envelopes, paths (**Convention**) |
| `rules/rag-vector-store.md` | RAG guardrails (**Convention**) |
| `rules/java-springboot.md` | Packages and layering (**Convention**) |

---

## 23. Acceptance mapping

Architecture supports verification of:

- [ ] **AC-CORE-09** — PostgreSQL system of record; vectors rebuildable from tickets
- [ ] **AC-CORE-12…14** — State machine in domain; integration tests
- [ ] **AC-CORE-15** — Embeddings with PDF metadata in vector store
- [ ] **AC-CORE-16…18** — Grounded ask, citations, honest no-match; no agent side effects
- [ ] **AC-CORE-19** — Chunking and embedding justification in **this document**
- [ ] **AC-CORE-20** — Re-ingestion after ticket text changes (per **DEC-01**)
- [ ] **AC-CORE-21** — Configurable top-K and threshold

---

## Revision history

| Date | Note |
|------|------|
| 2026-09-24 | Initial architecture draft from `requirements.md`, `docs/assessment-brief.md`, and project `rules/`. |
| 2026-09-24 | Review corrections: PgVector chosen; re-ingest execution deferred; HTTP details deferred to api-contract. |
| 2026-10-03 | Aligned with engineering rules: PDF vs project stack; Maven Wrapper, Liquibase, Ollama-as-provider, React+Vite+TS, Testcontainers. |
| 2026-10-03 | Backend package tree recorded as agreed convention (`rules/java-springboot.md`). |
| 2026-10-04 | Major expansion: business vs functional modules, ticket conceptual structure, tech and communication architecture, API map, vector DB and RAG depth, knowledge/chunking/embedding justification; synced with `requirements.md` (2026-10-04). |
