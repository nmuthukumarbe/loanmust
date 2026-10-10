# Deploying Loanmust

## First build

```bash
mvn clean verify
```

This compiles, runs the unit tests, and runs `ApplicationSmokeTest`, which boots the app on H2
and renders every screen. Fix anything it reports before deploying.

---

## Environment variables (production)

There is a single `application.properties` with dev-friendly defaults. The Docker image
already sets `JPA_DDL_AUTO=validate`, `JPA_OPEN_IN_VIEW=false`, `THYMELEAF_CACHE=true` and
`DEMO_SEED_TRANSACTIONS=false`.

| Variable | Required | Purpose |
|---|---|---|
| `DB_URL` | yes | e.g. `jdbc:mariadb://db:3306/loanmust_db?useSSL=false&serverTimezone=Asia/Kolkata` |
| `DB_USERNAME` | yes | database user |
| `DB_PASSWORD` | yes | database password |
| `APP_PUBLIC_BASE_URL` | yes | public https URL - used in referral links and shared proposals |
| `PORT` | no | defaults to 8080 |
| `STORAGE_LOCAL_DIR` | no | where uploaded documents are kept (mount a volume) |
| `DEMO_SEED_TRANSACTIONS` | no | `true` loads the demo loan book on an empty database |
| `PAYOUT_OVERDUE_CRON` | no | when lender payouts are flagged overdue (default 06:30 daily) |
| `MAX_FILE_SIZE` / `MAX_REQUEST_SIZE` | no | upload limits (15MB / 20MB) |
| `DB_POOL_MAX` / `DB_POOL_MIN` | no | Hikari pool sizing |

No credential is committed in the repository - unlike the reference project's properties
file, which shipped live database, storage and mail secrets. If you reuse anything from that
project, rotate those secrets.

---

## Database

Production runs with `ddl-auto=validate`; apply the schema first:

```bash
mysql -u root -p loanmust_db < db/migrations/V1__baseline_schema.sql
```

`docker compose` does this automatically on first start (the folder is mounted into the
MariaDB init directory). The file is generated from the entity classes and named to Flyway's
convention - add Flyway and point `spring.flyway.locations` at `filesystem:db/migrations` if
you prefer managed migrations.

---

## Before go-live

1. **Change every seeded password** (or create your own accounts and archive the demo ones).
2. **Replace the sample rate cards** under Banks & Rates with each lender's current card and
   your real DSA payout %, then click *Rate confirmed today* on each scheme.
3. **Set your state's MODD stamp duty** % and cap, your referral % and TDS % in Settings.
4. **Set `APP_PUBLIC_BASE_URL`** to the real https address, or referral links and shared
   proposals will point at localhost.
5. **Serve over HTTPS** behind a proxy; `server.forward-headers-strategy=framework` is already on.
6. **Back up two things**: the MariaDB database and the documents directory (`STORAGE_LOCAL_DIR`
   / the `loanmust_documents` volume) - it holds customers' KYC and property papers.
7. **Single scheduler node.** `PayoutOverdueScheduler` runs on every instance; with more than one
   node, run it on one or add ShedLock.

---

## Health

- `GET /actuator/health` - liveness/readiness (no details exposed).
