package com.loanmust.loan.reports;

import com.loanmust.loan.application.ApplicationStage;
import com.loanmust.loan.application.Disbursement;
import com.loanmust.loan.application.DisbursementRepository;
import com.loanmust.loan.application.LoanApplication;
import com.loanmust.loan.application.LoanApplicationRepository;
import com.loanmust.loan.audit.AuditLogRepository;
import com.loanmust.loan.commission.BankPayout;
import com.loanmust.loan.commission.BankPayoutRepository;
import com.loanmust.loan.common.PaymentStatus;
import com.loanmust.loan.finance.FeeReceipt;
import com.loanmust.loan.finance.FeeReceiptRepository;
import com.loanmust.loan.lead.Lead;
import com.loanmust.loan.lead.LeadRepository;
import com.loanmust.loan.lead.LeadSource;
import com.loanmust.loan.modd.ModdRecord;
import com.loanmust.loan.modd.ModdRepository;
import com.loanmust.loan.referral.ReferralPayout;
import com.loanmust.loan.referral.ReferralPayoutRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Every report the agency runs, as a {@link ReportTable}. The same table renders on screen
 * and exports to Excel, so the two can never disagree.
 */
@Service
public class ReportService {

    /** The report catalogue shown on /reports. */
    public enum Report {
        LEADS("leads", "Lead register", "Every enquiry in the period with channel, status and owner.", true, false),
        LEAD_SOURCES("lead-sources", "Conversion by channel", "Leads and conversions per source - builder, LIC, referral, direct.", true, false),
        PIPELINE("pipeline", "Loan file pipeline", "All open loan files with stage, lender and days in stage.", false, false),
        DISBURSEMENTS("disbursements", "Disbursement register", "Every tranche disbursed in the period.", true, false),
        LENDER_BUSINESS("lender-business", "Lender-wise business", "Files, sanctions, disbursal and payout by lender.", true, true),
        BUILDER_BUSINESS("builder-business", "Builder-wise business", "Leads, files and disbursal from each builder tie-up.", true, false),
        BANK_PAYOUTS("bank-payouts", "Bank payout receivable", "Payout due from lenders and what has come in.", false, true),
        REFERRAL_PAYOUTS("referral-payouts", "Referral payouts", "Every referral payout with TDS, status and UTR.", false, true),
        FEE_RECEIPTS("fee-receipts", "Fee receipts", "Customer service fees collected in the period.", true, true),
        MODD("modd", "MODD register", "Every MODD job with registration and deposit details.", false, false),
        STAFF_PRODUCTIVITY("staff-productivity", "Staff productivity", "Records created by each staff member - data entry output.", true, false);

        private final String key;
        private final String title;
        private final String description;
        private final boolean dated;
        private final boolean money;

        Report(String key, String title, String description, boolean dated, boolean money) {
            this.key = key;
            this.title = title;
            this.description = description;
            this.dated = dated;
            this.money = money;
        }

        public String getKey() { return key; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public boolean isDated() { return dated; }
        public boolean isMoney() { return money; }

        public static Report fromKey(String key) {
            for (Report r : values()) if (r.key.equals(key)) return r;
            return null;
        }
    }

    private final LeadRepository leadRepository;
    private final LoanApplicationRepository applicationRepository;
    private final DisbursementRepository disbursementRepository;
    private final BankPayoutRepository bankPayoutRepository;
    private final ReferralPayoutRepository referralPayoutRepository;
    private final FeeReceiptRepository feeReceiptRepository;
    private final ModdRepository moddRepository;
    private final AuditLogRepository auditLogRepository;

