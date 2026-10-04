# RAG / vector store guidelines

Cursor attaches this file via [`.cursor/rules/rag-vector-store.mdc`](../.cursor/rules/rag-vector-store.mdc) (pointer only). Edit **this** file; do not copy the body into the `.mdc`.

Applies when implementing ticket knowledge ingestion and `POST /api/ai/ask`.

| Read first | Purpose |
|------------|---------|
| [`spec/architecture.md`](../spec/architecture.md) | **§0** PDF/RAG map; pipeline (§15), vector DB (§14), chunking/embedding **justification** (§16); business RAG modules (§4, §8) |
| `rules/api-standards.md` | Ask HTTP path, envelopes, 200 no-match vs 400 validation |
| `rules/java-springboot.md` | `rag/` package, `@ConfigurationProperties`, no magic numbers in Java |
| `rules/testing.md` | Contract tests + doubles; not retrieval-quality golden strings |
| `commands/review-rag-output.md` | Manual grounding review of ask answers |
| [`spec/rag-ingestion.md`](../spec/rag-ingestion.md) | **§0** ingest map; chunking §6–§9, yaml §9.3, triggers §10; **DEC-09/16/18** agreed defaults |
| [`spec/rag-api-contract.md`](../spec/rag-api-contract.md) | Ask `data`, grounding, no-match (**DEC-11** agreed) |
| [`spec/evaluation-strategy.md`](../spec/evaluation-strategy.md) | Probabilistic retrieval quality (agreed) |
| [`spec/requirements.md`](../spec/requirements.md) | §2.3 **Reference** (do not implement); §2.5 probabilistic proof; FEAT-22; **DEC-01**, **DEC-09** §10 |

The PDF requires this **guidelines file** to cover chunking **convention**, embedding **model choice**, and retrieval-tuning **defaults**. **Justification** (non-numeric) lives in [`spec/architecture.md`](../spec/architecture.md) §16 (**AC-CORE-19**). **Numeric** chunk sizes, K, threshold, and model ids are defined in [`spec/rag-ingestion.md`](../spec/rag-ingestion.md) — load via `@ConfigurationProperties`; do not hardcode in Java. Treat **proposed** §9.3 values as defaults only after user confirmation.

## Assessment vs project conventions vs open questions

**Assessment PDF requires** (do not weaken these):

- Natural-language Q&A over support-ticket history, grounded **only** in ticket data the system has (not general model knowledge for support questions).
- Cite the **ticket ID(s)** actually used; if nothing relevant is retrieved, say so explicitly (**no relevant tickets found**) — do not fabricate tickets, facts, or citations.
- Ingest **description, comments, and resolution notes** into searchable knowledge.
- Attach metadata: `ticketId`, `status`, `priority`, `assignee`, `category` — shapes in [`spec/data-model.md`](../spec/data-model.md) §11 (`RagChunkMetadata` / JSONB on `ticket_vector_chunk`).
- **Re-ingest / refresh** derived embeddings when a ticket is **updated or closed** so knowledge does not go stale (**Agreed DEC-01 (B)**; demo emphasis on **updated** — [`spec/requirements.md`](../spec/requirements.md) **§11.1**).
- **top-K** and **similarity threshold** must be **configurable**, not hardcoded. The PDF does **not** give numeric values.
- Document chunking strategy and embedding-model tradeoffs in architecture / RAG ingestion **specs** (justification is required; a specific algorithm is **not** named by the PDF).
- **Single retrieve → generate** — not an agent: no tool chaining, ticket creation, or notifications from the ask path.
- Named stack examples: Spring AI, an embedding model, a vector store (**PGVector or Chroma**). The PDF does **not** pick one store, Ollama, Liquibase, or a model name.

**This project’s approved conventions** (not PDF mandates):

- **PostgreSQL with PgVector** is the selected vector store (same database instance family as tickets).
- **Spring AI** is the integration framework for embeddings, vector operations, and generation.
- **Ollama** is the **initial provider** — wire it through **configuration** (base URL, model ids as properties). Do not scatter Ollama-specific clients through services. The **model ids themselves are not chosen in this file**.
- Vector table / extension / column **schema** goes through **Liquibase**, consistent with Java/Spring standards.
- Default tests follow `rules/testing.md` (JUnit 5, Mockito doubles for embed/generate, PostgreSQL Testcontainers). **No CI** in this milestone.

