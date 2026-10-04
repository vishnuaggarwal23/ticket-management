# Ticket management backend

Spring Boot 3 / Java 21 API. Schema is owned by Liquibase. Tests use Testcontainers PostgreSQL, not this Compose file.

## Local database

```bash
docker compose up -d
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tickets
export SPRING_DATASOURCE_USERNAME=tickets
export SPRING_DATASOURCE_PASSWORD=tickets
./mvnw spring-boot:run
```

Variable names are listed in `.env.example`. Do not commit a `.env` file.

## Tests

```bash
./mvnw test
```
