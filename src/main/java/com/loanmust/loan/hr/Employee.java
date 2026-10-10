package com.loanmust.loan.hr;

import com.loanmust.loan.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A member of staff - data entry operators, field executives, processing staff. Linked to a
 * sign-in account where they have one, so their productivity (records captured) can be
 * read straight off the audit trail.
 */
@Entity
@Table(name = "employees")
public class Employee extends BaseEntity {

    @Column(nullable = false)
    private Long tenantId;

    private String code;   // EMP-0001

    @Column(nullable = false)
    private String name;

    private String mobile;
    private String email;
    private String designation;

    /** Data Entry, Sales, Processing, Accounts ... */
    private String department;

    private LocalDate joiningDate;
    private LocalDate dateOfBirth;
    private BigDecimal monthlySalary;

    /** Optional link to the staff sign-in account. */
    private Long userId;

    @Column(length = 500)
    private String address;
    private String emergencyContact;

    @Column(nullable = false)
    private boolean active = true;

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

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public LocalDate getJoiningDate() { return joiningDate; }
    public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public BigDecimal getMonthlySalary() { return monthlySalary; }
    public void setMonthlySalary(BigDecimal monthlySalary) { this.monthlySalary = monthlySalary; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @jakarta.persistence.Transient
    public String getInitials() {
        if (name == null || name.isBlank()) return "E";
        String[] p = name.trim().split("\\s+");
        if (p.length == 1) return p[0].substring(0, 1).toUpperCase();
        return (p[0].charAt(0) + "" + p[p.length - 1].charAt(0)).toUpperCase();
    }
}
