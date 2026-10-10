package com.loanmust.loan.hr;

public enum AttendanceStatus {
    PRESENT("Present", "emerald", "P"),
    HALF_DAY("Half day", "amber", "H"),
    FIELD_WORK("Field work", "brand", "F"),
    LEAVE("Leave", "violet", "L"),
    ABSENT("Absent", "rose", "A"),
    HOLIDAY("Holiday", "slate", "-");

    private final String label;
    private final String tone;
    private final String shortCode;

    AttendanceStatus(String label, String tone, String shortCode) {
        this.label = label;
        this.tone = tone;
        this.shortCode = shortCode;
    }

    public String getLabel() { return label; }
    public String getTone() { return tone; }
    public String getShortCode() { return shortCode; }

    /** Counts as a paid working day. */
    public double getDayValue() {
        return switch (this) {
            case PRESENT, FIELD_WORK, LEAVE, HOLIDAY -> 1.0;
            case HALF_DAY -> 0.5;
            case ABSENT -> 0.0;
        };
    }
}
