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
| Re-align docs with assignment (existing files only) | [`commands/improve-from-assessment-pdf.md`](../commands/improve-from-assessment-pdf.md) — `docs/Assessments.docx` |
| Reusable AI instructions (Cursor) | `.cursor/rules/*.mdc`, `.cursor/commands/*.md`, `.cursor/skills/` |

**Graphify:** `.cursor/rules/graphify.mdc` — run `graphify query` before broad codebase exploration when `graphify-out/` exists.

---

## 4. Spec artefacts (PDF list vs repo status)

Create specifications **before** implementation. PDF example set:

```
spec/                          # PDF lists ten names; eight files on disk (see note below)
├── requirements.md
├── architecture.md            # + UI flows (PDF ui-flow) → §12.3–§12.6
├── data-model.md
├── api-contract.md            # + ask semantics (PDF rag-api-contract) → §6.2–§6.5
├── state-machine.md
├── rag-ingestion.md
├── evaluation-strategy.md
└── test-strategy.md
```

### Present in repo (2026-10-04)

Eight standalone files under `spec/`. PDF also names `rag-api-contract.md` and `ui-flow.md` — **themes consolidated** (no separate files):

| Spec | Status | Role |
|------|--------|------|
| [`requirements.md`](../spec/requirements.md) | draft hub | PDF coverage §0.4, FEAT-01…23, flows A–E, AC-CORE-01…23, demo §8.7 (steps 1–18), OQ/DEC |
| [`architecture.md`](../spec/architecture.md) | draft | System design §16; **UI flows** PDF `ui-flow` themes → **§12.3–§12.6** |
| [`data-model.md`](../spec/data-model.md) | **agreed** | Entities, enums, Liquibase §14.5, DTOs, search scope (**DEC-08**), ticket id (**DEC-04**) |
| [`state-machine.md`](../spec/state-machine.md) | draft | T1–T5 / X1–X3; full §5.6 illegal matrix; **AC-SM-***; interim PATCH `status` |
| [`api-contract.md`](../spec/api-contract.md) | draft | Tickets/comments HTTP; ask + **§6.2–§6.5** (PDF `rag-api-contract` themes, **AC-RAG-API-***) |
| [`rag-ingestion.md`](../spec/rag-ingestion.md) | draft | Hybrid chunking; ingest; re-ingest (**DEC-01** interim); **AC-RAG-ING-*** |
| [`evaluation-strategy.md`](../spec/evaluation-strategy.md) | draft | FEAT-22 / **AC-EVAL-***; corpus §5; failures **F-01…F-10** §8 |
| [`test-strategy.md`](../spec/test-strategy.md) | draft | **§5** state machine determinism; **§6** ask Bands A–C; AC layer maps |

**PDF coverage:** theme → artefact map in [`requirements.md`](../spec/requirements.md) **§0.4**; summary table in **§13** below (open items = **DEC-*** in requirements §10.2).

[`architecture.md`](../spec/architecture.md) holds **system design** (modules, APIs, RAG narrative). Entity tables and indexes: [`data-model.md`](../spec/data-model.md). Chunking mechanics and **proposed** defaults: [`rag-ingestion.md`](../spec/rag-ingestion.md) §6–§9.3. Embedding model id, vector dimension, and top-K **values** remain **Open** (**DEC-09**, FEAT-19) until confirmed.

### Engineering steering (rules + commands)

| Layer | Location | Notes |
|-------|----------|--------|
| Rules (source of truth) | [`rules/`](../rules/) | `java-springboot`, `api-standards`, `testing`, `rag-vector-store`, `frontend`, `documentation` |
| Commands | [`commands/`](../commands/) | `review-spec`, `generate-tests`, `review-code`, `review-frontend`, `review-rag-output`, `update-prompt-history`, `improve-from-assessment-pdf` |
| Cursor pointers | `.cursor/rules/*.mdc`, `.cursor/commands/*.md` | Do not duplicate rule bodies |
| Documentation skill | [`skills/documentation/SKILL.md`](../skills/documentation/SKILL.md) | Spec template, artefact index, workflow |

---

## 5. Prompt history and AI mistake evidence

**Prompt history (PDF p.2):**

- [`.specstory/history/`](../.specstory/history/) — tracked session history (SpecStory).
- [`docs/prompt-history.md`](prompt-history.md) — index and expectations.

**AI mistake (PDF p.3, acceptance p.6):** Identify at least one **meaningful** mistake—wrong code **and/or** ungrounded or hallucinated assistant answers. Log in `docs/ai-mistakes.md` when the first entry exists (**not** in repo yet as of 2026-10-04).

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
| Runtime persistence | **PostgreSQL** + **PgVector** + **Liquibase** (not H2 for production path) |
| Frontend | **React + Vite + TypeScript** (**DEC-15** still open in requirements) |
| Initial LLM/embeddings provider | **Ollama via configuration** (model id still **Open** → **DEC-09**) |

Conventions must **not contradict** PDF requirements. **DEC-09** (vector store + embedding product) and **DEC-10** (H2 vs PostgreSQL roles) remain **Open**.

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
- **DEC-02 (interim):** Only T1–T5 edges—no skipped hops (e.g. `OPEN` → `RESOLVED`) unless **DEC-02** is revised ([`state-machine.md`](../spec/state-machine.md) §5.3).
- **DEC-06 (interim):** Status change via `PATCH` on `/api/v1/tickets/{id}` with `status` field ([`api-contract.md`](../spec/api-contract.md) §4.4).
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

