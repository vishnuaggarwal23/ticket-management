# Test strategy — acceptance mapping and layers

> **Status:** draft (2026-10-04) — maps **AC-CORE-***, **AC-SM-***, **AC-API-***, and **AC-DM-*** to backend test layers. Tooling and layer rules: `rules/testing.md` (JUnit 5, Mockito, Testcontainers, no frontend tests this milestone).  
> **Primary source:** [`requirements.md`](requirements.md) §8–§9, Flows A–E, FEAT-21.  
> **Contracts:** [`api-contract.md`](api-contract.md) (HTTP payloads, §2.11 catalog), [`state-machine.md`](state-machine.md) (T1–T5, §5.6 invalid register), [`data-model.md`](data-model.md) §16–§18.

---

## 1. Problem and context

The assessment requires proof of **deterministic** ticket behaviour (CRUD, validation, persistence, state machine) and **honest** ask behaviour (citations, no-match), plus **state-machine integration tests** (**PDF**, AC-CORE-14). This spec answers: **which acceptance IDs are proven by which test kind**, so `commands/generate-tests.md` and implementation do not guess coverage.

**Not in scope here:** RAG retrieval **quality** scoring (see draft [`evaluation-strategy.md`](evaluation-strategy.md)), UI automation (Vitest/Playwright), CI pipelines, coverage percentages.

---

## 2. Scope and non-goals

| In scope | Out of scope |
|----------|----------------|
| AC-CORE-01…23 → backend test layers | Frontend component/E2E tests |
| AC-SM-01…08, AC-API-01…09, AC-DM-01…08 | Golden LLM answer strings |
| Deterministic vs probabilistic split | Paid/real Ollama in default `./mvnw test` |
| Substitution for AC-CORE-11 (UI errors) | Inventing endpoints beyond [`api-contract.md`](api-contract.md) §2.11 |

---

## 3. Test kinds (summary)

Authoritative detail: `rules/testing.md`.

| Kind | Proves | Tools |
|------|--------|-------|
| **Domain unit** | State machine T1–T5, all illegal pairs (§5.6), domain exceptions | JUnit 5, real `TicketStatus`; **no** Spring, **no** Mockito on machine |
| **Service unit** | Orchestration, ingest port calls, illegal transition before `save` | JUnit 5, Mockito repositories/ports |
| **Controller slice** | Envelopes, `@Valid`, advice mapping, query binding | MockMvc + mock service |
| **Repository integration** | SQL, search `q` (DEC-08), filters, comments order | Testcontainers PostgreSQL + Liquibase |
| **API integration** | HTTP + DB: 201/`Location`, `meta`, **409** + row unchanged, restart | `@SpringBootTest` + MockMvc or `TestRestTemplate` + Testcontainers |

Both **API slice** and **API integration** are required for implemented endpoints (`rules/testing.md` §API testing).

---

## 4. Deterministic vs probabilistic

| Area | Nature | How to prove |
|------|--------|----------------|
| Tickets, comments, list/search/filter, validation, transitions | **Deterministic** | Fixed assertions on HTTP status, envelope, DB row, `status` enum |
| Ask: blank `question`, envelope shape, cited ids exist | **Deterministic** | API tests with stubbed embed/generate |
| Ask: answer prose wording, retrieval ranking | **Probabilistic** | `commands/review-rag-output.md` + [`evaluation-strategy.md`](evaluation-strategy.md) — **not** single golden string in unit tests |
| architecture.md chunking/model narrative | **Document review** | AC-CORE-19 — not a JUnit suite |

---

## 5. AC-CORE → primary proof (backend)

Use this table when naming tests and in PR checklists. UI-only rows use **API integration substitute** (§9).

| AC-CORE | Theme | Primary test layers |
|---------|-------|---------------------|
| **01–06** | CRUD + comments | API integration; service + repository; manual UI optional |
| **07–08** | Search + status filter | API integration + repository (`q` title/description only) |
| **09** | Restart survival | API integration nested context reload (`rules/testing.md`) |
| **10** | Validation | API slice + integration; AC-API-03, negative §4.1 |
| **11** | Meaningful UI errors | **Substitute:** API returns `error.message` / `details` (409, 400); `review-frontend` on UI |
| **12** | Valid transitions T1–T5 | Domain **AC-SM-01**; API integration **AC-API-09**; AC-CORE-12 |
| **13** | Invalid transitions | Domain **AC-SM-02–03, 06–07**; API **AC-API-04, 09**; X1–X3 minimum |
| **14** | State-machine integration tests | API integration suite green for T1–T5 + illegal paths (**FEAT-21**) |
| **15, 20** | Ingest / re-ingest | Service unit + optional vector integration when RAG schema exists |
| **16–18** | Ask grounded / citations / no-match | API integration with doubles; review for wording |
| **19** | architecture.md justification | Spec/review — not automated here |
| **21** | Configurable K/threshold | Spring context test when RAG config bound — defer until `rag-ingestion.md` |
| **22–23** | Hygiene | Manual / repo policy |

