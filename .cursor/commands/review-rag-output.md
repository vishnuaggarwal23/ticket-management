# Review RAG / AI output (hallucination & grounding)

Review an AI assistant answer (ticket RAG `POST /api/ai/ask` or coding-assistant output) for **hallucination and ungrounded claims**.

## Inputs

- The **question** asked
- The **assistant answer** (full text)
- **Cited ticket IDs** (if any)
- Retrieved context / ticket excerpts when available
- For code reviews of AI patches: the diff plus relevant specs

## Grounding checklist (ticket assistant)

1. Does every factual claim appear in retrieved ticket context?
2. Are cited ticket IDs real and actually supporting the claim?
3. If retrieval was empty/weak: does the answer explicitly say **no relevant tickets found** (or equivalent)—not a plausible fabrication?
4. Did the assistant avoid general-world knowledge for support-specific questions?
5. Did it take out-of-scope actions (create ticket, notify, multi-step agent)? If yes → fail.

## Code-assistant checklist

1. APIs/classes/config keys—do they exist in this repo or agreed stack?
2. State-machine transitions—legal per `spec/state-machine.md`?
3. Any invented requirements not in `spec/`?

## Output format

- **Verdict**: grounded / partially grounded / ungrounded (hallucinated)
- **Ungrounded claims** (quote + why)
- **Missing citations**
- **Recommended user-visible answer** (corrected) or **reject**
- **Log?** If this was a meaningful AI mistake, propose an entry for `docs/ai-mistakes.md` (confirm before writing)
