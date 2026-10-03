# HTTP API contract — tickets, comments, and ask (boundary)

> **Status:** draft (2026-10-04) — ticket REST paths and payloads align with **Convention** in [`rules/api-standards.md`](../rules/api-standards.md) and agreed [`data-model.md`](data-model.md). Resolves **OQ-04** for ticket/comment HTTP; **DEC-14** interim alignment recorded §10.  
> **Primary source:** `docs/Assessments.pdf` (capabilities restated in [`requirements.md`](requirements.md)).  
> **Related:** Envelopes, status codes, pagination query params → `rules/api-standards.md`. Status transitions → [`state-machine.md`](state-machine.md). Ask `data` field detail → `rag-api-contract.md` (**DEC-11** open). System map → [`architecture.md`](architecture.md) §11.

---

## 1. Problem and context

The assessment requires **REST** ticket operations (create, list, detail, update fields, comments, keyword search, status filter, validation, status machine) and **`POST /api/ai/ask`** with a `question` field. The PDF does not define URL paths, pagination, or error JSON.

This document is the **implementable contract** for:

- Request and response **payloads** (JSON inside success `data`, or raw request bodies where noted)
- **Positive and negative** scenarios per endpoint
- How this contract uses the **common success and error envelopes** (defined in `rules/api-standards.md`, summarized §2)

Field types, enums, DB columns, and Bean Validation limits → [`data-model.md`](data-model.md) §5, §10, §16.

---

## 2. Cross-cutting conventions

### 2.1 Transport

| Rule | Value |
|------|--------|
| Protocol | HTTPS in production; HTTP acceptable locally |
| Format | `Content-Type: application/json`, `Accept: application/json`, UTF-8 |
| Versioning | Ticket resources under **`/api/v1/...`** |
| Ask paths | **`POST /api/ai/ask`** (**PDF**, required) and **`POST /api/v1/ai/ask`** (alias, same handler) |

### 2.2 Request body shape

Ticket and comment **write** requests send a **flat JSON object** at the root (not wrapped in `data`).

```json
{ "title": "Example" }
```

### 2.3 Common success response structure (**Convention**)

All successful responses use:

```json
{
  "data": { }
}
```

| Response kind | HTTP | `data` shape | `meta` |
|---------------|------|--------------|--------|
| Single resource | 200 or 201 | Object | **Omitted** |
| Collection (list) | 200 | Array (may be empty) | **Required** pagination object |
| Ask (grounded or no-match) | 200 | Object per §6 / `rag-api-contract.md` | Omitted |

**201 Created** responses MUST include header:

```http
Location: /api/v1/tickets/{id}
```

(or `/api/v1/tickets/{id}/comments/{commentId}` when the created resource is a comment — see §5.1).

List example:

```json
{
  "data": [
    {
      "id": "TKT-1001",
      "title": "Payment failed",
      "status": "OPEN",
      "priority": "HIGH",
      "assignee": "agent@example.com",
      "category": "PAYMENTS",
      "createdAt": "2026-10-03T12:00:00Z",
      "updatedAt": "2026-10-03T12:00:00Z"
    }
  ],
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "sort": "createdAt,desc"
  }
}
```

### 2.4 Common error response structure (**Convention**)

All error responses use:

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
| `status` | Same numeric value as HTTP status |
| `code` | Stable string; see §2.5 |
| `message` | Human-readable; safe for UI |
| `details` | Optional; field-level validation entries |
| `timestamp` | ISO-8601 UTC |
| `path` | Request path (no query string required) |

Never return `error` on HTTP 2xx. Never use 404 for “empty search results” (use 200 + `data: []`).

### 2.5 HTTP status and `error.code` mapping

| Situation | HTTP | `error.code` | Notes |
|-----------|------|--------------|-------|
| Malformed JSON / wrong JSON type | 400 | `BAD_REQUEST` | Cannot parse body |
| Bean Validation / business validation | 400 | `VALIDATION_ERROR` | Include `details` when field-level |
| Unknown ticket id | 404 | `NOT_FOUND` | Message e.g. `Ticket not found: TKT-1001` |
| Illegal status transition | 409 | `ILLEGAL_TRANSITION` | Per [`state-machine.md`](state-machine.md); DB unchanged |
| Unexpected server fault | 500 | `INTERNAL_ERROR` | Generic message; no stack/SQL |

**409 message example:** `Cannot transition from CLOSED to OPEN`.

### 2.6 Shared enums (JSON strings, uppercase)

From [`data-model.md`](data-model.md) §5:

| Enum | Values |
|------|--------|
| `status` | `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED` |
| `priority` | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` |
| `category` | `PAYMENTS`, `SHIPMENT`, `BILLING`, `LOGIN`, `OTHER` |

Invalid enum on write or query → **400** `VALIDATION_ERROR`.

### 2.7 Ticket identifier

Public id: **`TKT-{n}`** (`n` ≥ 1001, **DEC-04**). Path parameter `{id}` is this string.

### 2.8 Base URL and URI catalog

**Base URL (**Convention**):** deployment-specific origin + optional context path. Examples below use host `http://localhost:8080` (local Spring Boot default). Clients (Vite) typically read `VITE_API_BASE_URL` and prepend it to relative paths.

