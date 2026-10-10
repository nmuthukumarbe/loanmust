package com.loanmust.loan.advisor;

import com.loanmust.loan.customer.EmploymentType;
import com.loanmust.loan.lender.LoanProduct;
import com.loanmust.loan.lender.LoanType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoanAdvisorTest {

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    private static LoanProduct product(long id, String lender, String min, String max, String fee, String foir,
                                       int minCibil, String women) {
        LoanProduct p = new LoanProduct();
        p.setId(id);
        p.setLenderId(id);
        p.setLenderName(lender);
        p.setName(lender + " Home Loan");
        p.setLoanType(LoanType.HOME_LOAN);
        p.setRateMin(bd(min));
        p.setRateMax(bd(max));
        p.setProcessingFeePercent(bd(fee));
        p.setMaxLtvPercent(bd("90"));
        p.setMaxFoirPercent(bd(foir));
        p.setMinCibil(minCibil);
        p.setBestRateCibil(800);
        p.setMaxTenureYears(30);
        p.setMaxAgeAtMaturity(70);
        p.setWomenConcession(women == null ? null : bd(women));
        p.setActive(true);
        p.setRateUpdatedOn(LocalDate.now());
        return p;
    }

    private static List<LoanProduct> panel() {
        return List.of(
                product(1, "SBI", "8.25", "9.15", "0.35", "60", 650, "0.05"),
                product(2, "HDFC", "8.40", "9.60", "0.50", "55", 700, null),
                product(3, "ICICI", "8.50", "9.80", "0.50", "55", 725, null),
                product(4, "Bajaj", "8.50", "11.00", "0.50", "65", 700, null));
    }

    private static AdvisorRequest strongProfile() {
        AdvisorRequest r = new AdvisorRequest();
        r.setLoanType(LoanType.HOME_LOAN);
        r.setLoanAmount(bd("6000000"));
        r.setTenureYears(25);
        r.setPropertyValue(bd("8200000"));
        r.setMonthlyIncome(bd("120000"));
        r.setExistingEmi(BigDecimal.ZERO);
        r.setCibilScore(776);
        r.setEmploymentType(EmploymentType.SALARIED);
        r.setApplicantAge(34);
        return r;
    }

    @Test
    void recommendsTheCheapestEligibleLoan() {
        AdvisorOutcome o = new LoanAdvisor(bd("18")).compare(strongProfile(), panel(), p -> null);
        assertEquals(4, o.getEligibleCount());
        assertNotNull(o.getRecommended());
        assertEquals("SBI", o.getRecommended().getLenderName());
        // ranked by total cost, ascending
        for (int i = 1; i < o.getOffers().size(); i++) {
            assertTrue(o.getOffers().get(i - 1).getTotalCost().compareTo(o.getOffers().get(i).getTotalCost()) <= 0);
        }
        assertFalse(o.getRecommendationReasons().isEmpty());
        assertEquals(25, o.getSchedule().size());
    }

    @Test
    void agencyPayoutNeverChangesTheRecommendation() {
        // Pay the agency far more on the costliest lender - the advice must not move.
        AdvisorOutcome o = new LoanAdvisor(bd("18")).compare(strongProfile(), panel(),
                p -> "Bajaj".equals(p.getLenderName()) ? bd("2.00") : bd("0.10"));
        assertEquals("SBI", o.getRecommended().getLenderName());
    }

    @Test
    void creditScoreBandsTheRateAndWomenGetTheConcession() {
        AdvisorRequest r = strongProfile();
        r.setCibilScore(810);
        r.setWomanApplicant(true);
        LoanOffer sbi = new LoanAdvisor(null).evaluate(r, panel().get(0), null);
        assertEquals(bd("8.20"), sbi.getApplicableRate());   // best rate 8.25 less 0.05

        r.setWomanApplicant(false);
        r.setCibilScore(null);
        assertEquals(bd("9.15"), new LoanAdvisor(null).evaluate(r, panel().get(0), null).getApplicableRate());
    }

    @Test
    void scoreBelowCutoffIsNotEligibleWithAReason() {
        AdvisorRequest r = strongProfile();
        r.setCibilScore(710);
        LoanOffer icici = new LoanAdvisor(null).evaluate(r, panel().get(2), null);
        assertEquals(OfferStatus.NOT_ELIGIBLE, icici.getStatus());
        assertTrue(icici.getReasons().get(0).contains("725"));
    }

    @Test
    void lowIncomeGivesAReducedEligibleAmount() {
        AdvisorRequest r = strongProfile();
        r.setMonthlyIncome(bd("60000"));
        r.setExistingEmi(bd("10000"));
        AdvisorOutcome o = new LoanAdvisor(null).compare(r, panel(), null);
        LoanOffer best = o.getRecommended();
        assertEquals(OfferStatus.ELIGIBLE_REDUCED, best.getStatus());
        assertTrue(best.getMaxEligibleAmount().compareTo(r.getLoanAmount()) < 0);
        // the one that can fund the most is recommended when nobody funds it all
        for (LoanOffer other : o.getOffers()) {
            if (other.isEligible()) assertTrue(best.getMaxEligibleAmount().compareTo(other.getMaxEligibleAmount()) >= 0);
        }
    }

    @Test
    void rbiLtvCeilingAppliesToLargeHousingLoans() {
        assertEquals(bd("90"), LoanAdvisor.regulatoryLtv(LoanType.HOME_LOAN, bd("3000000")));
        assertEquals(bd("80"), LoanAdvisor.regulatoryLtv(LoanType.HOME_LOAN, bd("7500000")));
        assertEquals(bd("75"), LoanAdvisor.regulatoryLtv(LoanType.HOME_LOAN, bd("7500001")));
        assertNull(LoanAdvisor.regulatoryLtv(LoanType.LAP, bd("9000000")));

        AdvisorRequest r = strongProfile();
        r.setLoanAmount(bd("9000000"));
        r.setPropertyValue(bd("11000000"));
        r.setMonthlyIncome(bd("400000"));
        LoanOffer o = new LoanAdvisor(null).evaluate(r, panel().get(0), null);
        assertEquals(bd("8250000"), o.getMaxEligibleAmount());   // 75% of 1.1 Cr
        assertEquals(OfferStatus.ELIGIBLE_REDUCED, o.getStatus());
    }

    @Test
    void tenureEndsByMaximumAge() {
        AdvisorRequest r = strongProfile();
        r.setApplicantAge(58);
        LoanOffer o = new LoanAdvisor(null).evaluate(r, panel().get(0), null);
        assertEquals(12 * 12, o.getTenureMonths());
    }

    @Test
    void processingFeeRespectsMinimumAndCap() {
        LoanProduct p = panel().get(0);
        p.setProcessingFeeMin(bd("2000"));
        p.setProcessingFeeMax(bd("10000"));
        LoanAdvisor a = new LoanAdvisor(null);
        assertEquals(bd("2000"), a.processingFee(bd("300000"), p));     // 0.35% = 1050 → minimum
        assertEquals(bd("10000"), a.processingFee(bd("6000000"), p));   // 0.35% = 21000 → cap
    }
}
