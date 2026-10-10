package com.loanmust.loan.customer;

public enum EmploymentType {
    SALARIED("Salaried"),
    SELF_EMPLOYED_PROFESSIONAL("Self-employed professional"),
    SELF_EMPLOYED_BUSINESS("Self-employed business"),
    PENSIONER("Pensioner"),
    NRI("NRI");

    private final String label;

    EmploymentType(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }

    public boolean isSelfEmployed() {
        return this == SELF_EMPLOYED_PROFESSIONAL || this == SELF_EMPLOYED_BUSINESS;
    }
}
