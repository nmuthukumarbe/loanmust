package com.loanmust.loan.common;

/** How a contact with a lead, customer or referrer happened. */
public enum Channel {
    CALL("Call", "bi-telephone"),
    WHATSAPP("WhatsApp", "bi-whatsapp"),
    VISIT("Visit / meeting", "bi-geo-alt"),
    EMAIL("Email", "bi-envelope"),
    SMS("SMS", "bi-chat-text");

    private final String label;
    private final String icon;

    Channel(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String getLabel() { return label; }
    public String getIcon() { return icon; }
}
