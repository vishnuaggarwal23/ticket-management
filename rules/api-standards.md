# API standards

Cursor attaches this file via [`.cursor/rules/api-standards.mdc`](../.cursor/rules/api-standards.mdc) (pointer only). Edit **this** file; do not copy the body into the `.mdc`.

JSON REST APIs for tickets and grounded Q&A. **Ticket field catalogs, enums, ids, and validation** are agreed in [`spec/data-model.md`](../spec/data-model.md) (§6, §10, §16); **HTTP capability map** (paths/methods): [`spec/architecture.md`](../spec/architecture.md) §11 until `spec/api-contract.md` exists. **Ask** `data` field names remain in `spec/rag-api-contract.md` when added. All resource specs MUST use the envelopes, query parameters, status codes, and URI versioning defined here — they must not invent a second public JSON shape.

Backend implementation: `rules/java-springboot.md`. Tests: `rules/testing.md`. System API map and client communication: [`spec/architecture.md`](../spec/architecture.md) §10–11.

## Assessment vs project conventions vs open decisions

**Assessment PDF requires:**

- Backend APIs (consumed by the UI) that support: create ticket, list, view details, update **title, description, priority, assignee**, add comments, **keyword search**, **filter by status**.
- Backend **validation**; **meaningful** errors the UI can show.
- Backend-enforced status machine; **invalid transitions rejected**.
- `POST /api/ai/ask` with JSON body containing `"question"` (example in the PDF).
- Ask **outcomes**: a grounded answer **and** cited ticket ID(s), **or** an explicit **no relevant tickets found** (equivalent wording allowed). No agent side effects (no create-ticket, notify, or tool-chain APIs from this call).
- No secrets in the repository.

The PDF does **not** specify ticket URL paths, PUT vs PATCH, pagination, error JSON, HTTP status for illegal transitions, ask **response** field names, authentication, or URI versioning.

**This project’s approved conventions** (not PDF mandates):

- JSON request/response; Spring Boot 3 as the HTTP server; Bean Validation at the API boundary
- URI versioning under `/api/v1` for ticket (and other versioned) resources
- Shared **success** and **error** envelopes below
- Offset pagination, `sort`, keyword `q`, and `status` filter on listing APIs
- Tests follow `rules/testing.md` (JUnit 5, Mockito, PostgreSQL Testcontainers)
- Ollama, PgVector, and model settings are **configuration**, not public API fields

**Agreed — [`spec/data-model.md`](../spec/data-model.md) (DEC-03, 04, 05, 07, 08, 13; do not contradict in controllers):**

- Ticket **id:** public string `TKT-{n}` (`n` from `ticket_number_seq`, start 1001); path param `{id}` uses this value
- **Create:** `title` required (`@NotBlank`); `description`, `assignee`, `category`, `priority` optional; `priority` defaults `MEDIUM`; `description` defaults empty; **`status` not** on create — server sets `OPEN` (DEC-07)
- **JSON properties:** camelCase — `resolutionNotes`, `createdAt`, `updatedAt`, `comments`; comment create field **`body`**; enums uppercase (`OPEN`, `HIGH`, `PAYMENTS`, …)
- **Priority:** `LOW` | `MEDIUM` | `HIGH` | `CRITICAL`
- **Category (optional):** `PAYMENTS` | `SHIPMENT` | `BILLING` | `LOGIN` | `OTHER`
- **Keyword `q`:** case-insensitive match on **`title` and `description` only** (DEC-08); not comments
- **Status transition:** PATCH `status` with **target** enum per [`spec/state-machine.md`](../spec/state-machine.md) §6.1 (**DEC-06** may add a dedicated sub-resource later); illegal → **409** `ILLEGAL_TRANSITION`

**Open — resolve in API / RAG API specs when those files exist (do not assume here):**

- Ask **business** JSON inside `data` (answer text, citation structure, no-match representation). No confidence field unless a spec adds it
- Dedicated transition sub-resource vs PATCH-only (**DEC-06** — interim PATCH documented in `state-machine.md`)
- Authentication / authorization / roles (not in the assessment)
- Whether OpenAPI is produced (optional; if added it MUST match these rules and the specs)

## REST conventions

- **Resource-oriented** URLs, plural nouns, lowercase kebab-case path segments: `/api/v1/tickets`, `/api/v1/tickets/{id}/comments`.
- Standard methods:

| Method | Use |
|--------|-----|
| `GET` | Read one or list. Safe and idempotent. No body. |
| `POST` | Create a ticket or comment; `POST /api/ai/ask` (ask is not a ticket create). Not idempotent. |
| `PATCH` | Partial update of ticket fields (title, description, priority, assignee) and status **when** the contract sends a status change on the ticket resource. One update style: **PATCH**, not PUT. |
| `DELETE` | Do **not** add unless a spec agrees (assessment has no delete-ticket requirement). |
| `PUT` | Do **not** use for tickets (would imply full replace). |

