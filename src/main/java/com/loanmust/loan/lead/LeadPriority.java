package com.loanmust.loan.lead;

public enum LeadPriority {
    HOT("Hot", "rose"),
    WARM("Warm", "amber"),
    COLD("Cold", "slate");

    private final String label;
    private final String tone;

    LeadPriority(String label, String tone) {
        this.label = label;
        this.tone = tone;
    }

    public String getLabel() { return label; }
    public String getTone() { return tone; }
}
