# OptiMaxx Heroku Production v1 Runbook

This runbook deploys the first production version on Heroku:

- `optimaxx-web-prod`: Next.js frontend
- `optimaxx-api-prod`: Spring Boot backend
- Public domains: `optimaxx.com.tr`, `www.optimaxx.com.tr`, `panel.optimaxx.com.tr`
- API domain: `api.optimaxx.com.tr`

Production v1 uses Heroku Postgres and Cloudinary as the only durable data stores. Redis is optional and ephemeral: it is used only for login/forgot-password attempt counters and similar short-lived cache/rate-limit state. ClickHouse is intentionally disabled for this first deploy.

## 0. Budget Target

The low-traffic production target is roughly `$13/month` or less. Confirm the final estimate in the Heroku Dashboard before release because plan prices can change.

| Component | Plan | Expected role | Approx. monthly cost |
| --- | --- | --- | --- |
| Frontend dyno | Eco | Next.js web dyno, may sleep when idle | Part of `$5` Eco pool |
| Backend dyno | Eco | Spring Boot API dyno, may sleep when idle | Part of `$5` Eco pool |
| Database | `heroku-postgresql:essential-0` | Durable source of truth | About `$5` |
| Redis / Heroku Key-Value Store | `heroku-redis:mini` | Ephemeral counters/cache only | About `$3` |
| Upload storage | Cloudinary Starter/Free | Durable site editor images | `$0` |
| ClickHouse | Disabled | Future optional mirror only | `$0` |

Eco dynos share the Eco monthly dyno-hour pool. Two web dynos can consume that pool faster if both stay awake continuously, so monitor Eco hours in the Heroku Dashboard. For the expected low traffic, sleep behavior should keep usage low. The first request after sleep can be slow, but this does not cause data loss because business data is in PostgreSQL and uploaded assets are in Cloudinary.

Redis Mini is not a durable data store. Reboot/failure data loss is acceptable because Redis stores only temporary auth attempt counters/cache. Customers, sales, stock, prescriptions, repairs, CMS content, users, audit records, and all business data must stay in PostgreSQL.

## 1. Login

```bash
heroku login
heroku container:login
```

## 2. Create Apps

Create only if the apps do not already exist.

```bash
heroku apps:create optimaxx-api-prod --region eu
heroku apps:create optimaxx-web-prod --region eu
```

## 3. Dyno Plan

Subscribe to the Eco dyno plan in Heroku Billing, then run one Eco web dyno for each app:

```bash
heroku ps:scale web=1:eco -a optimaxx-api-prod
heroku ps:scale web=1:eco -a optimaxx-web-prod
```

Eco is the budget choice for low traffic. Upgrade to Basic/Standard dynos later if sleep/wake latency becomes unacceptable.

## 4. Backend Add-ons

Attach Heroku Postgres to the backend app:

```bash
heroku addons:create heroku-postgresql:essential-0 -a optimaxx-api-prod
heroku pg:wait -a optimaxx-api-prod
```

Attach Heroku Key-Value Store / Redis Mini. The Heroku CLI add-on slug is still `heroku-redis`:

```bash
heroku addons:create heroku-redis:mini -a optimaxx-api-prod
```

The add-on provides `REDIS_URL`. Verify it exists:

```bash
heroku config:get REDIS_URL -a optimaxx-api-prod
```

Redis is recommended but optional for production v1. If `REDIS_URL` is absent, the backend still starts and falls back to per-dyno in-memory counters for login attempts and forgot-password attempts. Do not store business data or audit records in Redis.

Heroku Key-Value Store requires TLS. The backend enables Lettuce SSL when `REDIS_URL`/`spring.data.redis.url` is present.

Set up Cloudinary for persistent site editor assets. Either use the Heroku add-on and confirm the Starter/Free plan in the dashboard:

```bash
heroku addons:create cloudinary -a optimaxx-api-prod
```

Or use an external Cloudinary account and set one of these config styles:

```bash
heroku config:set CLOUDINARY_URL='cloudinary://<api_key>:<api_secret>@<cloud_name>' -a optimaxx-api-prod
```

