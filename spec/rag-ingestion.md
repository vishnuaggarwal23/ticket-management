# RAG ingestion — knowledge build, chunking, embed, refresh

> **Status:** draft (2026-10-04) — chunking **recommendation** and **proposed** numeric defaults for implementation; **DEC-01** (re-ingest on close) and **DEC-09** (embedding product/model id) remain **Open** with interim stance below.  
> **Primary source:** `docs/Assessments.docx` (restated in [`requirements.md`](requirements.md) FEAT-12…14, §11.1; [`docs/assessment-brief.md`](../docs/assessment-brief.md)).  
> **Related:** Justification narrative → [`architecture.md`](architecture.md) §15–§16; persistence → [`data-model.md`](data-model.md) §8–§11; grounding rules → `rules/rag-vector-store.md`; ask HTTP + response semantics → [`api-contract.md`](api-contract.md) §6, §6.2–§6.5.

**Label legend:** **PDF** | **Convention** | **Agreed** | **Proposed** (implementation default until user confirms) | **Example**

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

## 4. Ticket text sources (what is ingested)

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

## 5. Knowledge document (pre-chunk)

**One composite `KnowledgeDocument` per ticket per ingest run** ([`architecture.md`](architecture.md) §15.3) — then chunk → embed → write rows.

**Pipeline:**

```text
PostgreSQL (ticket + comments)
  → KnowledgeDocumentBuilder → assembledText + metadataSnapshot
  → Chunker (§8)
  → Embedder (same model as ask query)
  → DELETE existing chunks for ticket_id → INSERT new rows (§11)
```

---

## 6. Paragraph-based chunking

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

## 7. Fixed-size chunking

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

## 8. Comparison — paragraph vs fixed-size vs ticket fields

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

## 9. Recommendation (project default)

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

---

## 10. Ingestion triggers and freshness (**DEC-01**)

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

## 11. Storage and re-ingest mechanics

Per [`data-model.md`](data-model.md) §8.4 (**Convention**):

1. `DELETE FROM ticket_vector_chunk WHERE ticket_id = ?`
2. Insert all new chunks with `chunk_index` 0…n−1 in deterministic chunker order.

Vectors are **derived**; rebuild from PostgreSQL if the index is lost.

---

## 12. Embedding model and vector store (**DEC-09**)

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

## 13. Implementation placement

| Component | Package / layer | Responsibility |
|-----------|-----------------|----------------|
| `KnowledgeDocumentBuilder` | `rag` | §4–§5 assembly |
| `TicketChunker` | `rag` | §6–§9 algorithms |
| `TicketIngestionService` | `rag` or `service` | Orchestrate build → chunk → embed → store |
| `VectorChunkWriter` | `rag` | §11 delete + insert |
| Ingest hook | `service` | After ticket/comment commit → call ingestion port |

---

## 14. Acceptance criteria (testable)

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
