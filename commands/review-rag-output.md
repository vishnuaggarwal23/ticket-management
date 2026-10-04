# Review RAG / AI output (hallucination & grounding)

Cursor attaches this file via [`.cursor/commands/review-rag-output.md`](../.cursor/commands/review-rag-output.md) (pointer only). Edit **this** file; do not copy the body into `.cursor/commands/`.

Verify the **assistant answer** from `POST /api/ai/ask` (and the same handler at `POST /api/v1/ai/ask`). HTTP envelope rules: `rules/api-standards.md`. Grounding rules: `rules/rag-vector-store.md`. Pipeline context: [`spec/architecture.md`](../spec/architecture.md) §15–16; ingest/chunking [`spec/rag-ingestion.md`](../spec/rag-ingestion.md). Cited ids must be real **`ticket.id`** values (`TKT-{n}` per [`spec/data-model.md`](../spec/data-model.md) DEC-04). Ask JSON **inside** `data`: [`spec/rag-api-contract.md`](../spec/rag-api-contract.md) (**DEC-11** open). **Retrieval quality** (right tickets in top-K): [`spec/evaluation-strategy.md`](../spec/evaluation-strategy.md) (draft) and [`spec/requirements.md`](../spec/requirements.md) §2.5 / FEAT-22 / §4.3.

**Pass only if all three hold:**

1. **Every factual claim is traceable to a cited ticket ID** whose retrieved text actually supports that claim.
2. **Nothing is fabricated beyond the retrieved context** (no extra tickets, facts, citations, or general-knowledge “support” answers).
3. **“No relevant tickets found”** (or equivalent wording the RAG API spec agrees) is used **if and only if** nothing relevant was retrieved.

Do not rewrite production code or specs unless the user asks.

---

## Inputs (required)

Obtain as much of this as the user or logs provide. If retrieval context is missing, say so and treat unverified claims as **ungrounded**.

| Input | Role |
|-------|------|
| **Question** | Body field `"question"` |
| **HTTP outcome** | Status + success envelope (`data`) or error envelope |
| **Assistant answer** | Full generated text (and no-match wording if that is the outcome) |
| **Cited ticket IDs** | IDs the API/UI presented as sources |
| **Retrieved set** | Ticket IDs and excerpts that **similarity search actually returned** (empty list is valid evidence) |
| **Threshold / empty** | Whether retrieval was empty or all hits discarded (if known) |

Optional: full ticket records for cited ids — only to check that the **retrieved excerpt**, not the rest of the database, was used. Do not treat unretrieved ticket rows as allowed context.

### Where to get the “retrieved set” (public API does not expose chunks)

`rules/rag-vector-store.md` keeps prompts and chunk dumps off the public API. For review, use **one or more** of:

| Source | When |
|--------|------|
| **Test output** | Integration test stubs embed/search with fixed chunks + ticket ids |
| **Application logs** | Dev-only structured log of retrieved ids (no secrets; disable in prod if noisy) |
| **Reviewer harness** | Temporary admin/debug endpoint **only if** user agrees in spec — not default |
| **Manual replay** | Same question against a known DB seed in dev |

If none of the above is available, set verdict **cannot verify** for fabrication checks; still validate envelope and no-match wording from the HTTP body.

### PDF illustrative questions (use as review fixtures)

From the assessment — expect **grounded** answers only when retrieval supports them.

**Example seed corpus (not mandated production data):** [`spec/requirements.md`](../spec/requirements.md) **§4.3** maps five illustrative questions to sample ticket ids (e.g. TKT-1001) for manual review and eval fixtures.

- “Have we seen payment failures before?”
- “What was the resolution for ticket TKT-1001?”
- “What are the common causes of shipment tracking issues?”
- “Show me similar resolved tickets.”
- “Which high-priority tickets are related to payment?”

### Out-of-scope support questions

If the question is support-specific but **no ticket text** supports an answer, treat like **situation A**: must **not** answer from general LLM knowledge; use honest no-match (see `rules/rag-vector-store.md` **Out-of-scope vs no-match**).

---

## Step 0 — Classify the retrieval situation

Pick **one**:

| Situation | Correct assistant behaviour |
|-----------|-----------------------------|
| **A. Empty / none relevant** | Retrieval empty **or** nothing passed the similarity threshold. Answer MUST be honest **no relevant tickets found** (equivalent allowed). HTTP **200** + success envelope; no-match **inside `data`**. MUST NOT generate a support story, MUST NOT cite ticket ids. |
| **B. Relevant hits** | At least one retrieved excerpt is on-topic. Answer MUST be grounded in those excerpts only. MUST cite ticket ID(s) that appear in the **retrieved set**. MUST NOT say no-match. |

If you cannot tell A vs B (no retrieval log), verdict is **cannot verify** for fabrication and no-match correctness — still check claims vs cited text if any excerpts were provided.

---

## 1. Every claim → cited ticket ID

Split the answer into **atomic factual claims** (status, assignee, cause, dates, resolution, “ticket X said …”). Ignore pure connective wording.

For **each** claim:

- [ ] A **cited** ticket ID is attached to the answer (or clearly supports that sentence)
- [ ] That ID is in the **retrieved set** (not guessed, not only in the model’s head)
- [ ] The **retrieved excerpt** for that ID contains the fact (paraphrase OK; new facts not OK)
- [ ] No claim is supported only by “the model knows how payments work” or uncited tickets

**Fail** if any claim has no citation, cites an id not in retrieval, or the cited excerpt does not contain the fact.

---

## 2. Nothing fabricated beyond retrieved context

