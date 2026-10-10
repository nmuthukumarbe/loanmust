package com.loanmust.loan.referral;

/** Referral payout workflow: raised on disbursal, approved by a manager, paid by finance. */
public enum PayoutStatus {
    PENDING("Pending approval", "amber"),
    APPROVED("Approved - to pay", "brand"),
    PAID("Paid", "emerald"),
    ON_HOLD("On hold", "slate"),
    CANCELLED("Cancelled", "rose");

    private final String label;
    private final String tone;

    PayoutStatus(String label, String tone) {
        this.label = label;
        this.tone = tone;
    }

    public String getLabel() { return label; }
    public String getTone() { return tone; }
}
