package com.loanmust.loan.finance;

import com.loanmust.loan.application.DisbursementRepository;
import com.loanmust.loan.application.LoanApplication;
import com.loanmust.loan.application.LoanApplicationRepository;
import com.loanmust.loan.audit.AuditService;
import com.loanmust.loan.commission.BankPayoutRepository;
import com.loanmust.loan.commission.CommissionService;
import com.loanmust.loan.common.CodeSequenceService;
import com.loanmust.loan.common.Labels;
import com.loanmust.loan.customer.Customer;
import com.loanmust.loan.customer.CustomerRepository;
import com.loanmust.loan.referral.PayoutStatus;
import com.loanmust.loan.referral.ReferralPayoutRepository;
import com.loanmust.loan.security.CustomUserDetails;
import com.loanmust.loan.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Fee receipts and the agency's money picture: earned, received, owed to referrers, net. */
@Service
public class FinanceService {

    private final FeeReceiptRepository receiptRepository;
    private final BankPayoutRepository bankPayoutRepository;
    private final ReferralPayoutRepository referralPayoutRepository;
    private final DisbursementRepository disbursementRepository;
    private final CustomerRepository customerRepository;
    private final LoanApplicationRepository applicationRepository;
    private final CodeSequenceService codeSequenceService;
    private final AuditService auditService;

    public FinanceService(FeeReceiptRepository receiptRepository, BankPayoutRepository bankPayoutRepository,
                          ReferralPayoutRepository referralPayoutRepository, DisbursementRepository disbursementRepository,
                          CustomerRepository customerRepository, LoanApplicationRepository applicationRepository,
                          CodeSequenceService codeSequenceService, AuditService auditService) {
        this.receiptRepository = receiptRepository;
        this.bankPayoutRepository = bankPayoutRepository;
        this.referralPayoutRepository = referralPayoutRepository;
        this.disbursementRepository = disbursementRepository;
        this.customerRepository = customerRepository;
        this.applicationRepository = applicationRepository;
        this.codeSequenceService = codeSequenceService;
        this.auditService = auditService;
    }

    /** Money summary for a period. */
    public record Summary(LocalDate from, LocalDate to, BigDecimal disbursed, long filesDisbursed,
                          BigDecimal payoutEarned, BigDecimal payoutReceived, BigDecimal feesCollected,
                          BigDecimal referralPaid, BigDecimal payoutOutstanding, BigDecimal referralOwed,
                          BigDecimal netIncome) {
    }

    public record MonthPoint(String label, BigDecimal disbursed, BigDecimal earned, BigDecimal received) {
    }

    public Summary summary(Long tenantId, LocalDate from, LocalDate to) {
        BigDecimal disbursed = disbursementRepository.sumBetween(tenantId, from, to);
        long files = disbursementRepository.filesBetween(tenantId, from, to);
        BigDecimal earned = bankPayoutRepository.sumEarnedBetween(tenantId, from, to);
        BigDecimal received = bankPayoutRepository.sumReceivedBetween(tenantId, from, to);
        BigDecimal fees = receiptRepository.sumBetween(tenantId, from, to);
        BigDecimal refPaid = referralPayoutRepository.sumPaidBetween(tenantId, from, to);
        BigDecimal outstanding = bankPayoutRepository.sumOutstanding(tenantId, CommissionService.OPEN);
        BigDecimal owed = referralPayoutRepository.sumNetByStatuses(tenantId,
                List.of(PayoutStatus.PENDING, PayoutStatus.APPROVED, PayoutStatus.ON_HOLD));
        BigDecimal net = received.add(fees).subtract(refPaid);
        return new Summary(from, to, disbursed, files, earned, received, fees, refPaid, outstanding, owed, net);
    }

    public List<MonthPoint> trend(Long tenantId, int months) {
        List<MonthPoint> points = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            LocalDate from = ym.atDay(1), to = ym.atEndOfMonth();
            points.add(new MonthPoint(ym.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH) + " " + (ym.getYear() % 100),
                    disbursementRepository.sumBetween(tenantId, from, to),
                    bankPayoutRepository.sumEarnedBetween(tenantId, from, to),
                    bankPayoutRepository.sumReceivedBetween(tenantId, from, to)));
        }
        return points;
    }

    // ------------------------------------------------------------------ fee receipts

    public Page<FeeReceipt> receipts(Long tenantId, String q, LocalDate from, LocalDate to, int page, int size) {
        return receiptRepository.search(tenantId, q == null ? "" : q.trim(), from, to, PageRequest.of(Math.max(0, page), size));
    }

    public FeeReceipt getReceipt(Long tenantId, Long id) {
        return id == null ? null : receiptRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId).orElse(null);
    }

    public List<FeeReceipt> receiptsForCustomer(Long tenantId, Long customerId) {
        return receiptRepository.findByTenantIdAndCustomerIdAndDeletedFalseOrderByReceivedOnDesc(tenantId, customerId);
    }

    public List<FeeReceipt> receiptsForApplication(Long tenantId, Long applicationId) {
        return receiptRepository.findByTenantIdAndApplicationIdAndDeletedFalseOrderByReceivedOnDesc(tenantId, applicationId);
    }

    @Transactional
    public FeeReceipt saveReceipt(Long tenantId, FeeReceipt form) {
        if (form.getAmount() == null || form.getAmount().signum() <= 0) return null;
        LoanApplication app = form.getApplicationId() == null ? null
                : applicationRepository.findByIdAndTenantIdAndDeletedFalse(form.getApplicationId(), tenantId).orElse(null);
        Long customerId = app != null ? app.getCustomerId() : form.getCustomerId();
        Customer customer = customerId == null ? null : customerRepository.findByIdAndTenantIdAndDeletedFalse(customerId, tenantId).orElse(null);
        if (customer == null) return null;

        FeeReceipt r = new FeeReceipt();
        r.setTenantId(tenantId);
        r.setReceiptNo(codeSequenceService.next(tenantId, CodeSequenceService.Type.RECEIPT, receiptRepository.countByTenantId(tenantId)));
        r.setCustomerId(customer.getId());
        r.setCustomerName(customer.getName());
        r.setCustomerMobile(customer.getMobile());
        r.setApplicationId(app == null ? null : app.getId());
        r.setApplicationCode(app == null ? null : app.getCode());
        r.setFeeType(form.getFeeType() == null ? FeeType.OTHER : form.getFeeType());
        r.setAmount(form.getAmount());
        r.setReceivedOn(form.getReceivedOn() == null ? LocalDate.now() : form.getReceivedOn());
        r.setPaymentMode(form.getPaymentMode());
        r.setReference(form.getReference());
        r.setRemarks(form.getRemarks());
        CustomUserDetails me = SecurityUtils.currentUser();
        r.setReceivedByName(me == null ? null : me.getFullName());
        r.setCreatedBy(SecurityUtils.currentUserLogin());
        FeeReceipt saved = receiptRepository.save(r);
        auditService.log("PAYMENT", "FeeReceipt", saved.getId(), saved.getReceiptNo() + " · " + saved.getCustomerName(),
                saved.getFeeTypeLabel() + " ₹" + Labels.inr(saved.getAmount()));
        return saved;
    }

    @Transactional
    public void cancelReceipt(Long tenantId, Long id) {
        FeeReceipt r = getReceipt(tenantId, id);
        if (r == null) return;
        r.setDeleted(true);
        r.setModifiedBy(SecurityUtils.currentUserLogin());
        receiptRepository.save(r);
        auditService.log("ARCHIVE", "FeeReceipt", id, r.getReceiptNo(), "Receipt cancelled");
    }
}