---

## 6. State machine acceptance → tests (**AC-SM-***)

Source: [`state-machine.md`](state-machine.md) §5, §6.1.1, §9.

| ID | Criterion | Domain unit | Service unit | API integration | Notes |
|----|-----------|-------------|--------------|-----------------|-------|
| **AC-SM-01** | T1–T5: PATCH `status` → **200**, DB = target | `@ParameterizedTest` 5 rows | Each edge once with mock repo | **Required** per edge | Setup ticket in **from** state |
| **AC-SM-02** | X1–X3 → **409**, DB unchanged | 3 cases | Exception before save | **Required** | Flow C |
| **AC-SM-03** | All **Invalid** in §5.4 master table | Prefer merge with §5.6 set | — | Spot-check or parameterized subset | Same as **AC-SM-06** when full §5.6 covered |
| **AC-SM-04** | Create without `status` → `OPEN` | N/A | Defaults on create | POST create **AC-API-03** | DEC-07 |
| **AC-SM-05** | Domain without Spring; integration T1–T5 + X1–X3 | **Required** | — | **Required** minimum | AC-FEAT-11-05, AC-CORE-14 |
| **AC-SM-06** | All **20** rows §5.6 → **409**, DB unchanged | `@ParameterizedTest` 20 pairs | Optional duplicate | **Required** `@ParameterizedTest` recommended | Primary proof for full illegal matrix |
| **AC-SM-07** | `status` == current → **409** | Self-transition rows | — | One case per state (5) or parameterized | §6.1.1 |
| **AC-SM-08** | PATCH without `status` updates fields only | N/A | Merge logic | PATCH title only; assert `status` unchanged | §6.1.1 |

**Suggested parameterized source for AC-SM-06:** static table mirroring [`state-machine.md`](state-machine.md) §5.6 columns `From`, `To` (20 rows). Test name pattern: `illegalTransition_from{FROM}_to{TO}_returns409AndLeavesDbUnchanged`.

**Maps to requirements:** AC-CORE-12, AC-CORE-13, AC-CORE-14, FEAT-11, FEAT-21.

---

## 7. HTTP API contract acceptance → tests (**AC-API-***)

Source: [`api-contract.md`](api-contract.md) §2.11, §4–§6.

| ID | Criterion | API slice | API integration | Key assertions |
|----|-----------|-----------|-----------------|----------------|
| **AC-API-01** | Success/error envelopes | **All** endpoints | At least one happy path each | `data` vs `error`; list has `meta` |
| **AC-API-02** | List `meta.totalElements` | Query binding | Seed rows; filter `q` + `status` | AND semantics |
| **AC-API-03** | Create rejects `status` | POST 400 | No row inserted | `details.field` = `status` |
| **AC-API-04** | Illegal PATCH `status` → **409**, DB unchanged | Optional mock service | **Required** | Reload row after 409 |
| **AC-API-05** | Comment **201** + `Comment` in `data` | Yes | Yes + detail GET | `Location` header |
| **AC-API-06** | Ask blank → **400**; no-match → **200** empty citations | Both ask paths | Stub retrieval empty | No `error` on no-match |
| **AC-API-07** | `/api/ai/ask` ≡ `/api/v1/ai/ask` | Same test logic, two URIs | Same | |
| **AC-API-08** | §2.11 catalog: each row’s success HTTP + `data` type | Per endpoint | Per endpoint | See catalog table below |
| **AC-API-09** | §4.4.1 T1–T5 succeed; §5.6 examples **409** | PATCH mapping | **Required** lifecycle + illegal table | Overlaps **AC-SM-01**, **AC-SM-06** |

### 7.1 AC-API-08 — one integration test theme per catalog row

