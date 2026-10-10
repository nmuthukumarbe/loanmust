package com.loanmust.loan.tenant;

import com.loanmust.loan.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * The tenant: a loan agency / DSA (Direct Selling Agent) office. Every other record in the
 * system carries this record's id as its {@code tenantId}.
 */
@Entity
@Table(name = "agencies")
public class Agency extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String tradeName;
    private String ownerName;
    private String email;
    private String phone;

    /** DSA / connector code the agency is registered under with its lenders. */
    private String dsaCode;

    private String gstin;
    private String pan;

    @Column(length = 500)
    private String address;
    private String city;
    private String state;
    private String pincode;

    /** Prefixes used when generating human-friendly codes. */
    private String leadPrefix = "LD-";
    private String customerPrefix = "CU-";
    private String applicationPrefix = "LA-";
    private String referralPrefix = "RF-";
    private String proposalPrefix = "PR-";
    private String receiptPrefix = "FR-";

    /** Workspace accent colour id (see {@link com.loanmust.loan.settings.AccentPalette}). */
    @org.hibernate.annotations.ColumnDefault("'indigo'")
    private String accentColor = "indigo";

    // ---- Referral programme defaults (applied to a new referral partner) ----

    /** Default referral payout as a percent of the disbursed amount, e.g. 0.25. */
    private BigDecimal defaultReferralPercent = new BigDecimal("0.25");

    /** TDS deducted on referral payouts (section 194H style), e.g. 5.00. 0 disables. */
    private BigDecimal referralTdsPercent = new BigDecimal("5.00");

    // ---- MODD defaults (state specific - the agency sets its own values) ----

    /** Stamp duty on the MODD as a percent of the loan amount secured. */
    private BigDecimal moddStampDutyPercent = new BigDecimal("0.10");

    /** Upper limit on MODD stamp duty; null or 0 means no cap. */
    private BigDecimal moddStampDutyCap = new BigDecimal("50000.00");

    /** Agency's own MODD service charge billed to the customer. */
    private BigDecimal moddServiceCharge = new BigDecimal("3000.00");

    /** GST percent applied on bank processing fees in comparisons. */
    private BigDecimal processingFeeGstPercent = new BigDecimal("18.00");

    /** Proposals shared with a customer stay valid for this many days. */
    private Integer proposalValidityDays = 15;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTradeName() { return tradeName; }
    public void setTradeName(String tradeName) { this.tradeName = tradeName; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDsaCode() { return dsaCode; }
    public void setDsaCode(String dsaCode) { this.dsaCode = dsaCode; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public String getPan() { return pan; }
    public void setPan(String pan) { this.pan = pan; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public String getLeadPrefix() { return leadPrefix; }
    public void setLeadPrefix(String leadPrefix) { this.leadPrefix = leadPrefix; }

    public String getCustomerPrefix() { return customerPrefix; }
    public void setCustomerPrefix(String customerPrefix) { this.customerPrefix = customerPrefix; }

    public String getApplicationPrefix() { return applicationPrefix; }
    public void setApplicationPrefix(String applicationPrefix) { this.applicationPrefix = applicationPrefix; }

    public String getReferralPrefix() { return referralPrefix; }
    public void setReferralPrefix(String referralPrefix) { this.referralPrefix = referralPrefix; }

    public String getProposalPrefix() { return proposalPrefix; }
    public void setProposalPrefix(String proposalPrefix) { this.proposalPrefix = proposalPrefix; }

    public String getReceiptPrefix() { return receiptPrefix; }
    public void setReceiptPrefix(String receiptPrefix) { this.receiptPrefix = receiptPrefix; }

    public String getAccentColor() { return accentColor == null ? "indigo" : accentColor; }
    public void setAccentColor(String accentColor) { this.accentColor = accentColor; }

    public BigDecimal getDefaultReferralPercent() { return defaultReferralPercent; }
    public void setDefaultReferralPercent(BigDecimal defaultReferralPercent) { this.defaultReferralPercent = defaultReferralPercent; }

    public BigDecimal getReferralTdsPercent() { return referralTdsPercent; }
    public void setReferralTdsPercent(BigDecimal referralTdsPercent) { this.referralTdsPercent = referralTdsPercent; }

    public BigDecimal getModdStampDutyPercent() { return moddStampDutyPercent; }
    public void setModdStampDutyPercent(BigDecimal moddStampDutyPercent) { this.moddStampDutyPercent = moddStampDutyPercent; }

    public BigDecimal getModdStampDutyCap() { return moddStampDutyCap; }
    public void setModdStampDutyCap(BigDecimal moddStampDutyCap) { this.moddStampDutyCap = moddStampDutyCap; }

    public BigDecimal getModdServiceCharge() { return moddServiceCharge; }
    public void setModdServiceCharge(BigDecimal moddServiceCharge) { this.moddServiceCharge = moddServiceCharge; }

    public BigDecimal getProcessingFeeGstPercent() { return processingFeeGstPercent; }
    public void setProcessingFeeGstPercent(BigDecimal processingFeeGstPercent) { this.processingFeeGstPercent = processingFeeGstPercent; }

    public Integer getProposalValidityDays() { return proposalValidityDays == null ? 15 : proposalValidityDays; }
    public void setProposalValidityDays(Integer proposalValidityDays) { this.proposalValidityDays = proposalValidityDays; }

    @jakarta.persistence.Transient
    public String getDisplayName() {
        return tradeName != null && !tradeName.isBlank() ? tradeName : name;
    }
}
