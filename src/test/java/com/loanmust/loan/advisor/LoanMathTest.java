package com.loanmust.loan.advisor;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoanMathTest {

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    @Test
    void emiMatchesBankCalculators() {
        // ₹50 lakh @ 8.5% for 20 years - the figure every Indian bank calculator shows
        assertEquals(bd("43391"), LoanMath.emi(bd("5000000"), bd("8.5"), 240));
        // ₹10 lakh @ 10% for 5 years
        assertEquals(bd("21247"), LoanMath.emi(bd("1000000"), bd("10"), 60));
    }

    @Test
    void zeroRateIsStraightLine() {
        assertEquals(bd("10000"), LoanMath.emi(bd("1200000"), BigDecimal.ZERO, 120));
    }

    @Test
    void principalForIsTheInverseOfEmiRoundedDownToThousand() {
        BigDecimal p = LoanMath.principalFor(bd("43391"), bd("8.5"), 240);
        assertEquals(bd("4999000"), p);
        assertTrue(LoanMath.emi(p, bd("8.5"), 240).compareTo(bd("43391")) <= 0);
    }

    @Test
    void totalInterestIsEmiTimesMonthsLessPrincipal() {
        assertEquals(bd("5413840"), LoanMath.totalInterest(bd("5000000"), bd("8.5"), 240));
    }

    @Test
    void scheduleFullyAmortises() {
        List<LoanMath.YearRow> rows = LoanMath.yearlySchedule(bd("5000000"), bd("8.5"), 240);
        assertEquals(20, rows.size());
        BigDecimal principal = rows.stream().map(LoanMath.YearRow::getPrincipal).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, principal.compareTo(bd("5000000")));
        assertEquals(0, rows.get(rows.size() - 1).getClosing().signum());
    }
}