| Environment | Example base | Notes |
|-------------|--------------|-------|
| Local dev | `http://localhost:8080` | No trailing slash on base |
| Behind proxy | `https://support.example.com` | TLS required in production |

**Path parameters**

| Name | Format | Example |
|------|--------|---------|
| `{id}` | `TKT-{n}` | `TKT-1001` |
| `{commentId}` | UUID string | `a1b2c3d4-e5f6-7890-abcd-ef1234567890` |

**Complete URI reference** (method → path; query optional)

| # | Method | URI (relative to base) | Purpose |
|---|--------|------------------------|---------|
| 1 | `POST` | `/api/v1/tickets` | Create ticket |
| 2 | `GET` | `/api/v1/tickets` | List (optional `page`, `size`, `sort`, `q`, `status`) |
| 3 | `GET` | `/api/v1/tickets/{id}` | Ticket detail + comments |
| 4 | `PATCH` | `/api/v1/tickets/{id}` | Partial update / status transition |
| 5 | `POST` | `/api/v1/tickets/{id}/comments` | Add comment |
| 6 | `POST` | `/api/ai/ask` | Ask (**PDF** path) |
| 7 | `POST` | `/api/v1/ai/ask` | Ask (versioned alias) |

**Example absolute URIs**

```text
http://localhost:8080/api/v1/tickets
http://localhost:8080/api/v1/tickets?page=0&size=20&sort=createdAt,desc
http://localhost:8080/api/v1/tickets?q=payment&status=OPEN
http://localhost:8080/api/v1/tickets/TKT-1001
http://localhost:8080/api/v1/tickets/TKT-1001/comments
http://localhost:8080/api/v1/ai/ask
http://localhost:8080/api/ai/ask
```

**URIs that do not exist (do not implement without a new spec)**

```text
PUT    /api/v1/tickets/{id}
DELETE /api/v1/tickets/{id}
POST   /api/v1/tickets/search          (use GET + query params)
POST   /api/v1/tickets/{id}/transition (use PATCH + status field)
GET    /api/v1/tickets/{id}/comments   (comments only on detail GET)
```

### 2.9 Standard request and response headers

**Request (writes and reads)**

```http
Accept: application/json
Content-Type: application/json
```

`GET` list/detail: no `Content-Type` body; `Accept` recommended.

**Success response (typical)**

```http
HTTP/1.1 200 OK
Content-Type: application/json;charset=UTF-8
```

**201 Created** additionally:

```http
Location: /api/v1/tickets/TKT-1001
```

**Error response**

```http
HTTP/1.1 400 Bad Request
Content-Type: application/json;charset=UTF-8
```

No `WWW-Authenticate` in v1 (no auth in assessment).

### 2.10 Example error bodies (copy-paste for tests)

**Validation — missing title on create**

```http
POST /api/v1/tickets HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{}
```

```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "error": {
    "status": 400,
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed.",
    "details": [
      { "field": "title", "message": "must not be blank" }
    ],
    "timestamp": "2026-10-04T10:15:00Z",
    "path": "/api/v1/tickets"
  }
}
```

**Not found — unknown ticket**

```http
GET /api/v1/tickets/TKT-9999 HTTP/1.1
Host: localhost:8080
Accept: application/json
```

```http
HTTP/1.1 404 Not Found
Content-Type: application/json

{
  "error": {
    "status": 404,
    "code": "NOT_FOUND",
    "message": "Ticket not found: TKT-9999",
    "details": [],
    "timestamp": "2026-10-04T10:16:00Z",
    "path": "/api/v1/tickets/TKT-9999"
  }
}
```

**Illegal transition**

```http
PATCH /api/v1/tickets/TKT-1001 HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{ "status": "OPEN" }
```

(Assume current `status` is `CLOSED`.)

```http
HTTP/1.1 409 Conflict
Content-Type: application/json

{
  "error": {
    "status": 409,
    "code": "ILLEGAL_TRANSITION",
    "message": "Cannot transition from CLOSED to OPEN",
    "details": [],
    "timestamp": "2026-10-04T10:17:00Z",
    "path": "/api/v1/tickets/TKT-1001"
  }
}
```

**Malformed JSON**

```http
POST /api/v1/tickets HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{ "title":
```

```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "error": {
    "status": 400,
    "code": "BAD_REQUEST",
    "message": "Malformed JSON request body.",
    "details": [],
    "timestamp": "2026-10-04T10:18:00Z",
    "path": "/api/v1/tickets"
  }
}
```

### 2.11 Complete endpoint catalog (method, URI, payloads)

All paths are relative to **base URL** (§2.8). **Write** requests use flat JSON at the root (§2.2). **Success** responses wrap resources in `data` (§2.3); **errors** use `error` (§2.4).

