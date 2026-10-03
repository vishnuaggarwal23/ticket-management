# Review RAG / AI output (hallucination & grounding)

Cursor attaches this file via [`.cursor/commands/review-rag-output.md`](../.cursor/commands/review-rag-output.md) (pointer only). Edit **this** file; do not copy the body into `.cursor/commands/`.

Verify the **assistant answer** from `POST /api/ai/ask` (and the same handler at `POST /api/v1/ai/ask`). HTTP envelope rules: `rules/api-standards.md`. Grounding rules: `rules/rag-vector-store.md`. Ask JSON **inside** `data` follows `spec/rag-api-contract.md` when that spec exists — do not invent field names here.

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

From the assessment — expect **grounded** answers only when retrieval supports them:

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

Equivalent user-visible wording is allowed if `spec/rag-api-contract.md` defines it. Do not require a golden string.

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
- [ ] Status transitions legal per `spec/state-machine.md` if touched

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

**Grounding** asks: “Is every claim supported by **cited** retrieved text?”  
**Retrieval quality** asks: “Did search return the **right** ticket(s) for this question?”

Both matter for the assessment. Neither is proved by a single golden answer string. Document approach in **`spec/evaluation-strategy.md`**; use this section for manual or seeded checks.

### When to run

- After changing chunking, embeddings, top-K, threshold, or ingest text.
- When demoing PDF **illustrative questions** (payment failures, TKT-1001 resolution, shipment issues, similar resolved, high-priority payment).
- When grounding passes but the answer “feels wrong” — often retrieval missed the obvious ticket.

### Inputs

| Input | Role |
|-------|------|
| **Question** | Same as grounding review |
| **Expected ticket ids** (eval set) | From a **seeded** database or fixture doc agreed in `evaluation-strategy.md` — not invented during review |
| **Retrieved set** | Actual ids (and ranks/scores if logged in dev) |
| **Final cited ids** | What the API returned |

### Checklist

- [ ] For each eval question, **expected** ticket(s) exist in the DB seed with text that should match.
- [ ] Retrieved set includes those ids (or explains why threshold excluded them).
- [ ] If the right ticket was **not** retrieved, grounding review may still “pass” on wrong chunks — flag **retrieval miss**.
- [ ] If the right ticket was retrieved but **not cited**, flag **citation/orchestration** issue.
- [ ] Record failures as test data or notes for `evaluation-strategy.md` — do not add flaky golden-string unit tests.

### Output (add to review report when this section is used)

- **Retrieval verdict:** `good` | `partial` | `poor` | `not evaluated` (no seed / no logs)
- **Misses:** question → expected id(s) → retrieved ids → brief cause hypothesis (wording, chunking, threshold, metadata filter)
- **Follow-up:** spec or config change — do not implement unless the user asks