    public ReportService(LeadRepository leadRepository, LoanApplicationRepository applicationRepository,
                         DisbursementRepository disbursementRepository, BankPayoutRepository bankPayoutRepository,
                         ReferralPayoutRepository referralPayoutRepository, FeeReceiptRepository feeReceiptRepository,
                         ModdRepository moddRepository, AuditLogRepository auditLogRepository) {
        this.leadRepository = leadRepository;
        this.applicationRepository = applicationRepository;
        this.disbursementRepository = disbursementRepository;
        this.bankPayoutRepository = bankPayoutRepository;
        this.referralPayoutRepository = referralPayoutRepository;
        this.feeReceiptRepository = feeReceiptRepository;
        this.moddRepository = moddRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public ReportTable build(Report report, Long tenantId, LocalDate from, LocalDate to) {
        return switch (report) {
            case LEADS -> leads(tenantId, from, to);
            case LEAD_SOURCES -> leadSources(tenantId, from, to);
            case PIPELINE -> pipeline(tenantId);
            case DISBURSEMENTS -> disbursements(tenantId, from, to);
            case LENDER_BUSINESS -> lenderBusiness(tenantId, from, to);
            case BUILDER_BUSINESS -> builderBusiness(tenantId, from, to);
            case BANK_PAYOUTS -> bankPayouts(tenantId);
            case REFERRAL_PAYOUTS -> referralPayouts(tenantId);
            case FEE_RECEIPTS -> feeReceipts(tenantId, from, to);
            case MODD -> modd(tenantId);
            case STAFF_PRODUCTIVITY -> productivity(tenantId, from, to);
        };
    }

    private ReportTable leads(Long tenantId, LocalDate from, LocalDate to) {
        List<List<Object>> rows = new ArrayList<>();
        for (Lead l : leadRepository.findByTenantIdAndDeletedFalseOrderByIdDesc(tenantId)) {
            LocalDate created = l.getCreatedAt() == null ? null : l.getCreatedAt().toLocalDate();
            if (!within(created, from, to)) continue;
            rows.add(row(l.getCode(), created, l.getName(), l.getMobile(), l.getSourceLabel(), l.getChannelLine(),
                    l.getLoanTypeLabel(), l.getLoanAmountRequired(), l.getCibilScore(), l.getStatusLabel(),
                    l.getPriorityLabel(), l.getScore(), l.getAssignedUserName(), l.getNextFollowUpDate()));
        }
        return new ReportTable("Lead register", List.of("Code", "Created", "Name", "Mobile", "Source", "Channel",
                "Loan type", "Amount", "CIBIL", "Status", "Priority", "Score", "Assigned to", "Next follow-up"), rows);
    }

    private ReportTable leadSources(Long tenantId, LocalDate from, LocalDate to) {
        List<List<Object>> rows = new ArrayList<>();
        for (Object[] r : leadRepository.conversionBySource(tenantId,
                from == null ? null : from.atStartOfDay(), to == null ? null : to.atTime(23, 59, 59))) {
            long total = ((Number) r[1]).longValue();
            long converted = r[2] == null ? 0 : ((Number) r[2]).longValue();
            rows.add(row(((LeadSource) r[0]).getLabel(), total, converted,
                    total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(converted * 100.0 / total).setScale(1, RoundingMode.HALF_UP)));
        }
        return new ReportTable("Conversion by channel", List.of("Source", "Leads", "Converted to file", "Conversion %"), rows);
    }

    private ReportTable pipeline(Long tenantId) {
        List<List<Object>> rows = new ArrayList<>();
        for (LoanApplication a : applicationRepository.findByTenantIdAndDeletedFalseOrderByIdDesc(tenantId)) {
            if (!a.isOpen()) continue;
            rows.add(row(a.getCode(), a.getCustomerName(), a.getLoanTypeLabel(), a.getLenderName(), a.getStageLabel(),
                    a.getDaysInStage(), a.getRequestedAmount(), a.getSanctionedAmount(), a.getBankReference(),
                    a.getAssignedUserName(), a.getProcessorName(), a.getSanctionValidTill()));
        }
        return new ReportTable("Loan file pipeline", List.of("File", "Customer", "Loan type", "Lender", "Stage",
                "Days in stage", "Requested", "Sanctioned", "Bank ref", "Executive", "Processor", "Sanction valid till"), rows);
    }

    private ReportTable disbursements(Long tenantId, LocalDate from, LocalDate to) {
        Map<Long, LoanApplication> apps = appIndex(tenantId);
        List<List<Object>> rows = new ArrayList<>();
        for (Disbursement d : disbursementRepository.findByTenantIdAndDisbursedOnBetweenAndDeletedFalseOrderByDisbursedOnAsc(
                tenantId, from == null ? LocalDate.of(2000, 1, 1) : from, to == null ? LocalDate.now() : to)) {
            LoanApplication a = apps.get(d.getApplicationId());
            rows.add(row(d.getDisbursedOn(), a == null ? null : a.getCode(), a == null ? null : a.getCustomerName(),
                    a == null ? null : a.getLenderName(), a == null ? null : a.getLoanTypeLabel(), d.getTrancheNo(),
                    d.getAmount(), d.getPaidTo(), d.getReference(), a == null ? null : a.getBuilderName(),
                    a == null ? null : a.getReferralPartnerName()));
        }
        return new ReportTable("Disbursement register", List.of("Date", "File", "Customer", "Lender", "Loan type",
                "Tranche", "Amount", "Paid to", "Reference", "Builder", "Referral partner"), rows);
    }

    private ReportTable lenderBusiness(Long tenantId, LocalDate from, LocalDate to) {
        Map<String, BigDecimal[]> agg = new TreeMap<>();
        for (LoanApplication a : applicationRepository.findByTenantIdAndDeletedFalseOrderByIdDesc(tenantId)) {
            LocalDate opened = a.getCreatedAt() == null ? null : a.getCreatedAt().toLocalDate();
            if (!within(opened, from, to) || a.getLenderName() == null) continue;
            BigDecimal[] v = agg.computeIfAbsent(a.getLenderName(), k -> zeros(6));
            v[0] = v[0].add(BigDecimal.ONE);
            if (a.getSanctionedAmount() != null) { v[1] = v[1].add(BigDecimal.ONE); v[2] = v[2].add(a.getSanctionedAmount()); }
            if (a.getTotalDisbursed() != null) v[3] = v[3].add(a.getTotalDisbursed());
        }
        for (BankPayout p : bankPayoutRepository.findByTenantIdAndDeletedFalseOrderByIdDesc(tenantId)) {
            if (p.getLenderName() == null || !within(p.getDisbursedOn(), from, to)) continue;
            BigDecimal[] v = agg.computeIfAbsent(p.getLenderName(), k -> zeros(6));
            v[4] = v[4].add(nz(p.getExpectedAmount()));
            v[5] = v[5].add(nz(p.getReceivedAmount()));
        }
        List<List<Object>> rows = new ArrayList<>();
        agg.forEach((lender, v) -> rows.add(row(lender, v[0].longValue(), v[1].longValue(), v[2], v[3], v[4], v[5], v[4].subtract(v[5]))));
        return new ReportTable("Lender-wise business", List.of("Lender", "Files", "Sanctioned files", "Sanctioned amount",
                "Disbursed", "Payout earned", "Payout received", "Payout due"), rows);
    }

    private ReportTable builderBusiness(Long tenantId, LocalDate from, LocalDate to) {
        Map<String, BigDecimal[]> agg = new TreeMap<>();
        for (Lead l : leadRepository.findByTenantIdAndDeletedFalseOrderByIdDesc(tenantId)) {
            if (l.getBuilderName() == null) continue;
            if (!within(l.getCreatedAt() == null ? null : l.getCreatedAt().toLocalDate(), from, to)) continue;
            BigDecimal[] v = agg.computeIfAbsent(l.getBuilderName(), k -> zeros(4));
            v[0] = v[0].add(BigDecimal.ONE);
        }
        for (LoanApplication a : applicationRepository.findByTenantIdAndDeletedFalseOrderByIdDesc(tenantId)) {
            if (a.getBuilderName() == null) continue;
            if (!within(a.getCreatedAt() == null ? null : a.getCreatedAt().toLocalDate(), from, to)) continue;
            BigDecimal[] v = agg.computeIfAbsent(a.getBuilderName(), k -> zeros(4));
            v[1] = v[1].add(BigDecimal.ONE);
            if (a.getStage() == ApplicationStage.DISBURSED) v[2] = v[2].add(BigDecimal.ONE);
            v[3] = v[3].add(nz(a.getTotalDisbursed()));
        }
        List<List<Object>> rows = new ArrayList<>();
        agg.forEach((b, v) -> rows.add(row(b, v[0].longValue(), v[1].longValue(), v[2].longValue(), v[3])));
        return new ReportTable("Builder-wise business", List.of("Builder", "Leads", "Loan files", "Disbursed files", "Disbursed amount"), rows);
    }

    private ReportTable bankPayouts(Long tenantId) {
        List<List<Object>> rows = new ArrayList<>();
        for (BankPayout p : bankPayoutRepository.findByTenantIdAndDeletedFalseOrderByIdDesc(tenantId)) {
            if (p.getStatus() == PaymentStatus.CANCELLED) continue;
            rows.add(row(p.getLenderName(), p.getApplicationCode(), p.getCustomerName(), p.getDisbursedOn(),
                    p.getDisbursedAmount(), p.getPayoutPercent(), p.getExpectedAmount(), p.getInvoiceNumber(),
                    p.getReceivedAmount(), p.getReceivedOn(), p.getShortfall(), p.getStatusLabel()));
        }
        return new ReportTable("Bank payout receivable", List.of("Lender", "File", "Customer", "Disbursed on",
                "Disbursed", "Payout %", "Expected", "Invoice", "Received", "Received on", "Due", "Status"), rows);
    }

    private ReportTable referralPayouts(Long tenantId) {
        List<List<Object>> rows = new ArrayList<>();
        for (ReferralPayout p : referralPayoutRepository.findByTenantIdAndDeletedFalseOrderByIdDesc(tenantId)) {
            rows.add(row(p.getCode(), p.getPartnerName(), p.getPartnerMobile(), p.getApplicationCode(), p.getCustomerName(),
                    p.getLenderName(), p.getDisbursedAmount(), p.getGrossAmount(), p.getTdsAmount(), p.getNetAmount(),
                    p.getStatusLabel(), p.getApprovedByName(), p.getPaidOn(),
                    p.getPaymentMode() == null ? null : p.getPaymentMode().getLabel(), p.getPaymentReference()));
        }
        return new ReportTable("Referral payouts", List.of("Payout", "Partner", "Mobile", "File", "Customer", "Lender",
                "Disbursed", "Gross", "TDS", "Net", "Status", "Approved by", "Paid on", "Mode", "UTR / ref"), rows);
    }

    private ReportTable feeReceipts(Long tenantId, LocalDate from, LocalDate to) {
        List<List<Object>> rows = new ArrayList<>();
        for (FeeReceipt f : feeReceiptRepository.findByTenantIdAndReceivedOnBetweenAndDeletedFalseOrderByReceivedOnAsc(
                tenantId, from == null ? LocalDate.of(2000, 1, 1) : from, to == null ? LocalDate.now() : to)) {
            rows.add(row(f.getReceiptNo(), f.getReceivedOn(), f.getCustomerName(), f.getApplicationCode(), f.getFeeTypeLabel(),
                    f.getAmount(), f.getPaymentMode() == null ? null : f.getPaymentMode().getLabel(), f.getReference(),
                    f.getReceivedByName()));
        }
        return new ReportTable("Fee receipts", List.of("Receipt", "Date", "Customer", "File", "Fee", "Amount", "Mode",
                "Reference", "Received by"), rows);
    }

    private ReportTable modd(Long tenantId) {
        List<List<Object>> rows = new ArrayList<>();
        for (ModdRecord m : moddRepository.findByTenantIdAndDeletedFalseOrderByIdDesc(tenantId)) {
            rows.add(row(m.getCode(), m.getApplicationCode(), m.getCustomerName(), m.getLenderName(), m.getSubRegistrarOffice(),
                    m.getStatusLabel(), m.getAppointmentAt(), m.getRegistrationNumber(), m.getRegisteredOn(),
                    m.getSecuredAmount(), m.getStampDuty(), m.getRegistrationFee(), m.getServiceCharge(),
                    m.isChargesCollected() ? "Yes" : "No", m.getDepositedOn(), m.getAcknowledgementNumber()));
        }
        return new ReportTable("MODD register", List.of("MODD", "File", "Customer", "Lender", "Sub-registrar", "Status",
                "Appointment", "Registration no", "Registered on", "Secured amount", "Stamp duty", "Registration fee",
                "Service charge", "Collected", "Deposited on", "Acknowledgement"), rows);
    }

    private ReportTable productivity(Long tenantId, LocalDate from, LocalDate to) {
        List<String> types = List.of("Lead", "Customer", "LoanApplication", "Proposal", "FeeReceipt");
        Map<String, Map<String, Long>> byUser = new LinkedHashMap<>();
        for (Object[] r : auditLogRepository.createdByUser(tenantId,
                (from == null ? LocalDate.now().withDayOfMonth(1) : from).atStartOfDay(),
                (to == null ? LocalDate.now() : to).atTime(23, 59, 59))) {
            byUser.computeIfAbsent(String.valueOf(r[0]), k -> new HashMap<>())
                    .merge(String.valueOf(r[1]), ((Number) r[2]).longValue(), Long::sum);
        }
        List<List<Object>> rows = new ArrayList<>();
        byUser.forEach((user, counts) -> {
            List<Object> row = new ArrayList<>();
            row.add(user);
            long total = 0;
            for (String t : types) {
                long c = counts.getOrDefault(t, 0L);
                row.add(c);
                total += c;
            }
            long other = counts.values().stream().mapToLong(Long::longValue).sum() - total;
            row.add(other);
            row.add(total + other);
            rows.add(row);
        });
        return new ReportTable("Staff productivity", List.of("Staff member", "Leads", "Customers", "Loan files",
                "Proposals", "Receipts", "Other records", "Total"), rows);
    }

    // ------------------------------------------------------------------ helpers

    private Map<Long, LoanApplication> appIndex(Long tenantId) {
        Map<Long, LoanApplication> map = new HashMap<>();
        for (LoanApplication a : applicationRepository.findByTenantIdAndDeletedFalseOrderByIdDesc(tenantId)) map.put(a.getId(), a);
        return map;
    }

    private static boolean within(LocalDate d, LocalDate from, LocalDate to) {
        if (d == null) return from == null && to == null;
        return (from == null || !d.isBefore(from)) && (to == null || !d.isAfter(to));
    }

    private static List<Object> row(Object... values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    private static BigDecimal[] zeros(int n) {
        BigDecimal[] a = new BigDecimal[n];
        Arrays.fill(a, BigDecimal.ZERO);
        return a;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
