# Assessment brief (ATL / TL Assignment)

> **Primary source:** `docs/Assessments.docx` (6 pages, dated 2026-09-24 in file metadata).  
> **Implementable hub:** [`spec/requirements.md`](../spec/requirements.md) (PDF-faithful requirements, FEAT catalogue, AC-CORE checklist, OQ/DEC register).  
> **Status:** living summary of everything extracted and agreed in this repo through **2026-10-04**.  
> **Rule:** This brief restates and indexes; it does **not** grant permission to invent features. Behaviour not in the PDF stays **Open** until confirmed and recorded in `spec/` (see §12).

---

## 1. What is being assessed

Two layers:

1. **Process / AI engineering** — Spec-driven delivery with Cursor (or Kiro / VS Code), reusable AI instructions, prompt history, structured review (hallucination / ungrounded answers), and evidence that AI output is validated—not accepted blindly.
2. **Product** — An **AI-powered Support Ticket Management System** where the RAG assistant is designed **from the start**, not bolted on afterward.

The PDF states the application is important, but the **main assessment** is **how** you build it with AI and **how** you design and reason about the assistant (grounding, hallucination, retrieval quality vs deterministic ticket logic).

### Learning goals (PDF p.2–3)

- Analyse requirements and create specifications for a system that includes an **AI-native feature from the start**.
- Use **spec-driven development** for both conventional CRUD and a **RAG pipeline**.
- Manage AI context and validate **AI-generated code** and **AI-generated answers** (grounding, hallucination).
- Test and debug **deterministic** logic (ticket state machine) and **probabilistic** AI output (retrieval quality).

---

## 2. Required delivery workflow

```
Requirement → Specification → Plan / Tasks → Implementation → Testing → Review → Fix
```

**Do not** start by asking AI to “Build the complete application.”

Review commands in this repo map to the workflow: `commands/review-spec.md` before coding; `commands/generate-tests.md` / `commands/review-code.md` / `commands/review-frontend.md` during implementation; `commands/review-rag-output.md` for ask answers. To realign existing specs and steering docs with `docs/Assessments.docx` without adding files: `commands/improve-from-assessment-pdf.md`.

---

## 3. Hygiene artefacts (in this repository)

Regardless of IDE, the PDF requires steering files for standards and review. This project keeps **source of truth** under `rules/` and `commands/`; `.cursor/rules/*.mdc` and `.cursor/commands/*.md` are pointers only.

| PDF expectation | Location in repo |
|-----------------|------------------|
| Java Spring Boot guidelines | [`rules/java-springboot.md`](../rules/java-springboot.md) |
| Testing guidelines | [`rules/testing.md`](../rules/testing.md) |
| API standards | [`rules/api-standards.md`](../rules/api-standards.md) |
| Documentation skills | [`rules/documentation.md`](../rules/documentation.md), [`skills/documentation/SKILL.md`](../skills/documentation/SKILL.md) |
| RAG / vector store guidelines | [`rules/rag-vector-store.md`](../rules/rag-vector-store.md) |
| Review code | [`commands/review-code.md`](../commands/review-code.md) |
| Review spec | [`commands/review-spec.md`](../commands/review-spec.md) |
| Generate tests | [`commands/generate-tests.md`](../commands/generate-tests.md) |
| Review RAG output (hallucination / grounding) | [`commands/review-rag-output.md`](../commands/review-rag-output.md) |
| Review frontend | [`commands/review-frontend.md`](../commands/review-frontend.md) |
| Refresh prompt-history index | [`commands/update-prompt-history.md`](../commands/update-prompt-history.md) |
| Update AI error log | [`commands/update-ai-error.md`](../commands/update-ai-error.md) |
| Re-align docs with assignment (existing files only) | [`commands/improve-from-assessment-pdf.md`](../commands/improve-from-assessment-pdf.md) — `docs/Assessments.docx` |
| Reusable AI instructions (Cursor) | `.cursor/rules/*.mdc`, `.cursor/commands/*.md`, `.cursor/skills/` |

**Graphify:** `.cursor/rules/graphify.mdc` — run `graphify query` before broad codebase exploration when `graphify-out/` exists.

---

## 4. Spec artefacts (PDF list vs repo status)

Create specifications **before** implementation. PDF example set:

