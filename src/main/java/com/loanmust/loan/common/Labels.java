package com.loanmust.loan.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Indian-style number formatting for screens, messages and Excel (12,34,567.00). */
public final class Labels {

    private Labels() {
    }

    /**
     * Whole rupees with Indian digit grouping - 12,34,56,789. Done by hand because
     * java.text.DecimalFormat supports only one grouping size and cannot produce lakh/crore.
     */
    public static String inr(BigDecimal value) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value.setScale(0, RoundingMode.HALF_UP);
        String digits = v.abs().toPlainString();
        StringBuilder out = new StringBuilder();
        int n = digits.length();
        if (n <= 3) {
            out.append(digits);
        } else {
            String head = digits.substring(0, n - 3);
            String tail = digits.substring(n - 3);
            StringBuilder h = new StringBuilder();
            for (int i = head.length(); i > 0; i -= 2) {
                int start = Math.max(0, i - 2);
                h.insert(0, head.substring(start, i));
                if (start > 0) h.insert(0, ',');
            }
            out.append(h).append(',').append(tail);
        }
        return (v.signum() < 0 ? "-" : "") + out;
    }

    /** Short form: 45.5 L, 1.2 Cr. */
    public static String lakhs(BigDecimal value) {
        if (value == null) return "0";
        BigDecimal abs = value.abs();
        if (abs.compareTo(new BigDecimal("10000000")) >= 0) {
            return value.divide(new BigDecimal("10000000"), 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + " Cr";
        }
        if (abs.compareTo(new BigDecimal("100000")) >= 0) {
            return value.divide(new BigDecimal("100000"), 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + " L";
        }
        return inr(value);
    }
}