**Repo note:** Acceptance checklist (p.6) says re-ingestion when **updated** only; ingestion text says **updated or closed**. Requirements track this as **DEC-01** (still **Open**; **interim (B)** in [`rag-ingestion.md`](../spec/rag-ingestion.md) §10). **DEC-05 (agreed):** `resolution_notes` column on `ticket` for RAG text. Status **history** is not ingested as text—only current metadata snapshot on chunks (§4.2 of `rag-ingestion.md`).

### Ask API (PDF p.5)

- **Path:** `POST /api/ai/ask`
- **Request (PDF example):**

```json
{
  "question": "What caused previous payment failures?"
}
```

- **Response JSON shape:** interim in [`api-contract.md`](../spec/api-contract.md) §3.5 / **§6.2–§6.5** (**AC-RAG-API-***); exact no-match wording **DEC-11** still open (§6.3).
- **Convention:** alias `POST /api/v1/ai/ask` with identical behaviour ([`api-contract.md`](../spec/api-contract.md) §6).

### Retrieval quality (PDF p.5)

- Document and **justify** chunking strategy (paragraph vs fixed-size vs semantic) for ticket data → **`architecture.md`** §16 (PDF names this file); **mechanics and comparison** → [`rag-ingestion.md`](../spec/rag-ingestion.md) §6–§9 (**hybrid** draft default).
- **top-K** and **similarity threshold** configurable, not hardcoded.
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
- [ ] Re-ingestion when ticket is **updated** (see §10 for **close** vs **DEC-01**)
- [ ] top-K and similarity threshold configurable
- [ ] No secrets committed
- [ ] At least one meaningful AI mistake caught and documented

Demo walkthrough mapping: [`requirements.md`](../spec/requirements.md) §8.7.

---

## 12. Decisions scraped from specs (2026-10-04)

### Agreed (recorded in requirements §10.2 and `data-model.md`)

| DEC | Topic | Decision |
|-----|--------|----------|
| **DEC-03** | `category` | Optional user-selected `TicketCategory` enum on create/update |
| **DEC-04** | Ticket id | Public id `TKT-{n}` from sequence (start 1001); `ticket.id` `VARCHAR(16)` PK |
| **DEC-05** | Resolution notes | Nullable `resolution_notes` on `ticket`; ingested for RAG |
| **DEC-07** | Initial status | Server default `OPEN` on create; not in create body |
| **DEC-08** | Keyword search | `q` matches `title` and `description` (case-insensitive); comments excluded |
| **DEC-13** | Create validation | Non-blank `title` required; `priority` defaults `MEDIUM`; other fields per data model §16.1 |

### Interim (implementable draft; confirm before calling “agreed”)

| DEC | Topic | Interim stance |
|-----|--------|----------------|
| **DEC-02** | Skipped status hops | Only T1–T5 (no `OPEN` → `RESOLVED`, etc.) |
| **DEC-06** | Transition API | PATCH `status` on `PATCH /api/v1/tickets/{id}` |
| **DEC-14** | Ticket REST surface | Paths/payloads in [`api-contract.md`](../spec/api-contract.md); envelopes in `rules/api-standards.md` |
| **DEC-01** (interim) | Re-ingest on close | Implement update **or** close per [`rag-ingestion.md`](../spec/rag-ingestion.md) §10 pending user sign-off |

### Still open

| DEC | Topic |
|-----|--------|
| **DEC-01** | Re-ingest on **close** vs p.6 wording (interim **(B)** in `rag-ingestion.md` — confirm for sign-off) |
| **DEC-09** | Embedding model + vector store product |
| **DEC-10** | H2 vs PostgreSQL per environment |
| **DEC-11** | Ask response JSON; no-match vs out-of-scope messaging |
| **DEC-12** | Authentication (not in PDF) |
| **DEC-15** | Frontend stack confirmation (convention: React + Vite + TS) |

### Explicitly not in the PDF

Do not add without a new agreed spec: authentication, multi-tenancy, attachments, notifications, delete-ticket API, agentic tool use, confidence scores on ask responses.

---

## 13. PDF → repo traceability

Full page-level map: [`requirements.md`](../spec/requirements.md) **§0.4**. At a glance:

| PDF pages | Captured in |
|-----------|-------------|
| 1–2 | Process, hygiene, spec list, prompt history, AI mistake, token optimisation |
| 2–3 | Stack, learning goals, application feature list |
| 4 | CRUD, search, filter, state machine, example ask questions, RAG flow diagram |
| 5 | Ingestion, metadata, re-ingest, `POST /api/ai/ask`, retrieval documentation |
| 6 | Grounding guardrails, non-agent scope, core acceptance checklist |

**Hygiene file map (PDF p.1–2):** same table as [`requirements.md`](../spec/requirements.md) **§6.8** (`rules/*`, `commands/*`, `skills/documentation/SKILL.md`).

**Outstanding PDF delivery (not missing from specs — evidence at demo time):** `docs/ai-mistakes.md` first entry (**AC-CORE-23**); user confirmation on open **DEC-*** (see §12).

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
| Ask `data` JSON, citations, no-match | [`spec/api-contract.md`](../spec/api-contract.md) §6.2–§6.5 |
| Retrieval quality eval | [`spec/evaluation-strategy.md`](../spec/evaluation-strategy.md) |
| UI screens and flows | [`spec/architecture.md`](../spec/architecture.md) §12.3–§12.6 |
| PDF theme map (detailed) | [`spec/requirements.md`](../spec/requirements.md) **§0.4** |
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
| 2026-10-04 | `commands/improve-from-assessment-pdf.md` — edit-only PDF alignment for `spec/` + steering docs. |
| 2026-10-04 | §3 hygiene table: `update-prompt-history`, `improve-from-assessment-pdf` (parity with requirements §6.8). |
| 2026-10-04 | Primary source: `docs/Assessments.docx`. |
