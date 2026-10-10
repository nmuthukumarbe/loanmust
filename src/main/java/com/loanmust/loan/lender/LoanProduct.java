package com.loanmust.loan.lender;

import com.loanmust.loan.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * One loan scheme of a lender with its current rate card and credit policy - the row the
 * Rate Compare advisor evaluates for each customer. Rates change often, so every product
 * carries the date its rate was last confirmed and the advisor flags stale rates.
 */
@Entity
@Table(name = "loan_products")
public class LoanProduct extends BaseEntity {

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private Long lenderId;
    private String lenderName;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 40)
    private LoanType loanType = LoanType.HOME_LOAN;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 40)
    private RateType rateType = RateType.FLOATING;

    /** Benchmark the rate is linked to, e.g. "RLLR", "EBLR", "MCLR", "PLR". */
    private String benchmark;

    /** Best rate (excellent credit score), percent p.a. */
    @Column(nullable = false)
    private BigDecimal rateMin;

    /** Highest card rate (score at the minimum cut-off), percent p.a. */
    @Column(nullable = false)
    private BigDecimal rateMax;

    /** Concession for a woman (co-)applicant, percentage points, e.g. 0.05. */
    private BigDecimal womenConcession;

    private BigDecimal processingFeePercent;
    private BigDecimal processingFeeMin;
    /** Cap on the processing fee; null means no cap. */
    private BigDecimal processingFeeMax;

    private Integer maxTenureYears = 30;

    /** Maximum loan-to-value, percent of property value (secured loans). */
    private BigDecimal maxLtvPercent;

    /** Maximum FOIR - all EMIs including this one as a percent of net monthly income. */
    private BigDecimal maxFoirPercent;

    private Integer minCibil = 700;

    /** Score at or above which the best rate (rateMin) applies. */
    private Integer bestRateCibil = 800;

    private BigDecimal minMonthlyIncome;
    private BigDecimal minLoanAmount;
    private BigDecimal maxLoanAmount;

    /** The latest age at which the loan must be fully repaid. */
    private Integer maxAgeAtMaturity = 70;

    private boolean salariedAllowed = true;
    private boolean selfEmployedAllowed = true;
    private boolean pensionerAllowed = false;
    private boolean nriAllowed = false;

    @Column(length = 500)
    private String prepaymentCharges;

    /** Customer-facing benefits, one per line. */
    @Column(length = 2000)
    private String keyBenefits;

    /** Agent payout on disbursal for this scheme; null falls back to the lender default. */
    private BigDecimal payoutPercent;

    /** When the rate card was last confirmed with the lender. */
    private LocalDate rateUpdatedOn;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 1000)
    private String notes;

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Long getLenderId() { return lenderId; }
    public void setLenderId(Long lenderId) { this.lenderId = lenderId; }

    public String getLenderName() { return lenderName; }
    public void setLenderName(String lenderName) { this.lenderName = lenderName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LoanType getLoanType() { return loanType; }
    public void setLoanType(LoanType loanType) { this.loanType = loanType; }

    public RateType getRateType() { return rateType; }
    public void setRateType(RateType rateType) { this.rateType = rateType; }

    public String getBenchmark() { return benchmark; }
    public void setBenchmark(String benchmark) { this.benchmark = benchmark; }

    public BigDecimal getRateMin() { return rateMin; }
    public void setRateMin(BigDecimal rateMin) { this.rateMin = rateMin; }

    public BigDecimal getRateMax() { return rateMax; }
    public void setRateMax(BigDecimal rateMax) { this.rateMax = rateMax; }

    public BigDecimal getWomenConcession() { return womenConcession; }
    public void setWomenConcession(BigDecimal womenConcession) { this.womenConcession = womenConcession; }

    public BigDecimal getProcessingFeePercent() { return processingFeePercent; }
    public void setProcessingFeePercent(BigDecimal processingFeePercent) { this.processingFeePercent = processingFeePercent; }

    public BigDecimal getProcessingFeeMin() { return processingFeeMin; }
    public void setProcessingFeeMin(BigDecimal processingFeeMin) { this.processingFeeMin = processingFeeMin; }

    public BigDecimal getProcessingFeeMax() { return processingFeeMax; }
    public void setProcessingFeeMax(BigDecimal processingFeeMax) { this.processingFeeMax = processingFeeMax; }

    public Integer getMaxTenureYears() { return maxTenureYears; }
    public void setMaxTenureYears(Integer maxTenureYears) { this.maxTenureYears = maxTenureYears; }

    public BigDecimal getMaxLtvPercent() { return maxLtvPercent; }
    public void setMaxLtvPercent(BigDecimal maxLtvPercent) { this.maxLtvPercent = maxLtvPercent; }

    public BigDecimal getMaxFoirPercent() { return maxFoirPercent; }
    public void setMaxFoirPercent(BigDecimal maxFoirPercent) { this.maxFoirPercent = maxFoirPercent; }

    public Integer getMinCibil() { return minCibil; }
    public void setMinCibil(Integer minCibil) { this.minCibil = minCibil; }

    public Integer getBestRateCibil() { return bestRateCibil; }
    public void setBestRateCibil(Integer bestRateCibil) { this.bestRateCibil = bestRateCibil; }

    public BigDecimal getMinMonthlyIncome() { return minMonthlyIncome; }
    public void setMinMonthlyIncome(BigDecimal minMonthlyIncome) { this.minMonthlyIncome = minMonthlyIncome; }

    public BigDecimal getMinLoanAmount() { return minLoanAmount; }
    public void setMinLoanAmount(BigDecimal minLoanAmount) { this.minLoanAmount = minLoanAmount; }

    public BigDecimal getMaxLoanAmount() { return maxLoanAmount; }
    public void setMaxLoanAmount(BigDecimal maxLoanAmount) { this.maxLoanAmount = maxLoanAmount; }

    public Integer getMaxAgeAtMaturity() { return maxAgeAtMaturity; }
    public void setMaxAgeAtMaturity(Integer maxAgeAtMaturity) { this.maxAgeAtMaturity = maxAgeAtMaturity; }

    public boolean isSalariedAllowed() { return salariedAllowed; }
    public void setSalariedAllowed(boolean salariedAllowed) { this.salariedAllowed = salariedAllowed; }

    public boolean isSelfEmployedAllowed() { return selfEmployedAllowed; }
    public void setSelfEmployedAllowed(boolean selfEmployedAllowed) { this.selfEmployedAllowed = selfEmployedAllowed; }

    public boolean isPensionerAllowed() { return pensionerAllowed; }
    public void setPensionerAllowed(boolean pensionerAllowed) { this.pensionerAllowed = pensionerAllowed; }

    public boolean isNriAllowed() { return nriAllowed; }
    public void setNriAllowed(boolean nriAllowed) { this.nriAllowed = nriAllowed; }

    public String getPrepaymentCharges() { return prepaymentCharges; }
    public void setPrepaymentCharges(String prepaymentCharges) { this.prepaymentCharges = prepaymentCharges; }

    public String getKeyBenefits() { return keyBenefits; }
    public void setKeyBenefits(String keyBenefits) { this.keyBenefits = keyBenefits; }

    public BigDecimal getPayoutPercent() { return payoutPercent; }
    public void setPayoutPercent(BigDecimal payoutPercent) { this.payoutPercent = payoutPercent; }

    public LocalDate getRateUpdatedOn() { return rateUpdatedOn; }
    public void setRateUpdatedOn(LocalDate rateUpdatedOn) { this.rateUpdatedOn = rateUpdatedOn; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @jakarta.persistence.Transient
    public String getLoanTypeLabel() {
        return loanType == null ? "" : loanType.getLabel();
    }

    @jakarta.persistence.Transient
    public String getRateRange() {
        if (rateMin == null) return "—";
        String min = rateMin.stripTrailingZeros().toPlainString();
        if (rateMax == null || rateMax.compareTo(rateMin) == 0) return min + "%";
        return min + "% – " + rateMax.stripTrailingZeros().toPlainString() + "%";
    }

    @jakarta.persistence.Transient
    public List<String> getBenefitList() {
        if (keyBenefits == null || keyBenefits.isBlank()) return List.of();
        return Arrays.stream(keyBenefits.split("\\r?\\n"))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /** True when the rate card has not been re-confirmed for over 30 days. */
    @jakarta.persistence.Transient
    public boolean isRateStale() {
        return rateUpdatedOn == null || rateUpdatedOn.isBefore(LocalDate.now().minusDays(30));
    }
}
