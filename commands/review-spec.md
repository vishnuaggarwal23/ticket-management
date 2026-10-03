# Review spec

Cursor attaches this file via [`.cursor/commands/review-spec.md`](../.cursor/commands/review-spec.md) (pointer only). Edit **this** file; do not copy the body into `.cursor/commands/`.

Review specifications under `spec/` (or a named spec file) **before** implementation. Specs are the source of truth for generated code; they must be valid against the assessment and **must not contradict** project rules.

Do **not** rewrite the whole spec unless asked. Confirm with the user before editing files.

## Inputs

- Primary target spec file(s) the user named (or infer from the conversation)
- Full `spec/` set when reviewing consistency (read what exists; do not assume every file is present)
- `docs/Assessments.pdf` as background — specs must still stand alone
- Rules generated code will follow: `rules/java-springboot.md`, `rules/api-standards.md`, `rules/testing.md`, `rules/rag-vector-store.md`, `rules/frontend.md`

Mark checklist items **Pass** / **Fail** / **N/A**. Failures are blocking for implementation unless explicitly deferred as **open questions**.

### When most of `spec/` does not exist yet

Default **Ready for implementation?** → **no** for features that need missing contracts (`api-contract`, `data-model`, `state-machine`, `ui-flow`, `rag-api-contract`, etc.).

You may still review **`requirements.md`** / partial **`architecture.md`** for PDF alignment and rule consistency. List **blocking missing files** explicitly in the output — do not silently implement from rules alone except where rules already lock behaviour (HTTP envelopes, stack choices).

---

## Step 0 — Identify spec files in scope

Before judging content, **name every file** you used and its role. If a file is missing but required for the topic, say so.

| Role | Typical path | Use in this review |
|------|----------------|-------------------|
| Requirements | `spec/requirements.md` | PDF scope; **AC-CORE** / **FEAT**; **OQ** / **DEC** (§10); handoff map (§10.3); §8.7 demo script |
| Architecture | `spec/architecture.md` | layering, RAG placement, chunking *discussion* (not numeric locks unless agreed) |
| Data model | `spec/data-model.md` | fields, ids, enums |
| API contract | `spec/api-contract.md` | ticket HTTP payloads beyond `rules/api-standards.md` envelopes |
| State machine | `spec/state-machine.md` | legal/illegal transitions |
| RAG ingestion | `spec/rag-ingestion.md` | chunking, models, ingest mechanics (when written) |
| RAG API | `spec/rag-api-contract.md` | ask `data` fields, citations, no-match |
| Evaluation | `spec/evaluation-strategy.md` | RAG quality (not unit tests) |
| UI flow | `spec/ui-flow.md` | screens and interactions |
| Test strategy | `spec/test-strategy.md` | acceptance ↔ test mapping |

**Output must include:**

- **Primary spec(s)** — file path(s) under review
- **Related spec(s)** — other `spec/*.md` files read for cross-check
- **Not present** — expected files for this feature that do not exist yet (blocking or not)

Do not review “the spec” in the abstract without listing paths.

---

## 1. Validity (claims must be traceable)

For each **material claim** in the primary spec(s), classify whether it is valid. Cite **file + section** (or heading).

### Assessment vs conventions vs open

- [ ] PDF requirements are not weakened (CRUD, comments, search/filter, backend validation, backend state machine, persistence, grounded ask + citations or no-match)
- [ ] Project conventions (Boot 3, `/api/v1`, PgVector, JUnit, envelopes, PATCH) are **labeled** as conventions, **not** as PDF mandates
- [ ] Open items remain in **Open questions** (or equivalent), not smuggled into requirements as decided facts
- [ ] When reviewing `spec/requirements.md`: unresolved **DEC-*** rows (§10.2) are still **Open** — no child spec or code treats them as decided
- [ ] Child specs that resolve an **OQ-*** cite the matching **DEC-*** decision (or remain draft until user confirms)
- [ ] New material in feature specs aligns with **`spec/requirements.md` §10.3** handoff (each OQ has a primary owning spec)
- [ ] Assessment acceptance mappable to **`AC-CORE-*`** (§8); feature detail mappable to **`AC-FEAT-*`** (§4.2) where applicable
- [ ] No invented product features (auth, agents from ask, attachments, bulk ops, rerankers) unless explicitly in scope

### Consistency with engineering rules

- [ ] HTTP envelopes, 201/`Location`, 409 illegal transition, list `page`/`size`/`sort`/`q`/`status` align with `rules/api-standards.md` or the spec **explicitly revises** those rules with user agreement
- [ ] `POST /api/ai/ask` with `"question"` preserved; versioned ask path does not replace the assessment path
- [ ] Ask no-match is a success outcome, not an error envelope
- [ ] State machine in domain/services, not UI or repository `UPDATE`
- [ ] Test acceptance criteria can be implemented under `rules/testing.md`
- [ ] RAG content does not contradict `rules/rag-vector-store.md` (no locked chunking/model/K/threshold unless agreed in `rag-ingestion.md`)
- [ ] UI spec does not require Next.js or frontend tests; layout remains open until `spec/ui-flow.md` is agreed

### Cross-spec validity (name both files when flagging)

- [ ] `state-machine.md` ↔ `api-contract.md` — same transitions and how status is requested
- [ ] `data-model.md` ↔ `api-contract.md` — same fields, types, requiredness
- [ ] `requirements.md` ↔ feature specs — no dropped assessment capabilities; no **DEC-*** closed in code but still **Open** in requirements
- [ ] `architecture.md` ↔ `rag-ingestion.md` / `rag-api-contract.md` — no conflicting pipeline or API story
- [ ] `ui-flow.md` ↔ API/RAG contracts — UI does not require impossible API shapes

