# Loanmust

**Loan Agent (DSA) Management Platform**

A Spring Boot 3.5 / Java 21 / Thymeleaf application for the back office of an Indian loan
agent: capture enquiries from every channel (direct clients, builder tie-ups, LIC tie-up,
referral partners), compare every bank's rate card and recommend the cheapest loan the
customer can actually get, process the loan file with the bank through sanction, MODD and
disbursement, pay referral partners when loans disburse, and collect the agency's own payout
from lenders.

Built to the same house standards as Insumust: package-by-feature, `BaseEntity` soft deletes,
tenant-scoped repositories, constructor injection, thin controllers, opaque URL ids
(`IdCodec`), a role × module permission matrix, field-level audit, Thymeleaf + layout dialect
with the shared design tokens, six UI languages, MariaDB + Docker.

See **[BUSINESS_USE_CASES.md](BUSINESS_USE_CASES.md)** for the full list of business use cases
the application covers, screen by screen, and **[docs/USER_GUIDE.md](docs/USER_GUIDE.md)** for the
staff user guide (how to use each screen).

---

## What it is not

- It does not take credit decisions. The advisor applies each lender's **published** policy
  (rate band, FOIR, LTV, minimum score, age) to give an indicative comparison; the lender's
  own credit team decides.
- It does not pull CIBIL reports or call bank APIs. Scores and rates are entered by staff.
- It does not send WhatsApp/SMS on its own. Every message is a `wa.me` click-to-chat link a
  staff member taps, so nothing goes to a customer by accident and no gateway is needed.
- The seeded lender rate cards are **illustrative samples** so the advisor works on day one.
  Confirm every scheme with the lender (Banks & Rates → *Rate confirmed today*) before advising.

---

## Running it

### With Docker (recommended)

```bash
DEMO_SEED_TRANSACTIONS=true docker compose up --build
```

Open <http://localhost:8080>. This starts MariaDB 11 (database `loanmust_db`, schema from
`db/migrations`) and the application. On first boot the seeder creates the agency, nine staff
accounts across all seven roles, eight lenders with 22 sample schemes, and default role
permissions. `DEMO_SEED_TRANSACTIONS=true` also loads a demo loan book (builders, partners,
leads, files at every stage, payouts, MODD jobs, attendance) - leave it off for real use.

### Locally against your own MariaDB / MySQL

Requires JDK 21 and Maven 3.9+.

```bash
export DB_URL="jdbc:mariadb://localhost:3306/loanmust_db?createDatabaseIfNotExist=true"
export DB_USERNAME=root DB_PASSWORD=secret
mvn spring-boot:run
```

`ddl-auto=update` creates the tables in development. The demo loan book is seeded by default
locally (`app.demo.seed-transactions=true`); set `DEMO_SEED_TRANSACTIONS=false` to start empty.

### Tests

```bash
mvn test
```

Runs the loan-maths and advisor unit tests and `ApplicationSmokeTest`, which boots the whole
application on in-memory H2 with the demo book, **renders every screen** as the owner,
checks role restrictions, exercises the public referral and proposal pages, and walks a loan
from enquiry → customer → file → login → sanction → MODD → disbursement → referral payout paid.

---

## Demo sign-ins

**Change these before any real deployment.**

| Role | Email | Password | What they see |
|---|---|---|---|
| Owner | `owner@shreefinserv.in` | `owner123` | Everything, incl. settings, users, audit, money |
| Admin | `admin@shreefinserv.in` | `admin123` | Everything; administration |
| Manager | `manager@shreefinserv.in` | `manager123` | All leads and files; approves referral payouts |
| Loan Executive | `executive@shreefinserv.in` | `executive123` | Only the leads, customers and files assigned to them |
| Processing | `processing@shreefinserv.in` | `processing123` | Documents, bank login, sanction, MODD |
| Data Entry | `dataentry@shreefinserv.in` | `dataentry123` | Leads, customers, documents - no money, no reports |
| Finance | `finance@shreefinserv.in` | `finance123` | Bank payouts, referral payouts, fee receipts |

A second executive (`executive2@…`) and data-entry operator (`dataentry2@…`) are also seeded.
Every role's screen access can be changed under **Administration → Roles & Permissions**.

---

## Modules

| Module | Route | Notes |
|---|---|---|
| Dashboard | `/dashboard` | Disbursal & payout trend, pipeline funnel, follow-ups, expiring sanctions, MODD appointments |
| Leads | `/leads` | Every channel tagged (builder/project, LIC, referral partner); explainable score; follow-up timeline |
| Customers | `/customers` | Credit profile (income, EMIs, CIBIL, employment) used by the advisor |
| Rate Compare | `/advisor` | Every scheme evaluated for this customer, ranked by total cost; saved proposals with a public link |
| Banks & Rates | `/lenders` | Lender panel, schemes, rate card with staleness flags, Excel export |
| Loan Files | `/applications` | Document checklist, bank login, sanction, disbursement tranches, timeline |
| Pipeline | `/applications/board` | Kanban by stage with count and value |
| MODD | `/modd` | Sub-registrar appointment, stamp duty, registration, deposit of originals |
| Builders | `/builders` | Tie-ups, projects, lender approvals (APF), builder-wise business |
| Referral Partners | `/referrals` | Personal `/refer/{code}` links, payout rule, payment details |
| Referral Payouts | `/referral-payouts` | Raised on disbursal → approved → paid with UTR (TDS deducted) |
| Bank Payouts | `/commission` | DSA payout receivable per tranche; invoice, receipt, ₹1 reconciliation |
| Finance & Fees | `/finance` | Money summary, 6-month trend, customer fee receipts (printable) |
| Reports | `/reports` | 11 reports, all export to Excel |
| Employees / Attendance | `/employees`, `/attendance` | Staff records, daily sheet, monthly register (menu hidden for now; URLs still work) |
| Administration | `/admin/users`, `/admin/roles`, `/audit`, `/settings` | Users, permission matrix, audit log, agency settings |
| Public | `/`, `/refer/{code}`, `/p/{token}` | Sri Sai Associates website with enquiry form (→ Website lead); referral enquiry form; customer-facing loan proposal |