```
spec/                          # PDF lists ten names; ten files on disk (includes ui-flow.md)
├── requirements.md
├── architecture.md            # system design; UI summary §12
├── ui-flow.md                # screens, flows, AC-UI-*
├── data-model.md
├── api-contract.md            # ticket/comment REST + ask §6 summary
├── rag-api-contract.md        # PDF rag-api-contract (ask data, AC-RAG-API-*)
├── state-machine.md
├── rag-ingestion.md
├── evaluation-strategy.md
└── test-strategy.md
```

### Present in repo (2026-10-04)

Ten standalone files under `spec/`. Ten files include [`ui-flow.md`](../spec/ui-flow.md) (PDF name).

| Spec | Status | Role | §0 guide |
|------|--------|------|----------|
| [`requirements.md`](../spec/requirements.md) | draft hub | PDF **§0.4** + **§0.5** anchors; FEAT-01…23; flows A–E; AC-CORE-01…23; demo §8.7; OQ/DEC | §0.1–0.5 |
| [`architecture.md`](../spec/architecture.md) | draft | System design §16; RAG §15; frontend **§12**; verbatim PDF RAG ladder | §0 |
| [`ui-flow.md`](../spec/ui-flow.md) | draft | PDF `ui-flow`: screens, CRUD, ask UX, flows A–E, **AC-UI-*** | §0 |
| [`data-model.md`](../spec/data-model.md) | **agreed** | Entities, Liquibase §14.5, DTOs, **DEC-08** search, **DEC-04** id | §0 |
| [`state-machine.md`](../spec/state-machine.md) | draft | T1–T5 / X1–X3; §5.6 matrix; PATCH + JSON ex §6.5; **AC-SM-*** | §0 |
| [`api-contract.md`](../spec/api-contract.md) | draft | Tickets/comments HTTP; ask §6 summary | §0 |
| [`rag-api-contract.md`](../spec/rag-api-contract.md) | draft | Grounded ask, citations, no-match (**AC-RAG-API-***) | §0 |
| [`rag-ingestion.md`](../spec/rag-ingestion.md) | draft | Hybrid chunking; `application.yml` ex; re-ingest (**DEC-01**) | §0 |
| [`evaluation-strategy.md`](../spec/evaluation-strategy.md) | draft | FEAT-22 / **AC-EVAL-***; corpus §5; **F-01…F-10** | §0 |
| [`test-strategy.md`](../spec/test-strategy.md) | draft | **§5** SM; **§6** ask bands; AC maps | §0 |

**PDF coverage:** theme → artefact map in [`requirements.md`](../spec/requirements.md) **§0.4**; **§0.5** completeness checklist; page summary **§13** below. Each spec **§0.3** lists **independent reading units**; major sections are titled `· unit **ID**` for in-file search/jump (hub **§0.8**). Steering (`rules/`, `commands/`, this skill) **indexes** specs — see [`rules/documentation.md`](../rules/documentation.md) reviewer maps.

[`architecture.md`](../spec/architecture.md) holds **system design** (modules, APIs, RAG narrative). Entity tables and indexes: [`data-model.md`](../spec/data-model.md) (`vector(768)` per **DEC-09**). Chunking and retrieval **agreed defaults**: [`rag-ingestion.md`](../spec/rag-ingestion.md) §9.3, §12 (**DEC-16**). Ingest timing and failures: **DEC-18**. Ask limits: **DEC-17**. Full register: [`requirements.md`](../spec/requirements.md) §10.2 (**DEC-01…19**).

### Engineering steering (rules + commands)

| Layer | Location | Notes |
|-------|----------|--------|
| Rules (source of truth) | [`rules/`](../rules/) | `java-springboot`, `api-standards`, `testing`, `rag-vector-store`, `frontend`, `documentation` |
| Commands | [`commands/`](../commands/) | `review-spec`, `generate-tests`, `review-code`, `review-frontend`, `review-rag-output`, `update-prompt-history`, `update-ai-error`, `improve-from-assessment-pdf` |
| Cursor pointers | `.cursor/rules/*.mdc`, `.cursor/commands/*.md` | Do not duplicate rule bodies |
| Documentation skill | [`skills/documentation/SKILL.md`](../skills/documentation/SKILL.md) | Spec template, artefact index, workflow |

---

## 5. Prompt history and AI mistake evidence

**Prompt history (PDF p.2):**

- [`.specstory/history/`](../.specstory/history/) — tracked session history (SpecStory).
- [`docs/prompt-history.md`](prompt-history.md) — index and expectations.

**AI mistake (PDF p.3, acceptance p.6):** Identify at least one **meaningful** mistake—wrong code **and/or** ungrounded or hallucinated assistant answers. Canonical log: [`docs/ai-error.md`](ai-error.md) (chronological; includes how each was resolved). Alias [`docs/ai-mistakes.md`](ai-mistakes.md) for **AC-CORE-23**. Maintain with **`/update-ai-error`**.

