package com.loanmust.loan.lender;

/** The kinds of loan the agency places. "Secured" types are backed by property (LTV, MODD apply). */
public enum LoanType {
    HOME_LOAN("Home loan", true),
    PLOT_LOAN("Plot loan", true),
    CONSTRUCTION_LOAN("Construction loan", true),
    HOME_IMPROVEMENT("Home improvement", true),
    BALANCE_TRANSFER("Balance transfer", true),
    TOP_UP("Top-up loan", true),
    LAP("Loan against property", true),
    PERSONAL_LOAN("Personal loan", false),
    BUSINESS_LOAN("Business loan", false),
    VEHICLE_LOAN("Vehicle loan", false);

    private final String label;
    private final boolean secured;

    LoanType(String label, boolean secured) {
        this.label = label;
        this.secured = secured;
    }

    public String getLabel() { return label; }

    public boolean isSecured() { return secured; }
}
