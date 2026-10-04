# AI-Powered Support Ticket Management System

A spec-driven practice project for building a support ticket desk with **deterministic** ticket operations and a **grounded RAG** assistant over ticket history. Requirements come from the assessment in [`docs/Assessments.docx`](docs/Assessments.docx); the agreed product definition lives in [`spec/requirements.md`](spec/requirements.md).

## What it does

- **Tickets** — Create, list, search, filter, and view tickets; update fields; add comments; persist everything in PostgreSQL.
- **Status lifecycle** — Backend state machine (`OPEN` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`, plus cancellation paths); invalid transitions are rejected.
- **Ask (RAG)** — Natural-language questions over ingested ticket text; answers must be grounded in retrieved chunks, cite ticket IDs, and return an honest no-match when nothing relevant is found (no agent tools, no ticket creation from chat).
- **Ingestion** — On ticket create/update/close, ticket content is chunked, embedded, and stored in **PgVector** for similarity search before generation.

## Tech stack (project conventions)

| Layer | Choice |
|-------|--------|
| Backend | Java 25, Spring Boot 4, Spring AI 2.x, Maven Wrapper |
| Data | PostgreSQL + PgVector, Liquibase migrations |
| AI (local) | Ollama — `nomic-embed-text` (768-d embeddings) + a configurable chat model |
| Frontend | React, Next.js (App Router), Vite for shared `src/` dev, JavaScript (Node 24.x) |
| Tests | JUnit, Mockito, Testcontainers (PostgreSQL); no frontend tests this milestone |

The assessment PDF names Java 21 and allows H2; this repo standardizes on the versions above and PostgreSQL everywhere. See [`spec/architecture.md`](spec/architecture.md) and [`rules/`](rules/) for detail.

## Repository layout

| Path | Purpose |
|------|---------|
| [`backend/`](backend/) | Spring Boot REST API, domain state machine, RAG ingest/ask |
| [`frontend/`](frontend/) | Ticket list/detail UI, Ask screen — see [`frontend/README.md`](frontend/README.md) (UX patterns + changelog) |
| [`spec/`](spec/) | **Source of truth** — ten agreed specs (requirements, architecture, APIs, RAG, UI, tests, etc.) |
| [`rules/`](rules/) | Engineering standards (Java, API, RAG, frontend, testing, documentation) |
| [`commands/`](commands/) | Review and workflow commands used from Cursor (`.cursor/commands/` are pointers) |
| [`docs/`](docs/) | Assessment brief, prompt history index, AI mistake log |
| [`plan.md`](plan.md) | Implementation plan / task tracking |
| [`graphify-out/`](graphify-out/) | Codebase knowledge graph (optional; used for exploration) |
| [`.specstory/history/`](.specstory/history/) | Prompt/session history (SpecStory) |

## How we build it

Work follows **Spec-Driven Development**: Requirement → Specification → Plan/Tasks → Implementation → Testing → Review → Fix. Do not treat “build the whole app in one shot” as the starting point.

Before coding a slice, read the relevant `spec/*.md` files and the matching `rules/*.md`. Use [`commands/review-spec.md`](commands/review-spec.md) on spec changes and [`commands/review-code.md`](commands/review-code.md) / [`commands/review-frontend.md`](commands/review-frontend.md) on implementation.

## Run locally

You need **PostgreSQL with the pgvector extension**, **Ollama** (embed + chat models pulled), and env vars for the backend. Step-by-step setup is in the module READMEs:

- **API:** [`backend/README.md`](backend/README.md) — `./mvnw spring-boot:run` (default port **8080**)
- **UI:** [`frontend/README.md`](frontend/README.md) — `npm run dev` (default port **3000**); includes **UI architecture**, **UX patterns**, and a **changelog** of post-review frontend work (2026-10-04)

Copy `backend/.env.example` and `frontend/.env.example` to local env files; do not commit secrets.

## Key API surfaces (summary)

- Ticket REST under `/api/v1/...` — see [`spec/api-contract.md`](spec/api-contract.md)
- Ask: `POST /api/ai/ask` with `{ "question": "..." }` — see [`spec/rag-api-contract.md`](spec/rag-api-contract.md)

## Demo and acceptance

Grader-style walkthroughs: [`spec/requirements.md`](spec/requirements.md) §8.7 (end-to-end demo script) and [`spec/ui-flow.md`](spec/ui-flow.md). RAG output review: [`commands/review-rag-output.md`](commands/review-rag-output.md).

## License and context

This repository is a structured learning / assessment codebase: specs and process artefacts are as important as the runnable application. Start at [`spec/requirements.md`](spec/requirements.md) if you are new to the project.
