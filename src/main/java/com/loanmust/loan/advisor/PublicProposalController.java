package com.loanmust.loan.advisor;

import com.loanmust.loan.tenant.Agency;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * The customer-facing proposal at /p/{token}: every lender compared, the recommendation and
 * the reasons. Never shows the agency's payout. No sign-in - the token is the credential.
 */
@Controller
public class PublicProposalController {

    private final LoanAdvisorService advisorService;

    public PublicProposalController(LoanAdvisorService advisorService) {
        this.advisorService = advisorService;
    }

    @GetMapping("/p/{token}")
    public String view(@PathVariable String token, Model model) {
        // A signed-in staff member previewing the link is not the customer opening it.
        boolean staff = com.loanmust.loan.security.SecurityUtils.currentUser() != null;
        Proposal p = advisorService.openPublic(token, !staff);
        AdvisorOutcome outcome = advisorService.outcomeOf(p);
        if (p == null || outcome == null) {
            return "public/link-expired";
        }
        // Defence in depth: the agency's payout never leaves the building.
        for (LoanOffer o : outcome.getOffers()) {
            o.setPayoutPercent(null);
            o.setPayoutAmount(null);
        }
        if (outcome.getRecommended() != null) {
            outcome.getRecommended().setPayoutPercent(null);
            outcome.getRecommended().setPayoutAmount(null);
        }
        Agency agency = advisorService.agency(p.getTenantId());
        model.addAttribute("proposal", p);
        model.addAttribute("outcome", outcome);
        model.addAttribute("agency", agency);
        return "public/proposal";
    }
}
