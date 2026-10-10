package com.loanmust.loan.lender;

import com.loanmust.loan.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

/** A bank, HFC or NBFC the agency is empanelled with as a DSA. */
@Entity
@Table(name = "lenders")
public class Lender extends BaseEntity {

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private String name;

    private String shortName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 40)
    private LenderType lenderType;

    /** The lender's relationship manager / sales officer the agency logs files with. */
    private String rmName;
    private String rmMobile;
    private String rmEmail;
    private String branch;

    /** Default agent payout on disbursed amount (percent), unless a product overrides it. */
    private BigDecimal payoutPercent;

    @Column(length = 1000)
    private String payoutTerms;

    /** Hex colour used for the lender's badge on comparison screens. */
    private String brandColor;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 1000)
    private String notes;

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }

    public LenderType getLenderType() { return lenderType; }
    public void setLenderType(LenderType lenderType) { this.lenderType = lenderType; }

    public String getRmName() { return rmName; }
    public void setRmName(String rmName) { this.rmName = rmName; }

    public String getRmMobile() { return rmMobile; }
    public void setRmMobile(String rmMobile) { this.rmMobile = rmMobile; }

    public String getRmEmail() { return rmEmail; }
    public void setRmEmail(String rmEmail) { this.rmEmail = rmEmail; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public BigDecimal getPayoutPercent() { return payoutPercent; }
    public void setPayoutPercent(BigDecimal payoutPercent) { this.payoutPercent = payoutPercent; }

    public String getPayoutTerms() { return payoutTerms; }
    public void setPayoutTerms(String payoutTerms) { this.payoutTerms = payoutTerms; }

    public String getBrandColor() { return brandColor == null || brandColor.isBlank() ? "#4f46e5" : brandColor; }
    public void setBrandColor(String brandColor) { this.brandColor = brandColor; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @jakarta.persistence.Transient
    public String getDisplayName() {
        return shortName != null && !shortName.isBlank() ? shortName : name;
    }

    @jakarta.persistence.Transient
    public String getTypeLabel() {
        return lenderType == null ? "" : lenderType.getLabel();
    }

    @jakarta.persistence.Transient
    public String getInitials() {
        String s = getDisplayName();
        if (s == null || s.isBlank()) return "?";
        String[] p = s.trim().split("\\s+");
        return p.length == 1 ? p[0].substring(0, Math.min(3, p[0].length())).toUpperCase()
                : ("" + p[0].charAt(0) + p[1].charAt(0)).toUpperCase();
    }
}
