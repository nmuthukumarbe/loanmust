package com.loanmust.loan.common;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LabelsTest {

    @Test
    void indianDigitGrouping() {
        assertEquals("0", Labels.inr(null));
        assertEquals("999", Labels.inr(new BigDecimal("999")));
        assertEquals("1,00,000", Labels.inr(new BigDecimal("100000")));
        assertEquals("83,37,480", Labels.inr(new BigDecimal("8337480")));
        assertEquals("12,34,56,789", Labels.inr(new BigDecimal("123456789")));
        assertEquals("-45,00,001", Labels.inr(new BigDecimal("-4500000.6")));
    }

    @Test
    void lakhsAndCrores() {
        assertEquals("45 L", Labels.lakhs(new BigDecimal("4500000")));
        assertEquals("1.25 Cr", Labels.lakhs(new BigDecimal("12500000")));
    }

    @Test
    void whatsAppLinkAddsIndiaCode() {
        assertEquals("https://wa.me/919845012345?text=Hi%20there", WhatsApp.link("98450 12345", "Hi there"));
    }
}
