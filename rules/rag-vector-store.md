
# RAG / vector store guidelines

Applies when implementing ticket knowledge ingestion and `POST /api/ai/ask`.

## Pipeline

Tickets → knowledge documents → **chunk** → **embeddings** → vector store (PgVector preferred) → user question → similarity search → LLM + retrieved context → **grounded answer** + **ticket sources**.

## Ingestion

- Source fields: description, comments, resolution notes (and other ticket text agreed in `spec/rag-ingestion.md`).
- Metadata on each chunk/document: `ticketId`, `status`, `priority`, `assignee`, `category`.
- **Re-ingest / refresh** when a ticket is updated or closed—knowledge must not go stale.
- Document chunking choice (paragraph vs fixed-size vs semantic) for **ticket data** in `spec/architecture.md` / `rag-ingestion.md`.

## Retrieval

- `top-K` and **similarity threshold** are **configurable** (Spring config)—never hardcoded magic numbers in code.
- Document embedding model choice (e.g. local Ollama vs cloud) and cost/latency/quality tradeoff in architecture docs.

## Grounding & guardrails

- Answer **only** from retrieved ticket context—do not fall back to general LLM knowledge for support questions.
- If nothing relevant: say **no relevant tickets found**; do not fabricate.
- Cite specific ticket ID(s) used.
- **Single** retrieve → generate flow—not an autonomous agent (no tool-chaining, ticket creation, or notifications from the ask endpoint).

## Validation

Use `/review-rag-output` (or `commands/review-rag-output.md`) to check assistant answers for hallucination / ungrounded claims before accepting them.
