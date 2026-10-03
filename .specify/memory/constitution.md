<!--
Sync Impact Report
- Version change: placeholder [CONSTITUTION_VERSION] → 1.0.0
- Modified principles:
  - [PRINCIPLE_1_NAME] → I. Spec-Driven Assessment Workflow
  - [PRINCIPLE_2_NAME] → II. Assessment Authority
  - [PRINCIPLE_3_NAME] → III. Clear Responsibilities and Simple Design
  - [PRINCIPLE_4_NAME] → IV. Deterministic Quality and Honest Testing
  - [PRINCIPLE_5_NAME] → V. Grounded RAG Integrity
- Added principles:
  - VI. AI-Assisted Development Governance
  - VII. No Premature Decisions
- Added sections:
  - Approved Technology Conventions (project conventions, not PDF mandates)
  - Specification Organization and Standards
- Removed sections: none (placeholder sections replaced)
- Follow-up TODOs:
  - Spec Kit default feature path is `specs/[###-feature]/`; assessment
    artefacts already live under `spec/`. Do not silently merge these trees.
  - `resolve-template.sh constitution-template --json` was not executed in
    this pass (shell classification blocked). Scaffold used:
    `.specify/templates/constitution-template.md` (matches memory placeholder).
-->

# AI-Powered Support Ticket Management System Constitution

## Core Principles

### I. Spec-Driven Assessment Workflow

Work MUST follow: Requirement → Specification → Plan/Tasks → Implementation →
Testing → Review → Fix.

Implementation MUST NOT start while the relevant specifications in `spec/` have
not been reviewed and approved. Broad prompts such as "Build the complete
application" MUST NOT be used as a starting instruction. Delivery MUST be
incremental and tied to agreed acceptance criteria.

**Rationale:** The assessment grades process as well as product. Skipping
specification or collapsing the workflow into a one-shot build produces
unguessable scope and unreviewable diffs.

### II. Assessment Authority

`docs/Assessments.pdf` is authoritative for **what the assignment requires**.
`spec/` is the implementable extraction of those requirements plus **agreed
project conventions**.

Contributors MUST distinguish:

- **Assessment requirements** — explicit PDF obligations
- **Approved conventions** — project technology and process choices recorded
  here and in engineering rules
- **Open decisions** — unspecified details that MUST be recorded in the
  relevant specification, not silently promoted to mandatory requirements

Contributors MUST NOT invent product features the assessment does not support.

**Rationale:** Treating conventions as if the PDF mandated them, or filling
gaps by guesswork, misrepresents both the assignment and the design record.

### III. Clear Responsibilities and Simple Design

Keep responsibilities clear and avoid unnecessary complexity. Prefer simple,
understandable solutions over premature abstraction. One backend application
and one frontend application over REST is the default shape. Changing this
principle requires an explicit constitution amendment.

Ticket business rules and the status state machine MUST live in domain/services,
not in HTTP adapters, repositories, or the UI. Persistence MUST NOT bypass the
state machine. AI-provider choice MUST stay in configuration, not scattered
through services.

Detailed Java/Spring, API, frontend, testing, and RAG rules live in
`rules/` and `.cursor/rules/`. This constitution MUST NOT duplicate those
catalogues.

**Rationale:** Blurred layers make the state machine and RAG pipeline hard to
test, review, and keep aligned with specs.

### IV. Deterministic Quality and Honest Testing

Deterministic ticket behaviour MUST be validated: backend input validation,
legal and illegal status transitions, persistence across restart, and
Liquibase-applied schema where persistence is under test.

Integration tests that need a database MUST use PostgreSQL via Testcontainers
for that purpose. Docker Compose MAY support local infrastructure; it MUST NOT
be the only way backend integration tests can run. There is no CI pipeline in
this milestone; local, repeatable runs are sufficient.

Deterministic application tests MUST be kept separate from probabilistic RAG
evaluation. The default deterministic suite MUST NOT depend on live LLM or
embedding API calls; use doubles unless a later agreed evaluation specification
requires otherwise.

AI-generated code and specifications MUST be reviewed, tested, and corrected
where they fail specs or acceptance criteria.

**Rationale:** Ticket correctness is binary; retrieval and generation quality
are not. Mixing those concerns produces brittle tests and false confidence.

### V. Grounded RAG Integrity

Support answers MUST be grounded only in retrieved ticket data the system has
(description, comments, and resolution notes as ingested). Citations MUST be
ticket IDs that appear in the retrieval result, not identifiers guessed by the
model.

If nothing relevant is retrieved, the system MUST communicate that explicitly
(honest no-match). It MUST NOT fabricate tickets, facts, or citations, and MUST
NOT fall back on general model knowledge for support questions.

The ask path is a single retrieve-then-generate flow. Agents, tool chaining,
ticket creation, and notifications from `/api/ai/ask` are out of scope.
Expanding these AI capabilities requires an explicit constitution amendment.

