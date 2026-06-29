# OptiMaxx Backend

Spring Boot backend for the OptiMaxx management system.

## Local Infrastructure

```bash
docker compose up -d
```

Services:

- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- ClickHouse HTTP: `localhost:8123`

## Run Locally

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Default dev owner credentials come from `application.yml`:

- Username: `owner`
- Email: `owner@optimaxx.local`
- Password: `owner12345`

## Test

```bash
./mvnw test
```

Windows PowerShell:

```powershell
.\mvnw.cmd test
```

## Public Website CMS

The public homepage is managed through page blocks.

- Public: `GET /api/v1/public/pages/home`
- Admin draft: `GET /api/v1/admin/pages/home/draft`
- Admin save draft: `PUT /api/v1/admin/pages/home/draft`
- Admin publish: `POST /api/v1/admin/pages/home/publish`

Supported block types: `hero`, `services`, `featuredProducts`, `about`, `contact`, `hours`, `cta`.

## Production Environment

Set `SPRING_PROFILES_ACTIVE=prod` and provide:

```bash
DB_URL=jdbc:postgresql://host:5432/optimaxx
DB_USERNAME=...
DB_PASSWORD=...
REDIS_HOST=...
REDIS_PORT=6379
CLICKHOUSE_URL=http://host:8123
CLICKHOUSE_USERNAME=...
CLICKHOUSE_PASSWORD=...
JWT_SECRET_KEY=at-least-32-random-characters
APP_CORS_ALLOWED_ORIGINS=https://panel.example.com
BOOTSTRAP_OWNER_ENABLED=false
```

If a first owner account must be created automatically on first deploy, set `BOOTSTRAP_OWNER_ENABLED=true` once and provide `BOOTSTRAP_OWNER_USERNAME`, `BOOTSTRAP_OWNER_EMAIL`, and `BOOTSTRAP_OWNER_PASSWORD`; turn it off after the owner exists.

## API Docs and Health

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/actuator/health`
