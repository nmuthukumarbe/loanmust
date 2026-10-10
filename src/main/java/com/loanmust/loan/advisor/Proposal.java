package com.loanmust.loan.advisor;

import com.loanmust.loan.common.BaseEntity;
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
 * A saved comparison shared with a customer: "here is every bank, here is what each would
 * cost you, and here is the one we recommend". The full outcome is frozen as JSON at the
 * moment it was shared, so the proposal still reads the same after rates change - and the
 * agency can always show what it advised and why.
 */
@Entity
@Table(name = "proposals")
public class Proposal extends BaseEntity {

    @Column(nullable = false)
    private Long tenantId;

    private String code;   // PR-0001

    /** Random, unguessable token for the public link /p/{token}. */
    @Column(nullable = false, unique = true, length = 40)
    private String token;

    private Long customerId;
    private Long leadId;
    private String customerName;
    private String customerMobile;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 40)
    private LoanType loanType;

    private BigDecimal loanAmount;
    private Integer tenureYears;

    private Long recommendedProductId;
    private String recommendedLenderName;
    private String recommendedProductName;
    private BigDecimal recommendedRate;
    private BigDecimal recommendedEmi;
    private Integer eligibleCount;

    @Column(columnDefinition = "LONGTEXT")
    private String outcomeJson;

    private LocalDate validUntil;

    private Integer viewCount = 0;
    private LocalDateTime lastViewedAt;

    /** Set when a loan file is opened from this proposal. */
    private Long applicationId;

    private String preparedByName;

    @Column(length = 1000)
    private String remarks;

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Long getLeadId() { return leadId; }
    public void setLeadId(Long leadId) { this.leadId = leadId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerMobile() { return customerMobile; }
    public void setCustomerMobile(String customerMobile) { this.customerMobile = customerMobile; }

    public LoanType getLoanType() { return loanType; }
    public void setLoanType(LoanType loanType) { this.loanType = loanType; }

    public BigDecimal getLoanAmount() { return loanAmount; }
    public void setLoanAmount(BigDecimal loanAmount) { this.loanAmount = loanAmount; }

    public Integer getTenureYears() { return tenureYears; }
    public void setTenureYears(Integer tenureYears) { this.tenureYears = tenureYears; }

    public Long getRecommendedProductId() { return recommendedProductId; }
    public void setRecommendedProductId(Long recommendedProductId) { this.recommendedProductId = recommendedProductId; }

    public String getRecommendedLenderName() { return recommendedLenderName; }
    public void setRecommendedLenderName(String recommendedLenderName) { this.recommendedLenderName = recommendedLenderName; }

    public String getRecommendedProductName() { return recommendedProductName; }
    public void setRecommendedProductName(String recommendedProductName) { this.recommendedProductName = recommendedProductName; }

    public BigDecimal getRecommendedRate() { return recommendedRate; }
    public void setRecommendedRate(BigDecimal recommendedRate) { this.recommendedRate = recommendedRate; }

    public BigDecimal getRecommendedEmi() { return recommendedEmi; }
    public void setRecommendedEmi(BigDecimal recommendedEmi) { this.recommendedEmi = recommendedEmi; }

    public Integer getEligibleCount() { return eligibleCount; }
    public void setEligibleCount(Integer eligibleCount) { this.eligibleCount = eligibleCount; }

    public String getOutcomeJson() { return outcomeJson; }
    public void setOutcomeJson(String outcomeJson) { this.outcomeJson = outcomeJson; }

    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }

    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }

    public LocalDateTime getLastViewedAt() { return lastViewedAt; }
    public void setLastViewedAt(LocalDateTime lastViewedAt) { this.lastViewedAt = lastViewedAt; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public String getPreparedByName() { return preparedByName; }
    public void setPreparedByName(String preparedByName) { this.preparedByName = preparedByName; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    @jakarta.persistence.Transient
    public boolean isExpired() {
        return validUntil != null && validUntil.isBefore(LocalDate.now());
    }

    @jakarta.persistence.Transient
    public String getLoanTypeLabel() {
        return loanType == null ? "" : loanType.getLabel();
    }
}
