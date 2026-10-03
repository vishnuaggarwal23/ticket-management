# RAG / vector store guidelines

Cursor attaches this file via [`.cursor/rules/rag-vector-store.mdc`](../.cursor/rules/rag-vector-store.mdc) (pointer only). Edit **this** file; do not copy the body into the `.mdc`.

Applies when implementing ticket knowledge ingestion and `POST /api/ai/ask`.

| Read first | Purpose |
|------------|---------|
| [`spec/architecture.md`](../spec/architecture.md) | RAG pipeline (§15), vector DB (§14), knowledge/chunking/embedding **justification** (§16); business RAG modules (§4, §8) |
| `rules/api-standards.md` | Ask HTTP path, envelopes, 200 no-match vs 400 validation |
| `rules/java-springboot.md` | `rag/` package, `@ConfigurationProperties`, no magic numbers in Java |
| `rules/testing.md` | Contract tests + doubles; not retrieval-quality golden strings |
| `commands/review-rag-output.md` | Manual grounding review of ask answers |
| `spec/rag-ingestion.md` | **Numeric** chunking, models, K, threshold, ingest timing (when agreed) |
| `spec/rag-api-contract.md` | Field names inside ask `data` |
| `spec/evaluation-strategy.md` | Probabilistic retrieval quality (PDF learning goal) |

The PDF requires this **guidelines file** to cover chunking **convention**, embedding **model choice**, and retrieval-tuning **defaults**. Those are **documented below as slots and options** — actual numbers and chosen algorithms are agreed in `spec/rag-ingestion.md` / `architecture.md`, not invented in Java.

## Assessment vs project conventions vs open questions

**Assessment PDF requires** (do not weaken these):

- Natural-language Q&A over support-ticket history, grounded **only** in ticket data the system has (not general model knowledge for support questions).
- Cite the **ticket ID(s)** actually used; if nothing relevant is retrieved, say so explicitly (**no relevant tickets found**) — do not fabricate tickets, facts, or citations.
- Ingest **description, comments, and resolution notes** into searchable knowledge.
- Attach metadata: `ticketId`, `status`, `priority`, `assignee`, `category` — shapes in [`spec/data-model.md`](../spec/data-model.md) §11 (`RagChunkMetadata` / JSONB on `ticket_vector_chunk`).
- **Re-ingest / refresh** derived embeddings when a ticket is **updated or closed** so knowledge does not go stale. PDF p.6 acceptance wording emphasises **updated** only — resolve **DEC-01** in `spec/rag-ingestion.md` with [`spec/requirements.md`](../spec/requirements.md) **§11.1** before treating close-only triggers as out of scope.
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

**Open — do not assume, invent, or lock in these rules.** Record and agree in `spec/rag-ingestion.md` (models, chunking, ingest mechanics), `spec/rag-api-contract.md` (ask `data` fields), and evaluation strategy (quality scores) **before** implementing those details:

- Embedding **model** (name/id), vector **dimension**, generation **model**
- Whether ingest and query always share one embedding model (they **must** stay compatible once a choice is agreed)
- Chunking **algorithm**, chunk **size**, **overlap**, one document vs many per ticket
- Numeric **top-K**, similarity **threshold values**, distance **metric**, vector **index** type
- Default values and config **property names** for retrieval (keys must exist once the ingestion spec agrees them; **values are not set here**)
- Metadata columns **beyond** the assessment list / approved data model
- Synchronous vs asynchronous ingestion; delete-and-replace vs versioned historical embeddings
- Prompt text / template
- Exact ask JSON **inside** `data` (answer, citations, no-match representation). HTTP envelope is `rules/api-standards.md`; no confidence field unless a spec adds it
- Numerical RAG quality scores, golden-answer sets

Do not assume **versions** of Spring AI, PostgreSQL, PgVector, Ollama, or models until those are agreed.

## PDF guidance slots (conventions — values in specs)

