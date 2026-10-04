# Review spec

Cursor attaches this file via [`.cursor/commands/review-spec.md`](../.cursor/commands/review-spec.md) (pointer only). Edit **this** file; do not copy the body into `.cursor/commands/`.

Review specifications under `spec/` (or a named spec file) **before** implementation. Specs are the source of truth for generated code; they must be valid against the assessment and **must not contradict** project rules.

Do **not** rewrite the whole spec unless asked. Confirm with the user before editing files.

## Inputs

- Primary target spec file(s) the user named (or infer from the conversation)
- Full `spec/` set when reviewing consistency (read what exists; do not assume every file is present)
- `docs/Assessments.docx` as background — specs must still stand alone
- Rules generated code will follow: `rules/java-springboot.md`, `rules/api-standards.md`, `rules/testing.md`, `rules/rag-vector-store.md`, `rules/frontend.md`

Mark checklist items **Pass** / **Fail** / **N/A**. Failures are blocking for implementation unless explicitly deferred as **open questions**.

### When most of `spec/` does not exist yet

Default **Ready for implementation?** → **no** until draft specs needed for the slice are complete and open **DEC-*** are resolved or explicitly accepted as interim. **`data-model.md` is agreed**; all other PDF-listed `spec/` files are **draft** — ticket HTTP, ingest, ask, UI, and eval may proceed per those specs + `rules/*` unless a **DEC-*** blocks (e.g. **DEC-09** embedding model, **DEC-11** no-match wording).

You may still review **`requirements.md`** / **`architecture.md`** for PDF alignment (requirements **§0.4**, **§0.5**, [`docs/assessment-brief.md`](../docs/assessment-brief.md) **§13**). List **blocking open decisions** explicitly — do not silently implement from rules alone except where rules already lock behaviour (HTTP envelopes, stack choices).

---

## Step 0 — Identify spec files in scope

Before judging content, **name every file** you used and its role. If a file is missing but required for the topic, say so.

| Role | Typical path | Use in this review |
|------|----------------|-------------------|
| Requirements | [`spec/requirements.md`](../spec/requirements.md) | PDF scope; **AC-CORE** / **FEAT**; **OQ** / **DEC** (§10); **§0.4–§0.5**; handoff map (§10.3); §8.7 demo script |
| Architecture | [`spec/architecture.md`](../spec/architecture.md) | **§0** PDF/RAG ladder; business vs functional modules (§4, §8); ticket aggregate (§5); tech/deployment (§6–7); communication (§10); API map (§11); vector DB (§14); RAG pipeline (§15); chunking/embedding **justification** (§16, AC-CORE-19). Numeric locks → [`spec/rag-ingestion.md`](../spec/rag-ingestion.md) |
| Data model | [`spec/data-model.md`](../spec/data-model.md) | **§0** persistence map; entities, enums, DTOs §10, **indexes §14.5**, Liquibase order §14, **AC-DM-*** |
| API contract | [`spec/api-contract.md`](../spec/api-contract.md) | payloads, scenarios, ask boundary (envelopes in `rules/api-standards.md`) |
| State machine | [`spec/state-machine.md`](../spec/state-machine.md) | §5 legal/illegal matrix; **DEC-02** default (A) |
| RAG ingestion | [`spec/rag-ingestion.md`](../spec/rag-ingestion.md) | chunking §6–§9, ingest §10–§11, **AC-RAG-ING-*** |
| RAG API / ask `data` | [`spec/rag-api-contract.md`](../spec/rag-api-contract.md) | citations, grounding, no-match (**AC-RAG-API-***, **DEC-11**) |
| Evaluation | [`spec/evaluation-strategy.md`](../spec/evaluation-strategy.md) | Retrieval vs grounding §3–§4; corpus §5; procedure §6–§7; failures §8; **AC-EVAL-*** §10; FEAT-22 |
| UI model | [`spec/ui-model.md`](../spec/ui-model.md) | screens, CRUD, flows A–E, ask/RAG UX, **AC-UI-*** |
| UI architecture | [`spec/architecture.md`](../spec/architecture.md) §12 | frontend modules summary (detail in `ui-model.md`) |
| Test strategy | [`spec/test-strategy.md`](../spec/test-strategy.md) | **AC-SM** / **AC-API** / **AC-CORE** ↔ test layers |

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
- [ ] When reviewing [`spec/requirements.md`](../spec/requirements.md): **§0.4** coverage map — every PDF theme row has a capture path or explicit gap (**DEC-*** / demo evidence only)
- [ ] When reviewing [`spec/requirements.md`](../spec/requirements.md): **§0.5** verbatim anchor table — no assignment bullet left unmapped to a spec home
- [ ] Child specs in scope include **§0** (PDF map for that file; BRF/FRI/IRI where present) per [`rules/documentation.md`](../rules/documentation.md) writing bar
- [ ] Major **`##` sections** in child specs suffix **unit ids** from that file’s §0.3 table (`· unit **ID**` / `· units **A**…**B**`; hub [`requirements.md`](../spec/requirements.md) **§0.8**)
- [ ] **§6.8** hygiene map — each PDF-listed rule/command exists under `rules/` and `commands/` (and `skills/documentation/SKILL.md`)
- [ ] When reviewing [`spec/requirements.md`](../spec/requirements.md): unresolved **DEC-*** rows (§10.2) are still **Open** — no child spec or code treats them as decided
- [ ] Child specs that resolve an **OQ-*** cite the matching **DEC-*** decision (or remain draft until user confirms)
- [ ] New material in feature specs aligns with **[`spec/requirements.md`](../spec/requirements.md) §10.3** handoff (each OQ has a primary owning spec)
- [ ] Assessment acceptance mappable to **`AC-CORE-*`** (§8); feature detail mappable to **`AC-FEAT-*`** (§4.2) where applicable
- [ ] No invented product features (auth, agents from ask, attachments, bulk ops, rerankers) unless explicitly in scope

