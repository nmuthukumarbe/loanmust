package com.loanmust.loan.application;

import com.loanmust.loan.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One tranche released by the lender. Construction-linked home loans disburse in several
 * tranches; each one raises the agency's bank payout and the referrer's payout.
 */
@Entity
@Table(name = "disbursements")
public class Disbursement extends BaseEntity {

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private Long applicationId;

    private Integer trancheNo;

    @Column(nullable = false)
    private LocalDate disbursedOn;

    @Column(nullable = false)
    private BigDecimal amount;

    /** Paid to: builder / seller / customer / existing lender (balance transfer). */
    private String paidTo;

    /** Cheque / UTR reference from the lender. */
    private String reference;

    @Column(length = 500)
    private String remarks;

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public Integer getTrancheNo() { return trancheNo; }
    public void setTrancheNo(Integer trancheNo) { this.trancheNo = trancheNo; }

    public LocalDate getDisbursedOn() { return disbursedOn; }
    public void setDisbursedOn(LocalDate disbursedOn) { this.disbursedOn = disbursedOn; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaidTo() { return paidTo; }
    public void setPaidTo(String paidTo) { this.paidTo = paidTo; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
