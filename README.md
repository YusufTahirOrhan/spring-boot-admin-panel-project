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

## Commit Message Convention

Use English commit messages with this structure:

1. **Subject** (single line, conventional commit format)
   - Example: `feat: add login rate limit and temporary lockout protection`
2. **Body** (3-6 bullets)
   - `Added: ...`
   - `Updated: ...`
   - `Tests: ...`

Example:

```text
feat: add login rate limit and temporary lockout protection

- Added in-memory login attempt tracking with temporary lockout window.
- Updated AuthService login flow to enforce lock checks and reset attempts on success.
- Updated API exception handling to return structured 429 responses.
- Tests: ./mvnw test (passed)
```

## Notes

- Soft delete metadata exists in `BaseEntity` (`is_deleted`, `deleted_at`, `deleted_by`).
- Multi-store readiness is represented with `store_id` in base model and ClickHouse schema.
- ClickHouse audit log schema is defined at `src/main/resources/db/clickhouse/audit_events.sql` and designed as immutable INSERT-only events.
