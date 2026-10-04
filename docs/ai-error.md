# AI error log

Chronological record of **meaningful** AI mistakes caught in this project (wrong code, wrong specs/docs, or ungrounded / hallucinated answers). Required by the assessment (**AC-CORE-23** / **FEAT-23**): do not accept AI output blindly.

**Canonical file:** this document (`docs/ai-error.md`).  
**PDF path alias:** [`docs/ai-mistakes.md`](ai-mistakes.md) is a pointer here (demo step 15 still works).  
**Maintain:** run **`/update-ai-error`** ([`commands/update-ai-error.md`](../commands/update-ai-error.md)) when a new error is caught.

Entries are **oldest first**. Stable ids (`AE-NNN`) are never reused. Sort by **When** (UTC), then id.

| Kind | Meaning |
|------|---------|
| **spec / docs** | Wrong artefact vs PDF, agreed spec, or another file |
| **process** | Wrong workflow, index, or steering (rules/commands) |
| **code** | Wrong Java / Spring / tests |
| **RAG** | Ungrounded ask answer, fake citations, or invented retrieval |

**As of 2026-10-04:** no **RAG** ungrounded-answer entry yet (ask path not delivered). Do not invent one.

## Index

| Id | When (UTC) | Kind | Title |
|----|------------|------|-------|
| [AE-001](#ae-001) | 2026-10-03 17:18 | process | Cursor documentation rule duplicated the full body |
| [AE-002](#ae-002) | 2026-10-03 20:10 | spec / docs | State-machine draft cited resolved **OQ-13** |
| [AE-003](#ae-003) | 2026-10-04 07:40 | spec / docs | UI spec named `ui-model.md` instead of PDF `ui-flow.md` |
| [AE-004](#ae-004) | 2026-10-04 07:40 | process | Prompt-history index: wrong session order and missing rows |
| [AE-005](#ae-005) | 2026-10-04 08:30 | spec / docs | `"URGENT"` treated as invalid priority |
| [AE-006](#ae-006) | 2026-10-04 08:30 | spec / docs | Project conventions described as PDF requirements |
| [AE-007](#ae-007) | 2026-10-04 08:51 | spec / docs | Stale “interim” / async ingest after **DEC-06** / **DEC-18** |
| [AE-008](#ae-008) | 2026-10-04 08:51 | spec / docs | Glossary still pointed at **OQ-05** after **DEC-11** |
| [AE-009](#ae-009) | 2026-10-04 10:50 | code | Phase B review: page size, field `EntityManager`, silent 500 |
| [AE-010](#ae-010) | 2026-10-04 11:56 | code | `TicketService` dual constructors failed Spring boot |
| [AE-011](#ae-011) | 2026-10-04 13:15 | code | `DisabledEmbeddingPort` `@ConditionalOnMissingBean` left no `EmbeddingPort` |
| [AE-012](#ae-012) | 2026-10-04 15:33 | code | `OllamaAiConfig` fallback won over live Ollama embeddings at runtime |
| [AE-013](#ae-013) | 2026-10-04 15:51 | process | Created `frontend/` scaffold without user authorization |
| [AE-014](#ae-014) | 2026-10-04 16:00 | spec / docs | Stale draft / SPA / interim labels after agreed specs and **DEC-20** |
| [AE-015](#ae-015) | 2026-10-04 17:33 | code | Ask via Next rewrites hit ~30s proxy timeout (opaque 500 on :3000) |

---

## AE-001

- **When:** 2026-10-03 17:18 UTC
- **Kind:** process
- **What was wrong:** `.cursor/rules/documentation.mdc` contained a full copy of `rules/documentation.md`. Other rules used pointer-only `.mdc` files. Two bodies would drift.
- **How detected:** Gap analysis while expanding `rules/testing.md` ([`.specstory/history/2026-10-03_17-18-27Z-testing-rules-for-cursor.md`](../.specstory/history/2026-10-03_17-18-27Z-testing-rules-for-cursor.md)).
- **How resolved:** Replaced the `.mdc` with a pointer to [`rules/documentation.md`](../rules/documentation.md). Bodies stay in `rules/`; `.cursor/rules/*.mdc` stay pointers.

## AE-002

- **When:** 2026-10-03 20:10 UTC
- **Kind:** spec / docs
- **What was wrong:** Draft state-machine text referenced **OQ-13**, already resolved in the agreed data model (ticket id / sequence). Wrong open-question id.
- **How detected:** While drafting [`spec/state-machine.md`](../spec/state-machine.md) ([`.specstory/history/2026-10-03_20-10-18Z-state-machine-specification.md`](../.specstory/history/2026-10-03_20-10-18Z-state-machine-specification.md)).
- **How resolved:** Dropped the stale **OQ-13** cite. Remaining SM open items mapped to the correct **OQ-*** / **DEC-*** owners; `state-machine.md` became the transition-table source of truth.

## AE-003

- **When:** 2026-10-04 07:40 UTC
- **Kind:** spec / docs
- **What was wrong:** The PDF lists the UI spec as **`ui-flow`**. The assistant created `spec/ui-model.md` and linked the ten-file set to that name.
- **How detected:** Spec completeness vs `docs/Assessments.docx` (filename list on p.1–2); follow-on `/review-spec` and user sign-off to rename.
- **How resolved:** Renamed to [`spec/ui-flow.md`](../spec/ui-flow.md) and retargeted `spec/`, `rules/`, `commands/`, and `docs/` links (hub revision 2026-10-04).

## AE-004

- **When:** 2026-10-04 07:40 UTC
- **Kind:** process
- **What was wrong:** [`docs/prompt-history.md`](prompt-history.md) listed 17 of 19 SpecStory files. Two sessions share `2026-09-24_17-36-50Z`; the index put `lets-go-to-the` before `architecture-md-specification`, against lexicographic tie-break.
- **How detected:** `/update-prompt-history` vs files on disk ([`.specstory/history/2026-10-04_07-40-06Z-ui-model-specification.md`](../.specstory/history/2026-10-04_07-40-06Z-ui-model-specification.md)).
- **How resolved:** Rebuilt the chronological table from `.specstory/history/`. Command [`commands/update-prompt-history.md`](../commands/update-prompt-history.md) requires oldest-first sort and filename lexicographic tie-break; re-runs update existing rows.

## AE-005

- **When:** 2026-10-04 08:30 UTC
- **Kind:** spec / docs
- **What was wrong:** [`spec/api-contract.md`](../spec/api-contract.md) showed `"priority": "URGENT"` as an **invalid** create example. [`spec/data-model.md`](../spec/data-model.md) §5.2 and UI copy treated **URGENT** / **CRITICAL** as agreed catalog values (**DEC-13** / later **C-02**).
- **How detected:** `/review-spec` consistency pass ([`.specstory/history/2026-10-04_08-30-53Z-spec-review-gaps.md`](../.specstory/history/2026-10-04_08-30-53Z-spec-review-gaps.md)).
- **How resolved:** Domain enum is **`CRITICAL`** (not `URGENT`). JSON `"URGENT"` maps to `CRITICAL` in the **api** layer only. Invalid example is a true unknown (e.g. `P1`). Locked as **C-02** in `api-contract.md` and `rules/java-springboot.md`.

## AE-006

- **When:** 2026-10-04 08:30 UTC (pattern called out earlier; corrected in steering on this date)
- **Kind:** spec / docs
- **What was wrong:** Assistant text treated **Spring Boot 3**, Maven Wrapper, Liquibase, PgVector-over-Chroma, and the `api`/`domain`/`service` package tree as **PDF requirements**. The assignment names Java 21, Spring Boot, Spring AI, PostgreSQL/H2, and a vector store — not those conventions.
- **How detected:** Working-tree vs PDF audit and later `/review-spec` / review-code bar ([`.specstory/history/2026-10-03_15-47-01Z-working-directory-audit.md`](../.specstory/history/2026-10-03_15-47-01Z-working-directory-audit.md)).
- **How resolved:** “Assessment vs project conventions” sections in `rules/*`. [`commands/review-code.md`](../commands/review-code.md) **Fails** convention violations but must not label them PDF mandates.

## AE-007

- **When:** 2026-10-04 08:51 UTC
- **Kind:** spec / docs
- **What was wrong:** After **DEC-06** (PATCH `status`) and **DEC-18** (sync ingest after commit), child specs still said **interim** or implied **async** ingest / open DEC-06.
- **How detected:** Second `/review-spec` after DEC-09…19 ([`.specstory/history/2026-10-04_08-51-55Z-spec-review-gaps.md`](../.specstory/history/2026-10-04_08-51-55Z-spec-review-gaps.md)).
- **How resolved:** Scrubbed stale labels in `api-contract.md`, `state-machine.md`, `architecture.md`, `data-model.md`, and the UI spec. Hub §10.2 lists **DEC-01…20** as agreed.

## AE-008

- **When:** 2026-10-04 08:51 UTC
- **Kind:** spec / docs
- **What was wrong:** Hub glossary still cited **OQ-05** for no-match after **DEC-11** (honest no-match in `data.answer`) was agreed; owner is [`spec/rag-api-contract.md`](../spec/rag-api-contract.md).
- **How detected:** Same `/review-spec` pass as AE-007.
- **How resolved:** Glossary and OQ catalogue point at **DEC-11** / `rag-api-contract.md`, not a still-open **OQ-05**.

## AE-009

- **When:** 2026-10-04 10:50 UTC
- **Kind:** code
- **What was wrong:** Phase B list `size` used `@RequestParam(defaultValue = "20")` instead of `ApiProperties.pageSizeDefault`. `TicketRepositoryCustomImpl` field-injected `EntityManager` with `@PersistenceContext`. `RestExceptionHandler.handleUnexpected` returned a generic 500 with no error log.
- **How detected:** `/review-code` on the Phase B diff ([`.specstory/history/2026-10-04_09-57-27Z-plan-md-phases.md`](../.specstory/history/2026-10-04_09-57-27Z-plan-md-phases.md)).
- **How resolved:** Omitted `size` falls back to `pageSizeDefault`; constructor-inject `EntityManager`; log unexpected errors with path and ticket id. Covered by slice/unit tests. Commit `637662f`.

## AE-010

- **When:** 2026-10-04 11:56 UTC
- **Kind:** code
- **What was wrong:** Phase C added a package-private four-arg `TicketService` constructor for tests without `@Autowired` on the three-arg production constructor. Spring did not select it and failed with “No default constructor found”.
- **How detected:** `./mvnw test` — `ApplicationSmokeTest.contextLoadsAndRelationalSchemaIsApplied` (`IllegalStateException` / `BeanInstantiationException`).
- **How resolved:** Marked the three-arg constructor `@Autowired` (delegates to `new TicketStatusMachine()`). Context load and SM tests green. Commit `f74517a`. Follow-up in `9614178`: `TicketDomainConfig` exposes `TicketStatusMachine` as a bean and `TicketService` uses one constructor (no `@Autowired`).

## AE-011

- **When:** 2026-10-04 13:15 UTC
- **Kind:** code
- **What was wrong:** Phase D put `@ConditionalOnMissingBean(EmbeddingPort)` on `DisabledEmbeddingPort`. With Spring AI wiring, no `EmbeddingPort` bean was registered and the context failed (`NoSuchBeanDefinitionException` for `TicketIngestionService`).
- **How detected:** `./mvnw test` — `ApplicationSmokeTest` / full suite during Phase D ingest work ([`.specstory/history/2026-10-04_09-57-27Z-plan-md-phases.md`](../.specstory/history/2026-10-04_09-57-27Z-plan-md-phases.md)).
- **How resolved:** Dropped the misplaced condition on the disabled fallback; `OllamaAiConfig` registers live ports when models exist, and `@ConditionalOnMissingBean` on the disabled beans keeps tests on hash/stub doubles. `./mvnw test` green before RAG commit `9614178`. Post–Boot 4 runtime regression → **AE-012**.

## AE-012

- **When:** 2026-10-04 15:33 UTC
- **Kind:** code
- **What was wrong:** After the Java 25 / Boot 4 upgrade, `OllamaAiConfig` still used `@ConditionalOnMissingBean(EmbeddingPort)` (and the same pattern for generation). That registered `DisabledEmbeddingPort` before Spring AI’s `EmbeddingModel` existed, so `--debug` showed `OllamaEmbeddingAutoConfiguration` matched but `POST /api/ai/ask` returned **500** from the disabled stub. `application.yml` also used Spring AI 1.x `spring.ai.ollama.embedding.options.model` instead of `embedding.model`. A follow-up fix misused `ObjectProvider.ifAvailable(...)` (void) and briefly broke context creation.
- **How detected:** HTTP sanity on a running app with Ollama up ([`.specstory/history/2026-10-04_14-33-33Z-backend-stack-upgrade-plan.md`](../.specstory/history/2026-10-04_14-33-33Z-backend-stack-upgrade-plan.md)); server log `IllegalStateException: Embeddings are not configured…`.
- **How resolved:** `@Lazy` `@Bean` factories in `OllamaAiConfig` that choose `OllamaEmbeddingPort` / `OllamaGenerationPort` via `ObjectProvider.getIfAvailable()` else disabled stubs; plain classes for disabled ports; corrected embedding property path. `./mvnw test` green; ask **200** in sanity. Commit `f37984c`.

## AE-013

- **When:** 2026-10-04 15:51 UTC
- **Kind:** process
- **What was wrong:** While updating steering for **DEC-15** (React + Next.js + Vite + JavaScript), the assistant added a `frontend/` tree (`.nvmrc`, `package.json`, `.env.example`) and implied implementation. The user had asked only to change relevant files, not to scaffold or build the UI.
- **How detected:** User message: do not create the `frontend/` directory or implement anything there ([`.specstory/history/2026-10-04_15-51-31Z-frontend-tech-stack-update.md`](../.specstory/history/2026-10-04_15-51-31Z-frontend-tech-stack-update.md)).
- **How resolved:** Deleted the scaffold; updated [`rules/frontend.md`](../rules/frontend.md) and related specs to document the stack without assuming a committed `frontend/` tree until explicitly authorized. Backend CORS for dev ports kept as optional prep.

## AE-014

- **When:** 2026-10-04 16:00 UTC
- **Kind:** spec / docs
- **What was wrong:** After specs were **agreed** and **DEC-09…19** closed, several steering files still said “draft spec”, static SPA-only frontend, or interim stack copy (`commands/review-code.md`, `generate-tests.md`, `rules/java-springboot.md`, `spec/architecture.md` §18/§21, `docs/assessment-brief.md`, etc.). That contradicted **DEC-15**, **DEC-20**, and hub §10.2 “no blocking open”.
- **How detected:** Frontend readiness audit vs `docs/Assessments.docx`; user asked to close open/draft items, then a hygiene pass ([`.specstory/history/2026-10-04_15-51-31Z-frontend-tech-stack-update.md`](../.specstory/history/2026-10-04_15-51-31Z-frontend-tech-stack-update.md)).
- **How resolved:** Added **DEC-20** (routes/UX); synced `spec/ui-flow.md`, `spec/requirements.md`, `spec/architecture.md`, `rules/`, `commands/`, `docs/assessment-brief.md`, and skills; removed stale draft/SPA/interim wording. `grep` hygiene on `commands/` + `rules/` for `draft \`spec/` → no matches.

## AE-015

- **When:** 2026-10-04 17:33 UTC
- **Kind:** code
- **What was wrong:** The **UI-A** scaffold and dev docs treated Next.js `/api` **rewrites** as the normal browser path when `NEXT_PUBLIC_API_BASE_URL` is unset. Local **Ask** via Ollama often exceeds Next’s rewrite proxy limit (~**30s**), so `POST localhost:3000/api/v1/ai/ask` returned a plain **500** while the same call on **8080** succeeded.
- **How detected:** User Ask failure and `curl` through port **3000** during the Ollama model-name investigation ([`.specstory/history/2026-10-04_17-33-12Z-model-not-found-error.md`](../.specstory/history/2026-10-04_17-33-12Z-model-not-found-error.md)).
- **How resolved:** Recommended direct `:8080` in `frontend/.env.example` and README; clearer timeout hint in `AskPanel.jsx`; local `.env.local` with `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080`. Commit `c0fdc69`.

---

## Revision history

| Date | Note |
|------|------|
| 2026-10-04 | Initial chronological log AE-001…008 from SpecStory and spec/review fixes; no RAG ungrounded-answer row yet. |
| 2026-10-04 | Added AE-009…010 (2 new); skipped 0 duplicates; total 10 entries. |
| 2026-10-04 | Added AE-011 (1 new); extended AE-010 resolution; skipped 0 duplicates; total 11 entries. |
| 2026-10-04 | Added AE-012 (1 new); extended AE-011 resolution; skipped 0 duplicates; total 12 entries. |
| 2026-10-04 | Added AE-013…014 (2 new); skipped 0 duplicates; total 14 entries. |
| 2026-10-04 | Added AE-015 (1 new); skipped 0 duplicates; total 15 entries. |
