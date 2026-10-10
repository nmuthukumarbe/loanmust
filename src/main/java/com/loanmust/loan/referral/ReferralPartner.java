package com.loanmust.loan.referral;

import com.loanmust.loan.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

/**
 * Someone who refers borrowers and is paid when the loan disburses. Each partner gets a
 * personal link (/refer/{code}) they can share on WhatsApp; enquiries through it become
 * leads already tagged to them, so their payout is never missed or disputed.
 */
@Entity
@Table(name = "referral_partners")
public class ReferralPartner extends BaseEntity {

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 20)
    private String code;   // RF-0001 - also the public referral link

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String mobile;

    private String email;
    private String city;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 40)
    private PartnerType partnerType = PartnerType.INDIVIDUAL;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 40)
    private PayoutBasis payoutBasis = PayoutBasis.PERCENT_OF_DISBURSAL;

    /** Percent of disbursal (e.g. 0.25) or flat rupees per file, per {@link #payoutBasis}. */
    private BigDecimal payoutValue;

    // ---- where to send the money ----
    private String upiId;
    private String bankAccountName;
    private String bankAccountNumber;
    private String ifsc;
    private String bankName;
    private String pan;

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

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public PartnerType getPartnerType() { return partnerType; }
    public void setPartnerType(PartnerType partnerType) { this.partnerType = partnerType; }

    public PayoutBasis getPayoutBasis() { return payoutBasis; }
    public void setPayoutBasis(PayoutBasis payoutBasis) { this.payoutBasis = payoutBasis; }

    public BigDecimal getPayoutValue() { return payoutValue; }
    public void setPayoutValue(BigDecimal payoutValue) { this.payoutValue = payoutValue; }

    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }

    public String getBankAccountName() { return bankAccountName; }
    public void setBankAccountName(String bankAccountName) { this.bankAccountName = bankAccountName; }

    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }

    public String getIfsc() { return ifsc; }
    public void setIfsc(String ifsc) { this.ifsc = ifsc; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getPan() { return pan; }
    public void setPan(String pan) { this.pan = pan; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @jakarta.persistence.Transient
    public String getPartnerTypeLabel() { return partnerType == null ? "" : partnerType.getLabel(); }

    /** Payout rule in words: "0.25% of disbursal" or "₹5,000 per file". */
    @jakarta.persistence.Transient
    public String getPayoutRule() {
        if (payoutValue == null) return "Agency default";
        String v = payoutValue.stripTrailingZeros().toPlainString();
        return payoutBasis == PayoutBasis.FLAT_PER_FILE
                ? "₹" + com.loanmust.loan.common.Labels.inr(payoutValue) + " per file"
                : v + "% of disbursal";
    }

    /** Account number with all but the last four digits hidden. */
    @jakarta.persistence.Transient
    public String getMaskedAccount() {
        if (bankAccountNumber == null || bankAccountNumber.length() < 5) return bankAccountNumber;
        return "XXXX" + bankAccountNumber.substring(bankAccountNumber.length() - 4);
    }

    @jakarta.persistence.Transient
    public boolean isPayable() {
        return (upiId != null && !upiId.isBlank()) || (bankAccountNumber != null && !bankAccountNumber.isBlank());
    }
}