---

## 6. Token optimisation (PDF p.2)

Use plugins such as **Graphify**, **Caveman**, **Codebase-memory MCP** to reduce token use. Use **prompt caching** for static system-prompt portions (instructions, guardrails).

---

## 7. Technology stack

### Named in the PDF (p.2–3)

| Technology | PDF |
|------------|-----|
| Java 21 | Yes |
| Spring Boot | Yes |
| Spring AI | Yes |
| PostgreSQL / H2 | Yes (roles not fully specified) |
| Embedding model | Yes (product not mandated) |
| Vector store | e.g. PGVector or Chroma |
| REST API | Yes |
| React / Next.js or equivalent frontend | Yes |
| Cursor / GitHub Copilot / Kiro | Yes (workflow tooling) |

### Project conventions (not PDF mandates — recorded in `rules/*`)

| Choice | Convention |
|--------|------------|
| Spring Boot major version | **3** |
| Build | **Maven Wrapper** (`./mvnw`) |
| Runtime persistence | **PostgreSQL** + **PgVector** + **Liquibase** (**DEC-10** — dev, runtime, Testcontainers tests; **no H2** in v1) |
| Frontend | **React + Vite + TypeScript** (**DEC-15** agreed) |
| Embeddings | **Ollama `nomic-embed-text`** on **PgVector** `vector(768)` (**DEC-09**) |
| Chat LLM | **Ollama** via Spring AI config (model id env-specific — not fixed in DEC) |

Conventions must **not contradict** PDF requirements. Stack choices above are recorded as **agreed DEC** in [`requirements.md`](../spec/requirements.md) §10.2.

---

## 8. Application capabilities (PDF p.3–4)

**Support Ticket Management System** with:

| Capability | PDF |
|------------|-----|
| Create, list, view tickets | Yes |
| Update title, description, priority, assignee | Yes |
| Add comments | Yes |
| Keyword search | Yes |
| Filter by status | Yes |
| Persist in database; survive restart | Yes |
| Backend validation | Yes |
| Meaningful UI errors | Yes |
| Natural-language Q&A over ticket history (RAG) | Yes |
| Grounded strictly in real ticket data | Yes |
| Cite specific ticket(s) used for the answer | Yes |
| Explicit indication when no relevant tickets (no fabrication) | Yes |

Detailed functional requirements, flows, and testable acceptance: [`spec/requirements.md`](../spec/requirements.md) §4 (FEAT-*) and §8 (AC-CORE-*).

---

## 9. Ticket status state machine (PDF p.4)

**Backend-enforced.** Valid paths:

```
OPEN → IN_PROGRESS → RESOLVED → CLOSED
OPEN → CANCELLED
IN_PROGRESS → CANCELLED
```

| ID | From | To |
|----|------|-----|
| T1 | `OPEN` | `IN_PROGRESS` |
| T2 | `IN_PROGRESS` | `RESOLVED` |
| T3 | `RESOLVED` | `CLOSED` |
| T4 | `OPEN` | `CANCELLED` |
| T5 | `IN_PROGRESS` | `CANCELLED` |

**Invalid examples (must reject):**

| ID | From | To |
|----|------|-----|
| X1 | `CLOSED` | `OPEN` |
| X2 | `RESOLVED` | `OPEN` |
| X3 | `CANCELLED` | `OPEN` |

**Scraped / agreed in specs:**

- **DEC-07 (agreed):** New tickets start as `OPEN`; create body does not accept `status` ([`data-model.md`](../spec/data-model.md) §5.1).
- **DEC-02 (agreed):** Only T1–T5 edges—no skipped hops ([`state-machine.md`](../spec/state-machine.md) §5.3).
- **DEC-06 (agreed):** Status change via `PATCH` on `/api/v1/tickets/{id}` with `status` field ([`api-contract.md`](../spec/api-contract.md) §4.4).
- Full **20 illegal** transition pairs documented in [`state-machine.md`](../spec/state-machine.md) §5.6; backend tests mapped in [`test-strategy.md`](../spec/test-strategy.md) **§5.4** (**AC-SM-06**).

State-machine **integration tests** are required (PDF acceptance p.6). Test mapping: **AC-CORE-14**, **FEAT-21**, [`test-strategy.md`](../spec/test-strategy.md) §5.8 / §13 (**AC-TS-03**, **AC-TS-06**).

