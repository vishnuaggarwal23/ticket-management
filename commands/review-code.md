# Review code

Cursor attaches this file via [`.cursor/commands/review-code.md`](../.cursor/commands/review-code.md) (pointer only). Edit **this** file; do not copy the body into `.cursor/commands/`.

Review the current change set (or specified paths) as a senior Java / Spring Boot tech lead.

Do **not** apply fixes unless the user asks. Confirm before editing.

**Quick routing**

| Diff contains | Primary command |
|---------------|----------------|
| `src/main/java`, `src/test/java`, backend resources | This file |
| `frontend/`, `src/**/*.tsx`, Vite app | `commands/review-frontend.md` (no UI tests required) |
| Only `spec/` | `commands/review-spec.md` |

## Inputs

- Diff / named paths
- Agreed specs under `spec/` (do not invent missing contracts); layout reference: `spec/architecture.md` §8–9 when present
- Rules: `rules/java-springboot.md`, `rules/api-standards.md`, `rules/testing.md`, `rules/rag-vector-store.md`
- If the diff is **UI**: follow [`commands/review-frontend.md`](review-frontend.md) instead of (or in addition to) this backend checklist. Do not require frontend tests.
- Assessment PDF only as background — do not treat conventions as PDF requirements

Mark each item **Pass** / **Fail** / **N/A**. Failures need file references and severity: **blocker** / **major** / **minor**.

---

## 1. Code validity (correctness vs specs and rules)

### Spec and scope

- [ ] Implements only **agreed** specs; no extra product features (auth, agents, attachments, bulk ops, rerankers) unless a spec agrees
- [ ] No silent answers to open questions — check `spec/requirements.md` **§10.1 (OQ-*)** and **§10.2 (DEC-*)**; do not implement unresolved **DEC-*** as fixed behaviour (ticket id format, chunking, embedding model, numeric top-K/threshold, ask `data` fields, skipped transitions, re-ingest on close, etc.)
- [ ] Domain status machine matches `spec/state-machine.md` (or `spec/requirements.md` FEAT-11 / §2.6 until that spec exists)
- [ ] Illegal transitions rejected in **domain**, not only by hiding UI actions
- [ ] Assessment-invalid reopens rejected (`CLOSED`/`RESOLVED`/`CANCELLED` → `OPEN`)

### API (`rules/api-standards.md`)

- [ ] Ticket URLs under `/api/v1`; **PATCH** for field/status updates; no **PUT**
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
- [ ] Entities match changelog; no ad-hoc status `UPDATE` that bypasses the state machine
- [ ] `jakarta.*` not `javax.*`; constructor injection; no field `@Autowired`
- [ ] No secrets, machine-specific URLs, or hardcoded RAG model/K/threshold/chunk size in Java
- [ ] RAG provider (Ollama) only via configuration

### RAG (`rules/rag-vector-store.md`) — if ingest/ask touched

- [ ] Knowledge text only from description, comments, resolution notes
- [ ] Metadata limited to assessment/data-model fields (`ticketId`, `status`, `priority`, `assignee`, `category` as agreed)
- [ ] Re-ingest triggered on update and close (mechanism not invented if spec still open)
- [ ] Retrieve-then-generate once; no tools, ticket create, or notify from ask
- [ ] Empty/below-threshold retrieval does not call the LLM to invent an answer
- [ ] Citations are ticket IDs from **retrieval**, not model-guessed ids
- [ ] Chunking algorithm, model name, dimension, numeric K/threshold **not** invented in code

### Frontend

- [ ] If UI files changed, run **`commands/review-frontend.md`** (React/Vite/TypeScript, envelopes, ask display). **No frontend tests.**

---

## 2. Code structure (layering and design)

- [ ] Matches agreed **functional → technical** map in `spec/architecture.md` §8–9 when that spec exists (ticket ops in `service`/`persistence`, ask in `rag/`, state machine in `domain`)
- [ ] Single Spring Boot app; packages by layer: `api`, `domain`, `service`, `persistence`, `rag`, `config` (`rules/java-springboot.md`)
- [ ] Ticket mutations trigger RAG ingest via **service** hook/port after successful write — not from controller or repository directly (`spec/architecture.md` §10.2, §15.4)
- [ ] Controllers thin: HTTP → `@Valid` DTO → one service call → envelope; no `@Transactional` on controllers; no transition tables in controllers
- [ ] Domain has no Spring Web/JPA; status is an **enum**, not `String`
- [ ] Services own use cases and transactions; depend on repository **interfaces**
- [ ] Repositories do not encode the state machine
- [ ] Ask orchestration in `rag/` plus a thin `api` controller — not a second service
- [ ] One `@RestControllerAdvice` for the error envelope; no per-controller error JSON
- [ ] Names readable; records for API DTOs; no Lombok/MapStruct/QueryDSL unless a spec agrees
- [ ] No Spring Security unless an agreed spec

---

## 3. Code quality (hygiene, tests, AI mistakes)

### Quality

- [ ] No `null` from public application APIs (`Optional` / empty collections)
- [ ] No SQL/JPQL string concatenation of user input; `sort` is allowlisted
- [ ] No logging of secrets, prompts, or full model output by default
- [ ] No stack traces, SQL, or Hibernate state in HTTP bodies
- [ ] Methods stay focused; no god services that both mutate tickets and inline the transition table
- [ ] `spring.jpa.open-in-view` not relied on; no OSIV lazy loads in controllers

### Tests (`rules/testing.md`)

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

- [ ] Types, annotations, and Spring APIs exist on the agreed stack (Boot 3, Jakarta, no hallucinated libraries)
- [ ] No copied copyrighted dumps; no credentials in the diff
- [ ] Meaningful AI mistakes noted for `docs/ai-mistakes.md` (propose; do not write unless asked)

---

## Output

- **Findings** — severity, file, failed checklist item, brief fix (do not implement unless asked)
- **Spec gaps** — code without spec, or spec without code
- **Open questions the code assumed** — must confirm with the user
- **Ready to merge?** yes / no