---

## 2. Consistency (no contradictions)

Build a short **consistency matrix** in the output: rows = topics (status, fields, paths, RAG, errors), columns = spec files. Mark **aligned** / **conflict** / **one file silent**.

Checklist:

- [ ] No two spec files define the same fact differently (status enum, transition rules, field names, error codes)
- [ ] `rules/*.md` are not contradicted unless the spec documents an intentional revision and flags it for user confirmation
- [ ] Architecture diagrams or narratives match contract tables (not two stories)
- [ ] Acceptance criteria do not require behaviour that contracts forbid

When you find a conflict, cite **both** paths and quote or paraphrase the conflicting statements.

---

## 3. Justified vs unjustified

Separate **decisions that belong in specs** from **claims that need evidence**.

### Justified (OK when documented)

A case is **justified** when it has at least one of:

- Explicit **assessment PDF** requirement (cite requirement id or quote theme)
- **Agreed project convention** with pointer to `rules/*.md` and label “convention”
- **Documented tradeoff** (problem, options, choice, consequences) — required for chunking and embedding model in architecture / `rag-ingestion.md`
- **Explicit user/agreed decision** recorded in the spec revision history or open-questions resolution

Checklist:

- [ ] Every non-obvious design choice in the primary spec has a **why** (even one sentence)
- [ ] Conventions are not presented as PDF requirements
- [ ] RAG numeric or model choices appear only where `rag-ingestion.md` (or agreed section) justifies them

### Unjustified (flag for fix or open question)

A case is **unjustified** when:

- Stated as fact with **no** PDF, rule, or agreed-spec basis
- Numeric default (K, threshold, chunk size, dimension) **without** agreement section
- Endpoint, field, or transition **not** in assessment, rules, or sibling spec
- “Industry standard” or “best practice” with no project-specific reason

List each unjustified item with **spec file**, **location**, and **what evidence is missing**.

---

## 4. Assumptions

**Assumptions** are beliefs the spec relies on but does not prove (e.g. “assignee is a free-text string”, “one tenant”, “Ollama always available locally”).

Checklist:

- [ ] Assumptions are either **listed** in Open questions / Assumptions section or **derived** clearly in the review output
- [ ] No hidden assumption that implementation must guess (id format, PATCH body for status, sync ingest)
- [ ] Dependencies on tools (Docker, Ollama, Testcontainers) match project rules, not new mandates in spec alone

**Output:** table of **Assumption** | **Stated in (file)** | **Safe?** (yes / needs confirmation / blocking).

Do not treat assumptions as decided requirements unless the user has agreed.

---

## 5. Hallucinations (invented requirements)

**Hallucination** here means spec text that invents product or technical facts **not** supported by the assessment, existing `spec/`, or `rules/` — often from AI drafting.

Checklist:

- [ ] No capabilities the PDF does not support (agent actions, auth, delete-all, confidence scores on ask, public vector dumps)
- [ ] No HTTP paths or envelopes that contradict `rules/api-standards.md` without an explicit revision process
- [ ] No state transitions not in `state-machine.md` / requirements
- [ ] No metadata fields beyond assessment + agreed data model
- [ ] No test or CI mandates that contradict `rules/testing.md` (e.g. H2 as default, golden RAG string as sole proof)
- [ ] No stack versions or libraries pinned in spec as mandatory without being project conventions

**Output:** list **Hallucination** | **File + quote/snippet** | **Why it is unsupported** | **Fix** (remove, move to open questions, or cite real source).

If unsure whether something is a convention or a hallucination, mark **needs user confirmation** — do not silently accept.

---

## 6. Spec structure and quality

### Structure

- [ ] Title and status (draft / agreed / deprecated)
- [ ] Problem / context
- [ ] Scope and **non-goals**
- [ ] Requirements and testable **acceptance criteria**
- [ ] Contracts / data / flows where relevant
- [ ] **Open questions** (not buried in requirements prose); for `requirements.md` use **OQ-*** / **DEC-*** (§10) not ad-hoc lists only
- [ ] Revision history when the file is in flux

### Quality

- [ ] Every acceptance criterion maps to a test theme in `rules/testing.md` and, where present, to **`AC-CORE-*`** or **`AC-FEAT-*`** in `spec/requirements.md`
- [ ] Positive **and** negative / empty outcomes for implemented capabilities
- [ ] RAG: sources, metadata, re-ingest, grounding, citations, no-match; configurable K/threshold **without** numeric defaults unless agreed
- [ ] No “build the complete application” scope

---

## Output format (required sections)

Use this order. Every finding that cites a problem must include **`spec/<file>.md`** (and section/heading when possible).

1. **Spec files in scope** — primary, related, missing
2. **Validity summary** — Pass / Fail per primary file; blocking items
3. **Consistency matrix** — topics × files; list **conflicts** with both citations
4. **Justified decisions** — bullet list with file + basis (PDF / rule / tradeoff / agreed)
5. **Unjustified cases** — file, claim, missing evidence
6. **Assumptions** — table with safe / needs confirmation / blocking
7. **Hallucinations** — file, snippet, why unsupported, recommended fix
8. **Gaps / ambiguities** — must resolve before coding
9. **Suggested edits** — brief; do not rewrite whole specs unless asked
10. **Open questions for the user** — do not answer these yourself
11. **Demo readiness (optional)** — can `spec/requirements.md` §8.7 demo script be executed for the scope under review?
12. **Ready for implementation?** yes / no — if no, list blocking questions (include unresolved **DEC-***)

Confirm with the user before applying spec edits.