---

## Design decisions worth knowing

**The advice is customer-first by construction.** `LoanAdvisor` ranks eligible offers by the
total cost of the loan to the customer (interest + processing fee + GST). The agency's payout
is shown to money roles only, is never an input to ranking (a unit test proves that paying the
agency more on a costlier lender does not move the recommendation), and is stripped from the
public proposal page.

**Every verdict is explained.** Each offer carries the rate band it used, why tenure was capped,
the FOIR and LTV it computed, and exactly why it is not eligible. The recommendation lists its
reasons and the saving against the next-best and costliest options.

**Regulatory LTV is applied on top of lender cards.** Housing loans are capped at 90 % up to
₹30 lakh, 80 % up to ₹75 lakh and 75 % above, whatever a lender's card says.

**Proposals are frozen.** A saved comparison stores its full outcome as JSON, so it reads the
same after rates change and the agency can always show what it advised and why.

**Disbursement is the money event.** Recording a tranche moves the file to Disbursed, raises
the lender payout receivable at the payout % on the file, and raises the referral partner's
payout by the partner's rule (snapshotted, TDS deducted). Both are idempotent per tranche, and
a disbursement beyond the sanctioned amount is refused.

**Archive, never delete.** Every entity extends `BaseEntity` with a `deleted` flag; code
sequences count archived rows too, so a code is never reused.

**Ownership is enforced on the server.** A Loan Executive sees only their own leads, customers
and files - on lists, detail pages and every POST - not just in the menu.

**Privacy.** Only the last four digits of Aadhaar are stored; account numbers are masked on
screen; documents are served only after a tenant check.

**No foreign keys between tenant tables** (house standard). Integrity is enforced in the
service layer, which always filters by `tenant_id`.

---

## Configuration

Everything is optional for a local run; in production supply the values via environment
variables (see [DEPLOYMENT.md](DEPLOYMENT.md)). Agency-level business settings - code prefixes,
default referral %, referral TDS %, MODD stamp-duty % and cap, MODD service charge, GST on
processing fees, proposal validity - are edited in the app under **Settings**.

> **MODD stamp duty is state-specific and changes.** The seeded default (0.1 % capped at
> ₹50,000) is only a placeholder - set your state's current rate in Settings.

---

## Project layout

```
src/main/java/com/loanmust/loan/
├── access/        permission matrix, interceptor, roles screen
├── admin/         staff accounts, own-password change
├── advisor/       LoanMath, LoanAdvisor engine, proposals, public proposal page
├── application/   loan files, document checklist, events, disbursements
├── audit/         AuditLog, AuditService (+ field-level Diff), audit screen & export
├── builder/       builder tie-ups and projects
├── commission/    bank payout receivable, nightly overdue flag
├── common/        BaseEntity, Money, IdCodec, CodeSequenceService, Labels (₹ lakh formatting), WhatsApp
├── config/        security, web/i18n, global model, seeders
├── customer/      borrowers and credit profile
├── dashboard/     KPIs and attention lists
├── exception/     GlobalExceptionHandler
├── finance/       fee receipts, money summary
├── hr/            employees, attendance
├── lead/          leads, activities, explainable scoring
├── lender/        lenders, loan schemes (rate cards)
├── modd/          MODD jobs
├── publicsite/    sign-in and root redirect
├── referral/      partners, payouts, public referral form
├── reports/       ReportService (11 reports), ExcelExporter
├── security/      user details, SecurityUtils
├── settings/      agency settings, accent palette
├── storage/       document storage (local disk, R2-compatible keys)
├── tenant/        Agency
└── user/          User, Role
```

---

## Verification status

The authoring environment had JDK 21 but **no access to Maven Central**, so the project could
not be built with real dependencies or booted. What was done instead:

| Check | Result |
|---|---|
| `javac` of all 142 main source files against API stubs mirroring the Spring/JPA/POI signatures used | compiles clean |
| Unit tests (loan maths, advisor, Indian number format, document checklist) run with a JUnit shim | 20 / 20 pass |
| EMI against bank calculators (₹50 L @ 8.5 % × 20 y = ₹43,391) | exact |
| Template tag balance, CSRF on POST forms, checkbox markers, no `T()` / nested `${}` (55 templates) | clean |
| Every `${attr.prop}` in templates resolves to a controller model attribute and a real getter | clean |
| Every `@{…}` link / form action resolves to a controller mapping | clean |
| Every form field name binds to a real entity property / required `@RequestParam` | clean |
| `V1__baseline_schema.sql` generated from the entity classes | 23 tables |

**First step on a machine with network access:** `mvn clean verify`. The smoke test renders
every screen, so any remaining runtime template issue will show up there with its exact
location.
