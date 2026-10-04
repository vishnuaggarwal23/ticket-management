# Update AI error log

Cursor attaches this file via [`.cursor/commands/update-ai-error.md`](../.cursor/commands/update-ai-error.md) (pointer only). Edit **this** file; do not copy the body into `.cursor/commands/`.

Keep [`docs/ai-error.md`](../docs/ai-error.md) as the chronological log of **meaningful** AI mistakes (wrong code, wrong specs/docs, or ungrounded / hallucinated answers). Assessment: **AC-CORE-23** / **FEAT-23**. Alias [`docs/ai-mistakes.md`](../docs/ai-mistakes.md) stays a **pointer** — do not put entries there.

User input (`$ARGUMENTS`) may name an error, a path, or a session. Empty args still mean: scan for **new** errors and insert them in date order.

## When to run

- User invokes **`/update-ai-error`** (or asks to update the AI error log).
- After a review (`review-code`, `review-spec`, `review-rag-output`, `review-frontend`) finds a meaningful mistake the user wants logged.
- After a test, demo, or chat catch of wrong AI code or an ungrounded ask answer.
- Before demo step 15 ([`spec/requirements.md`](../spec/requirements.md) §8.7) if the log may be stale.

## Scope

| In scope | Out of scope |
|----------|----------------|
| Edit **`docs/ai-error.md`** (index table + entries + revision history) | Inventing errors that were not caught |
| Optionally one revision-history row on this command file if the **command** changed | Rewriting SpecStory transcripts |
| Keep `docs/ai-mistakes.md` as a pointer only | Logging style nits, typos, or “AI was slow” |
| | Duplicate of an existing **AE-*** (same event) |

Do **not** create a fake RAG hallucination to satisfy AC-CORE-23. If there is still no RAG/code product mistake, leave that gap explicit in the log intro.

## What counts

Log when **all** of these hold:

1. The assistant produced something **incorrect** vs PDF, agreed spec, `rules/`, tests, or retrieved ticket text.
2. A human or review command **caught** it (not blindly merged).
3. There is a **resolution** (fixed artefact, rejected output, or honest no-match correction).

Skip: open questions the assistant **asked** instead of guessing; user-directed experiments; already-logged events.

If unsure whether an item is meaningful, **confirm with the user** before writing.

## Procedure

### 1. Read the current log

Open [`docs/ai-error.md`](../docs/ai-error.md). Collect existing **AE-NNN** ids, **When** timestamps, and titles. Next id = max N + 1, zero-padded to three digits (`AE-009`). **Never reuse or renumber** ids.

### 2. Collect candidates

Use, in order:

1. **`$ARGUMENTS`** / the current user message (explicit error to add).
2. **This chat** — review findings, failed tests, user corrections.
3. **Recent SpecStory** under `.specstory/history/` only if needed to date an event or fill “how detected”.
4. Optional: last `review-*` output in the conversation.

For each candidate record:

| Field | Rule |
|-------|------|
| **When** | UTC date (and time if known) of the **mistake**, not of this command run. Filename prefix `YYYY-MM-DD_HH-MM-SSZ` if from SpecStory. |
| **Kind** | `spec / docs` \| `process` \| `code` \| `RAG` |
| **What was wrong** | Concrete: file, claim, or behaviour. No secrets. |
| **How detected** | Review command, test, user, demo — with transcript or test name when known. |
| **How resolved** | What changed (files, mapping, rejected answer). Required. |

Deduplicate: same **When** + same underlying fact as an existing entry → skip.

### 3. Insert in chronological order

Do **not** only append if the new **When** is earlier than the last entry.

1. Assign the next **AE-NNN**.
2. Insert the `## AE-NNN` section so **When** is ascending (oldest first); tie-break by id.
3. Rebuild the **Index** table in the same order. Columns: Id (link `#ae-nnn`), When (UTC), Kind, Title.
4. Keep the intro, kind legend, and “no invented RAG row” note unless the first **RAG** entry is added (then drop that sentence).
5. Heading format:

```markdown
## AE-NNN

- **When:** YYYY-MM-DD HH:MM UTC
- **Kind:** …
- **What was wrong:** …
- **How detected:** …
- **How resolved:** …
```

Optional evidence links to `.specstory/history/…` or spec paths (relative to `docs/`).

### 4. Revision history

Append one row to `docs/ai-error.md` **Revision history**:

`| YYYY-MM-DD | Added AE-NNN… (k new); skipped d duplicates; total N entries. |`

Use today’s date. If nothing new: `| YYYY-MM-DD | /update-ai-error: no new entries (N existing). |` — still allowed; do not invent.

### 5. Pointer file

If `docs/ai-mistakes.md` is missing or no longer points at `docs/ai-error.md`, restore the short pointer. Do not copy the log body into it.

### 6. Report

Reply briefly:

- **Added** ids and titles, or none
- **Skipped** duplicates (titles)
- **Total** entries in the log
- Confirm only `docs/ai-error.md` (and pointer if needed) was edited

## Quality bar

- **Good:** “`api-contract.md` marked `URGENT` invalid; data-model **CRITICAL** + inbound alias. Fixed as C-02.”
- **Bad:** “AI made a mistake somewhere.” / no resolution / guessed a RAG fail with no ask run.

## Errors

| Situation | Action |
|-----------|--------|
| No candidates and empty args | Do not invent; report no new entries |
| User asked not to edit docs | Stop |
| Resolution unknown | Confirm with user; do not log an unresolved guess |
| Secret in the draft | Strip; never paste keys or prompts |

---

## Revision history

| Date | Note |
|------|------|
| 2026-10-04 | Initial command: chronological `docs/ai-error.md` updates; `docs/ai-mistakes.md` pointer. |