```bash
heroku config:set \
  CLOUDINARY_CLOUD_NAME='<cloud_name>' \
  CLOUDINARY_API_KEY='<api_key>' \
  CLOUDINARY_API_SECRET='<api_secret>' \
  -a optimaxx-api-prod
```

Cloudinary is the durable upload store. Heroku dyno local disk is ephemeral and must not be treated as persistent asset storage.

## 5. Backend Config Vars

Heroku Postgres provides `DATABASE_URL`. The backend parses it into Spring JDBC settings. Heroku Key-Value Store provides `REDIS_URL` if the Redis add-on is installed. Do not set ClickHouse variables for production v1.

```bash
heroku config:set \
  SPRING_PROFILES_ACTIVE=prod \
  APP_CORS_ALLOWED_ORIGINS='https://optimaxx.com.tr,https://www.optimaxx.com.tr,https://panel.optimaxx.com.tr' \
  JWT_SECRET_KEY='<generate-a-long-random-secret>' \
  JWT_ACCESS_MINUTES=60 \
  JWT_REFRESH_MINUTES=10080 \
  BOOTSTRAP_OWNER_ENABLED=true \
  BOOTSTRAP_OWNER_USERNAME='<owner-username>' \
  BOOTSTRAP_OWNER_EMAIL='<owner-email>' \
  BOOTSTRAP_OWNER_PASSWORD='<temporary-strong-password>' \
  SITE_ASSET_STORAGE=cloudinary \
  CLOUDINARY_FOLDER='optimaxx/site' \
  -a optimaxx-api-prod
```

`BOOTSTRAP_OWNER_PASSWORD` is temporary. Disable bootstrap after the owner has logged in successfully.

Cloudinary must be configured for production uploads. With `SPRING_PROFILES_ACTIVE=prod` and `SITE_ASSET_STORAGE=cloudinary`, a missing Cloudinary configuration should fail uploads clearly instead of writing files to Heroku local disk.

## 6. Frontend Config Vars

Set these on `optimaxx-web-prod`:

```bash
heroku config:set \
  NEXT_PUBLIC_API_URL='https://api.optimaxx.com.tr' \
  PANEL_ORIGIN='https://panel.optimaxx.com.tr' \
  PANEL_HOSTNAMES='panel.optimaxx.com.tr' \
  -a optimaxx-web-prod
```

The public site must not expose login, admin, panel, or appointment/randevu buttons. Public `/login`, `/admin`, and `/sales` requests on non-panel hosts redirect to `PANEL_ORIGIN`.

## 7. Container Build and Release

Backend:

```bash
cd spring-boot-admin-panel-project
heroku container:push web -a optimaxx-api-prod
heroku container:release web -a optimaxx-api-prod
```

Frontend:

```bash
cd admin-panel-frontend
heroku container:push web \
  --arg NEXT_PUBLIC_API_URL=https://api.optimaxx.com.tr \
  --arg PANEL_ORIGIN=https://panel.optimaxx.com.tr \
  --arg PANEL_HOSTNAMES=panel.optimaxx.com.tr \
  -a optimaxx-web-prod
heroku container:release web -a optimaxx-web-prod
```

## 8. Domains

Backend:

```bash
heroku domains:add api.optimaxx.com.tr -a optimaxx-api-prod
heroku domains -a optimaxx-api-prod
```

Frontend:

```bash
heroku domains:add optimaxx.com.tr -a optimaxx-web-prod
heroku domains:add www.optimaxx.com.tr -a optimaxx-web-prod
heroku domains:add panel.optimaxx.com.tr -a optimaxx-web-prod
heroku domains -a optimaxx-web-prod
```

Copy the Heroku DNS target returned for each domain.

## 9. Cloudflare DNS

Create CNAME records in Cloudflare using the DNS targets from `heroku domains`:

| Name | Target |
| --- | --- |
| `@` | Heroku DNS target for `optimaxx.com.tr` |
| `www` | Heroku DNS target for `www.optimaxx.com.tr` |
| `panel` | Heroku DNS target for `panel.optimaxx.com.tr` |
| `api` | Heroku DNS target for `api.optimaxx.com.tr` |