| # | HTTP method | URI path | Request body | Query params | Success HTTP | Response `data` type | `meta` |
|---|-------------|----------|--------------|--------------|--------------|----------------------|--------|
| 1 | `POST` | `/api/v1/tickets` | `CreateTicketRequest` (§3.4) | — | **201** | `TicketDetail` | omitted |
| 2 | `GET` | `/api/v1/tickets` | — | `page`, `size`, `sort`, `q`, `status` (§4.2) | **200** | `TicketSummary[]` | **required** |
| 3 | `GET` | `/api/v1/tickets/{id}` | — | — | **200** | `TicketDetail` | omitted |
| 4 | `PATCH` | `/api/v1/tickets/{id}` | `UpdateTicketRequest` (§3.4) | — | **200** | `TicketDetail` | omitted |
| 5 | `POST` | `/api/v1/tickets/{id}/comments` | `CreateCommentRequest` (§3.4) | — | **201** | `Comment` | omitted |
| 6 | `POST` | `/api/ai/ask` | `AskRequest` (§3.4) | — | **200** | `AskResponseData` (§3.5) | omitted |
| 7 | `POST` | `/api/v1/ai/ask` | `AskRequest` (§3.4) | — | **200** | `AskResponseData` (§3.5) | omitted |

**Path parameters:** `{id}` = `TKT-{n}` (§2.7); `{commentId}` only appears in **201** `Location` for comments (§5.1), not as a separate GET route in v1.

**Idempotency / safety:** GET list and GET detail are safe and idempotent. POST and PATCH are not idempotent. Repeating the same PATCH `status` after a successful transition may return **409** if the ticket is already in the target state (self-transition — [`state-machine.md`](state-machine.md) §6.1.1).

---

## 3. Resource models (JSON schemas)

### 3.1 `TicketSummary` (list items)

| Property | Type | Required in response | Notes |
|----------|------|----------------------|-------|
| `id` | string | yes | `TKT-{n}` |
| `title` | string | yes | |
| `status` | string | yes | `TicketStatus` |
| `priority` | string | yes | |
| `assignee` | string \| null | yes | |
| `category` | string \| null | yes | |
| `createdAt` | string (date-time) | yes | UTC |
| `updatedAt` | string (date-time) | yes | UTC |

### 3.2 `TicketDetail` (get / create / patch response)

Extends summary fields plus:

| Property | Type | Required in response | Notes |
|----------|------|----------------------|-------|
| `description` | string | yes | May be `""` |
| `resolutionNotes` | string \| null | yes | **DEC-05** |
| `comments` | `Comment[]` | yes | Ordered `createdAt` ascending |

### 3.3 `Comment`

| Property | Type | Required | Notes |
|----------|------|----------|-------|
| `id` | string (UUID) | yes | |
| `body` | string | yes | |
| `createdAt` | string (date-time) | yes | UTC |

### 3.4 Write models (requests)

**Create ticket** — `CreateTicketRequest`

| Property | Type | Required | Validation |
|----------|------|----------|------------|
| `title` | string | yes | Non-blank, max 500 |
| `description` | string | no | Max 100_000; default `""` |
| `priority` | string | no | Enum; default `MEDIUM` |
| `assignee` | string | no | Max 320; email format if present |
| `category` | string | no | Enum |
| `status` | — | **forbidden** | **DEC-07** — if sent → **400** `VALIDATION_ERROR` |

**Update ticket** — `UpdateTicketRequest` (PATCH)

| Property | Type | Required | Notes |
|----------|------|----------|-------|
| `title` | string | no | Max 500 |
| `description` | string | no | Max 100_000 |
| `priority` | string | no | Enum |
| `assignee` | string | no | Max 320 |
| `category` | string | no | Enum |
| `resolutionNotes` | string | no | Max 100_000 |
| `status` | string | no | Target state; state machine |

Only properties **present** in JSON are applied (**partial PATCH**). Omitted properties leave DB values unchanged.

**Create comment** — `CreateCommentRequest`

| Property | Type | Required | Validation |
|----------|------|----------|------------|
| `body` | string | yes | Non-blank, max 50_000 |

**Ask** — `AskRequest` (**PDF**)

| Property | Type | Required | Validation |
|----------|------|----------|------------|
| `question` | string | yes | Non-blank after trim |

### 3.5 `AskResponseData` (success `data` for ask)

Returned inside the success envelope on **200** for §6.1. Field names are **interim** until **DEC-11** / `rag-api-contract.md` finalizes wording.

| Property | Type | Required in response | Notes |
|----------|------|----------------------|-------|
| `answer` | string | yes | Grounded narrative or honest no-match phrase |
| `citedTicketIds` | string[] | yes | Public ticket ids (`TKT-{n}`); empty when no-match; each id MUST exist in DB when non-empty (**PDF**) |

---

## 4. Endpoints — tickets

Per-endpoint detail below. Summary catalog: §2.11.

### 4.1 `POST /api/v1/tickets` — create ticket

| | |
|--|--|
| **URI** | `POST {base}/api/v1/tickets` |
| **Capability (**PDF**)** | Create ticket |
| **Auth** | None (assessment) |
| **Idempotent** | No |

**Request body** — `CreateTicketRequest` (§3.4); flat JSON, not wrapped in `data`.

**Full HTTP example (positive)**

```http
POST /api/v1/tickets HTTP/1.1
Host: localhost:8080
Accept: application/json
Content-Type: application/json

{
  "title": "Payment failed at checkout",
  "description": "Card declined at step 3",
  "priority": "HIGH",
  "assignee": "agent@example.com",
  "category": "PAYMENTS"
}
```

