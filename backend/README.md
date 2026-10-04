# Ticket management backend

Spring Boot 3 / Java 21 API. Schema is owned by Liquibase. Tests use Testcontainers PostgreSQL, not this Compose file.

## Local database

```bash
docker compose up -d
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tickets
export SPRING_DATASOURCE_USERNAME=tickets
export SPRING_DATASOURCE_PASSWORD=tickets
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_CHAT_MODEL=llama3.2
./mvnw spring-boot:run
```

Ollama is expected to already be running locally (not started by this Compose file). Pull `nomic-embed-text` and your chat model (`OLLAMA_CHAT_MODEL`, default `llama3.2`) before ingest/ask.

Variable names are listed in `.env.example`. Do not commit a `.env` file.

## Tests

```bash
./mvnw test
```
