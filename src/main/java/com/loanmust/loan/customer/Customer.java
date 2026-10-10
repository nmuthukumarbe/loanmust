package com.loanmust.loan.customer;

import com.loanmust.loan.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

/**
 * A borrower. Holds the credit profile the advisor needs (income, obligations, score,
 * employment) so a comparison can be run with one click from the customer screen.
 *
 * Privacy: only the last four digits of Aadhaar are stored - the full number never needs
 * to live in the agency's database; the scanned card sits in the document vault.
 */
@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {

    @Column(nullable = false)
    private Long tenantId;

    private String code;   // CU-0001

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String mobile;

    private String alternateMobile;
    private String email;
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private Gender gender;

    private String pan;
    private String aadhaarLast4;

    @Column(length = 500)
    private String address;
    private String city;
    private String state;
    private String pincode;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 40)
    private EmploymentType employmentType;

    private String employerName;
    private Integer workExperienceYears;

    /** Net monthly income (take-home for salaried, average monthly for self-employed). */
    private BigDecimal monthlyIncome;

    /** Sum of EMIs already being paid. */
    private BigDecimal existingEmi;

    private Integer cibilScore;
    private LocalDate cibilCheckedOn;

    private Long sourceLeadId;
    private Long ownerUserId;
    private String ownerName;

    private Long referralPartnerId;
    private String referralPartnerName;

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

    public String getAlternateMobile() { return alternateMobile; }
    public void setAlternateMobile(String alternateMobile) { this.alternateMobile = alternateMobile; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public String getPan() { return pan; }
    public void setPan(String pan) { this.pan = pan; }

    public String getAadhaarLast4() { return aadhaarLast4; }
    public void setAadhaarLast4(String aadhaarLast4) { this.aadhaarLast4 = aadhaarLast4; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public EmploymentType getEmploymentType() { return employmentType; }
    public void setEmploymentType(EmploymentType employmentType) { this.employmentType = employmentType; }

    public String getEmployerName() { return employerName; }
    public void setEmployerName(String employerName) { this.employerName = employerName; }

    public Integer getWorkExperienceYears() { return workExperienceYears; }
    public void setWorkExperienceYears(Integer workExperienceYears) { this.workExperienceYears = workExperienceYears; }

    public BigDecimal getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(BigDecimal monthlyIncome) { this.monthlyIncome = monthlyIncome; }

    public BigDecimal getExistingEmi() { return existingEmi; }
    public void setExistingEmi(BigDecimal existingEmi) { this.existingEmi = existingEmi; }

    public Integer getCibilScore() { return cibilScore; }
    public void setCibilScore(Integer cibilScore) { this.cibilScore = cibilScore; }

    public LocalDate getCibilCheckedOn() { return cibilCheckedOn; }
    public void setCibilCheckedOn(LocalDate cibilCheckedOn) { this.cibilCheckedOn = cibilCheckedOn; }

    public Long getSourceLeadId() { return sourceLeadId; }
    public void setSourceLeadId(Long sourceLeadId) { this.sourceLeadId = sourceLeadId; }

    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public Long getReferralPartnerId() { return referralPartnerId; }
    public void setReferralPartnerId(Long referralPartnerId) { this.referralPartnerId = referralPartnerId; }

    public String getReferralPartnerName() { return referralPartnerName; }
    public void setReferralPartnerName(String referralPartnerName) { this.referralPartnerName = referralPartnerName; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @jakarta.persistence.Transient
    public Integer getAge() {
        return dateOfBirth == null ? null : Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    @jakarta.persistence.Transient
    public String getEmploymentLabel() {
        return employmentType == null ? "—" : employmentType.getLabel();
    }

    @jakarta.persistence.Transient
    public String getInitials() {
        if (name == null || name.isBlank()) return "C";
        String[] p = name.trim().split("\\s+");
        if (p.length == 1) return p[0].substring(0, 1).toUpperCase();
        return (p[0].charAt(0) + "" + p[p.length - 1].charAt(0)).toUpperCase();
    }

    @jakarta.persistence.Transient
    public String getCibilTone() {
        if (cibilScore == null) return "slate";
        if (cibilScore >= 750) return "emerald";
        if (cibilScore >= 700) return "amber";
        return "rose";
    }
}