```http
HTTP/1.1 201 Created
Content-Type: application/json;charset=UTF-8
Location: /api/v1/tickets/TKT-1001

{
  "data": {
    "id": "TKT-1001",
    "title": "Payment failed at checkout",
    "description": "Card declined at step 3",
    "status": "OPEN",
    "priority": "HIGH",
    "assignee": "agent@example.com",
    "category": "PAYMENTS",
    "resolutionNotes": null,
    "comments": [],
    "createdAt": "2026-10-03T12:00:00Z",
    "updatedAt": "2026-10-03T12:00:00Z"
  }
}
```

**cURL (minimal create)**

```bash
curl -sS -X POST 'http://localhost:8080/api/v1/tickets' \
  -H 'Accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{"title":"Quick intake ticket"}'
```

**cURL (full create, show `Location`)**

```bash
curl -sS -D - -X POST 'http://localhost:8080/api/v1/tickets' \
  -H 'Accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "Payment failed at checkout",
    "description": "Card declined",
    "priority": "HIGH",
    "assignee": "agent@example.com",
    "category": "PAYMENTS"
  }'
```

#### 4.1.1 Scenarios

| Scenario | HTTP | `error.code` | Expected behaviour |
|----------|------|--------------|-------------------|
| Valid minimal body `{ "title": "x" }` | 201 | — | `status` = `OPEN`, `priority` = `MEDIUM`, `description` = `""` |
| Valid full optional fields | 201 | — | Persisted; ingestion hook runs (async timing → `rag-ingestion.md`) |
| Missing `title` | 400 | `VALIDATION_ERROR` | `details` on `title` (see §2.10) |
| Blank `title` `"   "` | 400 | `VALIDATION_ERROR` | Treated as blank after trim |
| `title` over 500 chars | 400 | `VALIDATION_ERROR` | |
| Invalid `priority` / `category` | 400 | `VALIDATION_ERROR` | e.g. `"priority": "URGENT"` |
| Client sends `status` | 400 | `VALIDATION_ERROR` | Field `status` rejected on create (**DEC-07**) |
| Malformed JSON | 400 | `BAD_REQUEST` | §2.10 |
| Unknown JSON properties | 201 | — | **Convention:** ignore unknown keys (e.g. `"foo": 1`) |

**Negative example — `status` on create**

```http
POST /api/v1/tickets HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{ "title": "Test", "status": "IN_PROGRESS" }
```

```json
{
  "error": {
    "status": 400,
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed.",
    "details": [
      { "field": "status", "message": "must not be provided on create" }
    ],
    "timestamp": "2026-10-04T11:00:00Z",
    "path": "/api/v1/tickets"
  }
}
```

**Traceability:** FEAT-01, AC-FEAT-01-*, AC-DM-01.

---

### 4.2 `GET /api/v1/tickets` — list, search, filter

| | |
|--|--|
| **URI** | `GET {base}/api/v1/tickets` |
| **Query** | `page`, `size`, `sort`, `q`, `status` (all optional except defaults apply) |
| **Body** | None |
| **Safe / idempotent** | Yes |

**Capabilities (**PDF**):** list tickets, keyword search, filter by status.

#### 4.2.0 Example URIs

| Use case | URI |
|----------|-----|
| Default list (page 0, size 20, newest first) | `/api/v1/tickets` |
| Explicit pagination | `/api/v1/tickets?page=1&size=10` |
| Keyword search | `/api/v1/tickets?q=payment` |
| Status filter only | `/api/v1/tickets?status=IN_PROGRESS` |
| Search + filter + sort | `/api/v1/tickets?q=checkout&status=OPEN&sort=priority,asc&page=0&size=50` |
| Sort by last update | `/api/v1/tickets?sort=updatedAt,desc` |

**Full HTTP example (combined query)**

```http
GET /api/v1/tickets?page=0&size=20&sort=createdAt,desc&q=payment&status=OPEN HTTP/1.1
Host: localhost:8080
Accept: application/json
```

```http
HTTP/1.1 200 OK
Content-Type: application/json;charset=UTF-8

{
  "data": [
    {
      "id": "TKT-1001",
      "title": "Payment failed at checkout",
      "status": "OPEN",
      "priority": "HIGH",
      "assignee": "agent@example.com",
      "category": "PAYMENTS",
      "createdAt": "2026-10-03T12:00:00Z",
      "updatedAt": "2026-10-03T12:00:00Z"
    },
    {
      "id": "TKT-1004",
      "title": "Payment gateway timeout",
      "status": "OPEN",
      "priority": "MEDIUM",
      "assignee": null,
      "category": "PAYMENTS",
      "createdAt": "2026-10-02T09:30:00Z",
      "updatedAt": "2026-10-02T09:30:00Z"
    }
  ],
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 2,
    "totalPages": 1,
    "sort": "createdAt,desc"
  }
}
```

**cURL**

```bash
curl -sS 'http://localhost:8080/api/v1/tickets?q=payment&status=OPEN' \
  -H 'Accept: application/json'
```

**Empty result (still 200)**

```http
GET /api/v1/tickets?q=nonexistentterm HTTP/1.1
Host: localhost:8080
Accept: application/json
```

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

