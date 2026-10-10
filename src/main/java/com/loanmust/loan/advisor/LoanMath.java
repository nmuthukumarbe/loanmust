package com.loanmust.loan.advisor;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Reducing-balance loan arithmetic, the way Indian lenders quote it: interest compounds
 * monthly at (annual rate / 12), EMI = P·r·(1+r)^n / ((1+r)^n − 1).
 *
 * All maths is BigDecimal at 16 significant digits; results are rounded to whole rupees
 * only at the end, as the lender's own EMI letters do.
 */
public final class LoanMath {

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal TWELVE_HUNDRED = new BigDecimal("1200");

    private LoanMath() {
    }

    /** Monthly rate as a fraction: 8.5% p.a. → 0.0070833… */
    static BigDecimal monthlyRate(BigDecimal annualPercent) {
        return annualPercent.divide(TWELVE_HUNDRED, MC);
    }

    /** EMI rounded to the rupee. Zero rate degenerates to straight-line repayment. */
    public static BigDecimal emi(BigDecimal principal, BigDecimal annualPercent, int months) {
        if (principal == null || principal.signum() <= 0 || months <= 0) return BigDecimal.ZERO;
        if (annualPercent == null || annualPercent.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(months), 0, RoundingMode.HALF_UP);
        }
        BigDecimal r = monthlyRate(annualPercent);
        BigDecimal pow = BigDecimal.ONE.add(r).pow(months, MC);
        BigDecimal emi = principal.multiply(r, MC).multiply(pow, MC).divide(pow.subtract(BigDecimal.ONE), MC);
        return emi.setScale(0, RoundingMode.HALF_UP);
    }

    /** The largest principal a given EMI can service: P = EMI·((1+r)^n − 1) / (r·(1+r)^n). */
    public static BigDecimal principalFor(BigDecimal emi, BigDecimal annualPercent, int months) {
        if (emi == null || emi.signum() <= 0 || months <= 0) return BigDecimal.ZERO;
        if (annualPercent == null || annualPercent.signum() == 0) {
            return emi.multiply(BigDecimal.valueOf(months));
        }
        BigDecimal r = monthlyRate(annualPercent);
        BigDecimal pow = BigDecimal.ONE.add(r).pow(months, MC);
        BigDecimal p = emi.multiply(pow.subtract(BigDecimal.ONE), MC).divide(r.multiply(pow, MC), MC);
        // round DOWN to the nearest thousand, the way eligibility is quoted
        return p.divide(new BigDecimal("1000"), 0, RoundingMode.FLOOR).multiply(new BigDecimal("1000"));
    }

    /** Total interest over the full term at the (rounded) EMI. */
    public static BigDecimal totalInterest(BigDecimal principal, BigDecimal annualPercent, int months) {
        BigDecimal emi = emi(principal, annualPercent, months);
        BigDecimal interest = emi.multiply(BigDecimal.valueOf(months)).subtract(principal == null ? BigDecimal.ZERO : principal);
        return interest.signum() < 0 ? BigDecimal.ZERO : interest.setScale(0, RoundingMode.HALF_UP);
    }

    /** Year-by-year amortisation summary (the last EMI absorbs rounding). */
    public static List<YearRow> yearlySchedule(BigDecimal principal, BigDecimal annualPercent, int months) {
        List<YearRow> rows = new ArrayList<>();
        if (principal == null || principal.signum() <= 0 || months <= 0) return rows;
        BigDecimal emi = emi(principal, annualPercent, months);
        BigDecimal r = annualPercent == null ? BigDecimal.ZERO : monthlyRate(annualPercent);
        BigDecimal balance = principal;
        int year = 1;
        BigDecimal yearOpening = balance, yearPrincipal = BigDecimal.ZERO, yearInterest = BigDecimal.ZERO;
        for (int m = 1; m <= months; m++) {
            BigDecimal interest = balance.multiply(r, MC).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalPart = emi.subtract(interest);
            if (m == months || principalPart.compareTo(balance) > 0) {
                principalPart = balance;
            }
            balance = balance.subtract(principalPart);
            yearPrincipal = yearPrincipal.add(principalPart);
            yearInterest = yearInterest.add(interest);
            if (m % 12 == 0 || m == months) {
                rows.add(new YearRow(year,
                        yearOpening.setScale(0, RoundingMode.HALF_UP),
                        yearPrincipal.setScale(0, RoundingMode.HALF_UP),
                        yearInterest.setScale(0, RoundingMode.HALF_UP),
                        balance.max(BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP)));
                year++;
                yearOpening = balance;
                yearPrincipal = BigDecimal.ZERO;
                yearInterest = BigDecimal.ZERO;
            }
            if (balance.signum() <= 0) break;
        }
        return rows;
    }

    /** One year of the amortisation table. */
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class YearRow {
        private int year;
        private BigDecimal opening;
        private BigDecimal principal;
        private BigDecimal interest;
        private BigDecimal closing;

        public YearRow() {
        }

        public YearRow(int year, BigDecimal opening, BigDecimal principal, BigDecimal interest, BigDecimal closing) {
            this.year = year;
            this.opening = opening;
            this.principal = principal;
            this.interest = interest;
            this.closing = closing;
        }

        public int getYear() { return year; }
        public void setYear(int year) { this.year = year; }
        public BigDecimal getOpening() { return opening; }
        public void setOpening(BigDecimal opening) { this.opening = opening; }
        public BigDecimal getPrincipal() { return principal; }
        public void setPrincipal(BigDecimal principal) { this.principal = principal; }
        public BigDecimal getInterest() { return interest; }
        public void setInterest(BigDecimal interest) { this.interest = interest; }
        public BigDecimal getClosing() { return closing; }
        public void setClosing(BigDecimal closing) { this.closing = closing; }
    }
}
