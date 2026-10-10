package com.loanmust.loan.common;

import java.math.BigDecimal;

/**
 * Template-facing formatter, exposed to every page as {@code fmt}:
 * {@code ${fmt.inr(app.sanctionedAmount)}} renders 45,00,000 and {@code ${fmt.lakhs(x)}} 45 L.
 * Instance methods because Thymeleaf 3.1 blocks static {@code T(...)} access in templates.
 */
public class Fmt {

    public String inr(BigDecimal value) { return Labels.inr(value); }

    public String lakhs(BigDecimal value) { return Labels.lakhs(value); }

    public String pct(BigDecimal value) {
        return value == null ? "—" : value.stripTrailingZeros().toPlainString() + "%";
    }
}
