package com.loanmust.loan.referral;

/** How a referrer is paid when a referred loan disburses. */
public enum PayoutBasis {
    PERCENT_OF_DISBURSAL("% of disbursed amount"),
    FLAT_PER_FILE("Flat amount per disbursed file");

    private final String label;

    PayoutBasis(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