For the apex/root record, use Cloudflare CNAME flattening. Keep the records DNS-only while Heroku ACM is issuing certificates; proxying can be evaluated after SSL is healthy.

## 10. ACM / SSL

Enable and verify automated certificates:

```bash
heroku certs:auto:enable -a optimaxx-api-prod
heroku certs:auto:enable -a optimaxx-web-prod
heroku certs:auto -a optimaxx-api-prod
heroku certs:auto -a optimaxx-web-prod
heroku domains:wait api.optimaxx.com.tr -a optimaxx-api-prod
heroku domains:wait optimaxx.com.tr -a optimaxx-web-prod
heroku domains:wait www.optimaxx.com.tr -a optimaxx-web-prod
heroku domains:wait panel.optimaxx.com.tr -a optimaxx-web-prod
```

Smoke check:

```bash
curl -I https://api.optimaxx.com.tr/actuator/health
curl -I https://optimaxx.com.tr
curl -I https://panel.optimaxx.com.tr
```

## 11. Owner Bootstrap

After release, watch logs and log in as the bootstrap owner at `https://panel.optimaxx.com.tr/login`.

```bash
heroku logs --tail -a optimaxx-api-prod
```

After the owner login succeeds:

```bash
heroku config:set BOOTSTRAP_OWNER_ENABLED=false -a optimaxx-api-prod
heroku restart -a optimaxx-api-prod
```

## 12. Backups

Manual Heroku Postgres backup:

```bash
heroku pg:backups:capture -a optimaxx-api-prod
heroku pg:backups -a optimaxx-api-prod
```

Optional scheduled Heroku Postgres backup:

```bash
heroku pg:backups:schedule DATABASE_URL --at '03:00 Europe/Istanbul' -a optimaxx-api-prod
heroku pg:backups:schedules -a optimaxx-api-prod
```

Restore drills should be planned before production traffic grows:

```bash
heroku pg:backups:download -a optimaxx-api-prod
```

Cloudinary assets are durable in Cloudinary, but they are not included in Heroku PGBackups. On the free plan, check Cloudinary export/backup options and quota limits separately if asset backup becomes an operational requirement.

Redis backups are not required because Redis is not a source of truth. ClickHouse backups are not required for production v1 because ClickHouse is disabled.

## 13. Backup Smoke Test

Run after the first production deploy:

```bash
heroku pg:backups:capture -a optimaxx-api-prod
heroku pg:backups -a optimaxx-api-prod
```

Cloudinary upload smoke:

```bash
curl -f \
  -H "Authorization: Bearer $OWNER_ACCESS_TOKEN" \
  -F "file=@/tmp/valid-site-image.png;type=image/png" \
  https://api.optimaxx.com.tr/api/v1/admin/pages/assets
```

Expected result: JSON with a Cloudinary `https://res.cloudinary.com/...` URL. If Cloudinary config is missing, the endpoint should return a clear configuration error instead of writing to Heroku local disk.

## 14. ClickHouse Status

ClickHouse is disabled for production v1 unless these are explicitly set:

```bash
CLICKHOUSE_URL
CLICKHOUSE_USERNAME
CLICKHOUSE_PASSWORD
```

Without `CLICKHOUSE_URL`, the app still starts, admin audit screens read PostgreSQL `activity_logs`, analytics reads PostgreSQL repositories, and the retry queue stays inactive. `SecurityAuditService` always writes to PostgreSQL first; ClickHouse is only a future optional audit/analytics mirror.

ClickHouse does not serve frontend data in production v1. Do not include ClickHouse in the first deploy checklist. If ClickHouse Cloud is added later, backup/restore and retention must be planned separately for that service.

## 15. Future Upgrades

Use these only when traffic or operational requirements justify the extra cost:

- Upgrade Eco dynos to Basic/Standard if sleep latency is hurting users.
- Upgrade Redis to a persistent/premium Heroku Key-Value Store plan only if Redis starts holding state that must survive restarts. Production v1 should not need this.
- Add ClickHouse Cloud only as an optional PostgreSQL audit/analytics mirror after audit volume grows. Plan ClickHouse backup/restore before enabling it.
