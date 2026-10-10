package com.loanmust.loan.lead;

import com.loanmust.loan.common.BaseEntity;
import com.loanmust.loan.customer.EmploymentType;
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
 * A loan enquiry, from first contact to a loan file (or loss). Captures which channel it
 * came through - a builder project, LIC, a referral partner, or a direct client - because
 * that is how the agency measures its tie-ups and pays its referrers.
 */
@Entity
@Table(name = "leads")
public class Lead extends BaseEntity {

    @Column(nullable = false)
    private Long tenantId;

    private String code;   // LD-0001

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String mobile;

    private String email;
    private String city;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 40)
    private LeadSource source = LeadSource.DIRECT;

    /** Free text: LIC branch / agent name, campaign name, website form ... */
    private String sourceDetail;

    private Long builderId;
    private String builderName;
    private Long projectId;
    private String projectName;

    private Long referralPartnerId;
    private String referralPartnerName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 40)
    private LoanType loanType = LoanType.HOME_LOAN;

    private BigDecimal loanAmountRequired;
    private BigDecimal propertyValue;
    private BigDecimal monthlyIncome;
    private BigDecimal existingEmi;
    private Integer cibilScore;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 40)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 40)
    private LeadStatus status = LeadStatus.NEW;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private LeadPriority priority = LeadPriority.COLD;

    /** 0-100 explainable quality score (see LeadScoringService). */
    private Integer score;

    @Column(length = 500)
    private String suggestedAction;

    private Long assignedUserId;
    private String assignedUserName;
    private LocalDateTime assignedAt;

    private LocalDate nextFollowUpDate;
    private LocalDateTime lastContactedAt;
    private Integer followUpCount = 0;

    private Long duplicateOfLeadId;

    private Long convertedCustomerId;
    private Long convertedApplicationId;
    private LocalDateTime convertedAt;
    private String lostReason;

    @Column(length = 1000)
    private String requirement;

    @Column(length = 1000)
    private String notes;

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public LeadSource getSource() { return source; }
    public void setSource(LeadSource source) { this.source = source; }

    public String getSourceDetail() { return sourceDetail; }
    public void setSourceDetail(String sourceDetail) { this.sourceDetail = sourceDetail; }

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

    public LoanType getLoanType() { return loanType; }
    public void setLoanType(LoanType loanType) { this.loanType = loanType; }

    public BigDecimal getLoanAmountRequired() { return loanAmountRequired; }
    public void setLoanAmountRequired(BigDecimal loanAmountRequired) { this.loanAmountRequired = loanAmountRequired; }

    public BigDecimal getPropertyValue() { return propertyValue; }
    public void setPropertyValue(BigDecimal propertyValue) { this.propertyValue = propertyValue; }

    public BigDecimal getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(BigDecimal monthlyIncome) { this.monthlyIncome = monthlyIncome; }

    public BigDecimal getExistingEmi() { return existingEmi; }
    public void setExistingEmi(BigDecimal existingEmi) { this.existingEmi = existingEmi; }

    public Integer getCibilScore() { return cibilScore; }
    public void setCibilScore(Integer cibilScore) { this.cibilScore = cibilScore; }

    public EmploymentType getEmploymentType() { return employmentType; }
    public void setEmploymentType(EmploymentType employmentType) { this.employmentType = employmentType; }

    public LeadStatus getStatus() { return status; }
    public void setStatus(LeadStatus status) { this.status = status; }

    public LeadPriority getPriority() { return priority; }
    public void setPriority(LeadPriority priority) { this.priority = priority; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getSuggestedAction() { return suggestedAction; }
    public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }

    public Long getAssignedUserId() { return assignedUserId; }
    public void setAssignedUserId(Long assignedUserId) { this.assignedUserId = assignedUserId; }

    public String getAssignedUserName() { return assignedUserName; }
    public void setAssignedUserName(String assignedUserName) { this.assignedUserName = assignedUserName; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    public LocalDate getNextFollowUpDate() { return nextFollowUpDate; }
    public void setNextFollowUpDate(LocalDate nextFollowUpDate) { this.nextFollowUpDate = nextFollowUpDate; }

    public LocalDateTime getLastContactedAt() { return lastContactedAt; }
    public void setLastContactedAt(LocalDateTime lastContactedAt) { this.lastContactedAt = lastContactedAt; }

    public Integer getFollowUpCount() { return followUpCount; }
    public void setFollowUpCount(Integer followUpCount) { this.followUpCount = followUpCount; }

    public Long getDuplicateOfLeadId() { return duplicateOfLeadId; }
    public void setDuplicateOfLeadId(Long duplicateOfLeadId) { this.duplicateOfLeadId = duplicateOfLeadId; }

    public Long getConvertedCustomerId() { return convertedCustomerId; }
    public void setConvertedCustomerId(Long convertedCustomerId) { this.convertedCustomerId = convertedCustomerId; }

    public Long getConvertedApplicationId() { return convertedApplicationId; }
    public void setConvertedApplicationId(Long convertedApplicationId) { this.convertedApplicationId = convertedApplicationId; }

    public LocalDateTime getConvertedAt() { return convertedAt; }
    public void setConvertedAt(LocalDateTime convertedAt) { this.convertedAt = convertedAt; }

    public String getLostReason() { return lostReason; }
    public void setLostReason(String lostReason) { this.lostReason = lostReason; }

    public String getRequirement() { return requirement; }
    public void setRequirement(String requirement) { this.requirement = requirement; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @jakarta.persistence.Transient
    public String getStatusLabel() { return status == null ? "" : status.getLabel(); }

    @jakarta.persistence.Transient
    public String getStatusTone() { return status == null ? "slate" : status.getTone(); }

    @jakarta.persistence.Transient
    public String getPriorityLabel() { return priority == null ? "" : priority.getLabel(); }

    @jakarta.persistence.Transient
    public String getPriorityTone() { return priority == null ? "slate" : priority.getTone(); }

    @jakarta.persistence.Transient
    public String getSourceLabel() { return source == null ? "" : source.getLabel(); }

    @jakarta.persistence.Transient
    public String getLoanTypeLabel() { return loanType == null ? "—" : loanType.getLabel(); }

    @jakarta.persistence.Transient
    public boolean isOpen() { return status != null && status.isOpen(); }

    @jakarta.persistence.Transient
    public String getInitials() {
        if (name == null || name.isBlank()) return "L";
        String[] p = name.trim().split("\\s+");
        if (p.length == 1) return p[0].substring(0, 1).toUpperCase();
        return (p[0].charAt(0) + "" + p[p.length - 1].charAt(0)).toUpperCase();
    }

    /** Where the lead came from, in one line: "Builder tie-up · Prestige Lakeside". */
    @jakarta.persistence.Transient
    public String getChannelLine() {
        String detail = switch (source == null ? LeadSource.DIRECT : source) {
            case BUILDER -> projectName != null ? builderName + " · " + projectName : builderName;
            case REFERRAL -> referralPartnerName;
            default -> sourceDetail;
        };
        return getSourceLabel() + (detail == null || detail.isBlank() ? "" : " · " + detail);
    }

    @jakarta.persistence.Transient
    public boolean isFollowUpOverdue() {
        return isOpen() && nextFollowUpDate != null && nextFollowUpDate.isBefore(LocalDate.now());
    }

    @jakarta.persistence.Transient
    public boolean isFollowUpToday() {
        return nextFollowUpDate != null && nextFollowUpDate.isEqual(LocalDate.now());
    }
}
