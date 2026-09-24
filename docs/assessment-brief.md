# Assessment brief (ATL / TL Assignment)

> **Source:** `docs/Assessments.pdf` only.  
> **Status:** draft restatement for team understanding.  
> **Rule:** Do not treat this file as permission to invent features. Anything not stated in the PDF remains an open decision for later specs.

---

## 1. What is being assessed

Two layers:

1. **Process / AI engineering** — Spec-driven delivery with Cursor (or Kiro / VS Code), reusable AI instructions, prompt history, review for hallucination/ungrounded answers, and evidence that AI output is not accepted blindly.
2. **Product** — An AI-powered Support Ticket Management System where the RAG/assistant capability is designed **from the start**, not bolted on afterward.

The PDF states the application is important, but the **main assessment** is how you build it using AI and how you design and reason about the AI assistant (grounding, hallucination, retrieval quality vs deterministic ticket logic).

### Learning goals (as stated)

- Analyse requirements and create specifications for a system that includes an AI-native feature from the start.
- Use spec-driven development for both conventional CRUD and a RAG pipeline.
- Manage AI context and validate AI-generated **code** and AI-generated **answers** (grounding, hallucination).
- Test and debug both deterministic logic (ticket state machine) and probabilistic AI output (retrieval quality).

---

## 2. Required delivery workflow

```
Requirement → Specification → Plan / Tasks → Implementation → Testing → Review → Fix
```

**Do not** start by asking AI to “Build the complete application.”

---

## 3. Hygiene artefacts (generic)

Regardless of IDE, maintain steering files that include at least:

| Artefact | Purpose (as stated) |
|----------|---------------------|
| Java Spring Boot guidelines | Coding standards |
| Testing guidelines | How to test |
| API standards | API conventions |
| Documentation skills | How to document |
| RAG / Vector Store guidelines | Chunking convention, embedding model choice, retrieval-tuning defaults |
| Commands to review code, spec, generate tests | Structured reviews / test generation |
| Command to review AI output for hallucination / ungrounded answers | Grounding checks |

Example Cursor-oriented layout from the PDF (structure may vary):

- `rules/` — e.g. java-springboot, testing, api-standards, rag-vector-store
- `skills/documentation/`
- `commands/` — review-code, review-spec, generate-tests, review-rag-output

**Important:** Demonstrate **reusable AI instructions** across the project context.

---

## 4. Spec artefacts (before implementation)

Create specifications before implementation. Example structure from the PDF:

```
spec/
├── requirements.md
├── architecture.md
├── data-model.md
├── api-contract.md
├── state-machine.md
├── rag-ingestion.md
├── rag-api-contract.md
├── evaluation-strategy.md
├── ui-flow.md
└── test-strategy.md
```

`architecture.md` is later called out specifically for documenting and justifying **chunking strategy** and **embedding model choice**.

---

## 5. Prompt history

- Any prompt must be saved to a file (e.g. SpecStory for Cursor/VS Code).
- Repository should contain:
  - `.specstory/history/`
  - `docs/prompt-history.md`

---

## 6. AI mistake evidence

Identify at least one **meaningful** mistake or incorrect AI suggestion during development:

- Wrong code, **and/or**
- Ungrounded or hallucinated assistant answers  

Document this to show AI is used as an engineering assistant, not blindly accepted.

---

## 7. Token optimisation (as stated)

Use plugins such as Graphify, Caveman, Codebase-memory MCP to optimize token usage. Use prompt caching for static system-prompt portions (instructions, guardrails).

---

## 8. Named technology stack

Build using:

- Java 21
- Spring Boot
- Spring AI
- PostgreSQL / H2
- An embedding model
- A vector store (e.g. PGVector or Chroma)
- REST API
- React / Next.js or equivalent frontend
- Cursor / GitHub Copilot / Kiro

---

## 9. Application summary

**Support Ticket Management System** with:

- Conventional ticket CRUD, comments, search, status filter, persistence, backend validation, meaningful UI errors
- Backend-enforced ticket **status state machine** that allows valid status transitions and rejects invalid ones
- Natural-language Q&A over ticket history via RAG, **grounded strictly in real ticket data**, with citations and honest no-match behavior

Detailed functional requirements and acceptance criteria: see [`spec/requirements.md`](../spec/requirements.md).

---

## 10. Explicitly out of scope (as stated for the assistant)

The ask flow is a **single retrieval → generate** path, **not** an autonomous agent. The assistant must **not** independently:

- Create tickets
- Send notifications
- Chain into other tools / further actions

---

## 11. Underspecified in the PDF (not filled in here)

The following are **not** fully defined in the assessment PDF and are **not** invented in this brief:

- Full ticket field catalog beyond what is listed (e.g. how `category` is set)
- Complete REST resource map for tickets/comments (only `POST /api/ai/ask` request shape is given)
- Exact response JSON for `/api/ai/ask`
- Auth, roles, multi-tenancy, attachments, notifications
- Mandatory choice of embedding model or vector store product (examples only)
- Whether H2 is for tests only or also runtime
- How a status transition is initiated from the UI/API
- Whether only the explicitly shown transitions are allowed, or skipped transitions such as `OPEN` → `RESOLVED` are allowed

Resolve these later in dedicated specs—after confirmation—not by silent assumption.

---

## Revision

| Date | Note |
|------|------|
| 2026-09-24 | Initial faithful restatement from `docs/Assessments.pdf` (option C with `spec/requirements.md`). |
| 2026-09-24 | State-machine summary clarified; status-transition gaps aligned with `spec/requirements.md`. |