**Agreed relational / metadata shape — [`spec/data-model.md`](../spec/data-model.md):** tables `ticket`, `ticket_comment`, `ticket_vector_chunk`; resolution text column `resolution_notes`; ingest includes description, comments, resolution notes; chunk row metadata keys §11.1; re-ingest deletes/replaces rows per ticket (§8.4).

**Agreed (hub §10.2) — implement per specs; do not invent in Java.** Chunking + defaults → [`spec/rag-ingestion.md`](../spec/rag-ingestion.md) (**DEC-09**, **DEC-16**, **DEC-18**). Ask `data` → [`spec/rag-api-contract.md`](../spec/rag-api-contract.md) (**DEC-11**, **DEC-17**). Retrieval **quality** → [`spec/evaluation-strategy.md`](../spec/evaluation-strategy.md) (**DEC-19**). Remaining **Convention** (not locked in DEC):

- **Chat / generation** model id (Ollama) — env/config; embedding fixed **DEC-09**
- Numeric **top-K**, similarity **threshold values** (configurable per PDF; property keys §12.1 — **values** open until agreed)
- **Proposed** chunk **size** / **overlap** in `rag-ingestion.md` §9.3 — confirm before treating as agreed defaults

**Reference only — do not build** ([`spec/requirements.md`](../spec/requirements.md) **§2.3**): async ingest queues, versioned embedding history, ask **confidence** / extra `reason` fields, metadata-only ask filters, semantic chunking v1, agents/rerankers, vector admin APIs.
- Numerical RAG quality scores, golden-answer sets

Do not assume **versions** of Spring AI, PostgreSQL, PgVector, Ollama, or models until those are agreed.

## PDF guidance slots (conventions — values in specs)

Record the **chosen** approach in [`spec/rag-ingestion.md`](../spec/rag-ingestion.md) (and justify tradeoffs in [`spec/architecture.md`](../spec/architecture.md) §16). Hybrid paragraph + fixed overflow with **DEC-16** defaults (§9.3).

### Chunking convention (PDF: paragraph vs fixed-size vs semantic)

| Approach | When it fits ticket text | Tradeoff |
|----------|--------------------------|----------|
| **Paragraph / comment boundaries** | Short threads; description + comments as blocks | Simple; may split long descriptions |
| **Fixed-size** | Uniform chunk length for vector index | Predictable size; may cut mid-sentence |
| **Semantic** | Rich long descriptions | Better boundaries; more tooling/cost |

Ingest builds one **knowledge document** per ticket per [`spec/rag-ingestion.md`](../spec/rag-ingestion.md) §5, then chunks before embed.

### Embedding model choice (PDF: local Ollama vs cloud)

| Option | Cost | Latency | Notes |
|--------|------|---------|--------|
| **Local** (e.g. via Ollama + Spring AI) | No per-token cloud bill | Hardware-dependent | Good for dev/demo; dimension fixed per model |
| **Cloud** hosted API | Per-token | Often stable | Needs API key in env only |

**Ingest and query must use the same embedding model** (or rebuild the whole index after a model change).

### Retrieval-tuning defaults (PDF: configurable, not hardcoded)

Wire through configuration (example shape — **property names and values** come from [`spec/rag-ingestion.md`](../spec/rag-ingestion.md) §9.3, §12):

```yaml
# application.yml — agreed defaults: spec/rag-ingestion.md §9.3, §12 (DEC-16, DEC-09)
rag:
  retrieval:
    top-k: 8
    similarity-threshold: 0.72
    distance-metric: COSINE
  chunking:
    max-chars: 800
    min-chars: 120
    overlap-chars: 80
spring.ai.ollama.embedding.options.model: nomic-embed-text  # 768-dim vectors
```

- **top-K:** max chunks passed to the LLM after search.
- **Similarity threshold:** discard hits below this; if none remain → **no relevant tickets found** (no LLM call on empty context).
- **Distance metric** (cosine vs inner product): [`spec/rag-ingestion.md`](../spec/rag-ingestion.md) §12.1 so threshold comparisons are meaningful.

### Illustrative ask questions (PDF — for eval and manual review)

Use these in `commands/review-rag-output.md` and [`spec/evaluation-strategy.md`](../spec/evaluation-strategy.md); they are **not** golden answers:

- “Have we seen payment failures before?”
- “What was the resolution for ticket TKT-1001?” (example id only)
- “What are the common causes of shipment tracking issues?”
- “Show me similar resolved tickets.”
- “Which high-priority tickets are related to payment?”

