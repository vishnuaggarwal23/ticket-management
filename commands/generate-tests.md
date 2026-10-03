# Generate tests

Cursor attaches this file via [`.cursor/commands/generate-tests.md`](../.cursor/commands/generate-tests.md) (pointer only). Edit **this** file; do not copy the body into `.cursor/commands/`.

Generate tests from **agreed specs** and `rules/testing.md` — not from guesswork. **Prioritize major implementations and user-visible flows**, then **edge cases**. Every proposed case must be **positive or negative** (or honest empty). Do not start with exhaustive combinatorial tests of minor helpers.

Do **not** invent endpoints, fields, transitions, or RAG shapes. If a criterion is missing or open, **stop and ask** — then generate.

### Stop if these are missing (do not invent payloads)

| Missing artefact | Action |
|------------------|--------|
| `spec/api-contract.md` / `data-model.md` for create/PATCH bodies | Catalogue tests with **TBD** fields; ask user before writing assertions on title/priority/etc. |
| `spec/rag-api-contract.md` for ask `data` | Test 400 blank question + envelope only; defer citation shape |
| `spec/state-machine.md` | Use PDF legal path + illegal reopen examples only |

## Inputs

- Specs that exist: `spec/test-strategy.md`, `spec/state-machine.md`, `spec/api-contract.md`, `spec/rag-api-contract.md`, `spec/data-model.md`, `spec/requirements.md`, `spec/evaluation-strategy.md`
- Contracts already locked in `rules/api-standards.md` (envelopes, `/api/v1`, pagination, 409)
- Testing standards: `rules/testing.md`
- Code under test (if present) — generate against **implemented** behaviour first
- Existing tests — extend; do not duplicate the same criterion

## Priority (do this order)

Write **P0** before P1/P2. Skip layers that have no implementation yet (note them; do not invent production code).

### P0 — Major implementations and flows (assessment-critical)

These are the ticket/ask **capabilities**. For each, propose both **happy path** and **failure / empty** at the layers that own the behaviour (domain, service, repository, API slice, API integration) per `rules/testing.md`.

| Flow | Positive (must) | Negative / empty (must) | Edge (prioritize) |
|------|-----------------|-------------------------|-------------------|
| Create ticket | Valid payload → persisted / 201 + `Location` + `data` | Validation / malformed JSON → 400; **no row** | Boundary lengths once spec agrees; missing required fields |
| List | Default page; `meta` present | Invalid `page`/`size`/`sort`/`status` → 400 | Last page; `size` 1 and 100; `size` 0 and 101 |
| Search `q` | Keyword hits | No hits → **200** empty `data`, not 404 | Case / partial match only if spec defines it |
| Filter `status` | Matching status | Invalid enum → 400 | `q` **and** `status` (AND) |
| Get detail | Found → 200 `data` | Unknown id → 404 | |
| PATCH fields | Title, description, priority, assignee updated | 400 validation; 404 | Partial body (only agreed fields) |
| Status transition | Each **legal** edge persisted | Assessment-illegal reopen (`CLOSED`/`RESOLVED`/`CANCELLED` → `OPEN`) and other illegal pairs per state-machine spec → domain reject + API **409**; **row unchanged** | Same-status no-op only if spec defines it |
| Comments | Add + return on detail | Empty comment; ticket not found | Comment on a status the spec forbids |
| Persistence / restart | GET after context reload still correct | | |
| Ask | `POST /api/ai/ask` **and** `/api/v1/ai/ask`; grounded `data` + citations from retrieval | Blank/missing `question` → 400; **no relevant tickets** → 200 + no-match **in `data`**, no fabricated ids | Do **not** golden-string the generated prose |

### P1 — Contract and orchestration (still major)

- Success/error **envelopes** (`data` / `error.*`); 500 does not leak SQL/stack
- Ingest port called after successful **update** and **close**; **not** called when the write fails
- Knowledge text built from description, comments, resolution notes (unit)
- `sort` whitelist vs unknown property

### P2 — After P0/P1 are listed

- Parameterized remaining illegal transition pairs
- Extra validation combinations already named in the spec
- Do **not** generate tests for open items (chunk size, embedding model, numeric K/threshold, unagreed fields)

## Positive, negative, and edges

Every P0/P1 case in the proposal table must include:

- **Polarity:** `positive` | `negative` | `empty`
- **Edge?** yes/no — edges are boundaries and combinations of **major** flows (empty list, max `size`, illegal transition after persist), not trivia (`toString`)
- **Layer:** domain | service | controller-slice | repository | API-integration | rag-unit
- **Acceptance criterion** (spec heading or `rules/testing.md` theme)

Do not emit a 200-case matrix of every DTO getter. Prefer `@ParameterizedTest` for closed sets (illegal transitions, required-field names).

## Layers (required for implemented code)

Follow `rules/testing.md`. A flow is not “covered” by one happy-path API test.

| Layer | Tools | Notes |
|-------|--------|--------|
| Domain / state machine | JUnit 5, real types | All legal edges (P0); illegal set parameterized |
| Service | JUnit 5, Mockito ports | Orchestration; ingest; no `save` of illegal status |
| Controller slice | MockMvc, mock service | Mapping, `@Valid`, envelopes — not JPQL |
| Repository | Testcontainers Postgres + Liquibase | Search/filter/find; empty `Optional`; no mocked DB |
| API integration | HTTP + Testcontainers | Persistence through `/api/v1`; 409 leaves row unchanged |
| RAG | Mockito embed/generate | Citations from retrieval; no-match; **no** real Ollama |

**Frontend tests: skip.** Do not propose or write React/Vite/component/e2e tests. UI is reviewed with `commands/review-frontend.md` only.

**RAG retrieval quality:** do not add golden-answer tests. Use `commands/review-rag-output.md` (**Retrieval quality** section) + `spec/evaluation-strategy.md` for seeded eval questions.

## Tooling (conventions, not PDF)

- JUnit 5 only; Mockito for unit doubles; Maven Wrapper `./mvnw test`
- No H2 default; no Compose-as-test-DB; no paid/local Ollama in the default suite
- Names describe behaviour (`illegalTransitionFromClosedToOpen_isRejected`)

## Workflow (mandatory)

1. **Scope** — List implemented packages/endpoints and which spec files exist. Mark P0 gaps.
2. **Catalogue** — Table of proposed tests: id, flow, polarity, edge?, layer, criterion, notes. **P0 first.**
3. **Confirm** — Stop if specs conflict, fields are open, or a major flow has no agreed contract. Ask the user.
4. **Write** — Only after the catalogue is clear (or the user said to proceed). Match existing test packages; smallest change.
5. **Report** — What was generated, what was deferred (P2 / missing impl), what was **not** run.

If production code is missing, prefer **failing tests** that encode P0 criteria — or wait if the user prefers stubs later.

## Do not

- Do not invent API or RAG JSON not in specs/`rules/api-standards.md`
- Do not treat RAG quality as a single expected answer string
- Do not generate tests for unapproved libraries or CI
- Do not skip negative paths on a P0 flow you did test positively
- Do not generate frontend tests
- Do not implement product code unless the user asked
