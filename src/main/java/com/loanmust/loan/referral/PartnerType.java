package com.loanmust.loan.referral;

public enum PartnerType {
    CUSTOMER("Existing customer"),
    INDIVIDUAL("Individual"),
    LIC_AGENT("LIC agent"),
    BUILDER_SALES("Builder sales team"),
    CHANNEL_PARTNER("Channel partner / sub-DSA"),
    CA_PROFESSIONAL("CA / tax consultant"),
    EMPLOYEE("Employee");

    private final String label;

    PartnerType(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
