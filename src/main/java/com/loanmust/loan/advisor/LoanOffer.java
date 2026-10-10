package com.loanmust.loan.advisor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One lender scheme evaluated for one customer: the rate they would get, the EMI, the full
 * cost of the loan, and - when it does not fit - exactly why. Plain getters/setters so the
 * same object renders in Thymeleaf and round-trips through JSON in a saved proposal.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoanOffer {

    private Long productId;
    private Long lenderId;
    private String lenderName;
    private String lenderColor;
    private String productName;
    private String rateTypeLabel;
    private String benchmark;

    private BigDecimal applicableRate;
    private String rateNote;
    private int tenureMonths;
    private BigDecimal emi;
    private BigDecimal totalInterest;
    private BigDecimal totalPayable;
    private BigDecimal processingFee;
    private BigDecimal processingFeeGst;
    /** Interest + processing fee + GST: what the loan really costs the customer. */
    private BigDecimal totalCost;

    private BigDecimal maxEligibleAmount;
    private BigDecimal ltvPercent;
    private BigDecimal foirPercent;

    private OfferStatus status = OfferStatus.ELIGIBLE;
    private List<String> reasons = new ArrayList<>();
    private List<String> notes = new ArrayList<>();
    private List<String> benefits = new ArrayList<>();
    private String prepaymentCharges;

    /** Agency's own payout on this scheme - shown to staff only, never on a customer proposal. */
    private BigDecimal payoutPercent;
    private BigDecimal payoutAmount;

    private boolean rateStale;
    private int rank;
    private boolean recommended;
    /** Extra cost of this option over the recommended one. */
    private BigDecimal extraCostVsBest;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getLenderId() { return lenderId; }
    public void setLenderId(Long lenderId) { this.lenderId = lenderId; }

    public String getLenderName() { return lenderName; }
    public void setLenderName(String lenderName) { this.lenderName = lenderName; }

    public String getLenderColor() { return lenderColor; }
    public void setLenderColor(String lenderColor) { this.lenderColor = lenderColor; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getRateTypeLabel() { return rateTypeLabel; }
    public void setRateTypeLabel(String rateTypeLabel) { this.rateTypeLabel = rateTypeLabel; }

    public String getBenchmark() { return benchmark; }
    public void setBenchmark(String benchmark) { this.benchmark = benchmark; }

    public BigDecimal getApplicableRate() { return applicableRate; }
    public void setApplicableRate(BigDecimal applicableRate) { this.applicableRate = applicableRate; }

    public String getRateNote() { return rateNote; }
    public void setRateNote(String rateNote) { this.rateNote = rateNote; }

    public int getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(int tenureMonths) { this.tenureMonths = tenureMonths; }

    public BigDecimal getEmi() { return emi; }
    public void setEmi(BigDecimal emi) { this.emi = emi; }

    public BigDecimal getTotalInterest() { return totalInterest; }
    public void setTotalInterest(BigDecimal totalInterest) { this.totalInterest = totalInterest; }

    public BigDecimal getTotalPayable() { return totalPayable; }
    public void setTotalPayable(BigDecimal totalPayable) { this.totalPayable = totalPayable; }

    public BigDecimal getProcessingFee() { return processingFee; }
    public void setProcessingFee(BigDecimal processingFee) { this.processingFee = processingFee; }

    public BigDecimal getProcessingFeeGst() { return processingFeeGst; }
    public void setProcessingFeeGst(BigDecimal processingFeeGst) { this.processingFeeGst = processingFeeGst; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public BigDecimal getMaxEligibleAmount() { return maxEligibleAmount; }
    public void setMaxEligibleAmount(BigDecimal maxEligibleAmount) { this.maxEligibleAmount = maxEligibleAmount; }

    public BigDecimal getLtvPercent() { return ltvPercent; }
    public void setLtvPercent(BigDecimal ltvPercent) { this.ltvPercent = ltvPercent; }

    public BigDecimal getFoirPercent() { return foirPercent; }
    public void setFoirPercent(BigDecimal foirPercent) { this.foirPercent = foirPercent; }

    public OfferStatus getStatus() { return status; }
    public void setStatus(OfferStatus status) { this.status = status; }

    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons; }

    public List<String> getNotes() { return notes; }
    public void setNotes(List<String> notes) { this.notes = notes; }

    public List<String> getBenefits() { return benefits; }
    public void setBenefits(List<String> benefits) { this.benefits = benefits; }

    public String getPrepaymentCharges() { return prepaymentCharges; }
    public void setPrepaymentCharges(String prepaymentCharges) { this.prepaymentCharges = prepaymentCharges; }

    public BigDecimal getPayoutPercent() { return payoutPercent; }
    public void setPayoutPercent(BigDecimal payoutPercent) { this.payoutPercent = payoutPercent; }

    public BigDecimal getPayoutAmount() { return payoutAmount; }
    public void setPayoutAmount(BigDecimal payoutAmount) { this.payoutAmount = payoutAmount; }

    public boolean isRateStale() { return rateStale; }
    public void setRateStale(boolean rateStale) { this.rateStale = rateStale; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public boolean isRecommended() { return recommended; }
    public void setRecommended(boolean recommended) { this.recommended = recommended; }

    public BigDecimal getExtraCostVsBest() { return extraCostVsBest; }
    public void setExtraCostVsBest(BigDecimal extraCostVsBest) { this.extraCostVsBest = extraCostVsBest; }

    public String getStatusLabel() { return status == null ? "" : status.getLabel(); }

    public String getStatusTone() { return status == null ? "slate" : status.getTone(); }

    public boolean isEligible() { return status != null && status != OfferStatus.NOT_ELIGIBLE; }

    public int getTenureYearsRounded() { return (tenureMonths + 11) / 12; }
}