---

## 10. RAG and assistant (PDF p.4–6)

### Basic flow (PDF)

```
Support Tickets → Knowledge Documents → Chunk → Embeddings → Vector Store
User Question → Similarity Search → Relevant Tickets → LLM + Context → Grounded Answer → Ticket Sources
```

### Ingestion (PDF p.5)

- Sources: **description**, **comments**, **resolution notes**.
- Metadata on chunks: `ticketId`, `status`, `priority`, `assignee`, `category`.
- **Re-ingest / refresh** when a ticket is **updated or closed** (do not let knowledge go stale).

**Repo note:** Acceptance checklist (p.6) vs ingestion p.5 reconciled as **DEC-01 (B)** — re-ingest on **update or close** ([`rag-ingestion.md`](../spec/rag-ingestion.md) §10). **DEC-18:** synchronous ingest after DB commit; no embeddings for empty ticket text; failures logged with recovery §10.2. **DEC-05:** optional `resolution_notes`. Status **history** is not ingested as text—metadata snapshot on chunks only.

### Ask API (PDF p.5)

- **Path:** `POST /api/ai/ask`
- **Request (PDF example):**

```json
{
  "question": "What caused previous payment failures?"
}
```

- **Response JSON shape:** [`rag-api-contract.md`](../spec/rag-api-contract.md) (**AC-RAG-API-***); **DEC-11** no-match phrase; **DEC-17** — `question` max 2000 chars, unknown properties → **400**, `citedTicketIds` in retrieval order (deduped).
- **Convention:** alias `POST /api/v1/ai/ask` with identical behaviour ([`api-contract.md`](../spec/api-contract.md) §6).

### Retrieval quality (PDF p.5)

- Document and **justify** chunking strategy (paragraph vs fixed-size vs semantic) for ticket data → **`architecture.md`** §16 (PDF names this file); **mechanics and comparison** → [`rag-ingestion.md`](../spec/rag-ingestion.md) §6–§9 (**hybrid** draft default).
- **top-K** and **similarity threshold** configurable (**DEC-16** defaults: top-k 8, threshold 0.72, cosine).
- Document and **justify** embedding model choice (e.g. local Ollama vs cloud) and cost/latency/quality tradeoffs → **`architecture.md`**.

### Grounding and guardrails (PDF p.5–6)

- Answer **only** from retrieved ticket context—no general LLM knowledge for support-specific questions.
- If no relevant tickets, say so explicitly—no plausible fabrication.
- **Single retrieval → generate** flow—not an autonomous agent. Must **not** independently create tickets, send notifications, or chain into other tools.

### Example questions (PDF p.4)

- “Have we seen payment failures before?”
- “What was the resolution for ticket TKT-1001?”
- “What are the common causes of shipment tracking issues?”
- “Show me similar resolved tickets.”
- “Which high-priority tickets are related to payment?”

Example demo corpus (not mandated by PDF): [`requirements.md`](../spec/requirements.md) §4.3.

---

## 11. Core acceptance criteria (PDF p.6)

The solution is complete when all of the following pass (expanded as **AC-CORE-*** in [`requirements.md`](../spec/requirements.md) §8):

- [ ] Ticket created from UI
- [ ] Tickets listed
- [ ] Ticket details viewed
- [ ] Ticket fields updated
- [ ] Assignee changed
- [ ] Comments added
- [ ] Search works
- [ ] Status filter works
- [ ] Valid status transitions work
- [ ] Invalid status transitions rejected by backend
- [ ] Data survives application restart
- [ ] Backend validation works
- [ ] UI shows meaningful errors
- [ ] State-machine integration tests pass
- [ ] Ticket data converted to embeddings and stored in vector store
- [ ] `POST /api/ai/ask` returns grounded, ticket-sourced answer for in-scope questions
- [ ] Response cites specific ticket ID(s) used
- [ ] Out-of-scope / no-match returns honest “no relevant tickets found” (not fabricated)
- [ ] Chunking strategy and embedding model choice documented and justified in `architecture.md`
- [ ] Re-ingestion when ticket is **updated** or **closed** (**DEC-01 (B)**)
- [ ] top-K and similarity threshold configurable
- [ ] No secrets committed
- [ ] At least one meaningful AI mistake caught and documented

Demo walkthrough mapping: [`requirements.md`](../spec/requirements.md) §8.7.

---

## 12. Decisions (authoritative: [`requirements.md`](../spec/requirements.md) §10.2)

