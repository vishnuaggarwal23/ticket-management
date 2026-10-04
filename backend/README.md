# Ticket management backend

Spring Boot 4 / Java 25 API under `com.ticketmanagement` with **type-based packages** for quick navigation:

**Build:** use **JDK 25** (`java.version` in `pom.xml`). With SDKMAN: `sdk use java 25.0.4-tem` (or set `JAVA_HOME`) before `./mvnw test`.

| Package | Role |
|---------|------|
| `controller` | REST endpoints |
| `dto` | HTTP request/response records (`common`, `request`, `response`, `serde`) |
| `advice` | Global exception → JSON error mapping |
| `service` | Application use cases (tickets, ask, ingest) |
| `entity` / `repository` | JPA entities + Spring Data JPA |
| `domain` | Enums and state machine |
| `exception` / `util` | Typed errors and small helpers |
| `rag` | Vector/embedding ports and adapters |
| `config` | Spring configuration |

Schema is owned by Liquibase. Tests use Testcontainers PostgreSQL only.

## Local run

Use your own PostgreSQL instance (pgvector extension required for RAG). Set datasource and Ollama variables (see `.env.example`), then:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tickets
export SPRING_DATASOURCE_USERNAME=tickets
export SPRING_DATASOURCE_PASSWORD=tickets
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_CHAT_MODEL=llama3.2
./mvnw spring-boot:run
```

Ollama is expected to already be running locally. Pull `nomic-embed-text` and your chat model (`OLLAMA_CHAT_MODEL`, default `llama3.2`) before ingest/ask.

Do not commit a `.env` file.

## Tests

```bash
./mvnw test
```

Requires **JDK 25** (see `java.version` in `pom.xml`). Integration tests use Testcontainers PostgreSQL only; RAG ports are stubbed in ITs (no real Ollama in the default suite).

### Recent test alignment (2026-10-04)

- **`AskApiIT.unknownAskPropertyIs400`** — Asserts HTTP **400** with `error.code` **`VALIDATION_ERROR`** and `details` for unknown ask body fields (e.g. `confidence`), consistent with `RestExceptionHandler`, `AiAskControllerSliceTest`, and [`spec/rag-api-contract.md`](../spec/rag-api-contract.md) **DEC-17** / **AC-RAG-API-01**.
