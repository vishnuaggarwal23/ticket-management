# Update prompt history index

Cursor attaches this file via [`.cursor/commands/update-prompt-history.md`](../.cursor/commands/update-prompt-history.md) (pointer only). Edit **this** file; do not copy the body into `.cursor/commands/`.

Rebuild or **refresh** the **Chronological session index (Cursor)** in [`docs/prompt-history.md`](../docs/prompt-history.md) from every SpecStory export under [`.specstory/history/`](../.specstory/history/). Assessment hygiene (**PDF**): prompt history index at `docs/prompt-history.md` plus tracked `.specstory/history/`.

**Row identity:** one table row per transcript file. Re-running this command **re-derives every row** from the current file on disk—not only appends new sessions. If a transcript gained more prompts since the last index run, **update that row’s description** (and Session (UTC) if the SpecStory header changed). If a transcript file was removed from `.specstory/history/`, **drop** its row. If filenames or sort order changed, **renumber** `#` accordingly.

## When to run

- After SpecStory saves one or more new session files under `.specstory/history/`.
- After an **existing** transcript file grows (same filename, more chat turns)—refresh descriptions so the index matches reality.
- Before a demo or sign-off when the index may be stale.
- When the user invokes **`/update-prompt-history`** (or asks to refresh the prompt-history index).

## Scope

| In scope | Out of scope |
|----------|----------------|
| Edit **only** `docs/prompt-history.md` (index table + revision history row) | Creating or deleting SpecStory transcript files |
| Read `.specstory/history/*.md` to summarise sessions | Rewriting full transcripts |
| Preserve intro, canonical location, expectations, Related, and Revision history sections | Changing assessment requirements or specs |

## Procedure

### 1. Discover sessions

1. List all `*.md` files in `.specstory/history/` (not subfolders unless SpecStory adds them later).
2. Sort **oldest first** by the UTC prefix in the filename: `YYYY-MM-DD_HH-MM-SSZ-...md`.
3. If two files share the same timestamp prefix, keep filename **lexicographic** order as tie-breaker (stable sort).

### 2. Derive each row

For each file:

| Column | Rule |
|--------|------|
| **#** | Sequential integer starting at 1 |
| **Session (UTC)** | From SpecStory header `# YYYY-MM-DD HH:MM:SSZ` if present; else parse filename `YYYY-MM-DD_HH-MM-SSZ` → `YYYY-MM-DD HH:MM:SS` |
| **Description** | **One or two sentences**, past tense, what the **user asked for** and what was **delivered** (specs, rules, docs, commits—not chat filler). Read the **whole** transcript (or all `_**User**_` sections if long)—not only the opening prompt—so updates after the row was first added are reflected. No secrets; no huge paste from transcripts. |
| **Transcript** | Markdown link: `` [`.specstory/history/<filename>`](../.specstory/history/<filename>) `` (path relative to `docs/prompt-history.md`) |

### 3. Sync index with disk (add, update, remove rows)

Match existing index rows to transcripts by **linked filename** (basename in the Transcript column). For each file on disk after step 1:

- **New filename** → new row (append in sort order; do not only tack onto the end if sort places it earlier).
- **Existing filename** → replace Session (UTC), Description, and Transcript link if anything changed; **do not** keep a stale description because a row already existed.
- **Filename in index but not on disk** → remove that row (transcript deleted or renamed; if renamed, treat as new file + remove old row).

Then replace **only** the markdown table under `## Chronological session index (Cursor)` (header row through last data row) with the full synced result. **Do not** remove or shorten:

- Title and SpecStory / Kiro notes
- `## Canonical location`
- `## Repository expectations` (including the bullet that points maintainers at this index)
- `## Related`
- `## Revision history` (append one new row—see below)

Keep this table header exactly:

```markdown
| # | Session (UTC) | Description | Transcript |
|---|---------------|-------------|------------|
```

Intro line above the table (keep or restore if missing):

`SpecStory export files, oldest first. Full transcripts (user + agent) are in the linked paths.`

### 4. Revision history

Append a row to `docs/prompt-history.md` **Revision history**:

`| YYYY-MM-DD | Synced chronological index from .specstory/history/ (N sessions; added A, updated U, removed R). |`

Use today’s date, session count **N**, and counts **A** / **U** / **R** (zero allowed). If you did a full replace without diffing, use “synced (N sessions)” only.

### 5. Report

Reply briefly:

- Number of sessions indexed (**N**)
- **Added / updated / removed** row counts when known (compare prior table in git or before overwrite)
- Filenames whose **descriptions changed** (optional short list if **U** > 0)
- Confirmation that only `docs/prompt-history.md` was edited

## Quality bar for descriptions

- **Good:** “Add `state-machine.md` and `api-contract.md` from PDF; expand illegal transition matrix; sync test-strategy AC mapping.”
- **Bad:** “Chat about specs.” / “Various updates.”
- Mention **artefacts** (`spec/…`, `rules/…`, `docs/…`) when they were the main outcome.
- Multi-topic sessions: summarise the **arc** in two sentences max.

## Errors

| Situation | Action |
|-----------|--------|
| `.specstory/history/` empty or missing | Stop; tell user to enable SpecStory and run at least one Cursor chat |
| Transcript unreadable | Use filename slug as fallback description; note in report |
| User asked not to edit docs | Do not run—confirm first |

---

## Revision history

| Date | Note |
|------|------|
| 2026-10-04 | Initial command: rebuild `docs/prompt-history.md` index from `.specstory/history/`. |
| 2026-10-04 | Sync semantics: update existing rows when transcripts change; remove stale rows. |
