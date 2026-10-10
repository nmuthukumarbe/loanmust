package com.loanmust.loan.application;

public enum DocumentStatus {
    PENDING("Pending", "amber"),
    RECEIVED("Received", "brand"),
    VERIFIED("Verified", "emerald"),
    REJECTED("Re-submit", "rose"),
    NOT_APPLICABLE("Not applicable", "slate");

    private final String label;
    private final String tone;

    DocumentStatus(String label, String tone) {
        this.label = label;
        this.tone = tone;
    }

    public String getLabel() { return label; }
    public String getTone() { return tone; }

    /** Counts toward "documents complete". */
    public boolean isDone() {
        return this == RECEIVED || this == VERIFIED || this == NOT_APPLICABLE;
    }
}