Record the **chosen** approach in `spec/rag-ingestion.md` (and justify tradeoffs in [`spec/architecture.md`](../spec/architecture.md)). Until agreed, do not hardcode in application code.

### Chunking convention (PDF: paragraph vs fixed-size vs semantic)

| Approach | When it fits ticket text | Tradeoff |
|----------|--------------------------|----------|
| **Paragraph / comment boundaries** | Short threads; description + comments as blocks | Simple; may split long descriptions |
| **Fixed-size** | Uniform chunk length for vector index | Predictable size; may cut mid-sentence |
| **Semantic** | Rich long descriptions | Better boundaries; more tooling/cost |

Ingest builds one **knowledge document** per ticket (or as `rag-ingestion.md` agrees), then chunks before embed.

### Embedding model choice (PDF: local Ollama vs cloud)

| Option | Cost | Latency | Notes |
|--------|------|---------|--------|
| **Local** (e.g. via Ollama + Spring AI) | No per-token cloud bill | Hardware-dependent | Good for dev/demo; dimension fixed per model |
| **Cloud** hosted API | Per-token | Often stable | Needs API key in env only |

**Ingest and query must use the same embedding model** (or rebuild the whole index after a model change).

### Retrieval-tuning defaults (PDF: configurable, not hardcoded)

Wire through configuration (example shape — **property names and values** come from `spec/rag-ingestion.md`):

```yaml
# application.yml — illustrative keys only; values TBD in spec
rag:
  retrieval:
    top-k: ${RAG_TOP_K:}           # set after agreement
    similarity-threshold: ${RAG_SIMILARITY_THRESHOLD:}
  # embedding / generation model ids under spring.ai.* per Spring AI docs
```

- **top-K:** max chunks passed to the LLM after search.
- **Similarity threshold:** discard hits below this; if none remain → **no relevant tickets found** (no LLM call on empty context).
- **Distance metric** (cosine vs inner product): agree in `rag-ingestion.md` so threshold comparisons are meaningful.

### Illustrative ask questions (PDF — for eval and manual review)

Use these in `commands/review-rag-output.md` and `spec/evaluation-strategy.md`; they are **not** golden answers:

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

## Pipeline (assessment behaviour)

```
Tickets → knowledge documents → chunk → embeddings → PgVector
  → user question → similarity search → retrieved ticket context
  → generate from that context only → grounded answer + ticket sources
```

This is **retrieve then generate** once. Do not add agents, multi-step tool orchestration, graph RAG, hybrid search, or reranking unless a later **agreed spec** says so.

Pick the chunking row from **PDF guidance slots** in `spec/rag-ingestion.md` before implementing split logic. **Why** (ticket-shaped text, paragraph vs fixed vs semantic) belongs in [`spec/architecture.md`](../spec/architecture.md) §16; **numbers** belong in `spec/rag-ingestion.md`.

## Ingestion and storage

- Build knowledge text only from **description, comments, and resolution notes**. Do not invent extra source types.
- Store assessment metadata on chunks/documents: `ticketId`, `status`, `priority`, `assignee`, `category`. Do not add metadata fields beyond the assessment and the **agreed** data model.
- Persist derived embeddings in **PgVector**. Ticket rows remain the system of record. Vectors must be rebuildable from tickets.
- On **update** or **close**, refresh that ticket’s derived data so stale vectors are not the only index. **How** (sync vs async, delete vs version) is **open** — `spec/rag-ingestion.md`, not this file.
- Use Spring AI for embed / store / search; keep the provider (Ollama first) in **configuration**.
- Liquibase owns extensions (`vector`, `pg_trgm` for ticket search per data-model), `ticket_vector_chunk`, and indexes ([`spec/data-model.md`](../spec/data-model.md) §14.5–14.6). Vector **dimension** and HNSW **opclass** must match `spec/rag-ingestion.md` — do not invent a dimension here.

Do **not** implement a chunking strategy, size, or overlap from this file.

## Retrieval and generation

