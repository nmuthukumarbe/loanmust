package com.loanmust.loan.commission;

import com.loanmust.loan.common.BaseEntity;
import com.loanmust.loan.common.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The agency's DSA payout receivable from a lender for one disbursement. Raised the moment
 * a tranche is recorded; finance then invoices the lender and records what actually came in.
 * A difference of more than ₹1 is flagged - lenders round, real short-payments stand out.
 */
@Entity
@Table(name = "bank_payouts")
public class BankPayout extends BaseEntity {

    public static final BigDecimal TOLERANCE = new BigDecimal("1.00");

    @Column(nullable = false)
    private Long tenantId;

    private Long lenderId;
    private String lenderName;

    private Long applicationId;
    private String applicationCode;
    private Long disbursementId;
    private String customerName;

    private LocalDate disbursedOn;
    private BigDecimal disbursedAmount;
    private BigDecimal payoutPercent;
    private BigDecimal expectedAmount;

    private String invoiceNumber;
    private LocalDate invoiceDate;

    private BigDecimal receivedAmount;
    private LocalDate receivedOn;
    private String receiptReference;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.UNPAID;

    @Column(length = 500)
    private String remarks;

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Long getLenderId() { return lenderId; }
    public void setLenderId(Long lenderId) { this.lenderId = lenderId; }

    public String getLenderName() { return lenderName; }
    public void setLenderName(String lenderName) { this.lenderName = lenderName; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public String getApplicationCode() { return applicationCode; }
    public void setApplicationCode(String applicationCode) { this.applicationCode = applicationCode; }

    public Long getDisbursementId() { return disbursementId; }
    public void setDisbursementId(Long disbursementId) { this.disbursementId = disbursementId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public LocalDate getDisbursedOn() { return disbursedOn; }
    public void setDisbursedOn(LocalDate disbursedOn) { this.disbursedOn = disbursedOn; }

    public BigDecimal getDisbursedAmount() { return disbursedAmount; }
    public void setDisbursedAmount(BigDecimal disbursedAmount) { this.disbursedAmount = disbursedAmount; }

    public BigDecimal getPayoutPercent() { return payoutPercent; }
    public void setPayoutPercent(BigDecimal payoutPercent) { this.payoutPercent = payoutPercent; }

    public BigDecimal getExpectedAmount() { return expectedAmount; }
    public void setExpectedAmount(BigDecimal expectedAmount) { this.expectedAmount = expectedAmount; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }

    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(BigDecimal receivedAmount) { this.receivedAmount = receivedAmount; }

    public LocalDate getReceivedOn() { return receivedOn; }
    public void setReceivedOn(LocalDate receivedOn) { this.receivedOn = receivedOn; }

    public String getReceiptReference() { return receiptReference; }
    public void setReceiptReference(String receiptReference) { this.receiptReference = receiptReference; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    @jakarta.persistence.Transient
    public BigDecimal getShortfall() {
        if (expectedAmount == null) return BigDecimal.ZERO;
        BigDecimal got = receivedAmount == null ? BigDecimal.ZERO : receivedAmount;
        return expectedAmount.subtract(got);
    }

    /** Paid but not within ₹1 of what was expected. */
    @jakarta.persistence.Transient
    public boolean isMismatch() {
        return receivedAmount != null && getShortfall().abs().compareTo(TOLERANCE) > 0;
    }

    @jakarta.persistence.Transient
    public String getStatusLabel() { return status == null ? "" : status.getLabel(); }

    @jakarta.persistence.Transient
    public String getStatusTone() {
        if (status == null) return "slate";
        return switch (status) {
            case PAID -> isMismatch() ? "amber" : "emerald";
            case PARTIAL -> "amber";
            case OVERDUE -> "rose";
            case CANCELLED -> "slate";
            default -> "brand";
        };
    }
}
