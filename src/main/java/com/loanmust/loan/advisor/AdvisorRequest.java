package com.loanmust.loan.advisor;

import com.loanmust.loan.customer.EmploymentType;
import com.loanmust.loan.lender.LoanType;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** The customer profile the advisor compares every lender scheme against. Bound from the form. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdvisorRequest {

    private LoanType loanType = LoanType.HOME_LOAN;
    private BigDecimal loanAmount;
    private Integer tenureYears = 20;
    private BigDecimal propertyValue;
    private BigDecimal monthlyIncome;
    private BigDecimal existingEmi;
    private Integer cibilScore;
    private EmploymentType employmentType = EmploymentType.SALARIED;
    private Integer applicantAge;
    private boolean womanApplicant;

    /** Optional links back to the people this comparison was run for. */
    private Long customerId;
    private Long leadId;
    private String customerName;
    private String customerMobile;

    public LoanType getLoanType() { return loanType; }
    public void setLoanType(LoanType loanType) { this.loanType = loanType; }

    public BigDecimal getLoanAmount() { return loanAmount; }
    public void setLoanAmount(BigDecimal loanAmount) { this.loanAmount = loanAmount; }

    public Integer getTenureYears() { return tenureYears; }
    public void setTenureYears(Integer tenureYears) { this.tenureYears = tenureYears; }

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

    public Integer getApplicantAge() { return applicantAge; }
    public void setApplicantAge(Integer applicantAge) { this.applicantAge = applicantAge; }

    public boolean isWomanApplicant() { return womanApplicant; }
    public void setWomanApplicant(boolean womanApplicant) { this.womanApplicant = womanApplicant; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Long getLeadId() { return leadId; }
    public void setLeadId(Long leadId) { this.leadId = leadId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerMobile() { return customerMobile; }
    public void setCustomerMobile(String customerMobile) { this.customerMobile = customerMobile; }

    public boolean isComplete() {
        return loanType != null && loanAmount != null && loanAmount.signum() > 0
                && tenureYears != null && tenureYears > 0;
    }
}
