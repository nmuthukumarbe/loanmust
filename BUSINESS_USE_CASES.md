# Loanmust — business use cases

The client is an Indian **loan agent / DSA (Direct Selling Agent)** who:

- gets clients through **builder tie-ups**, an **LIC tie-up** and **direct** enquiries;
- **shows the client every bank's interest rate and benefits, and suggests the best one**;
- **processes the loan** with the bank and **processes the MODD** (Memorandum of Deposit of
  Title Deeds) for secured loans;
- runs a **referral programme** and sends money to whoever referred a client once the loan
  disburses;
- has **data-entry employees** in the back office;
- earns a **payout from each bank** on every disbursed loan.

Below, each use case lists who does it, where in the app, and the business rules enforced.

---

## 1. Getting the client (lead management)

| # | Use case | Who | Where | Rules |
|---|---|---|---|---|
| 1.1 | Capture a walk-in / phone / website enquiry | Executive, Data entry | Leads → Add lead | Code `LD-0001`; duplicate mobile among open leads is flagged |
| 1.2 | Capture a **builder** lead tagged to builder & project | Executive, Data entry | Builder → *Add lead*, or source = Builder tie-up | Builder/project only kept when source is Builder |
| 1.3 | Capture an **LIC tie-up** lead (branch / agent noted) | Executive, Data entry | Lead source = LIC tie-up | Reported separately in *Conversion by channel* |
| 1.4 | Capture a **referral** lead tagged to the partner | Anyone, or the public | Partner → *Add lead*, or partner's `/refer/{code}` link | Tagged partner flows to customer → file → payout automatically |
| 1.5 | Qualify the lead with an **explainable score** | System | Lead screen | Score from CIBIL, income multiple, employment, channel, follow-ups; reasons shown |
| 1.6 | Assign / re-assign to an executive | Manager | Lead screen | Executives see only their own leads |
| 1.7 | Log a follow-up (call, WhatsApp, visit) and schedule the next | Executive | Lead screen | Status moves NEW → Contacted automatically |
| 1.8 | Work today's follow-up queue | Executive | Dashboard, Leads (*due today*) | Overdue shown in red |
| 1.9 | Close as **Lost** or **Not eligible** with a reason | Executive | Lead screen | Reason kept for reporting |
| 1.10 | Convert to a customer (credit profile carried over) | Executive | Lead → *Convert* | Mobile-matched; never creates a duplicate customer |
| 1.11 | Message the lead on WhatsApp | Executive | Lead screen | Click-to-chat link; nothing sent automatically |
| 1.12 | Export the lead register to Excel | Manager | Leads → Export | Audited |

## 2. Showing every bank and recommending the best (Rate Compare)

| # | Use case | Who | Where | Rules |
|---|---|---|---|---|
| 2.1 | Maintain the **lender panel** (banks, HFCs, NBFCs), RM contacts, DSA payout % | Owner, Processing | Banks & Rates | Payout % editable by money roles only |
| 2.2 | Maintain each **scheme's rate card**: rate band, women's concession, processing fee (min/cap), max tenure, LTV, FOIR, min CIBIL, age at maturity, employment eligibility, prepayment charges, benefits | Processing | Lender → Add scheme | Every change audited field-by-field |
| 2.3 | **Re-confirm rates** with the lender | Processing | Lender → *Rate confirmed today* | Schemes not confirmed for 30 days are flagged in the advisor |
| 2.4 | **Compare all banks** for a customer's profile | Executive | Rate Compare (from lead or customer, pre-filled) | Rate by CIBIL band; tenure capped by scheme & age; EMI, total interest, fee + GST, **total cost**; eligible amount = min(FOIR, LTV, ticket); RBI housing LTV slabs applied |
| 2.5 | See **why** a bank is not eligible | Executive | Comparison table | e.g. "CIBIL 710 below cut-off of 725", "Existing EMIs use up the 55 % FOIR" |
| 2.6 | Get the **recommendation** with reasons | Executive | Recommended card | Cheapest total cost among fully eligible; if none can fund the full amount, the one that funds the most. **Never ranked by agency payout** |
| 2.7 | See the year-wise repayment schedule | Executive / customer | Rate Compare, proposal | |
| 2.8 | **Save & share a proposal** with the customer | Executive | *Save & share* → WhatsApp | Frozen at the moment of sharing; public link `/p/{token}`; validity from Settings; payout never shown |
| 2.9 | Know whether the customer opened the proposal | Executive | Saved proposals | View count (staff previews not counted) |
| 2.10 | Open a loan file straight from an accepted proposal | Executive | Proposal → *Open loan file* | Lender, scheme, amount, tenure pre-filled |
| 2.11 | Export the rate card | Anyone with export | Banks & Rates → Export | |