### Out-of-scope vs no-match (both need honest handling)

| Case | Meaning | Correct behaviour |
|------|---------|-------------------|
| **No retrieval** | Empty search or all hits below threshold | 200 + **no relevant tickets found** in `data`; no citations |
| **Out-of-scope support question** | Question not answerable from ticket corpus even if the model “knows” | Same: do **not** use general LLM knowledge; say no relevant tickets / cannot answer from history |
| **Grounded hit** | Retrieved excerpts support an answer | 200 + answer + **cited ticket ids from retrieval** |

## Relational vs vector persistence

| Data | Access (**Convention** **C-06**) |
|------|-------------------------------------|
| `ticket`, `ticket_comment` | **Spring Data JPA** repositories in `persistence` — ingest loads tickets/comments through these, not JDBC |
| `ticket_vector_chunk` | **`rag`** `VectorChunkStore` implementation (e.g. `JdbcTemplate` + pgvector casts, or Spring AI PgVector) — keep SQL/vector ops behind the port |

Do not add JPA entities for chunks **and** a second JDBC writer without a spec change; pick the port adapter pattern already in `backend/`.

## Pipeline (assessment behaviour)

```
Tickets → knowledge documents → chunk → embeddings → PgVector
  → user question → similarity search → retrieved ticket context
  → generate from that context only → grounded answer + ticket sources
```

This is **retrieve then generate** once. Do not add agents, multi-step tool orchestration, graph RAG, hybrid search, or reranking unless a later **agreed spec** says so.

Implement split logic per [`spec/rag-ingestion.md`](../spec/rag-ingestion.md) §6–§9 before coding. **Why** belongs in [`spec/architecture.md`](../spec/architecture.md) §16; **numbers** in [`spec/rag-ingestion.md`](../spec/rag-ingestion.md) §9.3 / §12.

## Ingestion and storage

- Build knowledge text only from **description, comments, and resolution notes**. Do not invent extra source types.
- Store assessment metadata on chunks/documents: `ticketId`, `status`, `priority`, `assignee`, `category`. Do not add metadata fields beyond the assessment and the **agreed** data model.
- Persist derived embeddings in **PgVector**. Ticket rows remain the system of record. Vectors must be rebuildable from tickets.
- On **update** or **close**, refresh that ticket’s derived data so stale vectors are not the only index. **How** → [`spec/rag-ingestion.md`](../spec/rag-ingestion.md) §10–§11 (**DEC-01** agreed).
- Use Spring AI for embed / store / search; keep the provider (Ollama first) in **configuration**.
- Liquibase owns extensions (`vector`, `pg_trgm` for ticket search per data-model), `ticket_vector_chunk`, and indexes ([`spec/data-model.md`](../spec/data-model.md) §14.5–14.6). Vector **dimension 768** and HNSW **vector_cosine_ops** per **DEC-09** / **DEC-16** — do not invent a different dimension here.

Do **not** implement a chunking strategy, size, or overlap from this file alone — follow [`spec/rag-ingestion.md`](../spec/rag-ingestion.md).

## Retrieval and generation

- Once an embedding model is agreed, embed the question with the **same** model used at ingest (or rebuild the index if the model changes). Do not pick the model here.
- Similarity search MUST use **configurable** top-K and similarity threshold (external config / env — **no magic numbers in Java**). Property keys and agreed defaults in [`spec/rag-ingestion.md`](../spec/rag-ingestion.md) §12.1 (**DEC-16**).
- If nothing passes the threshold, or retrieval is empty: honest **no relevant tickets found** (ask success envelope per `rules/api-standards.md`). Do **not** call the LLM to invent an answer from empty or irrelevant context.
- Otherwise, pass **retrieved excerpts and their ticket ids** as the only factual context into generation.

## HTTP ask

- Assessment path: `POST /api/ai/ask` with JSON `{"question":"..."}`.
- Project convention: the same handler at `POST /api/v1/ai/ask`; success/error envelopes in `rules/api-standards.md`.
- Fields **inside** `data` per [`spec/rag-api-contract.md`](../spec/rag-api-contract.md) §7 (field catalog cross-ref [`api-contract.md`](../spec/api-contract.md) §3.5).
- Do not expose prompts, chunk dumps, model names, Ollama URLs, top-K, thresholds, or vector internals on the public API unless a spec explicitly makes them public (default: they are not).
- Do not add agent, chat-session, tool, notification, or “AI creates a ticket” endpoints.