| Query | Type | Default | Validation |
|-------|------|---------|------------|
| `page` | int | `0` | ≥ 0 |
| `size` | int | `20` | 1–100 |
| `sort` | string | `createdAt,desc` | Whitelist §4.2.1 |
| `q` | string | omitted | **DEC-08:** matches `title` OR `description` (case-insensitive contains) |
| `status` | string | omitted | Must be valid `TicketStatus` if present |

#### 4.2.1 `sort` whitelist

| Property | Example |
|----------|---------|
| `createdAt` | `createdAt,desc` |
| `updatedAt` | `updatedAt,desc` |
| `priority` | `priority,asc` |
| `status` | `status,asc` |

Unknown property or invalid direction → **400** `VALIDATION_ERROR`.

**Negative example — invalid `sort`**

```http
GET /api/v1/tickets?sort=title,desc HTTP/1.1
Host: localhost:8080
Accept: application/json
```

```json
{
  "error": {
    "status": 400,
    "code": "VALIDATION_ERROR",
    "message": "Invalid sort parameter.",
    "details": [
      { "field": "sort", "message": "property title is not sortable" }
    ],
    "timestamp": "2026-10-04T11:05:00Z",
    "path": "/api/v1/tickets"
  }
}
```

**Negative example — invalid `status` filter**

```http
GET /api/v1/tickets?status=OPENED HTTP/1.1
Host: localhost:8080
Accept: application/json
```

→ **400** `VALIDATION_ERROR` on query param `status`.

#### 4.2.2 Scenarios

`data` is `TicketSummary[]`; `meta` required (§2.3).

| Scenario | HTTP | Notes |
|----------|------|-------|
| No tickets in DB | 200 | `data: []`, `totalElements: 0` |
| Default pagination | 200 | First page, size 20, sort `createdAt,desc` |
| `q` matches title | 200 | Ticket included |
| `q` matches description only | 200 | Ticket included |
| `q` matches comment only | 200 | Ticket **excluded** (**DEC-08**) |
| `status=OPEN` | 200 | Only open tickets |
| `q` + `status` combined | 200 | AND semantics |
| `size=0` or `size=101` | 400 | `VALIDATION_ERROR` |
| `page=-1` | 400 | `VALIDATION_ERROR` |
| Invalid `status` query | 400 | `VALIDATION_ERROR` |
| Invalid `sort` | 400 | `VALIDATION_ERROR` |

**Traceability:** FEAT-02, FEAT-06, FEAT-07, AC-DM-05.

---

### 4.3 `GET /api/v1/tickets/{id}` — ticket detail

| | |
|--|--|
| **URI** | `GET {base}/api/v1/tickets/{id}` |
| **Example** | `GET http://localhost:8080/api/v1/tickets/TKT-1001` |
| **Capability (**PDF**)** | View ticket details including comments |

**Full HTTP example (positive)**

```http
GET /api/v1/tickets/TKT-1001 HTTP/1.1
Host: localhost:8080
Accept: application/json
```

```http
HTTP/1.1 200 OK
Content-Type: application/json;charset=UTF-8

{
  "data": {
    "id": "TKT-1001",
    "title": "Payment failed at checkout",
    "description": "Card declined at step 3",
    "status": "IN_PROGRESS",
    "priority": "HIGH",
    "assignee": "agent@example.com",
    "category": "PAYMENTS",
    "resolutionNotes": null,
    "comments": [
      {
        "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "body": "Customer retried with a different card.",
        "createdAt": "2026-10-03T12:05:00Z"
      }
    ],
    "createdAt": "2026-10-03T12:00:00Z",
    "updatedAt": "2026-10-03T12:05:00Z"
  }
}
```

**cURL**

```bash
curl -sS 'http://localhost:8080/api/v1/tickets/TKT-1001' -H 'Accept: application/json'
```

| Scenario | HTTP | `error.code` |
|----------|------|--------------|
| Existing id | 200 | — |
| Unknown id `TKT-9999` | 404 | `NOT_FOUND` (§2.10) |
| Malformed id path | 404 | `NOT_FOUND` |

**Traceability:** FEAT-03, FEAT-05 (view comments).

---

### 4.4 `PATCH /api/v1/tickets/{id}` — update fields and status

| | |
|--|--|
| **URI** | `PATCH {base}/api/v1/tickets/{id}` |
| **Example** | `PATCH http://localhost:8080/api/v1/tickets/TKT-1001` |
| **Content-Type** | `application/json` (partial body) |

**Capabilities (**PDF**):** update title, description, priority, assignee; backend-enforced status transitions.

**Status changes:** send `status` with the **target** value. Rules → [`state-machine.md`](state-machine.md). **DEC-06 interim:** status on PATCH body (this contract); no `/transition` sub-resource.

**Request examples**

Fields only:

```json
{
  "title": "Updated title",
  "assignee": "other@example.com"
}
```

Status only (legal edge):

```json
{
  "status": "IN_PROGRESS"
}
```

Combined (all fields valid; illegal status fails entire request per [`state-machine.md`](state-machine.md) §6.4):

```json
{
  "resolutionNotes": "Reset payment gateway cache.",
  "status": "RESOLVED"
}
```