- `Content-Type: application/json` and `Accept: application/json` for request/response bodies. Charset UTF-8.
- JSON property names: **camelCase**. Timestamps: ISO-8601 UTC (`Instant`). Enums: uppercase strings matching [`spec/data-model.md`](../spec/data-model.md) §5 (`OPEN`, `IN_PROGRESS`, `MEDIUM`, `PAYMENTS`, …).
- Path parameters identify a single resource. Query parameters filter, paginate, and sort **collections only**.
- Return **DTO records**, never JPA entities. Validate at the boundary (`@Valid`).
- `Location` header on **201 Created** pointing at the new resource URL.
- Empty list is **200** with `data: []` and pagination meta — not 404.
- Do not use verbs in paths (`/getTickets`, `/transition`). Do not add `/v1` inside the body.
- Prefer **additive** changes. Breaking changes require a spec revision **and** a new URI version (see versioning).
- OpenAPI is optional. If added, it must match this file and the agreed specs.

### Conventional ticket paths (project, not PDF)

Until `spec/api-contract.md` names otherwise, implement:

| Capability | Method and path |
|------------|-----------------|
| Create | `POST /api/v1/tickets` |
| List / search / filter | `GET /api/v1/tickets` |
| Get detail | `GET /api/v1/tickets/{id}` |
| Patch fields / status | `PATCH /api/v1/tickets/{id}` |
| Add comment | `POST /api/v1/tickets/{id}/comments` |

Status changes still go through the **domain state machine**. Illegal transitions use the error envelope and **409** (below). Do not add attachments, bulk ops, webhooks, or extra resources unless a spec says so.

## API versioning

- Public ticket (and future non-ask) HTTP APIs are versioned in the **URI**: `/api/v1/...`.
- The current version is **v1**. Do not ship unversioned `/api/tickets`.
- **Assessment path (required):** `POST /api/ai/ask` MUST keep working with body `{"question":"..."}`. Map the same handler at `POST /api/v1/ai/ask` so versioned clients have a consistent prefix. Do not drop the PDF path.
- Stay on v1 while changes are additive (new optional fields, new query params with defaults).
- A **breaking** change (rename/remove field, change error shape, change pagination semantics) requires `/api/v2` and a spec revision. Do not break v1 in place.
- Do not version with only custom headers or query `?version=` as the primary scheme.
- Versioning is a **project convention**, not a PDF requirement.

## Common success response structure

Every successful JSON response uses this envelope. Do not return a bare entity or a second ad-hoc wrapper per controller.

**Single resource** (get, create, patch, add comment):

```json
{
  "data": { }
}
```

`data` is the resource object whose fields are defined in `spec/api-contract.md` (or the RAG spec for ask).

**Collection** (list):

```json
{
  "data": [],
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "sort": "createdAt,desc"
  }
}
```

- `data` is always an array for list endpoints, even when there is one match.
- `meta` is **required** on paginated lists and **omitted** on single-resource responses.
- Create: HTTP **201**, envelope with `data`, plus `Location`.
- Ask: HTTP **200**, envelope with `data` holding the agreed ask payload. Honest **no relevant tickets found** is a **success body** (still 200), not the error envelope and not a fabricated citation list.

Do not put `error` on success responses. Do not put pagination `meta` on get-by-id.

## Common error response structure

Every failed JSON response uses this envelope. One `@RestControllerAdvice` maps domain and validation exceptions onto it. Controllers MUST NOT each invent a different error JSON.

```json
{
  "error": {
    "status": 400,
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed.",
    "details": [
      { "field": "title", "message": "must not be blank" }
    ],
    "timestamp": "2026-10-03T17:00:00Z",
    "path": "/api/v1/tickets"
  }
}
```

| Field | Rule |
|-------|------|
| `status` | Same as the HTTP status code |
| `code` | Stable machine-readable enum-like string (below) |
| `message` | Client-usable summary; safe to show in the UI |
| `details` | Optional array; use for field-level validation. Empty array or omit when none |
| `timestamp` | ISO-8601 UTC |
| `path` | Request path |

Do **not** include stack traces, SQL, Hibernate entity state, secrets, prompts, or model dumps.

### HTTP status and `code` mapping

| Situation | HTTP | `error.code` |
|-----------|------|----------------|
| Malformed JSON / type mismatch | 400 | `BAD_REQUEST` |
| Bean Validation / missing required field | 400 | `VALIDATION_ERROR` |
| Resource missing | 404 | `NOT_FOUND` |
| Illegal ticket status transition | 409 | `ILLEGAL_TRANSITION` |
| Unexpected server failure | 500 | `INTERNAL_ERROR` |