## Grounding and answer quality

- Support answers must be based on **retrieved ticket content**, not the model’s general knowledge.
- Citations must be ticket IDs that appear in the **retrieval result** — not ids the model guessed.
- Keep evidence (retrieved chunks / ticket ids) distinct from generated wording. Prompt instructions help but **do not guarantee** grounding. Check retrieval and claims against context **independently**.
- Use `commands/review-rag-output.md` (or `/review-rag-output`) before accepting assistant output — **Grounding** and **Retrieval quality** sections.
- Retrieval quality eval procedure: [`spec/evaluation-strategy.md`](../spec/evaluation-strategy.md) **§3–§9** (corpus §5, failures **F-01…F-10** §8) + **Retrieval quality** in `commands/review-rag-output.md`.
- Do **not** set numerical quality thresholds, formulas, or golden-answer corpora here.

## Testing

Follow `rules/testing.md` (including API tests for ask). In this domain:

- **Deterministic, where feasible:** assembling knowledge text from ticket fields; that update/close **triggers** ingest (testable port); Liquibase/PgVector schema **once** the ingestion spec defines it. PostgreSQL Testcontainers. Mockito (or other doubles) for embed/generate — no real Ollama in the default suite.
- **Not ordinary unit tests:** retrieval ranking quality and free-form generated answers — document procedure in [`spec/evaluation-strategy.md`](../spec/evaluation-strategy.md) (manual review with `commands/review-rag-output.md`, optional fixture tickets, no single expected prose string).
- Do not prescribe an eval harness, metrics, chunk fixtures that imply a frozen chunk size, or golden answer strings in `rules/testing.md`.

## Do not

- Do not treat PgVector, Ollama, or Liquibase as if the PDF mandated them.
- Do not invent or hardcode model names, Ollama URLs, top-K, thresholds, chunk sizes, overlap, dimensions, distance metrics, or index types.
- Do not implement chunking or pick an embedding model from these rules alone — implement per [`spec/rag-ingestion.md`](../spec/rag-ingestion.md).
- Do not implement agents, tool calling, or side effects from `/api/ai/ask`.
- Do not prescribe a prompt template or a second package layout (use `rules/java-springboot.md`).
- Do not implement **Reference** items in [`spec/requirements.md`](../spec/requirements.md) **§2.3** (auth, confidence on ask, rerankers, agents, extra public metadata APIs).

---

## Revision history

| Date | Note |
|------|------|
| 2026-09-24 | Initial RAG ingest, PgVector, retrieve-then-generate ask, and grounding guidelines. |
| 2026-10-03 | Aligned with approved stack; numeric chunk/K/model settings deferred to `spec/rag-ingestion.md`. |
| 2026-10-04 | Linked draft [`spec/rag-ingestion.md`](../spec/rag-ingestion.md); clarified open vs proposed defaults. |
| 2026-10-04 | SDD expansion: OQ/DEC pointers; cross-links to architecture RAG sections and evaluation strategy. |
| 2026-10-04 | Synced with expanded [`spec/requirements.md`](../spec/requirements.md) and [`spec/architecture.md`](../spec/architecture.md) §13–16. |
| 2026-10-04 | Added revision history section. |
| 2026-10-04 | **C-06:** JPA for ticket/comment loads; vector port JDBC/Spring AI exception. |
| 2026-10-04 | Metadata/table/index pointers to agreed [`spec/data-model.md`](../spec/data-model.md) §8, §11, §14.5. |
| 2026-10-04 | Interim: architecture §16 justification; requirements §10 for open RAG **DEC** when child specs absent. |
| 2026-10-04 | Eval pointers: [`evaluation-strategy.md`](../spec/evaluation-strategy.md) §3–§9. |
| 2026-10-04 | Ask `data` → [`api-contract.md`](../spec/api-contract.md) §6.2–§6.5 (not separate `rag-api-contract` file). |
| 2026-10-04 | Ask `data` → [`rag-api-contract.md`](../spec/rag-api-contract.md). |
| 2026-10-04 | Spec **§0** cross-refs; requirements **§0.5** completeness index. |
| 2026-10-04 | **Reference** §2.3; **DEC-01**/**DEC-11** agreed; narrow Open list to **DEC-09** + numeric tuning. |
| 2026-10-04 | **DEC-09/10/16–19** agreed; yaml defaults; sync ingest **DEC-18**. |
