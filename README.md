# OptiMaxx Management System Foundation

This repository contains the foundation layer of the OptiMaxx Management System backend.

## Architecture (Clean Foundation)

- `domain` → core entities and repository contracts
- `application` → use-case layer skeleton
- `infrastructure` → technical integration placeholders
- `interfaces` → REST API/controllers
- `security` → JWT and RBAC foundation
- `config` → OpenAPI and security configuration

## Local Infrastructure

Start dependencies:

```bash
docker compose up -d
```

Stop dependencies:

```bash
docker compose down
```

Services:
- PostgreSQL: `localhost:5432`
- ClickHouse HTTP: `localhost:8123`
- ClickHouse Native: `localhost:9000`
- Redis: `localhost:6379`

## Run Application

```bash
./mvnw spring-boot:run
```

## Test

```bash
./mvnw test
```

## API Docs

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Health Endpoints

- Actuator health: `http://localhost:8080/actuator/health`
- API health sample: `http://localhost:8080/api/v1/system/health`

## Notes

- Soft delete metadata exists in `BaseEntity` (`is_deleted`, `deleted_at`, `deleted_by`).
- Multi-store readiness is represented with `store_id` in base model and ClickHouse schema.
- ClickHouse audit log schema is defined at `src/main/resources/db/clickhouse/audit_events.sql` and designed as immutable INSERT-only events.
