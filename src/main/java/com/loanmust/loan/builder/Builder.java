package com.loanmust.loan.builder;

import com.loanmust.loan.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** A real-estate developer the agency has a tie-up with; its buyers are a lead channel. */
@Entity
@Table(name = "builders")
public class Builder extends BaseEntity {

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private String name;

    private String contactPerson;
    private String mobile;
    private String email;

    @Column(length = 500)
    private String address;
    private String city;

    private LocalDate tieUpSince;

    /** What was agreed: who refers whom, any sharing of payout, site-desk arrangement. */
    @Column(length = 1000)
    private String arrangement;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 1000)
    private String notes;

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public LocalDate getTieUpSince() { return tieUpSince; }
    public void setTieUpSince(LocalDate tieUpSince) { this.tieUpSince = tieUpSince; }

    public String getArrangement() { return arrangement; }
    public void setArrangement(String arrangement) { this.arrangement = arrangement; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
