# Generate tests

Cursor attaches this file via [`.cursor/commands/generate-tests.md`](../.cursor/commands/generate-tests.md) (pointer only). Edit **this** file; do not copy the body into `.cursor/commands/`.

Generate tests from **agreed specs** and `rules/testing.md` — not from guesswork. **Prioritize major implementations and user-visible flows**, then **edge cases**. Every proposed case must be **positive or negative** (or honest empty). Do not start with exhaustive combinatorial tests of minor helpers.

Do **not** invent endpoints, fields, transitions, or RAG shapes. If a criterion is missing or open, **stop and ask** — then generate.

### Stop if these are missing (do not invent payloads)

| Missing artefact | Action |
|------------------|--------|
| [`spec/data-model.md`](../spec/data-model.md) missing | Stop — do not invent ticket fields |
| Ask `data` shape unclear vs [`spec/rag-api-contract.md`](../spec/rag-api-contract.md) | Align with `answer` + `citedTicketIds` (**DEC-11** agreed) |

## Inputs

- Specs: all ten PDF-listed files under `spec/` (see [`rules/documentation.md`](../rules/documentation.md)); each child spec **§0** for domain scope; hub **§0.4–§0.8**; use **`##` heading unit tags** (`· unit **TS-B**`) to locate test targets quickly. All ten `spec/*.md` files are **agreed** (2026-10-04; **DEC-01…20**).
- **[`spec/test-strategy.md`](../spec/test-strategy.md)** — primary map for **AC-SM-***, **AC-API-***, **AC-DM-***, **AC-CORE-*** layers. **[`spec/requirements.md`](../spec/requirements.md)** — **`AC-FEAT-*`** (§4.2) and FR traceability (§9). **DEC-10:** Postgres Testcontainers only (no H2). **DEC-17:** ask validation tests. Do not invent tests for **Reference** §2.3 features.
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
| Create ticket | Valid payload → persisted / 201 + `Location` + `data`; id matches `TKT-{n}` | Blank **title** → 400; malformed JSON → 400; **no row** | `@Size` boundaries per `data-model.md` §16 |
| List | Default page; `meta` present | Invalid `page`/`size`/`sort`/`status` → 400 | Last page; `size` 1 and 100; `size` 0 and 101 |
| Search `q` | Keyword in **title or description** hits (DEC-08) | No hits → **200** empty `data`, not 404; keyword only in comment does **not** hit | Case-insensitive contains |
| Filter `status` | Matching status | Invalid enum → 400 | `q` **and** `status` (AND) |
| Get detail | Found → 200 `data` | Unknown id → 404 | |
| PATCH fields | Title, description, priority, assignee updated | 400 validation; 404 | Partial body (only agreed fields) |
| Status transition | Each **legal** edge T1–T5 persisted (**AC-SM-01**, **AC-API-09**) | **AC-SM-06** all 20 §5.6 pairs + X1–X3; **AC-SM-07** self-transition; API **409** + row unchanged (**AC-API-04**) | **AC-SM-08** PATCH without `status`; see [`spec/test-strategy.md`](../spec/test-strategy.md) **§5** |
| Comments | Add + return on detail | Empty comment; ticket not found | Comment on a status the spec forbids |
| Persistence / restart | GET after context reload still correct | | |
| Ask | **Band A** ([`spec/test-strategy.md`](../spec/test-strategy.md) **§6.2**): both ask paths; stubbed citations | Blank `question` → 400; empty retrieval → 200 no-match in `data` | **Band C** manual only (**§6.4**); no golden prose |
| RAG ingest | Knowledge doc + hybrid chunking per [`spec/rag-ingestion.md`](../spec/rag-ingestion.md) §14 (**AC-RAG-ING-***) | Re-ingest replaces rows; comment boundaries preserved | Mock embed in default suite |

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
- **Acceptance criterion** — `AC-CORE-*` / `AC-FEAT-*` from [`spec/requirements.md`](../spec/requirements.md), or spec heading, or `rules/testing.md` theme

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

**RAG / ask:** JUnit = **Band A** only ([`spec/test-strategy.md`](../spec/test-strategy.md) **§6.2**). **Band B** grounding + **Band C** retrieval: [`evaluation-strategy.md`](../spec/evaluation-strategy.md) **§5.2–§8** + `commands/review-rag-output.md`. **AC-TS-05**, **AC-TS-06** (state machine §5).

## Tooling (conventions, not PDF)

- JUnit 5 only; Mockito for unit doubles; Maven Wrapper `./mvnw test`
- No H2 default; no Compose-as-test-DB; no paid/local Ollama in the default suite
- Names describe behaviour (`illegalTransitionFromClosedToOpen_isRejected`)

## Workflow (mandatory)

1. **Scope** — List implemented packages/endpoints and which spec files exist. Mark P0 gaps.
2. **Catalogue** — Table of proposed tests: id, flow, polarity, edge?, layer, **AC-CORE / AC-FEAT** (or criterion), notes. **P0 first.**
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

---

## Revision history

| Date | Note |
|------|------|
| 2026-09-24 | Initial backend test-generation workflow and `rules/testing.md` checklist. |
| 2026-10-03 | Aligned with stack and spec-driven acceptance mapping. |
| 2026-10-04 | Expanded AC traceability and negative-path requirements for P0 flows. |
| 2026-10-04 | Synced with expanded [`spec/requirements.md`](../spec/requirements.md) and governance pass. |
| 2026-10-04 | Added revision history section. |
| 2026-10-04 | P0 assertions use agreed [`spec/data-model.md`](../spec/data-model.md) (id, title required, DEC-08 search). |
| 2026-10-04 | Inputs limited to three present specs + documentation interim map. |
| 2026-10-04 | [`spec/state-machine.md`](../spec/state-machine.md) drives transition test matrix. |
| 2026-10-04 | HTTP contract tests use [`spec/api-contract.md`](../spec/api-contract.md) (**agreed**). |
| 2026-10-04 | Hygiene: inputs list ten **agreed** specs; **DEC-01…20**. |
| 2026-10-04 | Inputs: [`spec/test-strategy.md`](../spec/test-strategy.md), [`spec/rag-ingestion.md`](../spec/rag-ingestion.md). |
| 2026-10-04 | RAG retrieval quality → [`evaluation-strategy.md`](../spec/evaluation-strategy.md) §5.2–§8; **AC-TS-05**. |
| 2026-10-04 | P0 maps to [`test-strategy.md`](../spec/test-strategy.md) **§5** (SM) and **§6** (ask bands). |
| 2026-10-04 | Inputs: hub **§0.4–§0.8**; child spec **§0**; heading unit tags for test targeting. |
| 2026-10-04 | No tests for **Reference** §2.3; **DEC-11** agreed ask shape. |
