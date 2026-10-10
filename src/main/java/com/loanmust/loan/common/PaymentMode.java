package com.loanmust.loan.common;

public enum PaymentMode {
    UPI("UPI"),
    NEFT("NEFT / RTGS"),
    IMPS("IMPS"),
    CHEQUE("Cheque"),
    CASH("Cash");

    private final String label;

    PaymentMode(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
