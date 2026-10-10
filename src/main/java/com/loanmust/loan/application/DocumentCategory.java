package com.loanmust.loan.application;

public enum DocumentCategory {
    KYC("KYC"),
    INCOME("Income"),
    BANKING("Banking"),
    PROPERTY("Property"),
    EXISTING_LOAN("Existing loan"),
    SANCTION("Sanction & agreement"),
    OTHER("Other");

    private final String label;

    DocumentCategory(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
