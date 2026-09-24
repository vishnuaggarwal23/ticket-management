# Generate tests

Generate tests from **agreed specs** and acceptance criteria—not from guesswork.

## Inputs

- Specs: especially `test-strategy.md`, `state-machine.md`, `api-contract.md`, `rag-api-contract.md`, `evaluation-strategy.md`
- Existing test layout and frameworks in the repo
- Code under test (if already present)

## Rules

- Prefer Given/When/Then names tied to acceptance criteria.
- Cover valid and **invalid** status transitions.
- Cover validation failures and meaningful API errors.
- For RAG: grounded citation cases + **no relevant tickets** case; stub LLM in unit/CI tests unless user approves live calls.
- Do not invent endpoints or fields absent from specs—ask first.
- Follow `.cursor/rules/testing.mdc`.

## Output

1. List of proposed test cases mapped to acceptance criteria
2. Confirm with user if any criterion is ambiguous
3. Only then write test code in the correct packages

If implementation is missing, generate test stubs/interfaces that will fail until code exists—or wait for implementation if the user prefers.
