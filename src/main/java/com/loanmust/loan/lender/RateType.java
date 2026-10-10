package com.loanmust.loan.lender;

public enum RateType {
    FLOATING("Floating"),
    FIXED("Fixed"),
    HYBRID("Hybrid (fixed then floating)");

    private final String label;

    RateType(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
