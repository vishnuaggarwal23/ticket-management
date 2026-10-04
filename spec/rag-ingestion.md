# RAG ingestion — knowledge build, chunking, embed, refresh

> **Status:** draft (2026-10-04) — chunking **recommendation** and **proposed** numeric defaults for implementation; **DEC-01** (re-ingest on close) and **DEC-09** (embedding product/model id) remain **Open** with interim stance below.  
> **Primary source:** `docs/Assessments.docx` (restated in [`requirements.md`](requirements.md) FEAT-12…14, §11.1; [`docs/assessment-brief.md`](../docs/assessment-brief.md)).  
> **Related:** Justification narrative → [`architecture.md`](architecture.md) §15–§16; persistence → [`data-model.md`](data-model.md) §8–§11; grounding rules → `rules/rag-vector-store.md`; ask HTTP + response semantics → [`rag-api-contract.md`](rag-api-contract.md).

**Label legend:** **PDF** | **Convention** | **Agreed** | **Proposed** (implementation default until user confirms) | **Example**

---

## Table of contents

0. [Document guide](#0-document-guide)  
1. [Problem and context](#1-problem-and-context)  
2. [Scope and non-goals](#2-scope-and-non-goals)  
3. [Requirements traceability](#3-requirements-traceability)  
4. [Ticket text sources · **ING-A**, **ING-B**](#4-ticket-text-sources-what-is-ingested--units-ing-a-ing-b)  
5. [Knowledge document · **ING-C**](#5-knowledge-document-pre-chunk--unit-ing-c)  
6. [Paragraph-based chunking · **ING-D**](#6-paragraph-based-chunking--unit-ing-d)  
7. [Fixed-size chunking · **ING-E**](#7-fixed-size-chunking--unit-ing-e)  
8. [Comparison · **ING-F**](#8-comparison--paragraph-vs-fixed-size-vs-ticket-fields--unit-ing-f)  
9. [Recommendation · **ING-G**](#9-recommendation-project-default--unit-ing-g)  
10. [Ingestion triggers · **ING-H**](#10-ingestion-triggers-and-freshness-dec-01--unit-ing-h)  
11. [Storage mechanics · **ING-I**](#11-storage-and-re-ingest-mechanics--unit-ing-i)  
12. [Embedding and retrieval config · **ING-J**](#12-embedding-model-and-vector-store-dec-09--unit-ing-j)  
13. [Implementation placement · **ING-J**](#13-implementation-placement--unit-ing-j)  
14. [Acceptance criteria · **ING-K**](#14-acceptance-criteria-testable--unit-ing-k)  
15. [Open questions](#15-open-questions-and-decisions)  
16. [Revision history](#16-revision-history)  

---

## 0. Document guide

### 0.1 Audience

| Reader | Use this file for |
|--------|-------------------|
| Backend implementer | What to ingest, how to chunk, when to re-index, config property names |
| Reviewer / grader | **AC-CORE-19**, **AC-CORE-20**, hybrid chunking justification vs [`architecture.md`](architecture.md) §16 |
| AI assistant | Do not invent semantic chunking or new metadata keys without **PDF** or **DEC** |

### 0.2 PDF coverage map (ingestion themes only)

| **PDF** text (p.5) | Section |
|--------------------|---------|
| Convert description, comments, resolution notes into searchable knowledge | §4, §5 |
| Metadata: `ticketId`, `status`, `priority`, `assignee`, `category` | §4.3 |
| Re-ingest / refresh when ticket **updated or closed** | §10 (**DEC-01** vs p.6) |
| Chunking strategy (paragraph vs fixed vs semantic) for ticket data | §6–§9 |
| top-K and similarity threshold configurable (ask path) | §12.1 |
| Embedding model choice documented (Ollama vs cloud tradeoffs) | §12; narrative §16 in architecture |

### 0.3 Business, functional, and implementation requirements

**Business requirements (**PDF**)** — why ingestion exists:

- Support agents ask questions over **historical** ticket text; answers must reflect **current** ticket content after edits and closure, not stale embeddings.
- Ticket narrative (description, thread, resolution) is the **only** approved knowledge source for the assistant—not generic LLM training data.

**Functional requirements**

| ID | Requirement | Proof |
|----|-------------|-------|
| FR-ING-01 | Build one knowledge document per ticket from description, all comments (ordered), resolution notes | **AC-RAG-ING-01…03** |
| FR-ING-02 | Attach PDF metadata snapshot on every chunk | **AC-RAG-ING-04** |
| FR-ING-03 | Re-run ingest on create, update, comment add, status change (incl. close) | §10, **AC-RAG-ING-06…07** |
| FR-ING-04 | Replace all vector rows for a ticket on re-ingest (no orphan chunks) | §11, **AC-RAG-ING-06** |
| FR-ING-05 | Use same embedding model for ingest and ask query | §12 |

**Implementation requirements**

| ID | Requirement | Notes |
|----|-------------|-------|
| IR-ING-01 | `KnowledgeDocumentBuilder` + `TicketChunker` + `TicketIngestionService` | §13 |
| IR-ING-02 | Hook from ticket **service** after successful DB commit | §10, [`architecture.md`](architecture.md) §15.4 |
| IR-ING-03 | `@ConfigurationProperties` for `rag.chunking.*` and `rag.retrieval.*` — no hardcoded K/threshold in Java | §9.3, §12.1 (**PDF**) |
| IR-ING-04 | Liquibase `vector(n)` dimension matches chosen model (**DEC-09**) | [`data-model.md`](data-model.md) §8.2 |
| IR-ING-05 | Deterministic chunk order for repeatable tests | §11 step 2 |

### 0.4 Independent reading units

| Unit | Section | Standalone? | Read first (this file) | Delivers | See also |
|------|---------|-------------|------------------------|----------|----------|
| **ING-A** | §4.1–§4.3 | Yes | — | What text + metadata keys ingest (**PDF** p.5) | [`data-model.md`](data-model.md) §11 |
| **ING-B** | §4.2 not ingested | Yes | **ING-A** | What is **not** in vectors (history, audit) | — |
| **ING-C** | §5 + §5.1 example | Yes | **ING-A** | `KnowledgeDocument` assembly + sample text | §6 |
| **ING-D** | §6 Paragraph chunking | Yes | **ING-C** | Comment-boundary rules | §8 compare |
| **ING-E** | §7 Fixed-size overflow | Yes | **ING-D** | When/how overflow splits | §9 hybrid |
| **ING-F** | §8 Comparison table | Yes | **ING-D**, **ING-E** | Paragraph vs fixed for ticket shapes | [`architecture.md`](architecture.md) §16 |
| **ING-G** | §9 Hybrid + yaml | Yes | **ING-F** | Default strategy + config snippet | **DEC-09** Open |
| **ING-H** | §10 Triggers | Yes | **ING-A** | When to re-ingest (**DEC-01**) | [`requirements.md`](requirements.md) §11.1 |
| **ING-I** | §11 Storage | Yes | **ING-C** | DELETE + INSERT per ticket | [`data-model.md`](data-model.md) §8.4 |
| **ING-J** | §12–§13 | Yes | **ING-G** | Model/store + Java placement | `rules/rag-vector-store.md` |
| **ING-K** | §14 AC-RAG-ING | Yes | **ING-A…H** | Testable ingest AC | [`test-strategy.md`](test-strategy.md) |

**PDF verbatim (p.5 ingestion):** “Convert ticket information (description, comments, resolution notes) into searchable knowledge documents. Include metadata: ticketId, status, priority, assignee, category. Re-ingest / refresh embeddings when a ticket is updated or closed.”

---

## 1. Problem and context

The assessment requires ticket **description**, **comments**, and **resolution-related text** to become **knowledge documents**, then **chunked**, **embedded**, and stored in a **vector store** with metadata (`ticketId`, `status`, `priority`, `assignee`, `category`). Embeddings must be **refreshed** when tickets change so ask does not search stale text (**PDF** p.5). The PDF also requires **documenting and justifying** chunking and embedding choices (**AC-CORE-19**, FEAT-20) — strategy narrative in [`architecture.md`](architecture.md) §16; **this file** locks ingest mechanics, source inclusion, chunk algorithms, and **proposed** numeric settings.

---

## 2. Scope and non-goals

| In scope | Out of scope |
|----------|----------------|
| What text is assembled into `KnowledgeDocument` | Prompt templates (internal only) |
| Paragraph vs fixed-size chunking; **hybrid** recommendation | Semantic / LLM-based chunking (v1) |
| Re-ingest triggers and delete-and-replace storage | Status **history** table or transition **audit** text |
| Metadata snapshot on each chunk | Public API exposure of chunks or models |
| Config property **names**; **proposed** default numbers | Retrieval-quality eval procedure ([`evaluation-strategy.md`](evaluation-strategy.md) §4.4 — not golden LLM answers) |

---

## 3. Requirements traceability

| PDF / requirements | This spec |
|--------------------|-----------|
| FEAT-12 — knowledge from ticket text | §4, §5 |
| FEAT-13 — chunk, embed, metadata | §6–§9, §11 |
| FEAT-14 — re-ingest on update/close | §10 (**DEC-01**) |
| FEAT-19 — configurable top-K, threshold | §12 (retrieval; ingest does not set K) |
| FEAT-20 / AC-CORE-19 — justified chunking | §7–§9; cross-ref architecture §16 |
| Flow D ([`requirements.md`](requirements.md)) | §10 |

---

## 4. Ticket text sources (what is ingested) · units **ING-A**, **ING-B**

### 4.1 In scope for embedding (**PDF** + **Agreed**)

| Source | Relational origin | Included in `assembledText`? | Chunking role |
|--------|-------------------|------------------------------|---------------|
| **Description** | `ticket.description` | Yes — `Description:` section | Often 1–3 paragraphs; may need overflow split |
| **Comments** | `ticket_comment.body` (ordered by `created_at` asc) | Yes — `Comments:` list with timestamp prefix | **One logical block per comment** (boundary) |
| **Resolution notes** | `ticket.resolution_notes` (**DEC-05**) | Yes — `Resolution:` section (or `(none)`) | Usually one short block |

Assembly template: [`data-model.md`](data-model.md) §9.1. **Convention:** include **title** and a one-line **status / priority / assignee / category** header in `assembledText` to help keyword overlap in embeddings (title is **not** a separate PDF ingest source).

### 4.2 Explicitly not ingested as narrative text

| Candidate | In this project? | Rationale |
|-----------|------------------|-----------|
| **Title** | Metadata + header line only | PDF ingest list is description, comments, resolution; title aids retrieval via header (**Convention**) |
| **Status transitions / history** | **No** transition log | No status-history entity ([`data-model.md`](data-model.md) §2.2). Lifecycle is **current** `status` only, copied into **chunk metadata** at ingest time |
| **Assignee / priority / category changes over time** | **No** event stream | Only **snapshot** at ingest in metadata JSON |
| **Keyword search index** | Separate concern | `q` on `title` + `description` only (**DEC-08**); comments are RAG sources but not list-search fields |
| **State machine audit** | N/A | Transitions enforced in domain; not text for chunking |

**Implication:** Questions like “when did this ticket move to RESOLVED?” are **not** answerable from ingested text unless that fact appears in comments or resolution notes. Current **status** in metadata supports filtering/display, not timeline reconstruction.

### 4.3 Metadata on every chunk (**PDF**)

Each stored chunk carries the same snapshot ([`data-model.md`](data-model.md) §11):

| Key | Source at ingest |
|-----|------------------|
| `ticketId` | Public id `TKT-{n}` |
| `status` | `ticket.status` |
| `priority` | `ticket.priority` |
| `assignee` | `ticket.assignee` |
| `category` | `ticket.category` |

Technical keys (`chunkIndex`, `ingestedAt`) per data model. Re-ingest after status → `CLOSED` must persist `status: "CLOSED"` in metadata (**AC-FEAT-14-03**).

---

## 5. Knowledge document (pre-chunk) · unit **ING-C**

**One composite `KnowledgeDocument` per ticket per ingest run** ([`architecture.md`](architecture.md) §15.3) — then chunk → embed → write rows.

**Pipeline:**

```text
PostgreSQL (ticket + comments)
  → KnowledgeDocumentBuilder → assembledText + metadataSnapshot
  → Chunker (§8)
  → Embedder (same model as ask query)
  → DELETE existing chunks for ticket_id → INSERT new rows (§11)
```

### 5.1 Example `assembledText` (**Example** — not mandated seed)

Illustrates §4 assembly before chunking (truncated comment for width):

```text
Title: Payment declined at checkout
Status: RESOLVED | Priority: HIGH | Assignee: jane.doe | Category: BILLING

Description:
Customer reported card declined with processor code 05 at checkout.

Comments:
- [2026-01-02T10:00:00Z] Verified expiry date; asked customer to update wallet.
- [2026-01-02T14:30:00Z] Customer confirmed retry succeeded.

Resolution:
Updated card on file; payment captured on second attempt.
```

**Chunking outcome (hybrid):** typically one block for description, **one chunk candidate per comment line**, one for resolution — metadata on each chunk repeats §4.3 keys from the ticket row at ingest time.

---

## 6. Paragraph-based chunking · unit **ING-D**

### 6.1 Definition

**Paragraph-based chunking** splits `assembledText` on **structure-aware boundaries** before any character-count limit:

1. **Section boundaries** — after the header block, treat `Description:`, `Comments:`, and `Resolution:` as separate regions.
2. **Comment boundaries** — each comment line item (`- [timestamp] body`) is its own **atomic block**; never merge two comments into one block in the primary pass.
3. **Paragraph boundaries** — within description or resolution, split on **blank lines** (`\n\s*\n`).
4. **Oversized blocks** — if a single block still exceeds **max-chars** (§9.3), apply **secondary fixed-size splitting** (§7) on that block only.

### 6.2 Behaviour per ticket source

| Source | Typical shape | Paragraph strategy |
|--------|---------------|-------------------|
| **Description** | Few paragraphs or one long block | Split on blank lines; long single paragraph → sentence-aware overflow (§7.2) |
| **Comments** | Many short messages | **One chunk candidate per comment**; preserves “who said what when” in the prefixed line |
| **Resolution notes** | Short summary | Usually one block; split on blank lines if agent wrote multiple paragraphs |

### 6.3 Strengths for ticket data

- Aligns with **PDF** intent: comments and resolution are **distinct** narrative units agents cite in answers.
- Avoids splitting mid-comment — critical for “what did we tell the customer on Tuesday?” style questions.
- Low complexity — no extra ML chunker (**architecture.md** §16.2).

### 6.4 Weaknesses

- Very long descriptions without paragraph breaks produce **one huge block** → must rely on fixed-size overflow.
- Extremely short comments (e.g. “ok”) may produce **tiny** embeddings → mitigated by **min-chars** merge (§9.3) **within the same section only**, never across comments.

---

## 7. Fixed-size chunking · unit **ING-E**

### 7.1 Definition

**Fixed-size chunking** splits text into segments of at most **max-chars** (character count as token proxy), optionally with **overlap** characters repeated at segment boundaries so sentences split across chunks retain partial context.

### 7.2 Mechanics (**Proposed** when used as overflow)

Apply only to a **single block** that still exceeds `max-chars` after §6:

1. Prefer split on **sentence boundaries** (`. `, `.\n`, `? `, `! `) working backward from `max-chars`.
2. If no sentence boundary in the second half of the window, hard split at `max-chars`.
3. **Overlap:** carry trailing `overlap-chars` of chunk *n* into the start of chunk *n+1* (same block only).

### 7.3 Fixed-size-only ingest (not recommended)

Running **fixed-size-only** on the full `assembledText` would:

- Cut across **comment** boundaries and mix agent notes with customer text in one chunk.
- Split **resolution** away from the comment that motivated it if they fall in the same character window.
- Still work for **search** on homogeneous long descriptions, but **hurts** citation clarity for thread-style tickets.

### 7.4 Strengths

- Predictable chunk count and embedding size for **very long** descriptions or pasted logs.
- Simple to test (deterministic splits given `max-chars` / `overlap-chars`).

### 7.5 Weaknesses

- Mid-sentence fragments reduce retrieval precision for short questions.
- Ignores ticket **structure** (comments, resolution sections).

---

## 8. Comparison — paragraph vs fixed-size vs ticket fields · unit **ING-F**

| Dimension | Paragraph / comment-boundary (§6) | Fixed-size only (§7.3) | Fixed-size as **overflow** (§7.2) |
|-----------|-------------------------------------|-------------------------|-----------------------------------|
| **Description** (short) | Usually 1 chunk | May split unnecessarily | Same as paragraph |
| **Description** (long, no newlines) | 1 block → overflow split | Many mid-text chunks | Sentence-biased splits |
| **Comments** (many short) | 1 block per comment | Often **merges** multiple comments | Preserved if primary path used |
| **Resolution notes** | Isolated section | May merge with tail comments | Isolated section |
| **History / transitions** | **Not in text**; **status** in metadata only | Same | Same |
| **Retrieval for “resolution for TKT-x”** | Strong — resolution section intact | Weaker if merged | Strong |
| **Retrieval for “payment failures”** | Good if in description/title header | Good for raw keyword density | Good |
| **Implementation cost** | Low | Lowest | Low–medium |
| **PDF / AC-CORE-19 fit** | Strong narrative justification | Weaker unless only long-text corpus | **Best balance** |

### 8.1 Combined sources in one ticket

| Ticket shape | Dominant source | Paragraph-first outcome | Risk if fixed-size-only |
|--------------|-----------------|-------------------------|-------------------------|
| Intake only (description, no comments) | Description | 1–2 chunks | Low |
| Active thread (5+ comments) | Comments | 5+ chunks, one per comment | **High** — merged timeline |
| Resolved + notes | Resolution + last comments | Resolution block + comment blocks | Medium — resolution diluted |
| Closed (`CLOSED`) | Metadata `status` | Same text; metadata shows closed | Re-ingest must run so ask sees closed (**DEC-01**) |

---

## 9. Recommendation (project default) · unit **ING-G**

### 9.1 Chosen strategy: **hybrid primary**

**Agreed direction for v1 (**Convention** — aligns [`architecture.md`](architecture.md) §16.2):**

| Step | Strategy |
|------|----------|
| 1 | Build `KnowledgeDocument` per §4–§5 |
| 2 | **Primary:** paragraph + **comment-boundary** blocks (§6) |
| 3 | **Secondary:** fixed-size + sentence bias + overlap **only** on blocks &gt; `max-chars` (§7.2) |
| 4 | **Optional merge:** adjacent blocks in the **same section** under `min-chars` → single chunk (never merge across comments) |
| 5 | Embed each final chunk; attach §4.3 metadata |

**Rejected for v1 default:** fixed-size-only on full document; semantic chunking (extra model/cost) unless `evaluation-strategy.md` shows retrieval gaps.

### 9.2 Why this matches ticket structure

Tickets are **semi-structured narratives**: a stable **description**, an **append-only comment thread**, and a **resolution** capstone — not a single essay. Paragraph/comment boundaries mirror how agents read tickets. Fixed-size is a **safety valve** for pasted stack traces or long descriptions, not the primary splitter.

### 9.3 Proposed numeric defaults (**Proposed** — confirm before treating as **Agreed**)

| Property | Proposed value | Notes |
|----------|----------------|-------|
| `rag.chunking.max-chars` | `800` | ~150–200 tokens; overflow trigger |
| `rag.chunking.min-chars` | `120` | Merge tiny **within-section** fragments only |
| `rag.chunking.overlap-chars` | `80` | Only for §7.2 overflow splits |
| `rag.chunking.strategy` | `HYBRID_PARAGRAPH_THEN_FIXED` | Enum for tests |

Tune after eval; do not hardcode in Java — use `@ConfigurationProperties` (**PDF** intent for retrieval params; same pattern for chunking).

**Example `application.yml` fragment (**Convention** — values **Proposed** until agreed):**

```yaml
rag:
  chunking:
    strategy: HYBRID_PARAGRAPH_THEN_FIXED
    max-chars: 800
    min-chars: 120
    overlap-chars: 80
  retrieval:
    top-k: 8          # Open — tune via evaluation-strategy.md
    similarity-threshold: 0.72
    distance-metric: COSINE
  embedding:
    provider: ollama  # DEC-09 Open
    model: nomic-embed-text
```

Bind with `@ConfigurationProperties(prefix = "rag")` per `rules/java-springboot.md`.

---

## 10. Ingestion triggers and freshness (**DEC-01**) · unit **ING-H**

| Event | Re-ingest? | Notes |
|-------|------------|-------|
| Ticket **created** | **Yes** | Initial index (**PDF** pipeline) |
| Ticket **updated** (fields, including description, resolution, assignee, etc.) | **Yes** | **PDF** p.6 acceptance |
| **Comment** added | **Yes** | Ticket updated; comment text must appear (**AC-FEAT-14-02**) |
| Status transition only (no text change) | **Yes** (**Proposed**) | Metadata snapshot must reflect new `status` (e.g. `CLOSED`) even if `assembledText` unchanged |
| Ticket **closed** (`status` → `CLOSED`) | **Yes** | **PDF** p.5 “updated **or** closed”; satisfies **AC-FEAT-14-03** |

**DEC-01 interim stance:** implement **(B) update or close** per ingestion p.5 — treat **close** as a re-ingest trigger (status-only change included). Document in demo script if graders use p.6 narrow reading.

**Execution (**Proposed**): synchronous ingest hook from ticket **service** after successful commit ([`architecture.md`](architecture.md) §15.4); async queue is **out of scope** unless latency requires it later.

---

## 11. Storage and re-ingest mechanics · unit **ING-I**

Per [`data-model.md`](data-model.md) §8.4 (**Convention**):

1. `DELETE FROM ticket_vector_chunk WHERE ticket_id = ?`
2. Insert all new chunks with `chunk_index` 0…n−1 in deterministic chunker order.

Vectors are **derived**; rebuild from PostgreSQL if the index is lost.

---

## 12. Embedding model and vector store (**DEC-09**) · unit **ING-J**

| Topic | Interim **Convention** | **Open** |
|-------|------------------------|----------|
| Vector store | PostgreSQL **PgVector** (project convention) | Chroma vs PGVector if **DEC-09** changes |
| Embedding provider | **Ollama** via Spring AI (local) | Cloud model + API key in env |
| Model id | — | e.g. `nomic-embed-text` — record in config when chosen |
| Vector dimension `n` | Must match model | Liquibase `vector(n)` per [`data-model.md`](data-model.md) §8.2 |

**Rule:** ingest and ask query **must** use the **same** embedding model; changing model requires full reindex.

### 12.1 Retrieval settings (ingest boundary)

Ingest does not apply top-K; ask does. Property names (**Proposed**, align `rules/rag-vector-store.md`):

| Property | Purpose |
|----------|---------|
| `rag.retrieval.top-k` | Max chunks to LLM |
| `rag.retrieval.similarity-threshold` | Min score; below → no-match |
| `rag.retrieval.distance-metric` | `COSINE` (**Proposed**) — threshold calibration depends on this |

Numeric defaults for K/threshold: **Open** until agreed (FEAT-19); do not embed in Java.

---

## 13. Implementation placement · unit **ING-J**

| Component | Package / layer | Responsibility |
|-----------|-----------------|----------------|
| `KnowledgeDocumentBuilder` | `rag` | §4–§5 assembly |
| `TicketChunker` | `rag` | §6–§9 algorithms |
| `TicketIngestionService` | `rag` or `service` | Orchestrate build → chunk → embed → store |
| `VectorChunkWriter` | `rag` | §11 delete + insert |
| Ingest hook | `service` | After ticket/comment commit → call ingestion port |

**Example orchestration (pseudocode — **Convention** layering):**

```java
// After ticketRepository.save(ticket) inside @Transactional service method
ticketIngestionPort.reindex(ticket.getId()); // builds §5 → chunks → embed → §11
```

Unit tests mock `TicketIngestionPort`; integration tests assert `ticket_vector_chunk` rows after HTTP create/update.

---

## 14. Acceptance criteria (testable) · unit **ING-K**

| ID | Criterion |
|----|-----------|
| **AC-RAG-ING-01** | Given ticket with description, when ingest runs, then at least one chunk whose `content` contains description text (**AC-FEAT-12-01**). |
| **AC-RAG-ING-02** | Given two comments, when ingest runs, then chunks do not merge both comment bodies into one chunk unless §9.3 min-merge within same synthetic block (**AC-FEAT-12-02**). |
| **AC-RAG-ING-03** | Given resolution notes set, when ingest runs, then resolution text appears in some chunk (**DEC-05**). |
| **AC-RAG-ING-04** | Every chunk metadata JSON contains PDF keys §4.3 (**AC-FEAT-13-01**). |
| **AC-RAG-ING-05** | Given description longer than `max-chars` with no blank lines, when chunked, then multiple chunks produced via overflow path with overlap (**Proposed** §7.2). |
| **AC-RAG-ING-06** | Given ticket update changing comment text, when re-ingest completes, then old comment text absent from `ticket_vector_chunk` rows (**AC-FEAT-14-01**). |
| **AC-RAG-ING-07** | Given transition to `CLOSED`, when re-ingest runs, then metadata `status` is `CLOSED` on all new chunks (**AC-FEAT-14-03**). |
| **AC-RAG-ING-08** | Chunking strategy and hybrid justification traceable to this file + architecture §16 (**AC-FEAT-13-03**, **AC-CORE-19**). |

---

## 15. Open questions and decisions

| ID | Topic | Status | Notes |
|----|-------|--------|-------|
| **DEC-01** | Close-only re-ingest vs p.6 wording | **Open** — interim **(B)** §10 | User confirm for sign-off |
| **DEC-09** | Model id + dimension | **Open** | Blocks Liquibase `vector(n)` final value |
| Chunk numeric defaults §9.3 | `max-chars` / `min-chars` / `overlap` | **Proposed** | Confirm to mark **Agreed** |
| Async ingest | Latency | **Open** | Default sync §10 |

---

## 16. Revision history

| Date | Note |
|------|------|
| 2026-10-04 | Initial spec: sources (incl. not-ingested history/transitions), paragraph vs fixed-size comparison, hybrid recommendation, re-ingest, storage, AC-RAG-ING-*. |
| 2026-10-04 | Scope: retrieval eval → [`evaluation-strategy.md`](evaluation-strategy.md) §4.4 (not golden answers). |
| 2026-10-04 | Cross-ref ask response semantics → [`api-contract.md`](api-contract.md) §6.2–§6.5. |
| 2026-10-04 | Ask response semantics → [`rag-api-contract.md`](rag-api-contract.md). |
| 2026-10-04 | §0 guide, PDF map, BRF/FRI/IRI triad; example assembledText + application.yml; ingest hook pseudocode. |
| 2026-10-04 | §0.4 **ING-*** independent reading units + PDF verbatim ingestion quote. |
| 2026-10-04 | Major `##` headings tagged with **ING-*** unit ids; TOC updated. |
