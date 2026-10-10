package com.loanmust.loan.finance;

/** Service charges the agency bills a customer directly (separate from bank payouts). */
public enum FeeType {
    LOGIN_FEE("File login / service fee"),
    MODD_CHARGES("MODD charges (incl. stamp duty)"),
    LEGAL_OPINION("Legal opinion / title search"),
    VALUATION("Valuation"),
    DOCUMENTATION("Documentation support"),
    OTHER("Other");

    private final String label;

    FeeType(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
