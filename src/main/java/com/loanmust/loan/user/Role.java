package com.loanmust.loan.user;

/**
 * Staff roles of a loan agency (DSA). Spring Security authorities are "ROLE_" + name().
 *
 *  OWNER          - agency owner; full access including settings, users, money.
 *  ADMIN          - office administrator; masters, users, audit.
 *  MANAGER        - runs the sales and processing desk; sees every lead and file,
 *                   approves referral payouts.
 *  LOAN_EXECUTIVE - field sales; works only the leads and loan files assigned to them.
 *  PROCESSING     - document collection, bank login, sanction, MODD, disbursement.
 *  DATA_ENTRY     - back-office data entry: leads, customers, documents. No money screens.
 *  FINANCE        - bank payouts, referral payouts, fee receipts.
 */
public enum Role {
    OWNER("Owner"),
    ADMIN("Admin"),
    MANAGER("Manager"),
    LOAN_EXECUTIVE("Loan Executive"),
    PROCESSING("Processing"),
    DATA_ENTRY("Data Entry"),
    FINANCE("Finance");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String authority() {
        return "ROLE_" + name();
    }

    /** Owner and Admin may reach administration screens. */
    public boolean isAdministrative() {
        return this == OWNER || this == ADMIN;
    }

    /** Roles that own leads and loan files and appear in "assign to" pickers. */
    public boolean isSales() {
        return this == OWNER || this == MANAGER || this == LOAN_EXECUTIVE;
    }

    /** Roles that may see money: payouts, referral payments, fee receipts. */
    public boolean isMoney() {
        return this == OWNER || this == ADMIN || this == FINANCE || this == MANAGER;
    }
}
