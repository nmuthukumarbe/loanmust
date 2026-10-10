package com.loanmust.loan.application;

public enum DisbursementStatus {
    NOT_STARTED("Not disbursed", "slate"),
    PARTIAL("Part disbursed", "amber"),
    FULL("Fully disbursed", "emerald");

    private final String label;
    private final String tone;

    DisbursementStatus(String label, String tone) {
        this.label = label;
        this.tone = tone;
    }

    public String getLabel() { return label; }
    public String getTone() { return tone; }
}