**Full HTTP — field update (positive)**

```http
PATCH /api/v1/tickets/TKT-1001 HTTP/1.1
Host: localhost:8080
Accept: application/json
Content-Type: application/json

{
  "title": "Payment failed — escalated",
  "priority": "CRITICAL",
  "assignee": "lead@example.com"
}
```

```http
HTTP/1.1 200 OK
Content-Type: application/json;charset=UTF-8

{
  "data": {
    "id": "TKT-1001",
    "title": "Payment failed — escalated",
    "description": "Card declined at step 3",
    "status": "OPEN",
    "priority": "CRITICAL",
    "assignee": "lead@example.com",
    "category": "PAYMENTS",
    "resolutionNotes": null,
    "comments": [],
    "createdAt": "2026-10-03T12:00:00Z",
    "updatedAt": "2026-10-04T08:00:00Z"
  }
}
```

**Full HTTP — status transition T1 (`OPEN` → `IN_PROGRESS`)**

```http
PATCH /api/v1/tickets/TKT-1001 HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{ "status": "IN_PROGRESS" }
```

→ **200**; `data.status` is `IN_PROGRESS`; `updatedAt` changes.

**cURL — lifecycle step**

```bash
curl -sS -X PATCH 'http://localhost:8080/api/v1/tickets/TKT-1001' \
  -H 'Content-Type: application/json' \
  -d '{"status":"IN_PROGRESS"}'
```

**Happy-path URI sequence (Flow A — [`requirements.md`](requirements.md))**

```text
POST   /api/v1/tickets                              → 201 TKT-1001, status OPEN
PATCH  /api/v1/tickets/TKT-1001  {"status":"IN_PROGRESS"}
PATCH  /api/v1/tickets/TKT-1001  {"resolutionNotes":"...","status":"RESOLVED"}
PATCH  /api/v1/tickets/TKT-1001  {"status":"CLOSED"}
GET    /api/v1/tickets/TKT-1001                   → status CLOSED
```

#### 4.4.1 Legal status transitions (T1–T5) — request and response

Rules: [`state-machine.md`](state-machine.md) §5.1, §5.5. Assume ticket `TKT-1001` exists and is in the **current** column before each PATCH.

| ID | Current `status` | Request body (`UpdateTicketRequest`) | Success HTTP | Response `data.status` |
|----|------------------|--------------------------------------|--------------|------------------------|
| **T1** | `OPEN` | `{ "status": "IN_PROGRESS" }` | **200** | `IN_PROGRESS` |
| **T2** | `IN_PROGRESS` | `{ "status": "RESOLVED" }` or `{ "resolutionNotes": "…", "status": "RESOLVED" }` | **200** | `RESOLVED` |
| **T3** | `RESOLVED` | `{ "status": "CLOSED" }` | **200** | `CLOSED` |
| **T4** | `OPEN` | `{ "status": "CANCELLED" }` | **200** | `CANCELLED` |
| **T5** | `IN_PROGRESS` | `{ "status": "CANCELLED" }` | **200** | `CANCELLED` |

**Example response shape (T1)** — same `TicketDetail` as §4.3; only `status` and `updatedAt` differ from prior GET:

```json
{
  "data": {
    "id": "TKT-1001",
    "title": "Payment failed at checkout",
    "description": "Card declined at step 3",
    "status": "IN_PROGRESS",
    "priority": "HIGH",
    "assignee": "agent@example.com",
    "category": "PAYMENTS",
    "resolutionNotes": null,
    "comments": [],
    "createdAt": "2026-10-03T12:00:00Z",
    "updatedAt": "2026-10-04T09:00:00Z"
  }
}
```

**Illegal examples (must not return 200)** — full matrix in [`state-machine.md`](state-machine.md) §5.6:

| Example | Current | Request `status` | HTTP | `error.code` |
|---------|---------|------------------|------|--------------|
| Skipped hop | `OPEN` | `RESOLVED` | **409** | `ILLEGAL_TRANSITION` |
| Reopen X1 | `CLOSED` | `OPEN` | **409** | `ILLEGAL_TRANSITION` |
| Reopen X2 | `RESOLVED` | `OPEN` | **409** | `ILLEGAL_TRANSITION` |
| Reopen X3 | `CANCELLED` | `OPEN` | **409** | `ILLEGAL_TRANSITION` |
| Self-transition | `IN_PROGRESS` | `IN_PROGRESS` | **409** | `ILLEGAL_TRANSITION` |

| Scenario | HTTP | `error.code` | Notes |
|----------|------|--------------|-------|
| Patch one field | 200 | — | Others unchanged |
| Legal transition T1–T5 | 200 | — | `status` updated |
| Unknown ticket | 404 | `NOT_FOUND` | |
| Illegal transition (e.g. X1–X3) | 409 | `ILLEGAL_TRANSITION` | Row unchanged |
| Illegal skipped hop `OPEN`→`RESOLVED` | 409 | `ILLEGAL_TRANSITION` | **DEC-02** default (A) |
| Invalid enum in body | 400 | `VALIDATION_ERROR` | |
| Empty JSON `{}` | 400 | `VALIDATION_ERROR` | Message: no updatable fields provided |
| Validation failure on title with `status` in same body | 400 | `VALIDATION_ERROR` | No partial apply |
| Illegal `status` with other valid fields | 409 | `ILLEGAL_TRANSITION` | Whole transaction rolled back |