### Consistency with engineering rules

- [ ] HTTP envelopes, 201/`Location`, 409 illegal transition, list `page`/`size`/`sort`/`q`/`status` align with `rules/api-standards.md` or the spec **explicitly revises** those rules with user agreement
- [ ] `POST /api/ai/ask` with `"question"` preserved; versioned ask path does not replace the assessment path
- [ ] Ask no-match is a success outcome, not an error envelope
- [ ] State machine in domain/services, not UI or repository `UPDATE`
- [ ] Test acceptance criteria can be implemented under `rules/testing.md`
- [ ] RAG content does not contradict `rules/rag-vector-store.md` (chunking/ingest per [`spec/rag-ingestion.md`](../spec/rag-ingestion.md); no locked model/K/threshold unless **DEC-09** / §12 agreed)
- [ ] UI spec does not require Next.js or frontend tests; layout remains open until [`ui-model.md`](../spec/ui-model.md) / **DEC-15** / **DEC-06** is agreed

### Cross-spec validity (name both files when flagging)

When a child spec is **draft** or a section is **Open**, cross-check [`spec/requirements.md`](../spec/requirements.md) **§0.4** and the **DEC-*** register (§10.2). Flag **conflicts** among specs and `rules/*`.

- [ ] [`state-machine.md`](../spec/state-machine.md) ↔ [`api-contract.md`](../spec/api-contract.md) — same transitions; PATCH `status` §4.4
- [ ] [`data-model.md`](../spec/data-model.md) ↔ [`api-contract.md`](../spec/api-contract.md) — same fields, types, requiredness
- [ ] `requirements.md` ↔ feature specs — no dropped assessment capabilities; no **DEC-*** closed in code but still **Open** in requirements
- [ ] [`architecture.md`](../spec/architecture.md) ↔ [`rag-ingestion.md`](../spec/rag-ingestion.md) / [`api-contract.md`](../spec/api-contract.md) §6 — no conflicting pipeline or API story
- [ ] [`ui-model.md`](../spec/ui-model.md) ↔ [`rag-api-contract.md`](../spec/rag-api-contract.md) / [`api-contract.md`](../spec/api-contract.md) / [`state-machine.md`](../spec/state-machine.md) — screens do not require impossible API shapes or illegal-only transitions
- [ ] [`ui-model.md`](../spec/ui-model.md) ↔ [`architecture.md`](../spec/architecture.md) §12 — no contradictory screen/API story

