package com.loanmust.loan.application;

import java.util.List;

/**
 * The life of a loan file at an Indian bank / HFC, in order. REJECTED and WITHDRAWN are
 * terminal side-exits; every other stage sits on the pipeline board.
 */
public enum ApplicationStage {
    DOCS_PENDING("Collecting documents", "slate", "bi-folder2"),
    LOGGED_IN("Logged in with bank", "brand", "bi-box-arrow-in-right"),
    CREDIT_REVIEW("Credit appraisal / PD", "indigo", "bi-person-check"),
    TECHNICAL_LEGAL("Technical & legal", "violet", "bi-house-check"),
    SANCTIONED("Sanctioned", "amber", "bi-patch-check"),
    MODD_AGREEMENT("Agreement & MODD", "amber", "bi-file-earmark-lock"),
    DISBURSED("Disbursed", "emerald", "bi-cash-coin"),
    REJECTED("Rejected", "rose", "bi-x-octagon"),
    WITHDRAWN("Withdrawn", "slate", "bi-slash-circle");

    private final String label;
    private final String tone;
    private final String icon;

    ApplicationStage(String label, String tone, String icon) {
        this.label = label;
        this.tone = tone;
        this.icon = icon;
    }

    public String getLabel() { return label; }
    public String getTone() { return tone; }
    public String getIcon() { return icon; }

    public boolean isOpen() {
        return this != DISBURSED && this != REJECTED && this != WITHDRAWN;
    }

    public boolean isTerminalFailure() {
        return this == REJECTED || this == WITHDRAWN;
    }

    /** The forward path shown on the progress bar and the pipeline board. */
    public static List<ApplicationStage> path() {
        return List.of(DOCS_PENDING, LOGGED_IN, CREDIT_REVIEW, TECHNICAL_LEGAL, SANCTIONED, MODD_AGREEMENT, DISBURSED);
    }

    public static List<ApplicationStage> openStages() {
        return List.of(DOCS_PENDING, LOGGED_IN, CREDIT_REVIEW, TECHNICAL_LEGAL, SANCTIONED, MODD_AGREEMENT);
    }
}
