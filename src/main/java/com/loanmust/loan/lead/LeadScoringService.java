package com.loanmust.loan.lead;

import com.loanmust.loan.customer.EmploymentType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Explainable lead quality score (0-100). Returns the score AND the reasons for it, so a
 * manager can see why a lead ranks hot and an auditor can reproduce the number. Weights
 * reflect what actually gets a loan sanctioned: credit score, repayment capacity, a clear
 * property, and a warm channel (builder / referral) - not just how keen the caller sounded.
 */
@Service
public class LeadScoringService {

    public record Assessment(int score, LeadPriority priority, String suggestedAction, List<String> reasons) {
    }

    public Assessment assess(Lead lead, long activityCount) {
        int score = 10;
        List<String> reasons = new ArrayList<>();

        Integer cibil = lead.getCibilScore();
        if (cibil == null) {
            reasons.add("+0 credit score not known yet");
        } else if (cibil >= 750) {
            score += 25; reasons.add("+25 CIBIL " + cibil + " (strong)");
        } else if (cibil >= 700) {
            score += 15; reasons.add("+15 CIBIL " + cibil + " (acceptable)");
        } else if (cibil >= 650) {
            score += 5; reasons.add("+5 CIBIL " + cibil + " (limited lenders)");
        } else {
            score -= 10; reasons.add("-10 CIBIL " + cibil + " (below most cut-offs)");
        }

        BigDecimal income = lead.getMonthlyIncome();
        BigDecimal amount = lead.getLoanAmountRequired();
        if (income != null && income.signum() > 0 && amount != null && amount.signum() > 0) {
            // rough capacity: 60x net monthly income is a comfortable home-loan multiple
            BigDecimal multiple = amount.divide(income, 1, java.math.RoundingMode.HALF_UP);
            if (multiple.compareTo(new BigDecimal("60")) <= 0) {
                score += 20; reasons.add("+20 loan is " + multiple + "x monthly income (comfortable)");
            } else if (multiple.compareTo(new BigDecimal("90")) <= 0) {
                score += 10; reasons.add("+10 loan is " + multiple + "x monthly income (stretched)");
            } else {
                reasons.add("+0 loan is " + multiple + "x monthly income (likely needs a co-applicant)");
            }
        } else {
            reasons.add("+0 income or loan amount not captured");
        }

        if (lead.getEmploymentType() == EmploymentType.SALARIED) {
            score += 10; reasons.add("+10 salaried (simplest underwriting)");
        } else if (lead.getEmploymentType() != null && lead.getEmploymentType().isSelfEmployed()) {
            score += 5; reasons.add("+5 self-employed (needs ITRs)");
        }

        if (lead.getSource() != null) {
            switch (lead.getSource()) {
                case BUILDER -> { score += 15; reasons.add("+15 builder tie-up (property identified)"); }
                case REFERRAL, EXISTING_CUSTOMER -> { score += 12; reasons.add("+12 referred (warm introduction)"); }
                case LIC -> { score += 10; reasons.add("+10 LIC tie-up"); }
                case WALK_IN -> { score += 8; reasons.add("+8 walked in"); }
                default -> { }
            }
        }
        if (lead.getPropertyValue() != null && lead.getPropertyValue().signum() > 0) {
            score += 5; reasons.add("+5 property value known");
        }
        if (activityCount > 0) {
            int bonus = (int) Math.min(10, activityCount * 3);
            score += bonus; reasons.add("+" + bonus + " " + activityCount + " follow-up(s) logged");
        }
        if (lead.getStatus() == LeadStatus.DOCS_REQUESTED) {
            score += 10; reasons.add("+10 documents already requested");
        } else if (lead.getStatus() == LeadStatus.INTERESTED) {
            score += 5; reasons.add("+5 customer confirmed interest");
        }
        if (lead.getDuplicateOfLeadId() != null) {
            score -= 15; reasons.add("-15 possible duplicate of another open lead");
        }

        score = Math.max(0, Math.min(100, score));
        LeadPriority priority = score >= 65 ? LeadPriority.HOT : score >= 40 ? LeadPriority.WARM : LeadPriority.COLD;
        return new Assessment(score, priority, suggest(lead, priority), reasons);
    }

    private String suggest(Lead lead, LeadPriority priority) {
        if (!lead.isOpen()) return "No action - lead is closed.";
        if (lead.getStatus() == LeadStatus.NEW) return "Call within the hour - first contact converts best.";
        if (lead.getCibilScore() == null) return "Take consent and pull the CIBIL report before comparing lenders.";
        if (lead.getMonthlyIncome() == null) return "Capture net monthly income and existing EMIs to check eligibility.";
        if (lead.getStatus() == LeadStatus.INTERESTED) return "Run Rate Compare and share the proposal on WhatsApp.";
        if (lead.getStatus() == LeadStatus.DOCS_REQUESTED) return "Collect KYC and income documents, then open the loan file.";
        if (lead.getNextFollowUpDate() != null && lead.getNextFollowUpDate().isBefore(LocalDate.now()))
            return "Follow-up is overdue - call today.";
        return priority == LeadPriority.HOT ? "Meet in person and collect documents." : "Keep warm - follow up as scheduled.";
    }
}
