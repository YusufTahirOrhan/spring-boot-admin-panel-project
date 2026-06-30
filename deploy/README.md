# OptiMaxx VPS Deploy

Bu klasor tek Oracle Cloud Always Free VPS uzerinde Docker Compose, Caddy, PostgreSQL, Redis, ClickHouse, backend ve Next.js frontend calistirmak icin hazirlandi.

## Hedef Dizinler

```bash
sudo mkdir -p /opt/optimaxx
cd /opt/optimaxx
git clone https://github.com/YusufTahirOrhan/admin-panel-frontend.git
git clone https://github.com/YusufTahirOrhan/spring-boot-admin-panel-project.git
```

Compose build contextleri bu iki dizini kullanir:

- `/opt/optimaxx/admin-panel-frontend`
- `/opt/optimaxx/spring-boot-admin-panel-project`

## DNS

Cloudflare DNS kayitlari VPS public IP adresine gitmelidir:

- `optimaxx.com.tr` -> A kaydi
- `www.optimaxx.com.tr` -> A veya CNAME
- `panel.optimaxx.com.tr` -> A veya CNAME
- `api.optimaxx.com.tr` -> A veya CNAME

VPS firewall tarafinda sadece 80 ve 443 public acik olmali. PostgreSQL, Redis ve ClickHouse icin dis port yayinlanmaz.

## Ilk Kurulum

```bash
cd /opt/optimaxx/spring-boot-admin-panel-project/deploy
cp .env.example .env
nano .env
```

`.env` icinde en az su alanlari guclu degerlerle degistirin:

- `CADDY_EMAIL`
- `POSTGRES_PASSWORD`
- `CLICKHOUSE_PASSWORD`
- `JWT_SECRET_KEY`
- `BOOTSTRAP_OWNER_EMAIL`
- `BOOTSTRAP_OWNER_PASSWORD`

Konfigurasyonu dogrulayin:

```bash
docker compose --env-file .env -f docker-compose.prod.yml config
```

Servisleri build edip baslatin:

```bash
docker compose --env-file .env -f docker-compose.prod.yml up -d --build
```

Log takibi:

```bash
docker compose --env-file .env -f docker-compose.prod.yml logs -f caddy frontend backend
```

Ilk owner hesabi ile panele girildikten sonra `.env` icinde:

```bash
BOOTSTRAP_OWNER_ENABLED=false
```

Sonra backend'i yenileyin:

```bash
docker compose --env-file .env -f docker-compose.prod.yml up -d backend
```

## Host Yonlendirmeleri

- `https://optimaxx.com.tr` ve `https://www.optimaxx.com.tr` public siteye gider.
- `https://panel.optimaxx.com.tr` admin ve satis paneline gider.
- `https://api.optimaxx.com.tr` backend API'ye gider.

Public sitede login/admin/panel/randevu linkleri tutulmamalidir. Ana sayfa yayinlanmis CMS bloklarini `enabled` ve `order` alanlarina gore render eder.

## Backup

```bash
cd /opt/optimaxx/spring-boot-admin-panel-project/deploy
./scripts/backup.sh
```

Script su dosyalari `deploy/backups/` altina yazar:

- PostgreSQL `pg_dump`
- ClickHouse `audit_events` JSONEachRow export
- `site_uploads` volume tar.gz arsivi
