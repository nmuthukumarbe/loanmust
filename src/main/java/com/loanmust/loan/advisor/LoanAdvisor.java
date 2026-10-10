package com.loanmust.loan.advisor;

import com.loanmust.loan.common.Labels;
import com.loanmust.loan.customer.EmploymentType;
import com.loanmust.loan.lender.LoanProduct;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * The comparison engine behind "show the client every bank and suggest the best one".
 *
 * For each scheme it works out, in this order:
 *   1. the rate this customer would actually get (credit-score band, women's concession);
 *   2. the tenure the lender allows (scheme maximum, age at maturity);
 *   3. EMI, total interest, processing fee + GST, and the total cost of the loan;
 *   4. the maximum loan the lender would sanction - the lower of the FOIR (income) limit,
 *      the LTV (property value) limit and the scheme's ticket size;
 *   5. hard policy checks - employment profile, minimum score, minimum income, minimum ticket.
 *
 * Offers are ranked eligible-first, then by total cost - never by the agency's payout - so
 * the recommendation is always the cheapest loan the customer can actually get. Every
 * verdict carries its reasons, so the advice can be explained to the customer line by line.
 *
 * Pure Java with no Spring dependency, so it is unit-testable in isolation.
 */
public class LoanAdvisor {

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal RATE_STEP = new BigDecimal("0.05");

    private final BigDecimal feeGstPercent;

    public LoanAdvisor(BigDecimal feeGstPercent) {
        this.feeGstPercent = feeGstPercent == null ? new BigDecimal("18") : feeGstPercent;
    }

    /**
     * @param payoutLookup returns the agency payout percent for a scheme (may return null)
     */
    public AdvisorOutcome compare(AdvisorRequest req, List<LoanProduct> products,
                                  Function<LoanProduct, BigDecimal> payoutLookup) {
        AdvisorOutcome outcome = new AdvisorOutcome();
        outcome.setRequest(req);
        if (req == null || !req.isComplete() || products == null) {
            return outcome;
        }
        List<LoanOffer> offers = new ArrayList<>();
        for (LoanProduct p : products) {
            if (p.getLoanType() != req.getLoanType() || !p.isActive()) continue;
            offers.add(evaluate(req, p, payoutLookup == null ? null : payoutLookup.apply(p)));
        }

        offers.sort(Comparator
                .comparing((LoanOffer o) -> o.getStatus().ordinal())
                .thenComparing(o -> o.getStatus() == OfferStatus.ELIGIBLE_REDUCED
                        ? o.getMaxEligibleAmount().negate() : o.getTotalCost())
                .thenComparing(LoanOffer::getApplicableRate)
                .thenComparing(LoanOffer::getLenderName, Comparator.nullsLast(String::compareTo)));

        int rank = 1;
        for (LoanOffer o : offers) o.setRank(rank++);

        outcome.setOffers(offers);
        outcome.setEvaluatedCount(offers.size());
        outcome.setEligibleCount((int) offers.stream().filter(LoanOffer::isEligible).count());
        offers.stream().filter(LoanOffer::isEligible).map(LoanOffer::getApplicableRate)
                .min(BigDecimal::compareTo).ifPresent(outcome::setLowestRate);
        offers.stream().filter(LoanOffer::isEligible).map(LoanOffer::getMaxEligibleAmount)
                .filter(a -> a != null).max(BigDecimal::compareTo).ifPresent(outcome::setHighestEligibleAmount);

        if (!offers.isEmpty() && offers.get(0).isEligible()) {
            LoanOffer best = offers.get(0);
            best.setRecommended(true);
            outcome.setRecommended(best);
            for (LoanOffer o : offers) {
                if (o.isEligible() && o.getTotalCost() != null) {
                    o.setExtraCostVsBest(o.getTotalCost().subtract(best.getTotalCost()).max(BigDecimal.ZERO));
                }
            }
            outcome.setRecommendationReasons(reasonsFor(best, offers, req));
            BigDecimal principal = best.getStatus() == OfferStatus.ELIGIBLE
                    ? req.getLoanAmount() : best.getMaxEligibleAmount();
            outcome.setSchedule(LoanMath.yearlySchedule(principal, best.getApplicableRate(), best.getTenureMonths()));
        }
        return outcome;
    }

    // ------------------------------------------------------------------ one scheme

    LoanOffer evaluate(AdvisorRequest req, LoanProduct p, BigDecimal payoutPercent) {
        LoanOffer o = new LoanOffer();
        o.setProductId(p.getId());
        o.setLenderId(p.getLenderId());
        o.setLenderName(p.getLenderName());
        o.setProductName(p.getName());
        o.setRateTypeLabel(p.getRateType() == null ? null : p.getRateType().getLabel());
        o.setBenchmark(p.getBenchmark());
        o.setBenefits(new ArrayList<>(p.getBenefitList()));
        o.setPrepaymentCharges(p.getPrepaymentCharges());
        o.setRateStale(p.isRateStale());

        BigDecimal amount = req.getLoanAmount();
        boolean hardFail = false;

        // 1. Rate for this customer
        o.setApplicableRate(rateFor(req, p, o));

        // 2. Tenure
        int years = req.getTenureYears();
        if (p.getMaxTenureYears() != null && years > p.getMaxTenureYears()) {
            years = p.getMaxTenureYears();
            o.getNotes().add("Tenure capped at the scheme maximum of " + years + " years");
        }
        if (req.getApplicantAge() != null && p.getMaxAgeAtMaturity() != null) {
            int byAge = p.getMaxAgeAtMaturity() - req.getApplicantAge();
            if (byAge <= 0) {
                o.getReasons().add("Applicant age is above the lender's limit of " + p.getMaxAgeAtMaturity() + " at maturity");
                hardFail = true;
                byAge = 1;
            }
            if (years > byAge) {
                years = byAge;
                o.getNotes().add("Tenure limited to " + years + " years so the loan ends by age " + p.getMaxAgeAtMaturity());
            }
        }
        int months = Math.max(1, years) * 12;
        o.setTenureMonths(months);

        // 3. Cost of the loan
        BigDecimal rate = o.getApplicableRate();
        BigDecimal emi = LoanMath.emi(amount, rate, months);
        BigDecimal interest = LoanMath.totalInterest(amount, rate, months);
        o.setEmi(emi);
        o.setTotalInterest(interest);
        o.setTotalPayable(amount.add(interest));
        BigDecimal fee = processingFee(amount, p);
        BigDecimal gst = fee.multiply(feeGstPercent).divide(HUNDRED, 0, RoundingMode.HALF_UP);
        o.setProcessingFee(fee);
        o.setProcessingFeeGst(gst);
        o.setTotalCost(interest.add(fee).add(gst));

        // 4. How much would the lender give?
        BigDecimal maxEligible = p.getMaxLoanAmount();
        BigDecimal existing = req.getExistingEmi() == null ? BigDecimal.ZERO : req.getExistingEmi();
        if (req.getMonthlyIncome() != null && req.getMonthlyIncome().signum() > 0) {
            o.setFoirPercent(existing.add(emi).multiply(HUNDRED).divide(req.getMonthlyIncome(), 1, RoundingMode.HALF_UP));
            if (p.getMaxFoirPercent() != null) {
                BigDecimal allowedEmi = req.getMonthlyIncome().multiply(p.getMaxFoirPercent())
                        .divide(HUNDRED, 0, RoundingMode.FLOOR).subtract(existing);
                BigDecimal byIncome = allowedEmi.signum() > 0 ? LoanMath.principalFor(allowedEmi, rate, months) : BigDecimal.ZERO;
                if (byIncome.signum() <= 0) {
                    o.getReasons().add("Existing EMIs already use up the " + strip(p.getMaxFoirPercent())
                            + "% of income this lender allows (FOIR)");
                    hardFail = true;
                }
                maxEligible = min(maxEligible, byIncome);
            }
        } else {
            o.getNotes().add("Income not given - eligibility by income not checked");
        }
        if (p.getLoanType() != null && p.getLoanType().isSecured()) {
            if (req.getPropertyValue() != null && req.getPropertyValue().signum() > 0) {
                o.setLtvPercent(amount.multiply(HUNDRED).divide(req.getPropertyValue(), 1, RoundingMode.HALF_UP));
                BigDecimal ltvCap = min(p.getMaxLtvPercent(), regulatoryLtv(p.getLoanType(), amount));
                if (ltvCap != null) {
                    BigDecimal byProperty = req.getPropertyValue().multiply(ltvCap)
                            .divide(HUNDRED, 0, RoundingMode.FLOOR);
                    maxEligible = min(maxEligible, byProperty);
                    if (amount.compareTo(byProperty) > 0) {
                        o.getNotes().add("Lender funds up to " + strip(ltvCap)
                                + "% of property value (LTV) - own contribution needed");
                    }
                }
            } else {
                o.getNotes().add("Property value not given - LTV not checked");
            }
        }
        o.setMaxEligibleAmount(maxEligible == null ? amount : maxEligible.max(BigDecimal.ZERO));

        // 5. Hard credit-policy checks
        EmploymentType emp = req.getEmploymentType();
        if (emp != null && !employmentAllowed(p, emp)) {
            o.getReasons().add("Scheme is not offered to " + emp.getLabel().toLowerCase() + " applicants");
            hardFail = true;
        }
        if (req.getCibilScore() != null && p.getMinCibil() != null && req.getCibilScore() < p.getMinCibil()) {
            o.getReasons().add("CIBIL " + req.getCibilScore() + " is below the lender's cut-off of " + p.getMinCibil());
            hardFail = true;
        }
        if (req.getMonthlyIncome() != null && p.getMinMonthlyIncome() != null
                && req.getMonthlyIncome().compareTo(p.getMinMonthlyIncome()) < 0) {
            o.getReasons().add("Needs a monthly income of at least ₹" + Labels.inr(p.getMinMonthlyIncome()));
            hardFail = true;
        }
        if (p.getMinLoanAmount() != null && amount.compareTo(p.getMinLoanAmount()) < 0) {
            o.getReasons().add("Below the lender's minimum loan of ₹" + Labels.inr(p.getMinLoanAmount()));
            hardFail = true;
        }

        if (hardFail) {
            o.setStatus(OfferStatus.NOT_ELIGIBLE);
        } else if (o.getMaxEligibleAmount().compareTo(amount) < 0) {
            if (o.getMaxEligibleAmount().signum() <= 0) {
                o.setStatus(OfferStatus.NOT_ELIGIBLE);
            } else {
                o.setStatus(OfferStatus.ELIGIBLE_REDUCED);
                o.getReasons().add("Eligible up to ₹" + Labels.inr(o.getMaxEligibleAmount())
                        + " against ₹" + Labels.inr(amount) + " asked");
            }
        } else {
            o.setStatus(OfferStatus.ELIGIBLE);
        }

        if (payoutPercent != null) {
            o.setPayoutPercent(payoutPercent);
            o.setPayoutAmount(amount.multiply(payoutPercent).divide(HUNDRED, 0, RoundingMode.HALF_UP));
        }
        if (p.isRateStale()) {
            o.getNotes().add("Rate not re-confirmed with the lender in the last 30 days");
        }
        return o;
    }

    /**
     * Credit-score banding: the best rate at or above {@code bestRateCibil}, the card's top
     * rate at the minimum cut-off, a straight line in between (rounded to 0.05%). No score
     * given means the top of the card - the advisor never promises a rate it cannot defend.
     */
    BigDecimal rateFor(AdvisorRequest req, LoanProduct p, LoanOffer o) {
        BigDecimal min = p.getRateMin();
        BigDecimal max = p.getRateMax() == null ? min : p.getRateMax();
        BigDecimal rate;
        Integer score = req.getCibilScore();
        int best = p.getBestRateCibil() == null ? 800 : p.getBestRateCibil();
        int cutoff = p.getMinCibil() == null ? 650 : p.getMinCibil();
        if (score == null) {
            rate = max;
            o.setRateNote("Card rate - credit score not given");
        } else if (score >= best || max.compareTo(min) == 0) {
            rate = min;
            o.setRateNote("Best rate for CIBIL " + score);
        } else if (score <= cutoff || best <= cutoff) {
            rate = max;
            o.setRateNote("Card rate for CIBIL " + score);
        } else {
            BigDecimal fraction = BigDecimal.valueOf(best - score).divide(BigDecimal.valueOf(best - cutoff), MC);
            rate = min.add(max.subtract(min).multiply(fraction, MC));
            rate = rate.divide(RATE_STEP, 0, RoundingMode.HALF_UP).multiply(RATE_STEP);
            o.setRateNote("Banded rate for CIBIL " + score);
        }
        if (req.isWomanApplicant() && p.getWomenConcession() != null && p.getWomenConcession().signum() > 0) {
            rate = rate.subtract(p.getWomenConcession()).max(BigDecimal.ZERO);
            o.setRateNote(o.getRateNote() + ", less " + strip(p.getWomenConcession()) + "% women's concession");
        }
        return rate.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * RBI's ceiling on loan-to-value for individual housing loans: 90% up to ₹30 lakh,
     * 80% above ₹30 lakh up to ₹75 lakh, 75% above ₹75 lakh. Applies whatever a lender's
     * own card says. Not applicable to plot loans, LAP or unsecured loans (null).
     */
    static BigDecimal regulatoryLtv(com.loanmust.loan.lender.LoanType type, BigDecimal amount) {
        if (type == null || amount == null) return null;
        switch (type) {
            case HOME_LOAN, CONSTRUCTION_LOAN, HOME_IMPROVEMENT, BALANCE_TRANSFER -> { }
            default -> { return null; }
        }
        if (amount.compareTo(new BigDecimal("3000000")) <= 0) return new BigDecimal("90");
        if (amount.compareTo(new BigDecimal("7500000")) <= 0) return new BigDecimal("80");
        return new BigDecimal("75");
    }

    BigDecimal processingFee(BigDecimal amount, LoanProduct p) {
        if (p.getProcessingFeePercent() == null) {
            return p.getProcessingFeeMin() == null ? BigDecimal.ZERO : p.getProcessingFeeMin();
        }
        BigDecimal fee = amount.multiply(p.getProcessingFeePercent()).divide(HUNDRED, 0, RoundingMode.HALF_UP);
        if (p.getProcessingFeeMin() != null && fee.compareTo(p.getProcessingFeeMin()) < 0) fee = p.getProcessingFeeMin();
        if (p.getProcessingFeeMax() != null && p.getProcessingFeeMax().signum() > 0
                && fee.compareTo(p.getProcessingFeeMax()) > 0) fee = p.getProcessingFeeMax();
        return fee.setScale(0, RoundingMode.HALF_UP);
    }

    static boolean employmentAllowed(LoanProduct p, EmploymentType emp) {
        return switch (emp) {
            case SALARIED -> p.isSalariedAllowed();
            case SELF_EMPLOYED_PROFESSIONAL, SELF_EMPLOYED_BUSINESS -> p.isSelfEmployedAllowed();
            case PENSIONER -> p.isPensionerAllowed();
            case NRI -> p.isNriAllowed();
        };
    }

    private List<String> reasonsFor(LoanOffer best, List<LoanOffer> offers, AdvisorRequest req) {
        List<String> reasons = new ArrayList<>();
        List<LoanOffer> eligible = offers.stream().filter(LoanOffer::isEligible).toList();
        if (best.getStatus() == OfferStatus.ELIGIBLE) {
            reasons.add("Lowest total cost of loan among " + eligible.size() + " eligible offer"
                    + (eligible.size() == 1 ? "" : "s") + " - ₹" + Labels.inr(best.getTotalCost())
                    + " in interest and fees over " + best.getTenureYearsRounded() + " years");
        } else {
            reasons.add("No lender can fund the full ₹" + Labels.inr(req.getLoanAmount())
                    + " on this profile; " + best.getLenderName() + " sanctions the most - up to ₹"
                    + Labels.inr(best.getMaxEligibleAmount()));
        }
        reasons.add("Rate " + strip(best.getApplicableRate()) + "% (" + best.getRateNote() + "), EMI ₹" + Labels.inr(best.getEmi()));
        if (eligible.size() > 1) {
            LoanOffer next = eligible.get(1);
            LoanOffer costliest = eligible.stream()
                    .filter(o -> o.getStatus() == best.getStatus())
                    .max(Comparator.comparing(LoanOffer::getTotalCost)).orElse(next);
            BigDecimal saveNext = next.getTotalCost().subtract(best.getTotalCost());
            if (best.getStatus() == OfferStatus.ELIGIBLE && next.getStatus() == OfferStatus.ELIGIBLE && saveNext.signum() > 0) {
                reasons.add("Saves ₹" + Labels.inr(saveNext) + " against the next best option (" + next.getLenderName() + ")");
            }
            BigDecimal saveMax = costliest.getTotalCost().subtract(best.getTotalCost());
            if (best.getStatus() == OfferStatus.ELIGIBLE && costliest != next && saveMax.signum() > 0) {
                reasons.add("Saves ₹" + Labels.inr(saveMax) + " against the costliest eligible option (" + costliest.getLenderName() + ")");
            }
        }
        if (best.getProcessingFee() != null && best.getProcessingFee().signum() == 0) {
            reasons.add("No processing fee");
        }
        if (!best.getBenefits().isEmpty()) {
            reasons.add("Also offers: " + String.join("; ", best.getBenefits()));
        }
        return reasons;
    }

    private static BigDecimal min(BigDecimal a, BigDecimal b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.min(b);
    }

    private static String strip(BigDecimal v) {
        return v == null ? "" : v.stripTrailingZeros().toPlainString();
    }
}