**Traceability:** FEAT-04, FEAT-09, FEAT-11, AC-SM-*.

---

## 5. Endpoints — comments

### 5.1 `POST /api/v1/tickets/{id}/comments` — add comment

| | |
|--|--|
| **URI** | `POST {base}/api/v1/tickets/{id}/comments` |
| **Example** | `POST http://localhost:8080/api/v1/tickets/TKT-1001/comments` |
| **Capability (**PDF**)** | Add comment |

**Full HTTP example (positive)**

```http
POST /api/v1/tickets/TKT-1001/comments HTTP/1.1
Host: localhost:8080
Accept: application/json
Content-Type: application/json

{
  "body": "Customer retried with a different card."
}
```

```http
HTTP/1.1 201 Created
Content-Type: application/json;charset=UTF-8
Location: /api/v1/tickets/TKT-1001/comments/a1b2c3d4-e5f6-7890-abcd-ef1234567890

{
  "data": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "body": "Customer retried with a different card.",
    "createdAt": "2026-10-03T12:05:00Z"
  }
}
```

**Agreed response shape:** created **`Comment`** in `data` (not full ticket).

**cURL**

```bash
curl -sS -X POST 'http://localhost:8080/api/v1/tickets/TKT-1001/comments' \
  -H 'Content-Type: application/json' \
  -d '{"body":"Customer retried with a different card."}'
```

**Negative — blank body**

```http
POST /api/v1/tickets/TKT-1001/comments HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{ "body": "" }
```

→ **400** `VALIDATION_ERROR`, `details[].field` = `body`.

| Scenario | HTTP | `error.code` |
|----------|------|--------------|
| Valid body on existing ticket | 201 | — |
| Missing/blank `body` | 400 | `VALIDATION_ERROR` |
| `body` over 50_000 chars | 400 | `VALIDATION_ERROR` |
| Ticket not found | 404 | `NOT_FOUND` |
| Malformed JSON | 400 | `BAD_REQUEST` |

After success, `GET /api/v1/tickets/{id}` includes the new comment in `comments`.

**Traceability:** FEAT-05, AC-FEAT-05-*, AC-DM-02.

---

## 6. Endpoint — ask (boundary)

**Capability (**PDF**):** natural-language questions over ticket knowledge; grounded answer with citations or honest no-match; **no** side effects.

### 6.1 `POST /api/ai/ask` and `POST /api/v1/ai/ask`

| | |
|--|--|
| **URIs** | `POST {base}/api/ai/ask` (**PDF**), `POST {base}/api/v1/ai/ask` (alias) |
| **Examples** | `http://localhost:8080/api/ai/ask`, `http://localhost:8080/api/v1/ai/ask` |
| **Handler** | Same controller method; identical request/response |

Same handler, same contract. Frontends **should** prefer `/api/v1/ai/ask`; assessors may call `/api/ai/ask` per PDF.

**Full HTTP — PDF path (positive, grounded)**

```http
POST /api/ai/ask HTTP/1.1
Host: localhost:8080
Accept: application/json
Content-Type: application/json

{
  "question": "What caused previous payment failures?"
}
```

```http
HTTP/1.1 200 OK
Content-Type: application/json;charset=UTF-8

{
  "data": {
    "answer": "Previous payment failures were linked to gateway timeouts (see cited tickets).",
    "citedTicketIds": ["TKT-1001", "TKT-1004"]
  }
}
```

**Full HTTP — versioned path (equivalent)**

```http
POST /api/v1/ai/ask HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{ "question": "Have we seen login issues before?" }
```

→ Same envelope rules as above.

**cURL (assessment path)**

```bash
curl -sS -X POST 'http://localhost:8080/api/ai/ask' \
  -H 'Content-Type: application/json' \
  -d '{"question":"What caused previous payment failures?"}'
```

**cURL (versioned path)**

```bash
curl -sS -X POST 'http://localhost:8080/api/v1/ai/ask' \
  -H 'Content-Type: application/json' \
  -d '{"question":"What caused previous payment failures?"}'
```

Always success envelope on **200**. Grounded answer and no-match are both **200** (not `error`).

**Interim `data` shape** (until `rag-api-contract.md` / **DEC-11** finalizes wording):

| Property | Type | Rules |
|----------|------|-------|
| `answer` | string | Grounded text when retrieval succeeds; honest no-match phrase when not |
| `citedTicketIds` | string[] | Ticket ids from retrieval; empty when no-match; must exist in DB when non-empty (**PDF**) |

Example grounded:

```json
{
  "data": {
    "answer": "Previous payment failures were linked to gateway timeouts (see cited tickets).",
    "citedTicketIds": ["TKT-1001", "TKT-1004"]
  }
}
```

Example no-match:

```json
{
  "data": {
    "answer": "No relevant tickets found.",
    "citedTicketIds": []
  }
}
```

