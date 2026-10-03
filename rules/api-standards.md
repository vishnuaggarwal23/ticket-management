# API standards

JSON REST APIs for tickets and grounded Q&A. Contracts live in `spec/api-contract.md` and `spec/rag-api-contract.md` once those files exist and are agreed. These rules do not replace those specs.

## Assessment vs project conventions vs open decisions

**Assessment PDF requires:**

- Backend APIs (consumed by the UI) that support: create ticket, list, view details, update **title, description, priority, assignee**, add comments, **keyword search**, **filter by status**.
- Backend **validation**; **meaningful** errors the UI can show.
- Backend-enforced status machine; **invalid transitions rejected**.
- `POST /api/ai/ask` with JSON body containing `"question"` (example in the PDF).
- Ask **outcomes**: a grounded answer **and** cited ticket ID(s), **or** an explicit **no relevant tickets found** (equivalent wording allowed). No agent side effects (no create-ticket, notify, or tool-chain APIs from this call).
- No secrets in the repository.

The PDF does **not** specify ticket URL paths, PUT vs PATCH, pagination, error JSON, HTTP status for illegal transitions, ask **response** field names, or authentication.

**This project’s approved conventions** (not PDF mandates): JSON request/response; Spring Boot 3 as the HTTP server; Bean Validation at the API boundary; do not leak internals; tests follow the testing standards (JUnit 5, PostgreSQL Testcontainers for persistence-backed API tests). Ollama, PgVector, and model settings are **configuration**, not public API fields.

**Open — resolve in API / RAG API / state-machine specs before implementing (do not assume):**

- Ticket and comment **paths**, path parameters, and identifier format (`TKT-1001` is an example in a sample question only).
- Create/update **payloads**, required fields, `category`, resolution notes, how a **status transition** is requested.
- PUT vs PATCH (or a dedicated transition resource).
- List **pagination**, sort, and query-parameter names for search/filter.
- Error **JSON** shape and which HTTP status means “illegal transition” vs validation vs not found.
- Ask **response** JSON (answer text, citation structure, no-match representation). No confidence field unless a spec adds it.
- API **versioning** scheme and whether OpenAPI is produced.
- Authentication / authorization / roles (not in the assessment).

## HTTP and REST (engineering guidance)

- Prefer **resource-oriented** URLs and standard methods: GET read, POST create, PUT or PATCH update — **one** update style, chosen in `spec/api-contract.md`.
- Use JSON (`Content-Type: application/json`) unless a later spec says otherwise.
- `/api/` as a **conventional** prefix is consistent with the documented ask path; **do not** treat `/api/tickets` or nested comment URLs as assessment-mandated. Propose them in the API contract if that is the agreed design.
- Request/response types should be explicit DTOs at the HTTP boundary, validated there, then mapped to domain types. Do not expose persistence entities as the public contract.
- Pagination and filtering: support list + status filter + keyword search **capabilities**; **do not** freeze page-size query names, cursor vs offset, or a response envelope here.
- Prefer **additive** contract changes after a spec is agreed. Breaking changes need a spec revision. Do not invent a versioning URL scheme (`/v1`) without an approved decision.
- OpenAPI (or similar) is **optional**. If added, it must match the approved specs. No mandatory documentation framework.

## Ticket APIs and state transitions

- Capabilities listed in the assessment must be reachable over HTTP. Exact routes and bodies wait on `spec/api-contract.md`.
- Status rules are **backend** domain logic, not “the UI hid the button.” The API must reject illegal transitions with a **client-usable** error (clear message). **Do not** lock 409 vs 400 (or another code) in these rules.
- Do not add extra ticket resources (attachments, assignees-as-users, bulk ops, webhooks) unless a spec says so.
- Do not add transition endpoints or status fields beyond what the API and state-machine specs define.

## RAG API

Preserve:

```http
POST /api/ai/ask
Content-Type: application/json

{ "question": "What caused previous payment failures?" }
```

- The contract must be able to represent: grounded answer, **ticket ID** citations tied to retrieval, and honest no-match. **Field names and nesting are unspecified** until `spec/rag-api-contract.md`.
- Do not expose prompts, retrieved chunk dumps, model names, Ollama URLs, top-K, or vector internals unless a spec explicitly makes them part of the public API (default: they are not).
- Do not add agent, chat-session, tool, notification, or “AI creates a ticket” endpoints.

## Errors, validation, security

- Validate at the API boundary. Return **useful** messages and, where practical, field-level hints for validation failures — exact JSON is specified in the API contract, not here.
- Typical HTTP categories (guidance, not a frozen map): client input errors, not found, illegal state, unexpected server failure. Server faults must **not** include stack traces, SQL, or secret material.
- Do not invent auth. If it is added later, it must be an agreed spec — not silent Spring Security on all routes.
- Do not log secrets, credentials, or unnecessary full ticket bodies. Avoid logging prompts and raw model output by default.
- Never put credentials or machine-specific URLs in committed API examples except placeholder **names**.

## Testing and consistency

- API behavior is proven against **agreed** contracts and the state machine (deterministic tests). Persistence-backed API tests use PostgreSQL/Testcontainers per testing standards.
- RAG **quality** of generated text is not a substitute for contract tests of ask request validation and no-match/citation **shape** once that shape is specified.
- Do not implement endpoints that contradict `spec/` or these assessment capabilities.

## Do not

- Do not treat proposed paths, error envelopes, 409, or pagination as PDF requirements.
- Do not silently answer open questions in `spec/requirements.md`.
- Do not prescribe Spring Security, API keys, or multi-tenancy.
