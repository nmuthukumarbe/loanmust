package com.loanmust.loan.lead;

/** Working state of an enquiry. Terminal states are CONVERTED, NOT_ELIGIBLE and LOST. */
public enum LeadStatus {
    NEW("New", "slate"),
    CONTACTED("Contacted", "brand"),
    INTERESTED("Interested", "indigo"),
    FOLLOW_UP("Follow-up", "amber"),
    DOCS_REQUESTED("Documents requested", "violet"),
    CONVERTED("Converted to file", "emerald"),
    NOT_ELIGIBLE("Not eligible", "rose"),
    LOST("Lost", "rose");

    private final String label;
    private final String tone;

    LeadStatus(String label, String tone) {
        this.label = label;
        this.tone = tone;
    }

    public String getLabel() { return label; }

    public String getTone() { return tone; }

    public boolean isOpen() {
        return this != CONVERTED && this != NOT_ELIGIBLE && this != LOST;
    }
}