### When [`spec/architecture.md`](../spec/architecture.md) is in scope (expanded checklist)

Use `rules/documentation.md` section map for headings. Mark **N/A** for sections not yet written.

- [ ] **PDF vs Convention vs Open** labels used; conventions (PgVector, `/api/v1`, Boot 3, React+Vite+TS) not presented as PDF mandates
- [ ] **Business modules** (§4) trace to `requirements.md` FEAT catalogue without inventing capabilities
- [ ] **Ticket structure** (§5) stays conceptual — no smuggled field catalogs that belong in `data-model.md`
- [ ] **Functional modules** (§8) map to **technical** packages (§9) consistently with `rules/java-springboot.md`
- [ ] **Communication** (§10): synchronous REST only unless an agreed spec adds async; ask no-match vs error envelope matches `rules/api-standards.md`
- [ ] **API architecture** (§11) aligns with `rules/api-standards.md` (`POST /api/ai/ask`, PATCH tickets, envelopes)
- [ ] **Vector DB** (§14): PgVector as **Convention**; relational DB remains SoR; rebuild-from-tickets story present
- [ ] **RAG** (§15–16): ingest sources (description, comments, resolution notes); re-ingest on update/close per PDF with **DEC-01** noted if close-only is unresolved
- [ ] **Chunking** (§16): strategy **justified** for ticket text; aligns with [`rag-ingestion.md`](../spec/rag-ingestion.md) §9; **proposed** numeric size/overlap §9.3 confirmed or still marked Proposed
- [ ] **Embedding** (§16): local vs cloud tradeoffs documented; same model at ingest/query; model id not invented without [`rag-ingestion.md`](../spec/rag-ingestion.md) §12 (**DEC-09**)
- [ ] **Open questions** (§21) align with `requirements.md` §10 OQ/DEC — no decisions closed only in architecture

### When [`spec/data-model.md`](../spec/data-model.md) is in scope

- [ ] **Status** is **agreed** (or draft decisions explicitly marked **Proposed**, not mixed with agreed DEC rows in `requirements.md` §10.2)
- [ ] **DEC-03, 04, 05, 07, 08, 13** match `requirements.md` §10.2 decision text
- [ ] **§14.5 index catalog** names every required BTREE, `pg_trgm` GIN, and vector HNSW index; §14.6 SQL matches §14.5
- [ ] RAG metadata §11 uses only PDF keys + agreed technical keys (`chunkIndex`, `ingestedAt`)
- [ ] No field catalog duplicated inside `architecture.md` §5 that contradicts §6/§10

### When [`spec/evaluation-strategy.md`](../spec/evaluation-strategy.md) is in scope

Use `rules/documentation.md` section map. Mark **N/A** only if the review topic has no RAG ask/retrieval angle.

- [ ] **§3–§4** — retrieval quality defined separately from grounding; no golden LLM string as sole proof
- [ ] **§5** — five **PDF** questions map to **Example** expected ticket ids; aligns with [`requirements.md`](../spec/requirements.md) §4.3 (no drift)
- [ ] **§6–§7** — procedure and “retrieved set” evidence match `commands/review-rag-output.md` and `rules/rag-vector-store.md` (no public chunk dump unless agreed)
- [ ] **§8** — failure taxonomy usable for demo/debug (retrieval miss vs citation drop vs false empty)
- [ ] **§10** — **AC-EVAL-01…05** trace to **FEAT-22** / **AC-FEAT-22-01/02** and demo §8.7 steps 9–10
- [ ] **§12** — does not invent numeric K/threshold/model (points to [`rag-ingestion.md`](../spec/rag-ingestion.md) / **DEC-09**)
- [ ] [`test-strategy.md`](../spec/test-strategy.md) **§5–§6** — state machine determinism vs ask Bands A/B/C consistent with [`evaluation-strategy.md`](../spec/evaluation-strategy.md)

### When [`spec/ui-model.md`](../spec/ui-model.md) is in scope

Use `rules/documentation.md` ui-model reviewer map. Mark **N/A** only if the review topic has no UI angle.