- [ ] No ticket IDs in citations or prose that were **not** retrieved
- [ ] No facts that appear in neither retrieved excerpts nor (for wording only) harmless restatement of the **question**
- [ ] No invented comments, resolution notes, assignees, or root causes
- [ ] No confidence scores, chunk dumps, model names, or vector internals unless the RAG API spec makes them public (default: not public)
- [ ] No agent side effects in the answer (create ticket, notify, tool chain)
- [ ] Situation B: generation used retrieved excerpts + those ticket ids only — not empty-context invention

**Fail** on any fabricated id, fact, or out-of-scope action.

---

## 3. “No relevant tickets found” used correctly

| Retrieval | Answer says no-match | Verdict |
|-----------|----------------------|---------|
| Empty / below threshold (A) | Yes, explicit, no fake citations | **Pass** this check |
| Empty / below threshold (A) | Fluent answer and/or citations | **Fail** — fabricated relevance |
| Relevant hits (B) | No-match | **Fail** — false no-match |
| Relevant hits (B) | Grounded answer + retrieved ids | **Pass** this check |

Equivalent user-visible wording is allowed when [`spec/api-contract.md`](../spec/api-contract.md) §6.3 defines it (or user confirms **DEC-11**). Do not require a golden string.

HTTP: no-match is **not** the error envelope and **not** 404.

---

## Contract shape (if a JSON body was provided)

- [ ] Success envelope `{ "data": … }` on 200; no `error` object on success
- [ ] No-match still 200; match still 200 when retrieval succeeded
- [ ] 400 only for invalid `question` (blank/missing) — that is **not** a RAG no-match
- [ ] Field names inside `data` match the RAG API spec when it exists

---

## Code-assistant output (secondary)

Use this section only when the user asked to review a **coding** assistant, not an ask response.

- [ ] Types, paths, and config keys exist in the repo or agreed rules
- [ ] No invented spec requirements
- [ ] Status transitions legal per [`spec/state-machine.md`](../spec/state-machine.md) §5 if touched

---

## Output format (required)

1. **Retrieval situation** — A or B (or cannot verify), with evidence (empty list, ids retrieved)
2. **Claim table** — claim | cited ticket ID | in retrieved set? | excerpt supports? | pass/fail
3. **Fabrication** — list of extra ids/facts, or none
4. **No-match check** — correct / false no-match / missing no-match
5. **Verdict** — `grounded` | `partially grounded` | `ungrounded` | `cannot verify`
   - `grounded`: all three bullets pass
   - `partially grounded`: some claims traceable; at least one fail in 1–3
   - `ungrounded`: fabricated answer, fake citations, or no-match used wrongly
6. **Recommended user-visible answer** — corrected grounded text **or** honest no-match **or** **reject**
7. **Log?** — If this was a meaningful AI mistake, propose `docs/ai-mistakes.md` (confirm before writing)

**Pass/fail of the ask answer:** fail unless verdict is `grounded` (or situation A with correct no-match and no citations).

---

## Retrieval quality (PDF: probabilistic — separate from grounding)

**Grounding** (steps 0–3 above) asks whether every claim is supported by **cited** retrieved text.  
**Retrieval quality** asks whether similarity search returned the **right ticket id(s)** for the question.

Authoritative procedure, corpus, examples, and failure taxonomy: [`spec/evaluation-strategy.md`](../spec/evaluation-strategy.md) **§3–§9** (especially **§5.2** expected ids, **§8** failures **F-01…F-10**). **FEAT-22** / **AC-EVAL-01…05**.

### When to run

Same triggers as [`spec/evaluation-strategy.md`](../spec/evaluation-strategy.md) **§6.3** — e.g. after chunking/embed/K/threshold/ingest changes, before demo steps 9–10 ([`spec/requirements.md`](../spec/requirements.md) §8.7), or when grounding passes but the answer “feels wrong”.

### Minimal checklist (detail in [`rag-api-contract.md`](../spec/rag-api-contract.md))

- [ ] Seed or fixture matches **Example** corpus ([`spec/evaluation-strategy.md`](../spec/evaluation-strategy.md) §5.1 or requirements §4.3).
- [ ] **Retrieved set** captured per §7 (logs, harness, or replay) — else verdict `not evaluated`.
- [ ] Compare retrieved ids to §5.2 **required** ids → `good` | `partial` | `poor`.
- [ ] Classify misses with **F-*** ids from spec §8 when helpful.
- [ ] Record in eval log (spec §9); **do not** add golden-string unit tests.

### Output (add to review report when this section is used)

- **Retrieval verdict:** `good` | `partial` | `poor` | `not evaluated`
- **Misses:** question → expected id(s) → retrieved ids → **F-*** hypothesis (spec §8)
- **Follow-up:** spec or config change — do not implement unless the user asks

---

## Revision history

| Date | Note |
|------|------|
| 2026-09-24 | Initial grounded-ask review workflow: citations, no-match, retrieval quality hooks. |
| 2026-10-04 | Expanded retrieval verdict output; links to `spec/evaluation-strategy.md`. |
| 2026-10-04 | Synced with expanded [`spec/requirements.md`](../spec/requirements.md) and RAG rules. |
| 2026-10-04 | Added revision history section. |
| 2026-10-04 | Citation ids must match [`spec/data-model.md`](../spec/data-model.md) `TKT-{n}` ticket PK. |
| 2026-10-04 | Eval interim: requirements §2.5 / §4.3 when `evaluation-strategy.md` absent. |
| 2026-10-04 | Retrieval quality section defers to [`spec/evaluation-strategy.md`](../spec/evaluation-strategy.md) §3–§9; AC-EVAL / F-* taxonomy. |
