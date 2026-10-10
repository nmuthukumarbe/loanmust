package com.loanmust.loan.lender;

public enum LenderType {
    PSU_BANK("Public sector bank"),
    PRIVATE_BANK("Private bank"),
    HFC("Housing finance company"),
    NBFC("NBFC"),
    SMALL_FINANCE_BANK("Small finance bank"),
    COOPERATIVE_BANK("Co-operative bank");

    private final String label;

    LenderType(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