- **409** is the project convention for illegal transitions (not a PDF mandate). Message MUST be meaningful (current status, requested status).
- 500 `message` is generic (`An unexpected error occurred.`). Do not echo internals.
- Do not use 200 with an error envelope. Do not use 404 for “no search hits”.

## Pagination, sorting, and search (listing APIs)

Applies to `GET /api/v1/tickets` (and any later collection GET). All of these are query parameters on **GET**, not a POST search body.

| Query param | Meaning | Default / limits |
|-------------|---------|------------------|
| `page` | 0-based page index | `0` |
| `size` | Page size | Default **20**, maximum **100**. Reject `size` &lt; 1 or &gt; 100 with `VALIDATION_ERROR` |
| `sort` | `{property},{asc\|desc}` | `createdAt,desc` (or the agreed created-at field name in the data model) |
| `q` | Keyword search | Optional. Omit or blank = no keyword constraint |
| `status` | Filter by ticket status | Optional. Invalid enum value → `VALIDATION_ERROR` |

- **Search** (`q`) and **status filter** are assessment **capabilities**; the parameter **names** above are project conventions.
- Combine `q` and `status` with AND when both are present.
- `sort` MUST be restricted to a **whitelist** of resource fields (e.g. `createdAt`, `updatedAt`, `priority`, `status`). Unknown `sort` → 400 `VALIDATION_ERROR`. Never concatenate `sort` into SQL/JPQL.
- Response `meta.sort` echoes the applied sort. `totalElements` / `totalPages` reflect the filtered set.
- Do not use cursor pagination unless a later spec replaces this convention.
- Page size default/max belong in configuration for the **implementation**, but the **public query names and semantics** stay as in this table.

### `sort` whitelist (project convention until `api-contract.md` revises)

Only these `sort` properties are valid on `GET /api/v1/tickets` (reject others with 400 `VALIDATION_ERROR`):

| Property | Meaning |
|----------|---------|
| `createdAt` | Ticket created timestamp |
| `updatedAt` | Last update timestamp |
| `priority` | Priority field once defined in data model |
| `status` | `OPEN`, `IN_PROGRESS`, … |

Default when omitted: `createdAt,desc`. Example: `GET /api/v1/tickets?sort=priority,asc&status=OPEN`.

### Keyword search `q` (agreed DEC-08 — [`spec/data-model.md`](../spec/data-model.md) §15.2)

- Assessment capability: **search tickets by keyword**.
- **`q` matches `title` and `description`** (case-insensitive contains / `ILIKE`). Comments are **not** in scope unless a future spec revises DEC-08.
- Persistence uses `pg_trgm` GIN indexes per [`spec/data-model.md`](../spec/data-model.md) §14.5 (`idx_ticket_title_trgm`, `idx_ticket_description_trgm`).
- Blank or missing `q` = no keyword filter (still allow `status` filter).
- Multiple words: treat as a single phrase unless the contract defines token AND/OR.

### Comments and ticket detail

- Assessment: **add** and **view** comments.
- **View:** `GET /api/v1/tickets/{id}` returns ticket `data` **including a `comments` array** (shape in `api-contract.md`). No separate list-comments route required unless the contract adds one.
- **Add:** `POST /api/v1/tickets/{id}/comments` → **201** + `data` for the new comment (or updated ticket — contract chooses).

### PATCH body and status transitions

- Field updates: send only changed fields in PATCH body (partial update).
- **Status change:** include a `status` field (or name agreed in `state-machine.md` / `api-contract.md`) with the **target** enum value. Service runs the domain state machine; illegal → **409** `ILLEGAL_TRANSITION`.
- Do not use a separate PUT or `/transition` URL unless a future spec replaces this convention.

### Illustrative ticket payloads (envelope + `data` — validation in [`spec/data-model.md`](../spec/data-model.md) §16)

Examples use agreed field names; `spec/api-contract.md` may add narrative only.

**Create** — `POST /api/v1/tickets` → **201** + `Location: /api/v1/tickets/{id}`

```json
{
  "title": "Payment failed at checkout",
  "description": "Customer reports card declined",
  "priority": "HIGH",
  "assignee": "agent@example.com"
}
```

**Response:**

```json
{
  "data": {
    "id": "TKT-1001",
    "title": "Payment failed at checkout",
    "description": "Customer reports card declined",
    "priority": "HIGH",
    "assignee": "agent@example.com",
    "category": "PAYMENTS",
    "resolutionNotes": null,
    "status": "OPEN",
    "comments": [],
    "createdAt": "2026-10-03T12:00:00Z",
    "updatedAt": "2026-10-03T12:00:00Z"
  }
}
```

**PATCH fields** — `PATCH /api/v1/tickets/{id}` → **200**

```json
{
  "title": "Updated title",
  "assignee": "other@example.com"
}
```

