package com.loanmust.loan.common;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Builds wa.me "click to chat" links. Sending is always a deliberate tap by a staff member
 * on their own phone or WhatsApp Web - nothing is sent automatically, so no gateway or
 * credentials are needed.
 */
public final class WhatsApp {

    private WhatsApp() {
    }

    public static String link(String mobile, String message) {
        String digits = mobile == null ? "" : mobile.replaceAll("[^0-9]", "");
        if (digits.length() == 10) {
            digits = "91" + digits;   // Indian 10-digit number without country code
        }
        String text = message == null ? "" : URLEncoder.encode(message, StandardCharsets.UTF_8).replace("+", "%20");
        return "https://wa.me/" + digits + (text.isEmpty() ? "" : "?text=" + text);
    }
}