- Once an embedding model is agreed, embed the question with the **same** model used at ingest (or rebuild the index if the model changes). Do not pick the model here.
- Similarity search MUST use **configurable** top-K and similarity threshold (external config / env — **no magic numbers in Java**). Do not put default numeric values in this file or in application code until `spec/rag-ingestion.md` agrees them.
- If nothing passes the threshold, or retrieval is empty: honest **no relevant tickets found** (ask success envelope per `rules/api-standards.md`). Do **not** call the LLM to invent an answer from empty or irrelevant context.
- Otherwise, pass **retrieved excerpts and their ticket ids** as the only factual context into generation.

## HTTP ask

- Assessment path: `POST /api/ai/ask` with JSON `{"question":"..."}`.
- Project convention: the same handler at `POST /api/v1/ai/ask`; success/error envelopes in `rules/api-standards.md`.
- Fields **inside** `data` wait on `spec/rag-api-contract.md`.
- Do not expose prompts, chunk dumps, model names, Ollama URLs, top-K, thresholds, or vector internals on the public API unless a spec explicitly makes them public (default: they are not).
- Do not add agent, chat-session, tool, notification, or “AI creates a ticket” endpoints.

## Grounding and answer quality

- Support answers must be based on **retrieved ticket content**, not the model’s general knowledge.
- Citations must be ticket IDs that appear in the **retrieval result** — not ids the model guessed.
- Keep evidence (retrieved chunks / ticket ids) distinct from generated wording. Prompt instructions help but **do not guarantee** grounding. Check retrieval and claims against context **independently**.
- Use `commands/review-rag-output.md` (or `/review-rag-output`) before accepting assistant output — **Grounding** and **Retrieval quality** sections.
- Retrieval quality eval procedure: `spec/evaluation-strategy.md` + retrieval section of `commands/review-rag-output.md`.
- Do **not** set numerical quality thresholds, formulas, or golden-answer corpora here.

## Testing

Follow `rules/testing.md` (including API tests for ask). In this domain:

- **Deterministic, where feasible:** assembling knowledge text from ticket fields; that update/close **triggers** ingest (testable port); Liquibase/PgVector schema **once** the ingestion spec defines it. PostgreSQL Testcontainers. Mockito (or other doubles) for embed/generate — no real Ollama in the default suite.
- **Not ordinary unit tests:** retrieval ranking quality and free-form generated answers — document procedure in **`spec/evaluation-strategy.md`** (manual review with `commands/review-rag-output.md`, optional fixture tickets, no single expected prose string).
- Do not prescribe an eval harness, metrics, chunk fixtures that imply a frozen chunk size, or golden answer strings in `rules/testing.md`.

## Do not

- Do not treat PgVector, Ollama, or Liquibase as if the PDF mandated them.
- Do not invent or hardcode model names, Ollama URLs, top-K, thresholds, chunk sizes, overlap, dimensions, distance metrics, or index types.
- Do not implement chunking or pick an embedding model from these rules — wait for `spec/rag-ingestion.md`.
- Do not implement agents, tool calling, or side effects from `/api/ai/ask`.
- Do not prescribe a prompt template or a second package layout (use `rules/java-springboot.md`).
- Do not invent product features (auth on ask, rerankers, extra metadata) not in the assessment or an agreed spec.

---

## Revision history

| Date | Note |
|------|------|
| 2026-09-24 | Initial RAG ingest, PgVector, retrieve-then-generate ask, and grounding guidelines. |
| 2026-10-03 | Aligned with approved stack; numeric chunk/K/model settings deferred to `spec/rag-ingestion.md`. |
| 2026-10-04 | SDD expansion: OQ/DEC pointers; cross-links to architecture RAG sections and evaluation strategy. |
| 2026-10-04 | Synced with expanded [`spec/requirements.md`](../spec/requirements.md) and [`spec/architecture.md`](../spec/architecture.md) §13–16. |
| 2026-10-04 | Added revision history section. |
| 2026-10-04 | Metadata/table/index pointers to agreed [`spec/data-model.md`](../spec/data-model.md) §8, §11, §14.5. |
