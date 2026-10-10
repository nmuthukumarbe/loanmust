package com.loanmust.loan.builder;

public enum ProjectStatus {
    PRE_LAUNCH("Pre-launch", "violet"),
    UNDER_CONSTRUCTION("Under construction", "amber"),
    READY_TO_MOVE("Ready to move", "emerald"),
    COMPLETED("Sold out / completed", "slate");

    private final String label;
    private final String tone;

    ProjectStatus(String label, String tone) {
        this.label = label;
        this.tone = tone;
    }

    public String getLabel() { return label; }
    public String getTone() { return tone; }
}
