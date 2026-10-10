package com.loanmust.loan.advisor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** The full result of one comparison: ranked offers, the recommendation and why. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdvisorOutcome {

    private AdvisorRequest request;
    private List<LoanOffer> offers = new ArrayList<>();
    private LoanOffer recommended;
    private List<String> recommendationReasons = new ArrayList<>();
    private List<LoanMath.YearRow> schedule = new ArrayList<>();
    private int eligibleCount;
    private int evaluatedCount;
    private BigDecimal lowestRate;
    private BigDecimal highestEligibleAmount;
    private LocalDateTime generatedAt = LocalDateTime.now();

    public AdvisorRequest getRequest() { return request; }
    public void setRequest(AdvisorRequest request) { this.request = request; }

    public List<LoanOffer> getOffers() { return offers; }
    public void setOffers(List<LoanOffer> offers) { this.offers = offers; }

    public LoanOffer getRecommended() { return recommended; }
    public void setRecommended(LoanOffer recommended) { this.recommended = recommended; }

    public List<String> getRecommendationReasons() { return recommendationReasons; }
    public void setRecommendationReasons(List<String> recommendationReasons) { this.recommendationReasons = recommendationReasons; }

    public List<LoanMath.YearRow> getSchedule() { return schedule; }
    public void setSchedule(List<LoanMath.YearRow> schedule) { this.schedule = schedule; }

    public int getEligibleCount() { return eligibleCount; }
    public void setEligibleCount(int eligibleCount) { this.eligibleCount = eligibleCount; }

    public int getEvaluatedCount() { return evaluatedCount; }
    public void setEvaluatedCount(int evaluatedCount) { this.evaluatedCount = evaluatedCount; }

    public BigDecimal getLowestRate() { return lowestRate; }
    public void setLowestRate(BigDecimal lowestRate) { this.lowestRate = lowestRate; }

    public BigDecimal getHighestEligibleAmount() { return highestEligibleAmount; }
    public void setHighestEligibleAmount(BigDecimal highestEligibleAmount) { this.highestEligibleAmount = highestEligibleAmount; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public boolean isHasRecommendation() { return recommended != null; }
}