**PATCH status** (legal edge only — illegal → **409**):

```json
{
  "status": "IN_PROGRESS"
}
```

**Add comment** — `POST /api/v1/tickets/{id}/comments` → **201**

```json
{
  "body": "Customer retried with a different card."
}
```

**Detail with comments** — `GET /api/v1/tickets/{id}` → **200** (`comments` array in `data`; exact comment shape in contract).

### Payload fields — source of truth

| Topic | Spec |
|-------|------|
| Columns, enums, sizes, DB indexes | [`spec/data-model.md`](../spec/data-model.md) §6, §14.5, §16 |
| REST request/response records | [`spec/data-model.md`](../spec/data-model.md) §10; refine in `spec/api-contract.md` when present |
| Ask `data` JSON | `spec/rag-api-contract.md` |

Do not invent fields beyond these specs. **Assignee** is a nullable string (email-like), not a user FK.

## RAG API

Preserve the assessment request:

```http
POST /api/ai/ask
Content-Type: application/json

{ "question": "What caused previous payment failures?" }
```

Also expose `POST /api/v1/ai/ask` with the same body and the **success envelope**. Field names **inside** `data` wait on `spec/rag-api-contract.md`.

**Frontend default:** call `POST /api/v1/ai/ask` for consistency with other `/api/v1` routes. The assessment path `/api/ai/ask` must remain supported for the same handler.

**Illustrative success shapes** (field names are placeholders until RAG API spec):

```json
{
  "data": {
    "answer": "…",
    "citedTicketIds": ["…"]
  }
}
```

```json
{
  "data": {
    "answer": "No relevant tickets found.",
    "citedTicketIds": []
  }
}
```

Grounding rules: `rules/rag-vector-store.md`. Review: `commands/review-rag-output.md`.

- Blank/missing `question` → 400 `VALIDATION_ERROR`.
- No relevant retrieval → 200 + success envelope + honest no-match inside `data` (wording/fields per RAG spec).
- Do not expose prompts, chunk dumps, model names, Ollama URLs, top-K, or vector internals.
- Do not add agent, chat-session, tool, notification, or “AI creates a ticket” endpoints.

## Errors, validation, security

- Validate at the API boundary. Field-level `details` for validation failures.
- Do not invent auth. If added later, it must be an agreed spec — not silent Spring Security on all routes.
- Do not log secrets, credentials, unnecessary full ticket bodies, prompts, or raw model output by default.
- Never put credentials or machine-specific URLs in committed API examples except placeholder **names**.

## Testing and consistency

- Prove list pagination (`page`/`size`/`meta`), `sort` whitelist, `q`, `status` filter, success envelope, and error envelope (400/404/409/500) in controller and persistence-backed tests per `rules/testing.md`.
- RAG **quality** of generated text is not a substitute for contract tests of ask validation and no-match/citation **shape** once specified.
- Do not implement endpoints that contradict `spec/` or these assessment capabilities.

## Do not

- Do not treat `/api/v1`, PATCH, 409, or these envelopes as PDF requirements.
- Do not silently answer open questions in [`spec/requirements.md`](../spec/requirements.md) — use **§10.1 (OQ-*)** and **§10.2 (DEC-*)**; do not implement still-open **DEC-*** as fixed behaviour (**DEC-01, 02, 06, 09–12, 14, 15**, ask `data` fields, etc.). **DEC-03, 04, 05, 07, 08, 13** are agreed via [`spec/data-model.md`](../spec/data-model.md).
- Do not prescribe Spring Security, API keys, or multi-tenancy.
- Do not return persistence entities or a second JSON error shape from one controller.
- Do not use PUT, unversioned `/api/tickets`, or cursor pagination unless a spec revises this file.

---

## Revision history

| Date | Note |
|------|------|
| 2026-09-24 | Initial REST conventions: envelopes, `/api/v1`, list params, ticket and ask capabilities. |
| 2026-10-03 | Aligned with approved stack; AI and vector settings as configuration, not public API fields. |
| 2026-10-04 | SDD expansion: assessment vs convention vs open decisions; [`spec/requirements.md`](../spec/requirements.md) §10 handoff. |
| 2026-10-04 | Synced with expanded [`spec/requirements.md`](../spec/requirements.md) and [`spec/architecture.md`](../spec/architecture.md) API map. |
| 2026-10-04 | Added revision history section. |
| 2026-10-04 | Locked ticket id, enums, create/PATCH fields, `q` scope from agreed [`spec/data-model.md`](../spec/data-model.md); trgm index pointer §14.5. |
| 2026-10-04 | Ticket HTTP map: [`spec/architecture.md`](../spec/architecture.md) §11 until `api-contract.md` added. |
| 2026-10-04 | Status transitions point to draft [`spec/state-machine.md`](../spec/state-machine.md) §6.1. |
