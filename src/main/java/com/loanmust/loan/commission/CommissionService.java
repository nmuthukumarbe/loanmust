package com.loanmust.loan.commission;

import com.loanmust.loan.application.Disbursement;
import com.loanmust.loan.application.LoanApplication;
import com.loanmust.loan.audit.AuditService;
import com.loanmust.loan.common.Labels;
import com.loanmust.loan.common.PaymentStatus;
import com.loanmust.loan.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/** DSA payout receivable from lenders: raise on disbursal, invoice, record receipt, reconcile. */
@Service
public class CommissionService {

    public static final List<PaymentStatus> OPEN = List.of(PaymentStatus.UNPAID, PaymentStatus.PARTIAL, PaymentStatus.OVERDUE);

    private final BankPayoutRepository repository;
    private final AuditService auditService;

    public CommissionService(BankPayoutRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    public Page<BankPayout> search(Long tenantId, String q, PaymentStatus status, Long lenderId, int page, int size) {
        return repository.search(tenantId, q == null ? "" : q.trim(), status, lenderId, PageRequest.of(Math.max(0, page), size));
    }

    public BankPayout get(Long tenantId, Long id) {
        return id == null ? null : repository.findByIdAndTenantIdAndDeletedFalse(id, tenantId).orElse(null);
    }

    public List<BankPayout> forApplication(Long tenantId, Long applicationId) {
        return repository.findByTenantIdAndApplicationIdAndDeletedFalseOrderByIdDesc(tenantId, applicationId);
    }

    public BigDecimal outstanding(Long tenantId) {
        return repository.sumOutstanding(tenantId, OPEN);
    }

    /** Idempotent per disbursement. Skipped when no payout percent is agreed for the file. */
    @Transactional
    public BankPayout raiseForDisbursement(Long tenantId, LoanApplication a, Disbursement d) {
        if (a.getPayoutPercent() == null || a.getPayoutPercent().signum() <= 0) return null;
        if (repository.findFirstByTenantIdAndDisbursementIdAndDeletedFalse(tenantId, d.getId()).isPresent()) return null;
        BankPayout p = new BankPayout();
        p.setTenantId(tenantId);
        p.setLenderId(a.getLenderId());
        p.setLenderName(a.getLenderName());
        p.setApplicationId(a.getId());
        p.setApplicationCode(a.getCode());
        p.setDisbursementId(d.getId());
        p.setCustomerName(a.getCustomerName());
        p.setDisbursedOn(d.getDisbursedOn());
        p.setDisbursedAmount(d.getAmount());
        p.setPayoutPercent(a.getPayoutPercent());
        p.setExpectedAmount(d.getAmount().multiply(a.getPayoutPercent()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
        p.setStatus(PaymentStatus.UNPAID);
        p.setCreatedBy(SecurityUtils.currentUserLogin());
        BankPayout saved = repository.save(p);
        auditService.log("CREATE", "BankPayout", saved.getId(), a.getCode() + " · " + a.getLenderName(),
                "Payout receivable ₹" + Labels.inr(saved.getExpectedAmount()) + " at " + a.getPayoutPercent().stripTrailingZeros().toPlainString() + "%");
        return saved;
    }

    @Transactional
    public void recordInvoice(Long tenantId, Long id, String invoiceNumber, LocalDate invoiceDate) {
        BankPayout p = get(tenantId, id);
        if (p == null) return;
        p.setInvoiceNumber(invoiceNumber);
        p.setInvoiceDate(invoiceDate == null ? LocalDate.now() : invoiceDate);
        p.setModifiedBy(SecurityUtils.currentUserLogin());
        repository.save(p);
        auditService.write("UPDATE", "BankPayout", id, p.getApplicationCode(), "Invoice", null, invoiceNumber);
    }

    /** Records money received; a cumulative figure, so a second part-payment adds to the first. */
    @Transactional
    public void recordReceipt(Long tenantId, Long id, BigDecimal amount, LocalDate receivedOn, String reference) {
        BankPayout p = get(tenantId, id);
        if (p == null || amount == null || amount.signum() <= 0) return;
        BigDecimal total = (p.getReceivedAmount() == null ? BigDecimal.ZERO : p.getReceivedAmount()).add(amount);
        p.setReceivedAmount(total);
        p.setReceivedOn(receivedOn == null ? LocalDate.now() : receivedOn);
        p.setReceiptReference(reference);
        BigDecimal expected = p.getExpectedAmount() == null ? BigDecimal.ZERO : p.getExpectedAmount();
        BigDecimal gap = expected.subtract(total);
        p.setStatus(gap.compareTo(BankPayout.TOLERANCE) > 0 ? PaymentStatus.PARTIAL : PaymentStatus.PAID);
        p.setModifiedBy(SecurityUtils.currentUserLogin());
        repository.save(p);
        auditService.write("PAYMENT", "BankPayout", id, p.getApplicationCode() + " · " + p.getLenderName(),
                "Received", null, "₹" + Labels.inr(amount) + (reference == null ? "" : " · " + reference));
    }

    @Transactional
    public void writeOff(Long tenantId, Long id, String remarks) {
        BankPayout p = get(tenantId, id);
        if (p == null) return;
        String before = p.getStatus().getLabel();
        p.setStatus(PaymentStatus.CANCELLED);
        p.setRemarks(remarks);
        p.setModifiedBy(SecurityUtils.currentUserLogin());
        repository.save(p);
        auditService.logStatus("BankPayout", id, p.getApplicationCode(), before, PaymentStatus.CANCELLED.getLabel(), remarks);
    }

    /** Unpaid payouts older than 60 days become overdue - run daily. */
    @Transactional
    public int flagOverdue(Long tenantId) {
        int n = 0;
        LocalDate cutoff = LocalDate.now().minusDays(60);
        for (BankPayout p : repository.findByTenantIdAndDeletedFalseOrderByIdDesc(tenantId)) {
            if (p.getStatus() == PaymentStatus.UNPAID && p.getDisbursedOn() != null && p.getDisbursedOn().isBefore(cutoff)) {
                p.setStatus(PaymentStatus.OVERDUE);
                repository.save(p);
                n++;
            }
        }
        return n;
    }
}
