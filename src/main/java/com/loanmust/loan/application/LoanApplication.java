package com.loanmust.loan.application;

import com.loanmust.loan.common.BaseEntity;
import com.loanmust.loan.lead.LeadSource;
import com.loanmust.loan.lender.LoanType;
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
 * A loan file: one customer, one lender scheme, one property - from document collection
 * through bank login, sanction, MODD and disbursement. Names are denormalised onto the row
 * (customer, lender, builder, partner) so lists and reports never need joins and stay
 * readable even if a master is later archived.
 */
@Entity
@Table(name = "loan_applications")
public class LoanApplication extends BaseEntity {

    @Column(nullable = false)
    private Long tenantId;

    private String code;   // LA-0001

    @Column(nullable = false)
    private Long customerId;
    private String customerName;
    private String customerMobile;

    private Long leadId;
    private Long proposalId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 40)
    private LoanType loanType = LoanType.HOME_LOAN;

    private Long lenderId;
    private String lenderName;
    private Long productId;
    private String productName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 40)
    private LeadSource source = LeadSource.DIRECT;

    private Long builderId;
    private String builderName;
    private Long projectId;
    private String projectName;

    private Long referralPartnerId;
    private String referralPartnerName;

    // ---- property ----
    @Column(length = 500)
    private String propertyAddress;
    private String propertyType;
    private BigDecimal propertyValue;

    // ---- the ask ----
    private BigDecimal requestedAmount;
    private Integer tenureYears;

    // ---- co-applicant ----
    private String coApplicantName;
    private String coApplicantRelation;
    private BigDecimal coApplicantIncome;

    // ---- bank side ----
    /** The lender's own application / LOS number given at login. */
    private String bankReference;
    private LocalDate loginDate;

    private BigDecimal sanctionedAmount;
    private BigDecimal sanctionedRate;
    private Integer sanctionedTenureYears;
    private BigDecimal sanctionedEmi;
    private LocalDate sanctionDate;
    private LocalDate sanctionValidTill;

    private BigDecimal totalDisbursed;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private DisbursementStatus disbursementStatus = DisbursementStatus.NOT_STARTED;

    private LocalDate firstDisbursementDate;
    private LocalDate expectedDisbursementDate;

    /** Agency payout agreed with the lender for this file, percent of disbursal. */
    private BigDecimal payoutPercent;

    // ---- workflow ----
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 40)
    private ApplicationStage stage = ApplicationStage.DOCS_PENDING;

    private LocalDateTime stageChangedAt;

    @Column(length = 500)
    private String rejectionReason;

    /** Field executive who owns the customer relationship. */
    private Long assignedUserId;
    private String assignedUserName;

    /** Processing staff handling documents and the bank. */
    private Long processorUserId;
    private String processorName;

    @Column(length = 1000)
    private String notes;

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerMobile() { return customerMobile; }
    public void setCustomerMobile(String customerMobile) { this.customerMobile = customerMobile; }

    public Long getLeadId() { return leadId; }
    public void setLeadId(Long leadId) { this.leadId = leadId; }

    public Long getProposalId() { return proposalId; }
    public void setProposalId(Long proposalId) { this.proposalId = proposalId; }

    public LoanType getLoanType() { return loanType; }
    public void setLoanType(LoanType loanType) { this.loanType = loanType; }

    public Long getLenderId() { return lenderId; }
    public void setLenderId(Long lenderId) { this.lenderId = lenderId; }

    public String getLenderName() { return lenderName; }
    public void setLenderName(String lenderName) { this.lenderName = lenderName; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public LeadSource getSource() { return source; }
    public void setSource(LeadSource source) { this.source = source; }

    public Long getBuilderId() { return builderId; }
    public void setBuilderId(Long builderId) { this.builderId = builderId; }

    public String getBuilderName() { return builderName; }
    public void setBuilderName(String builderName) { this.builderName = builderName; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public Long getReferralPartnerId() { return referralPartnerId; }
    public void setReferralPartnerId(Long referralPartnerId) { this.referralPartnerId = referralPartnerId; }

    public String getReferralPartnerName() { return referralPartnerName; }
    public void setReferralPartnerName(String referralPartnerName) { this.referralPartnerName = referralPartnerName; }

    public String getPropertyAddress() { return propertyAddress; }
    public void setPropertyAddress(String propertyAddress) { this.propertyAddress = propertyAddress; }

    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }

    public BigDecimal getPropertyValue() { return propertyValue; }
    public void setPropertyValue(BigDecimal propertyValue) { this.propertyValue = propertyValue; }

    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public void setRequestedAmount(BigDecimal requestedAmount) { this.requestedAmount = requestedAmount; }

    public Integer getTenureYears() { return tenureYears; }
    public void setTenureYears(Integer tenureYears) { this.tenureYears = tenureYears; }

    public String getCoApplicantName() { return coApplicantName; }
    public void setCoApplicantName(String coApplicantName) { this.coApplicantName = coApplicantName; }

    public String getCoApplicantRelation() { return coApplicantRelation; }
    public void setCoApplicantRelation(String coApplicantRelation) { this.coApplicantRelation = coApplicantRelation; }

    public BigDecimal getCoApplicantIncome() { return coApplicantIncome; }
    public void setCoApplicantIncome(BigDecimal coApplicantIncome) { this.coApplicantIncome = coApplicantIncome; }

    public String getBankReference() { return bankReference; }
    public void setBankReference(String bankReference) { this.bankReference = bankReference; }

    public LocalDate getLoginDate() { return loginDate; }
    public void setLoginDate(LocalDate loginDate) { this.loginDate = loginDate; }

    public BigDecimal getSanctionedAmount() { return sanctionedAmount; }
    public void setSanctionedAmount(BigDecimal sanctionedAmount) { this.sanctionedAmount = sanctionedAmount; }

    public BigDecimal getSanctionedRate() { return sanctionedRate; }
    public void setSanctionedRate(BigDecimal sanctionedRate) { this.sanctionedRate = sanctionedRate; }

    public Integer getSanctionedTenureYears() { return sanctionedTenureYears; }
    public void setSanctionedTenureYears(Integer sanctionedTenureYears) { this.sanctionedTenureYears = sanctionedTenureYears; }

    public BigDecimal getSanctionedEmi() { return sanctionedEmi; }
    public void setSanctionedEmi(BigDecimal sanctionedEmi) { this.sanctionedEmi = sanctionedEmi; }

    public LocalDate getSanctionDate() { return sanctionDate; }
    public void setSanctionDate(LocalDate sanctionDate) { this.sanctionDate = sanctionDate; }

    public LocalDate getSanctionValidTill() { return sanctionValidTill; }
    public void setSanctionValidTill(LocalDate sanctionValidTill) { this.sanctionValidTill = sanctionValidTill; }

    public BigDecimal getTotalDisbursed() { return totalDisbursed; }
    public void setTotalDisbursed(BigDecimal totalDisbursed) { this.totalDisbursed = totalDisbursed; }

    public DisbursementStatus getDisbursementStatus() { return disbursementStatus; }
    public void setDisbursementStatus(DisbursementStatus disbursementStatus) { this.disbursementStatus = disbursementStatus; }

    public LocalDate getFirstDisbursementDate() { return firstDisbursementDate; }
    public void setFirstDisbursementDate(LocalDate firstDisbursementDate) { this.firstDisbursementDate = firstDisbursementDate; }

    public LocalDate getExpectedDisbursementDate() { return expectedDisbursementDate; }
    public void setExpectedDisbursementDate(LocalDate expectedDisbursementDate) { this.expectedDisbursementDate = expectedDisbursementDate; }

    public BigDecimal getPayoutPercent() { return payoutPercent; }
    public void setPayoutPercent(BigDecimal payoutPercent) { this.payoutPercent = payoutPercent; }

    public ApplicationStage getStage() { return stage; }
    public void setStage(ApplicationStage stage) { this.stage = stage; }

    public LocalDateTime getStageChangedAt() { return stageChangedAt; }
    public void setStageChangedAt(LocalDateTime stageChangedAt) { this.stageChangedAt = stageChangedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public Long getAssignedUserId() { return assignedUserId; }
    public void setAssignedUserId(Long assignedUserId) { this.assignedUserId = assignedUserId; }

    public String getAssignedUserName() { return assignedUserName; }
    public void setAssignedUserName(String assignedUserName) { this.assignedUserName = assignedUserName; }

    public Long getProcessorUserId() { return processorUserId; }
    public void setProcessorUserId(Long processorUserId) { this.processorUserId = processorUserId; }

    public String getProcessorName() { return processorName; }
    public void setProcessorName(String processorName) { this.processorName = processorName; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @jakarta.persistence.Transient
    public String getStageLabel() { return stage == null ? "" : stage.getLabel(); }

    @jakarta.persistence.Transient
    public String getStageTone() { return stage == null ? "slate" : stage.getTone(); }

    @jakarta.persistence.Transient
    public String getLoanTypeLabel() { return loanType == null ? "" : loanType.getLabel(); }

    @jakarta.persistence.Transient
    public boolean isOpen() { return stage != null && stage.isOpen(); }

    /** Sanctioned figure when known, else the amount asked for - the file's working value. */
    @jakarta.persistence.Transient
    public BigDecimal getLoanAmount() {
        return sanctionedAmount != null ? sanctionedAmount : requestedAmount;
    }

    @jakarta.persistence.Transient
    public BigDecimal getPendingDisbursal() {
        if (sanctionedAmount == null) return null;
        BigDecimal done = totalDisbursed == null ? BigDecimal.ZERO : totalDisbursed;
        return sanctionedAmount.subtract(done).max(BigDecimal.ZERO);
    }

    /** Progress along the forward path, 0-100, for the progress bar. */
    @jakarta.persistence.Transient
    public int getProgressPercent() {
        if (stage == null || stage.isTerminalFailure()) return 0;
        int idx = ApplicationStage.path().indexOf(stage);
        return idx < 0 ? 0 : Math.round(100f * (idx + 1) / ApplicationStage.path().size());
    }

    /** Sanction letters lapse; warn when validity ends within 15 days and nothing is disbursed. */
    @jakarta.persistence.Transient
    public boolean isSanctionExpiring() {
        return sanctionValidTill != null && disbursementStatus == DisbursementStatus.NOT_STARTED
                && isOpen() && !sanctionValidTill.isAfter(LocalDate.now().plusDays(15));
    }

    @jakarta.persistence.Transient
    public long getDaysInStage() {
        if (stageChangedAt == null) return 0;
        return java.time.Duration.between(stageChangedAt, LocalDateTime.now()).toDays();
    }
}