## 3. Processing the loan (loan files)

| # | Use case | Who | Where | Rules |
|---|---|---|---|---|
| 3.1 | Open a loan file for a customer | Executive, Processing, Data entry | Loan Files → Open | Code `LA-0001`; payout % snapshotted from scheme/lender |
| 3.2 | Get a **document checklist** tailored to the profile | System | File → Documents | KYC; income docs by salaried / self-employed / pensioner / NRI; property papers (builder allotment vs title chain + EC + khata); BT/top-up papers; co-applicant KYC |
| 3.3 | Mark documents received / verified / re-submit / N/A; upload scans | Data entry, Processing | File → Documents | Uploads stored per tenant; downloads tenant-checked |
| 3.4 | Send the customer a status update listing pending documents | Executive | File → WhatsApp | |
| 3.5 | Record the **bank login** (lender's application number) | Processing | File → Record bank login | Moves to *Logged in*; required before that stage |
| 3.6 | Move through credit appraisal / PD and technical & legal | Processing | File → Move stage | Every move on the timeline and in audit |
| 3.7 | Record **bank queries** and notes | Processing | File → Note / bank query | |
| 3.8 | Record the **sanction** (amount, rate, tenure, validity) | Processing | File → Record sanction | EMI computed; validity defaults to 6 months |
| 3.9 | Get warned before a **sanction letter lapses** | Manager | Dashboard, file list | Within 15 days, not yet disbursed |
| 3.10 | Record a **disbursement** (one or many tranches) | Processing | File → Record disbursement | Cannot exceed sanction; part / full status; raises bank payout and referral payout |
| 3.11 | Reject or withdraw a file | Processing | Move stage | Rejection needs a reason |
| 3.12 | Track the **pipeline** by stage with value | Manager | Pipeline board | |

## 4. MODD processing

| # | Use case | Who | Where | Rules |
|---|---|---|---|---|
| 4.1 | Open the MODD job for a secured file | Processing | File → Start MODD, or automatically on *Agreement & MODD* | One per file |
| 4.2 | Compute **stamp duty** | System | MODD screen | Agency's state rate % and cap from Settings |
| 4.3 | Book the **sub-registrar appointment** and inform the customer | Processing | MODD → WhatsApp | Appointments listed on dashboard for 7 days |
| 4.4 | Record **registration number** and date | Processing | MODD | Status advances automatically from the facts entered |
| 4.5 | Record **deposit of originals** with the bank and acknowledgement | Processing | MODD | List of originals kept |
| 4.6 | Bill and collect MODD charges + service charge | Finance | MODD → Record fee receipt | Printable receipt |
| 4.7 | MODD register report | Manager | Reports | Excel |

## 5. Builder tie-ups

| # | Use case | Who | Where | Rules |
|---|---|---|---|---|
| 5.1 | Record builder, contact and tie-up arrangement | Owner | Builders | |
| 5.2 | Record projects with RERA no., price band, possession, **approved lenders (APF)** | Processing | Builder → Add project | APF lenders shown as chips |
| 5.3 | See leads and files from each builder | Owner | Builder screen | |
| 5.4 | Builder-wise business report | Owner | Reports | Leads, files, disbursed |

## 6. Referral programme

| # | Use case | Who | Where | Rules |
|---|---|---|---|---|
| 6.1 | Enrol a referral partner (customer, LIC agent, CA, sub-DSA, builder sales…) | Executive | Referral Partners | Personal code / link |
| 6.2 | Set the partner's **payout rule** (% of disbursal or flat per file) | Owner, Finance | Partner form | Non-money roles cannot change it |
| 6.3 | Record where to pay (UPI / bank / PAN for TDS) | Executive | Partner form | Account number masked on screen |
| 6.4 | Send the partner their **personal referral link** on WhatsApp | Executive | Partner screen | |
| 6.5 | Partner's contacts submit enquiries on the public link | Public | `/refer/{code}` | Becomes a NEW lead tagged to the partner; honeypot against bots |
| 6.6 | **Payout raised automatically** when a referred loan disburses | System | Referral Payouts | Rule snapshotted; flat rule pays once per file; TDS at agency % |
| 6.7 | **Approve** the payout | Owner, Manager | Payout screen | |
| 6.8 | **Pay** and record mode + UTR | Finance | Payout screen | Only approved payouts can be paid |
| 6.9 | Send the partner a thank-you with payment details | Finance | Payout screen | |
| 6.10 | Hold or cancel a payout with a reason | Owner, Manager | Payout screen | Paid payouts cannot be changed |
| 6.11 | Referral payouts report | Finance | Reports | |

## 7. Agency income (bank payouts and fees)

| # | Use case | Who | Where | Rules |
|---|---|---|---|---|
| 7.1 | Payout receivable raised per tranche | System | Bank Payouts | At the file's payout % |
| 7.2 | Record invoice to the lender | Finance | Bank Payouts | |
| 7.3 | Record receipt (part or full) | Finance | Bank Payouts | Cumulative; differences over ₹1 flagged |
| 7.4 | Flag unpaid payouts as **overdue** after 60 days | System | Nightly job | |
| 7.5 | Write off an unrecoverable payout | Finance | Bank Payouts | Reason audited |
| 7.6 | Collect customer service fees (login fee, legal, valuation, MODD) | Finance | Finance → Record fee | Printable receipt `FR-0001` |
| 7.7 | See the money picture: disbursed, earned, received, fees, referrals paid, net | Owner, Finance | Finance | 6-month trend |

## 8. Employees (data entry and staff)

| # | Use case | Who | Where | Rules |
|---|---|---|---|---|
| 8.1 | Give staff sign-ins with a role | Owner, Admin | Users | Temporary password, forced change on first sign-in |
| 8.2 | Restrict what each role may do per screen | Owner | Roles & Permissions | Seeded sensible defaults (Data Entry: no money/reports) |
| 8.3 | Keep employee records (department, salary, joining date) | Owner, Manager | Employees | Linked to sign-in |
| 8.4 | Mark daily attendance (present, half day, field work, leave, absent) | Manager | Attendance | One row per person per day |
| 8.5 | Monthly attendance register with paid days | Manager | Attendance | |
| 8.6 | **Staff productivity** — records each person created | Owner | Reports | Read from the audit trail |

## 9. Control and compliance

| # | Use case | Who | Where |
|---|---|---|---|
| 9.1 | Field-level audit of every change (who, when, old → new) | Owner, Admin | Audit Log + panel on each record |
| 9.2 | Export the audit log | Owner, Admin | Audit Log → Export |
| 9.3 | Agency profile, DSA code, GSTIN, code prefixes, accent colour | Owner | Settings |
| 9.4 | Six UI languages (English, Hindi, Tamil, Telugu, Kannada, Malayalam) | Everyone | Header language switch |
| 9.5 | Light / dark / system theme | Everyone | Header |

## 10. Reports (all export to Excel)

Lead register · Conversion by channel · Loan file pipeline · Disbursement register ·
Lender-wise business · Builder-wise business · Bank payout receivable · Referral payouts ·
Fee receipts · MODD register · Staff productivity.