| Scenario | HTTP | Notes |
|----------|------|-------|
| Valid `question`, relevant corpus | 200 | Non-empty `answer`; `citedTicketIds` ⊆ existing tickets |
| Valid `question`, no retrieval | 200 | No-match `answer`; empty citations; **do not** invent facts |
| Missing `question` | 400 | `VALIDATION_ERROR` |
| Blank/whitespace `question` | 400 | `VALIDATION_ERROR` |
| Malformed JSON | 400 | `BAD_REQUEST` |
| Ask does not create tickets | — | No `ticket` row created (**PDF**) |

**Negative — missing question**

```http
POST /api/v1/ai/ask HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{}
```

```json
{
  "error": {
    "status": 400,
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed.",
    "details": [
      { "field": "question", "message": "must not be blank" }
    ],
    "timestamp": "2026-10-04T11:30:00Z",
    "path": "/api/v1/ai/ask"
  }
}
```

**Out of scope for this file:** prompts, chunk text, model ids, top-K — not in API responses (`rules/rag-vector-store.md`).

**Traceability:** FEAT-15–18, AC-CORE-16–18.

---

## 7. End-to-end example (demo script URIs)

Sequence aligned with [`requirements.md`](requirements.md) §8.7 / Flow A + B. Replace host as needed.

| Step | URI | Method | Body (summary) | Expected |
|------|-----|--------|----------------|----------|
| 1 | `/api/v1/tickets` | POST | `title`, `description`, `category` | **201**, `OPEN`, `Location` |
| 2 | `/api/v1/tickets/TKT-1001` | GET | — | **200**, `comments: []` |
| 3 | `/api/v1/tickets/TKT-1001/comments` | POST | `body` | **201**, `Comment` |
| 4 | `/api/v1/tickets/TKT-1001` | PATCH | `status: IN_PROGRESS` | **200** |
| 5 | `/api/v1/tickets?q=payment` | GET | — | **200**, includes TKT-1001 if keyword matches title/description |
| 6 | `/api/v1/tickets?status=IN_PROGRESS` | GET | — | **200**, filtered list |
| 7 | `/api/v1/ai/ask` | POST | `question` | **200**, answer + citations or no-match |
| 8 | `/api/v1/tickets/TKT-1001` | PATCH | `status: OPEN` on `CLOSED` ticket | **409** after close (Flow C) |

---

## 8. REST design summary

| Method | Path | Safe | Idempotent | Body |
|--------|------|------|------------|------|
| POST | `/api/v1/tickets` | no | no | Create ticket |
| GET | `/api/v1/tickets` | yes | yes | — |
| GET | `/api/v1/tickets/{id}` | yes | yes | — |
| PATCH | `/api/v1/tickets/{id}` | no | no | Partial update |
| POST | `/api/v1/tickets/{id}/comments` | no | no | Create comment |
| POST | `/api/ai/ask`, `/api/v1/ai/ask` | no | no | Ask question |

- **No** `DELETE` or `PUT` on tickets (**assessment** has no delete; **Convention** uses PATCH).
- **No** verb paths (`/search`, `/transition`).
- Collections use **GET** query params, not POST bodies.

---

## 9. Acceptance criteria (API contract)

| ID | Criterion |
|----|-----------|
| **AC-API-01** | All ticket endpoints return success envelope §2.3 or error envelope §2.4. |
| **AC-API-02** | List endpoint returns `meta` with correct `totalElements` under filter/search. |
| **AC-API-03** | Create rejects `status` in body with 400 (**DEC-07**). |
| **AC-API-04** | PATCH illegal status returns 409 and unchanged `status` in DB. |
| **AC-API-05** | Comment create returns 201 `Comment` in `data`. |
| **AC-API-06** | Ask blank `question` → 400; no-match → 200 with empty `citedTicketIds`. |
| **AC-API-07** | Both ask paths behave identically. |
| **AC-API-08** | Endpoint catalog §2.11: each row returns the documented `data` type and HTTP success code. |
| **AC-API-09** | PATCH §4.4.1: each T1–T5 succeeds from the documented current state; §5.6 illegal examples return **409**. |

Maps to **AC-CORE-*** and **AC-FEAT-*** in [`requirements.md`](requirements.md).

---

## 10. Open questions and decisions

| ID | Topic | Status | Notes |
|----|-------|--------|-------|
| **DEC-06** | Transition API shape | **Interim closed in this contract** | PATCH `status` on ticket resource §4.4 |
| **DEC-11** | Ask no-match wording | Open | `rag-api-contract.md` |
| **DEC-14** | Ticket REST surface | **Interim agreed** | Paths/methods match `rules/api-standards.md` §4.4 |
| **DEC-02** | Skipped hops | Open (implement A) | [`state-machine.md`](state-machine.md) |

---

## 11. Revision history

| Date | Note |
|------|------|
| 2026-10-04 | Initial contract: envelopes, ticket/comment/ask payloads, scenarios, REST table; DEC-06/14 interim. |
| 2026-10-04 | Expanded URI catalog §2.8–2.10; full HTTP/cURL examples per endpoint; demo URI table §7. |
| 2026-10-04 | §2.11 full endpoint catalog; §3.5 `AskResponseData`; §4.4.1 T1–T5 PATCH table; AC-API-08/09. |