**Status (2026-10-04):** **DEC-01…19** agreed. No blocking open rows in §10.2. Steering (`rules/`, `commands/`, this brief) indexes the hub — do not contradict it in code.

| DEC | Topic | Summary |
|-----|--------|---------|
| **DEC-01** | Re-ingest | Update **or** close (**B**) |
| **DEC-02** | Status hops | Only T1–T5 (**A**) |
| **DEC-03** | `category` | Optional enum on create/update |
| **DEC-04** | Ticket id | `TKT-{n}` from sequence (1001+) |
| **DEC-05** | Resolution notes | Optional `resolution_notes` column |
| **DEC-06** | Transitions | PATCH `status` on ticket resource |
| **DEC-07** | Initial status | `OPEN` on create (not in body) |
| **DEC-08** | Search `q` | `title` + `description` only |
| **DEC-09** | Vector + embed | PgVector + Ollama `nomic-embed-text`, **768** dims |
| **DEC-10** | Database | PostgreSQL everywhere; **no H2** in v1 |
| **DEC-11** | No-match | Honest phrase in `data.answer`; empty citations |
| **DEC-12** | Auth | None in assessment scope |
| **DEC-13** | Create fields | `title` required; defaults per data model |
| **DEC-14** | Ticket REST | [`api-contract.md`](../spec/api-contract.md) + `rules/api-standards.md` |
| **DEC-15** | Frontend | React + Vite + TypeScript |
| **DEC-16** | RAG defaults | Chunk 800/120/80 hybrid; top-k 8; threshold 0.72; cosine |
| **DEC-17** | Ask request | `question` ≤ 2000; unknown JSON → 400; citation order + dedupe |
| **DEC-18** | Ingest | Sync after commit; failure visibility; skip empty text; status-only re-ingest |
| **DEC-19** | Eval | No metadata pre-filter on ask in v1 |

### Explicitly not in the PDF (**Reference** — document only; do not work on)

These are **out of delivery scope** for the assessment. They may be mentioned in specs or reviews for clarity; **do not** implement, **do not** add to plan/tasks, and **do not** register as blocking **DEC-*** items. Full list: [`requirements.md`](../spec/requirements.md) **§2.3**.

Includes: **authentication** (none per **DEC-12**), multi-tenancy, attachments, notifications, delete-ticket API, agentic tool use, ask confidence scores, and related items in that table.

---

## 13. PDF → repo traceability

Full page-level map: [`requirements.md`](../spec/requirements.md) **§0.4**. **Completeness checklist (every assignment bullet):** **§0.5**. Each child spec adds a **§0** PDF map for its slice (see [`rules/documentation.md`](../rules/documentation.md) reviewer maps). At a glance:

| PDF pages | Captured in |
|-----------|-------------|
| 1–2 | Process, hygiene, spec list, prompt history, AI mistake, token optimisation |
| 2–3 | Stack, learning goals, application feature list |
| 4 | CRUD, search, filter, state machine, example ask questions, RAG flow diagram |
| 5 | Ingestion, metadata, re-ingest, `POST /api/ai/ask`, retrieval documentation |
| 6 | Grounding guardrails, non-agent scope, core acceptance checklist |

**Hygiene file map (PDF p.1–2):** same table as [`requirements.md`](../spec/requirements.md) **§6.8** (`rules/*`, `commands/*`, `skills/documentation/SKILL.md`).

**PDF spec filenames (p.1–2, ten names → ten repo files):** includes [`ui-flow.md`](../spec/ui-flow.md).

**Outstanding PDF delivery (not missing from specs — evidence at demo time):** [`docs/ai-error.md`](ai-error.md) (**AC-CORE-23**); local verify Ollama `nomic-embed-text` → **768** dims; runnable app + demo §8.7.

---

## 14. Where to read next

