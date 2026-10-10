package com.loanmust.loan.modd;

import java.util.List;

/**
 * Memorandum of Deposit of Title Deeds - the equitable mortgage registered at the
 * sub-registrar before (or alongside) disbursement, after which the original title deeds
 * are deposited with the lender.
 */
public enum ModdStatus {
    NOT_STARTED("Not started", "slate"),
    DOCS_COLLECTED("Originals collected", "brand"),
    DRAFT_READY("Draft & stamp duty ready", "indigo"),
    APPOINTMENT_BOOKED("Sub-registrar appointment booked", "violet"),
    REGISTERED("Registered", "amber"),
    DEPOSITED_WITH_BANK("Originals deposited with bank", "emerald"),
    COMPLETED("Completed", "emerald"),
    CANCELLED("Cancelled", "rose");

    private final String label;
    private final String tone;

    ModdStatus(String label, String tone) {
        this.label = label;
        this.tone = tone;
    }

    public String getLabel() { return label; }
    public String getTone() { return tone; }

    public boolean isOpen() {
        return this != COMPLETED && this != CANCELLED;
    }

    public static List<ModdStatus> path() {
        return List.of(NOT_STARTED, DOCS_COLLECTED, DRAFT_READY, APPOINTMENT_BOOKED, REGISTERED, DEPOSITED_WITH_BANK, COMPLETED);
    }
}
