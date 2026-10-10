package com.loanmust.loan.advisor;

/** How a lender scheme fits the customer. Order matters: it is the ranking order. */
public enum OfferStatus {
    ELIGIBLE("Eligible", "emerald"),
    ELIGIBLE_REDUCED("Eligible for a lower amount", "amber"),
    NOT_ELIGIBLE("Not eligible", "rose");

    private final String label;
    private final String tone;

    OfferStatus(String label, String tone) {
        this.label = label;
        this.tone = tone;
    }

    public String getLabel() { return label; }
    public String getTone() { return tone; }
}