| # | Method | Path | Success HTTP | Assert on `data` |
|---|--------|------|--------------|------------------|
| 1 | POST | `/api/v1/tickets` | **201** | `TicketDetail`; `status`=`OPEN`; `comments`=[] |
| 2 | GET | `/api/v1/tickets` | **200** | Array of summaries; `meta` present |
| 3 | GET | `/api/v1/tickets/{id}` | **200** | `TicketDetail` with `comments` |
| 4 | PATCH | `/api/v1/tickets/{id}` | **200** | `TicketDetail` after field update |
| 5 | POST | `/api/v1/tickets/{id}/comments` | **201** | `Comment` object (not full ticket) |
| 6 | POST | `/api/ai/ask` | **200** | `answer` + `citedTicketIds` (§3.5) |
| 7 | POST | `/api/v1/ai/ask` | **200** | Same as row 6 |

Negative paths for each endpoint remain in [`api-contract.md`](api-contract.md) §4.1–§6.1 scenario tables (400/404/409).

---

## 8. Data model acceptance → tests (**AC-DM-***)

| ID | Primary layer |
|----|----------------|
| **AC-DM-01** | API integration create + repository read |
| **AC-DM-02** | API integration comment + FK |
| **AC-DM-03** | RAG integration when chunks exist |
| **AC-DM-04** | Service/RAG integration after PATCH |
| **AC-DM-05** | Repository + API list `q` |
| **AC-DM-06** | Ask API with seeded ids |
| **AC-DM-07** | Same as AC-CORE-09 |
| **AC-DM-08** | Liquibase migration test optional |

Detail: [`data-model.md`](data-model.md) §18.

---

## 9. Frontend acceptance without UI tests

**PDF** expects UI create and readable errors (**AC-CORE-01**, **AC-CORE-11**). This milestone **does not** add frontend test frameworks.

| PDF expectation | Backend substitute |
|-----------------|-------------------|
| Create from UI | `POST /api/v1/tickets` integration (**AC-API-08** row 1) |
| List/detail/update | Matching GET/PATCH integration rows |
| Transition errors in UI | **409** body with `ILLEGAL_TRANSITION` message (**AC-API-04**, Flow C) |
| Validation errors in UI | **400** `VALIDATION_ERROR` + `details` |

Manual demo: [`requirements.md`](requirements.md) §8.7 / Flow A–C. UI diffs: `commands/review-frontend.md`.

---

## 10. FEAT-21 — state-machine integration suite (sign-off)

Green `./mvnw test` MUST include an integration suite that collectively proves:

1. **AC-FEAT-21-01** — all T1–T5 via HTTP PATCH (**AC-SM-01**, **AC-API-09**).
2. **AC-FEAT-21-02** — X1–X3 via HTTP (**AC-SM-02**).
3. **AC-FEAT-21-03** — same suite runs locally without Compose-only DB.

**Recommended minimum for AC-CORE-14:** parameterized **AC-SM-06** (20 illegal pairs) + happy path OPEN→CLOSED + field-only PATCH (**AC-SM-08**).

---

## 11. Open decisions affecting tests

| ID | Impact on tests |
|----|-----------------|
| **DEC-02** | Implement illegal set for option **(A)** only — full §5.6 register |
| **DEC-06** | PATCH `status` only — no `/transition` route tests |
| **DEC-10** (OQ-08) | Postgres-only vs H2 — follow `architecture.md` when closed; default per `rules/testing.md` is Testcontainers PostgreSQL |
| **DEC-11** | Ask `data` / no-match wording — [`rag-api-contract.md`](rag-api-contract.md) (draft) + [`api-contract.md`](api-contract.md) §3.5 / §6 |
| Chunking defaults §9.3 | [`rag-ingestion.md`](rag-ingestion.md) — unit tests for comment boundaries + overflow |

---

## 12. Acceptance criteria (this spec)

| ID | Criterion |
|----|-----------|
| **AC-TS-01** | Every **AC-SM-01…08** row has at least one implemented test in the listed layer when that layer exists in the codebase. |
| **AC-TS-02** | Every **AC-API-01…09** row has slice and/or integration coverage per §7. |
| **AC-TS-03** | `./mvnw test` proves AC-CORE-12…14 without manual DB steps. |
| **AC-TS-04** | No default-suite test calls real Ollama or paid LLM APIs. |

---

## 13. Revision history

| Date | Note |
|------|------|
| 2026-10-04 | Initial strategy: AC-CORE map, AC-SM-01…08, AC-API-01…09 (incl. §2.11 / §4.4.1), FEAT-21, UI substitute, AC-TS-*. |
