package com.loanmust.loan.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Null-safe BigDecimal helpers. Money is always BigDecimal, scale 2, HALF_UP. */
public final class Money {

    private Money() {
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal of(BigDecimal value) {
        return value == null ? zero() : value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return of(of(a).add(of(b)));
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return of(of(a).subtract(of(b)));
    }

    /** percentage of amount, e.g. percentOf(10000, 12.5) = 1250.00 */
    public static BigDecimal percentOf(BigDecimal amount, BigDecimal percent) {
        return of(of(amount).multiply(of(percent)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}
