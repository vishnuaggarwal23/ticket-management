# Requirements

> **Source:** `docs/Assessments.pdf` only.  
> **Status:** draft — seed for later refinement.  
> **Rule:** No requirements beyond the PDF. Gaps are listed as open questions, not decisions.

Related human-readable overview: [`docs/assessment-brief.md`](../docs/assessment-brief.md).

---

## 1. Problem / context

Build an **AI-Powered Support Ticket Management System** that supports normal ticket operations and a **grounded** natural-language question-answering capability over ticket history (RAG), designed as an AI-native feature from the start.

---

## 2. Scope

### In scope (as stated in the PDF)

**Ticket management**

- Create a ticket
- List tickets
- View ticket details
- Update title, description, priority, and assignee
- Add comments
- Search tickets by keyword
- Filter tickets by status
- Persist data in a database
- Validate input at the backend
- Display meaningful errors in the UI

**Status state machine (backend-enforced)**

Allowed transitions:

- `OPEN` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`
- `OPEN` → `CANCELLED`
- `IN_PROGRESS` → `CANCELLED`

Invalid transitions must be rejected. Examples given as invalid:

- `CLOSED` → `OPEN`
- `RESOLVED` → `OPEN`
- `CANCELLED` → `OPEN`

**AI / RAG assistant**

- Natural-language Q&A endpoint over ticket history, grounded strictly in real ticket data
- Cite the specific ticket(s) used to produce any assistant answer
- Explicitly indicate when no relevant tickets are found, rather than fabricate an answer

**RAG ingestion**

- Convert ticket information (**description, comments, resolution notes**) into searchable knowledge documents
- Include metadata: `ticketId`, `status`, `priority`, `assignee`, `category`
- Re-ingest / refresh embeddings when a ticket is **updated or closed** (knowledge must not go stale)

**RAG API (as named)**

- `POST /api/ai/ask`
- Request body example:

```json
{
  "question": "What caused previous payment failures?"
}
```

**RAG pipeline (as stated)**

Support Tickets → Create Knowledge Documents → Chunk → Generate Embeddings → Vector Store → User Question → Similarity Search → Relevant Tickets → LLM + Context → Grounded Answer → Ticket Sources

**Retrieval quality / documentation (as stated)**

- Document chunking strategy (paragraph-based vs fixed-size vs semantic splitting) for ticket data specifically — in architecture documentation
- `top-K` and similarity threshold must be **configurable**, not hardcoded
- Document embedding model choice (e.g. local via Ollama vs cloud) and cost/latency/quality tradeoff — in architecture documentation

**Grounding & guardrails (as stated)**

- Answer only from retrieved ticket context; do not fall back on general LLM knowledge for support-specific questions
- If no tickets are relevant, say so explicitly; do not produce a fabricated but plausible answer
- Single retrieval → generate flow only; not an autonomous agent (no independently creating tickets, sending notifications, or chaining tools)

**Example questions the assistant should be able to answer (illustrative)**

- “Have we seen payment failures before?”
- “What was the resolution for ticket TKT-1001?”
- “What are the common causes of shipment tracking issues?”
- “Show me similar resolved tickets.”
- “Which high-priority tickets are related to payment?”

**Technology (as named)**

- Java 21, Spring Boot, Spring AI, PostgreSQL/H2, embedding model, vector store (e.g. PGVector or Chroma), REST API, React/Next.js or equivalent frontend, Cursor / GitHub Copilot / Kiro

### Non-goals / out of scope (as stated)

- Autonomous agent behavior from the ask endpoint (creating tickets, notifications, tool chaining, further independent actions)

---

## 3. Functional requirements (PDF-derived)

| ID | Requirement |
|----|-------------|
| FR-01 | Users can create a ticket from the UI |
| FR-02 | Users can list tickets |
| FR-03 | Users can view ticket details |
| FR-04 | Users can update title, description, priority, and assignee |
| FR-05 | Users can add comments |
| FR-06 | Users can search tickets by keyword |
| FR-07 | Users can filter tickets by status |
| FR-08 | Ticket data is persisted in a database and survives application restart |
| FR-09 | Backend validates input |
| FR-10 | UI displays meaningful errors |
| FR-11 | Backend enforces the defined status state machine and allows valid status transitions |
| FR-12 | Backend rejects invalid status transitions |
| FR-13 | Ticket text (description, comments, resolution notes) is converted into embeddings and stored in a vector store, with stated metadata |
| FR-14 | Embeddings are re-ingested/refreshed when a ticket is updated or closed |
| FR-15 | `POST /api/ai/ask` returns a grounded, ticket-sourced answer for in-scope questions |
| FR-16 | AI responses cite the specific ticket ID(s) used |
| FR-17 | When no relevant tickets are found, the system returns an honest “no relevant tickets found” (or equivalent explicit indication), not a fabricated answer |
| FR-18 | Retrieval parameters top-K and similarity threshold are configurable (not hardcoded) |

---

## 4. Non-functional / process requirements (PDF-derived)

| ID | Requirement |
|----|-------------|
| NFR-01 | Spec-driven workflow: Requirement → Specification → Plan/Tasks → Implementation → Testing → Review → Fix |
| NFR-02 | Do not start from “build the complete application” |
| NFR-03 | Maintain hygiene artefacts (Java/Spring Boot, testing, API, documentation, RAG/vector guidelines; review/generate commands including hallucination review) |
| NFR-04 | Create specifications before implementation (see assessment example `spec/` set) |
| NFR-05 | Prompt history saved (`.specstory/history/` and `docs/prompt-history.md`) |
| NFR-06 | No secrets committed |
| NFR-07 | Chunking strategy and embedding model choice documented and justified in `architecture.md` |
| NFR-08 | At least one meaningful AI mistake (wrong code and/or ungrounded answer) caught and documented during development |
| NFR-09 | State-machine integration tests pass |
| NFR-10 | Demonstrate reusable AI instructions across project context; validate AI-generated code and answers |

---

## 5. Acceptance criteria (verbatim themes from PDF “Core Acceptance Criteria”)

The solution is complete when:

- [ ] Ticket can be created from UI
- [ ] Tickets can be listed
- [ ] Ticket details can be viewed
- [ ] Ticket fields can be updated
- [ ] Assignee can be changed
- [ ] Comments can be added
- [ ] Search works
- [ ] Status filter works
- [ ] Valid status transitions work
- [ ] Invalid status transitions are rejected by backend
- [ ] Data survives application restart
- [ ] Backend validation works
- [ ] UI shows meaningful errors
- [ ] State-machine integration tests pass
- [ ] Ticket data is converted into embeddings and stored in a vector store
- [ ] `POST /api/ai/ask` returns a grounded, ticket-sourced answer for in-scope questions
- [ ] The response cites the specific ticket ID(s) used to generate it
- [ ] Out-of-scope / no-match questions return an honest “no relevant tickets found” response, not a fabricated answer
- [ ] Chunking strategy and embedding model choice are documented and justified in `architecture.md`
- [ ] Re-ingestion happens when a ticket is updated or closed — embeddings do not go stale
- [ ] Retrieval parameters (top-K, similarity threshold) are configurable, not hardcoded
- [ ] No secrets are committed
- [ ] At least one meaningful AI mistake — in code or in a RAG answer — was caught and documented during development

---

## 6. Open questions (underspecified in PDF — do not assume answers)

Resolve only after confirmation in later specs:

1. Exact ticket identity format (PDF example uses `TKT-1001` in a sample question only).
2. Full set of ticket fields and which are required (beyond title, description, priority, assignee, status, comments, resolution notes, category metadata).
3. How `category` is populated (user input vs derived vs free text).
4. Complete REST API for tickets/comments (paths, methods, payloads) beyond `POST /api/ai/ask`.
5. Exact response schema for `/api/ai/ask` (answer text, citations structure, no-match representation).
6. Authentication / authorization requirements (not stated).
7. Choice of embedding model and vector store product (examples given; not mandated).
8. Role of H2 vs PostgreSQL in local/dev/prod/test.
9. Frontend framework choice within “React/Next.js or equivalent”.
10. Whether “resolution notes” is a distinct field or derived from comments/status content.
11. How is a status transition initiated from the UI/API?
12. Are only the explicitly shown transitions allowed, or are skipped transitions such as `OPEN` → `RESOLVED` allowed?

---

## Revision

| Date | Note |
|------|------|
| 2026-09-24 | Seed requirements restated only from `docs/Assessments.pdf`. |
| 2026-09-24 | FR-11 clarified; acceptance re-ingestion aligned with RAG text; status-transition open questions added. |
