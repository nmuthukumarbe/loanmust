package com.loanmust.loan.common;

/** Shared payment state used by invoices and commission entries. */
public enum PaymentStatus {
    UNPAID("Unpaid"),
    PARTIAL("Partially paid"),
    PAID("Paid"),
    OVERDUE("Overdue"),
    CANCELLED("Cancelled");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
