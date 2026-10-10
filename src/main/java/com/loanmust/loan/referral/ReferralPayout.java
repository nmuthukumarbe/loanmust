package com.loanmust.loan.referral;

import com.loanmust.loan.common.BaseEntity;
import com.loanmust.loan.common.PaymentMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Money owed to a referral partner for one disbursement. Raised automatically when a
 * referred loan disburses; the rule in force at that moment is copied onto the row so a
 * later change to the partner's rate never rewrites history.
 */
@Entity
@Table(name = "referral_payouts")
public class ReferralPayout extends BaseEntity {

    @Column(nullable = false)
    private Long tenantId;

    private String code;   // RP-0001

    @Column(nullable = false)
    private Long partnerId;
    private String partnerName;
    private String partnerMobile;

    private Long applicationId;
    private String applicationCode;
    private Long disbursementId;
    private String customerName;
    private String lenderName;

    private BigDecimal disbursedAmount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 40)
    private PayoutBasis payoutBasis;
    private BigDecimal payoutValue;

    private BigDecimal grossAmount;
    private BigDecimal tdsPercent;
    private BigDecimal tdsAmount;
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private PayoutStatus status = PayoutStatus.PENDING;

    private String approvedByName;
    private LocalDateTime approvedAt;

    private LocalDate paidOn;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private PaymentMode paymentMode;

    /** UTR / UPI transaction id / cheque number. */
    private String paymentReference;

    @Column(length = 1000)
    private String remarks;

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Long getPartnerId() { return partnerId; }
    public void setPartnerId(Long partnerId) { this.partnerId = partnerId; }

    public String getPartnerName() { return partnerName; }
    public void setPartnerName(String partnerName) { this.partnerName = partnerName; }

    public String getPartnerMobile() { return partnerMobile; }
    public void setPartnerMobile(String partnerMobile) { this.partnerMobile = partnerMobile; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public String getApplicationCode() { return applicationCode; }
    public void setApplicationCode(String applicationCode) { this.applicationCode = applicationCode; }

    public Long getDisbursementId() { return disbursementId; }
    public void setDisbursementId(Long disbursementId) { this.disbursementId = disbursementId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getLenderName() { return lenderName; }
    public void setLenderName(String lenderName) { this.lenderName = lenderName; }

    public BigDecimal getDisbursedAmount() { return disbursedAmount; }
    public void setDisbursedAmount(BigDecimal disbursedAmount) { this.disbursedAmount = disbursedAmount; }

    public PayoutBasis getPayoutBasis() { return payoutBasis; }
    public void setPayoutBasis(PayoutBasis payoutBasis) { this.payoutBasis = payoutBasis; }

    public BigDecimal getPayoutValue() { return payoutValue; }
    public void setPayoutValue(BigDecimal payoutValue) { this.payoutValue = payoutValue; }

    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }

    public BigDecimal getTdsPercent() { return tdsPercent; }
    public void setTdsPercent(BigDecimal tdsPercent) { this.tdsPercent = tdsPercent; }

    public BigDecimal getTdsAmount() { return tdsAmount; }
    public void setTdsAmount(BigDecimal tdsAmount) { this.tdsAmount = tdsAmount; }

    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }

    public PayoutStatus getStatus() { return status; }
    public void setStatus(PayoutStatus status) { this.status = status; }

    public String getApprovedByName() { return approvedByName; }
    public void setApprovedByName(String approvedByName) { this.approvedByName = approvedByName; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public LocalDate getPaidOn() { return paidOn; }
    public void setPaidOn(LocalDate paidOn) { this.paidOn = paidOn; }

    public PaymentMode getPaymentMode() { return paymentMode; }
    public void setPaymentMode(PaymentMode paymentMode) { this.paymentMode = paymentMode; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    @jakarta.persistence.Transient
    public String getStatusLabel() { return status == null ? "" : status.getLabel(); }

    @jakarta.persistence.Transient
    public String getStatusTone() { return status == null ? "slate" : status.getTone(); }
}