| Need | Document |
|------|----------|
| Full FEAT / AC / flows | [`spec/requirements.md`](../spec/requirements.md) |
| Tables, Liquibase, DTOs | [`spec/data-model.md`](../spec/data-model.md) |
| HTTP tickets + ask boundary | [`spec/api-contract.md`](../spec/api-contract.md) |
| Transition matrix + tests | [`spec/state-machine.md`](../spec/state-machine.md) |
| Chunking, ingest, re-ingest | [`spec/rag-ingestion.md`](../spec/rag-ingestion.md) |
| AC → backend test layers | [`spec/test-strategy.md`](../spec/test-strategy.md) |
| Ask `data` JSON, citations, no-match | [`spec/rag-api-contract.md`](../spec/rag-api-contract.md) |
| Retrieval quality eval | [`spec/evaluation-strategy.md`](../spec/evaluation-strategy.md) |
| UI screens and flows | [`spec/ui-flow.md`](../spec/ui-flow.md) |
| PDF theme map (detailed) | [`spec/requirements.md`](../spec/requirements.md) **§0.4** |
| PDF verbatim completeness checklist | [`spec/requirements.md`](../spec/requirements.md) **§0.5** |
| Per-spec PDF maps + BRF/FRI/IRI | Each `spec/*.md` **§0** |
| Jump to a reading chunk | Search `· unit **` in spec files, or hub [`requirements.md`](../spec/requirements.md) **§0.6–§0.8** |
| Modules, RAG placement, chunking narrative | [`spec/architecture.md`](../spec/architecture.md) |
| Artefact index and review workflow | [`rules/documentation.md`](../rules/documentation.md) |
| Spec writing skill | [`skills/documentation/SKILL.md`](../skills/documentation/SKILL.md) |
| REST envelopes (not duplicated in specs) | [`rules/api-standards.md`](../rules/api-standards.md) |
| Backend test conventions | [`rules/testing.md`](../rules/testing.md) |
| RAG ingest / ask grounding | [`rules/rag-vector-store.md`](../rules/rag-vector-store.md) |

---

## Revision history

| Date | Note |
|------|------|
| 2026-09-24 | Initial faithful restatement from `docs/Assessments.docx` (with `spec/requirements.md`). |
| 2026-09-24 | State-machine summary clarified; status-transition gaps aligned with requirements. |
| 2026-10-04 | `architecture.md` scope note; aligned with expanded system-design spec. |
| 2026-10-04 | **Major update:** Merged PDF text (6 pages via extraction), all five `spec/` artefacts to date, agreed **DEC-03/04/05/07/08/13**, interim **DEC-02/06/14**, open **DEC** list, repo hygiene paths, conventions vs PDF, full acceptance checklist, RAG/ingestion reconciliation note. |
| 2026-10-04 | Seven `spec/` files (added `rag-ingestion`, `test-strategy`); `rules/` + `commands/` index; AC-SM / AC-API / AC-RAG-ING pointers; hybrid chunking; links to documentation skill. |
| 2026-10-04 | PDF lists ten spec names; `evaluation-strategy` added; §0.4 / §13 traceability (later: eight files on disk — see 2026-10-04 consolidation row). |
| 2026-10-04 | `evaluation-strategy.md` detail: §5–§8 corpus, procedure, failure taxonomy; cross-links in rules/commands/test-strategy. |
| 2026-10-04 | `test-strategy.md` §5–§6: deterministic SM + retrieval/AI test bands. |
| 2026-10-04 | §13 hygiene map pointer; §6.8-aligned audit note (no new doc files). |
| 2026-10-04 | Spec audit: 8 files in `spec/`; `rag-api-contract` → `api-contract` §6; `ui-flow` → `architecture` §12; SM tests §5.4 not §6. |
| 2026-10-04 | Added [`ui-flow.md`](../spec/ui-flow.md); PDF `ui-flow` detail; nine files in `spec/`. |
| 2026-10-04 | `improve-from-assessment-pdf`: §13 ten-name → nine-file consolidation note. |
| 2026-10-04 | Added [`rag-api-contract.md`](../spec/rag-api-contract.md); ten-file spec set. |
| 2026-10-04 | `improve-from-assessment-pdf`: requirements **DEC-11** / **OQ-05** → `rag-api-contract.md`. |
| 2026-10-04 | `commands/improve-from-assessment-pdf.md` — edit-only PDF alignment for `spec/` + steering docs. |
| 2026-10-04 | §3 hygiene table: `update-prompt-history`, `improve-from-assessment-pdf` (parity with requirements §6.8). |
| 2026-10-04 | Primary source: `docs/Assessments.docx`. |
| 2026-10-04 | §4 spec table + §13: requirements §0.5 and per-spec §0 maps; steering sync with `rules/` / `commands/` / `skills/`. |
| 2026-10-04 | §14: hub §0.8 heading unit suffix; in-spec search `· unit **` for chunk jump. |
| 2026-10-04 | **DEC-01…19** agreed; §7 conventions, §10 RAG, §12 decision table; steering sync with `rules/` / `commands/` / `skills/`. |
| 2026-10-04 | AI error log [`docs/ai-error.md`](ai-error.md); `/update-ai-error`; `ai-mistakes.md` pointer. |
