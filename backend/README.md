# Ticket management backend

Spring Boot 3 / Java 21 API under `com.ticketmanagement` with **type-based packages** for quick navigation:

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