- [ ] **PDF** capabilities only — no delete-ticket UI, auth, agent-from-ask, vector admin console (§2.2 non-goals)
- [ ] **§7–§8** — screen catalog covers list, create, detail, comments; CRUD matrix matches [`api-contract.md`](../spec/api-contract.md) §2.11
- [ ] **§9** — legal status targets match [`state-machine.md`](../spec/state-machine.md) §5.1; **409** UX for Flow C
- [ ] **§10** — ask panel matches [`rag-api-contract.md`](../spec/rag-api-contract.md) (grounded **200**, no-match **200**, validation **400**); citations link to detail
- [ ] **§11–§12** — flows A–E and demo §8.7 steps trace to [`requirements.md`](../spec/requirements.md)
- [ ] **§13** — **AC-UI-01…12** trace to **AC-CORE-01…11** and **AC-CORE-16…18** without contradiction
- [ ] **§14** — **DEC-06**, **11**, **15** still **Open** where not user-confirmed

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
- [ ] RAG numeric or model choices appear only where [`rag-ingestion.md`](../spec/rag-ingestion.md) (or agreed section) justifies them

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

- [ ] Every acceptance criterion maps to a test theme in [`spec/test-strategy.md`](../spec/test-strategy.md) and `rules/testing.md`, and to **`AC-CORE-*`** or **`AC-FEAT-*`** in [`spec/requirements.md`](../spec/requirements.md)
- [ ] Positive **and** negative / empty outcomes for implemented capabilities
- [ ] RAG: sources, metadata, re-ingest, grounding, citations, no-match; configurable K/threshold **without** numeric defaults unless agreed
- [ ] When `architecture.md` is primary: §16 chunking + embedding justification present for **AC-CORE-19** / FEAT-20
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
11. **Demo readiness (optional)** — can [`spec/requirements.md`](../spec/requirements.md) §8.7 demo script be executed for the scope under review?
12. **Ready for implementation?** yes / no — if no, list blocking questions (include unresolved **DEC-***)

Confirm with the user before applying spec edits.

---

## Revision history

| Date | Note |
|------|------|
| 2026-09-24 | Initial spec review command: consistency, hallucinations, readiness gate. |
| 2026-10-04 | Expanded for `requirements.md` OQ/DEC, architecture §16, demo script §8.7, AC mapping. |
| 2026-10-04 | Synced with expanded [`spec/requirements.md`](../spec/requirements.md) and [`spec/architecture.md`](../spec/architecture.md). |
| 2026-10-04 | Added revision history section. |
| 2026-10-04 | `data-model.md` agreed gate; index catalog checklist §14.5. |
| 2026-10-04 | Requirements §0.4 coverage check; interim map when only three specs exist (`rules/documentation.md`). |
| 2026-10-04 | Draft [`spec/state-machine.md`](../spec/state-machine.md) in readiness and cross-spec checks. |
| 2026-10-04 | Draft [`spec/api-contract.md`](../spec/api-contract.md) in readiness and cross-spec checks. |
| 2026-10-04 | Draft [`spec/rag-ingestion.md`](../spec/rag-ingestion.md), [`spec/test-strategy.md`](../spec/test-strategy.md) in file map and readiness. |
| 2026-10-04 | Expanded checklist when [`spec/evaluation-strategy.md`](../spec/evaluation-strategy.md) is in scope (AC-EVAL, §5 corpus). |
| 2026-10-04 | Cross-check [`test-strategy.md`](../spec/test-strategy.md) §5–§6 when RAG or SM in scope. |
| 2026-10-04 | Eight-file spec set: ask → `api-contract` §6.2–§6.5; UI → `architecture` §12.3–§12.6. |
| 2026-10-04 | Nine-file set: UI detail → [`ui-model.md`](../spec/ui-model.md); **AC-UI-*** review section. |
| 2026-10-04 | Ten-file set: ask detail → [`rag-api-contract.md`](../spec/rag-api-contract.md). |
| 2026-10-04 | Checklist: requirements **§0.5**; child spec **§0** guides; file map **§0** columns. |
| 2026-10-04 | Checklist: **`##` heading unit suffix** aligned with §0.3 independent reading units. |