**Rationale:** Ungrounded answers fail the assessment's RAG integrity bar even
if the wording looks plausible.

### VI. AI-Assisted Development Governance

AI-generated output is a proposal, not verified truth. Diffs MUST be reviewed.
Unsupported assumptions MUST be challenged. Specs and code MUST be corrected
when they invent requirements, skip the state machine, or assert ungrounded RAG
claims.

Prompts and meaningful development decisions MUST remain in the project's
prompt-history artefacts (SpecStory under `.specstory/history/` and
`docs/prompt-history.md`). When a meaningful AI mistake occurs (wrong code or
an ungrounded answer), it MUST be documented (for example in
`docs/ai-mistakes.md`) with what was wrong, how it was found, and how it was
corrected. At least one such record is required during development.

**Rationale:** The assessment requires reusable AI instructions, prompt
history, and evidence that AI mistakes were caught rather than accepted.

### VII. No Premature Decisions

Unresolved design MUST remain open until recorded in the appropriate `spec/`
artefact and agreed. Do not prescribe in this constitution (or silently in
code) unless already explicitly approved in the repository:

- exact dependency versions (including Spring Boot patch, Spring AI, PostgreSQL,
  PgVector, embedding model)
- package structures
- API paths, payloads, error JSON, or ask-response schemas beyond what the PDF
  names
- authentication or authorization
- vector dimensions, chunking algorithm and sizes, numeric top-K or similarity
  thresholds
- frontend testing frameworks
- other product or design choices listed as open questions in `spec/`

**Rationale:** Guessing these values creates competing "requirements" that the
assessment never stated.

## Approved Technology Conventions

These are **approved project conventions**. They are **not** claims that the
assessment PDF mandates every specific technology or version.

| Area | Convention |
|------|------------|
| Language | Java 21 |
| Backend | Spring Boot 3 |
| Build | Maven with Maven Wrapper (`./mvnw`) |
| Database / vectors | PostgreSQL with PgVector |
| Schema | Liquibase migrations |
| AI framework | Spring AI |
| Initial AI provider | Ollama, via configuration |
| Frontend | React with Vite and TypeScript (not Next.js) |
| Integration tests | Testcontainers with PostgreSQL |
| Local infrastructure | Docker Compose |
| CI | None initially |

The PDF names Java 21, Spring Boot, Spring AI, PostgreSQL/H2, an embedding
model, a vector store (examples include PGVector or Chroma), REST, and
React/Next.js or equivalent. Where this table is narrower than the PDF, the
narrowing is a **project choice**.

Secrets, API keys, and machine-specific paths MUST NOT be committed.

## Specification Organization and Standards

Assessment-expected implementable specifications live under `spec/`, including
as work proceeds: `requirements.md`, `architecture.md`, `data-model.md`,
`api-contract.md`, `state-machine.md`, `rag-ingestion.md`,
`rag-api-contract.md`, `evaluation-strategy.md`, `ui-flow.md`, and
`test-strategy.md`.

Existing `spec/requirements.md` and `spec/architecture.md` are the current
requirement and architecture drafts. They MUST NOT be discarded or silently
replaced by a parallel Spec Kit tree.

Spec Kit's installed scripts and templates use `specs/[###-feature-name]/`
(`spec.md`, `plan.md`, `tasks.md`). That layout MUST NOT be treated as a
second product source of truth. Integrating Spec Kit feature directories with
`spec/` requires a **separate, explicit decision**. This constitution does not
resolve that conflict by changing templates, commands, or repository layout.

Engineering detail belongs in:

- `rules/` and `.cursor/rules/` (Java/Spring, API, frontend, testing, RAG,
  documentation)
- `commands/` and `.cursor/commands/` (review and test-generation prompts)

Code and tests MUST follow approved specifications. When a specification is
missing or contradictory, stop and confirm rather than guessing.

## Governance

This constitution governs process and principle for this repository. Detailed
engineering rules MUST remain consistent with it; if they conflict, the
conflict MUST be raised and resolved by amendment rather than ignored.

**Amendments:** Changes require an explicit human review of the proposed text,
a version bump, and an updated Last Amended date. Do not amend by implication
in a feature spec or a code comment.

**Versioning:** Semantic versioning.

- MAJOR: incompatible removal or redefinition of a principle
- MINOR: new principle or materially expanded guidance
- PATCH: clarification, wording, or non-semantic refinement

**Compliance:** Reviews of specifications, plans, tasks, and implementation
MUST check: workflow not skipped; assessment vs convention vs open question
labelled correctly; no invented product scope; RAG grounding and no-match
behaviour preserved; tests split deterministic vs probabilistic; AI output
reviewed. Complexity beyond this constitution requires justification in an
agreed specification.

**Ratification:** First project-specific constitution, replacing the Spec Kit
placeholder scaffold.

**Version**: 1.0.0 | **Ratified**: 2026-10-03 | **Last Amended**: 2026-10-03
