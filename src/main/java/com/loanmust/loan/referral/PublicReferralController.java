package com.loanmust.loan.referral;

import com.loanmust.loan.lead.LeadService;
import com.loanmust.loan.lender.LoanType;
import com.loanmust.loan.tenant.Agency;
import com.loanmust.loan.tenant.AgencyRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/**
 * A referral partner's personal page at /refer/{code}. Anyone can submit an enquiry; it
 * lands as a NEW lead tagged to the partner, so the partner's payout is automatic.
 * A hidden honeypot field turns away simple form-filling bots.
 */
@Controller
public class PublicReferralController {

    private final ReferralService referralService;
    private final LeadService leadService;
    private final AgencyRepository agencyRepository;

    public PublicReferralController(ReferralService referralService, LeadService leadService, AgencyRepository agencyRepository) {
        this.referralService = referralService;
        this.leadService = leadService;
        this.agencyRepository = agencyRepository;
    }

    @GetMapping("/refer/{code}")
    public String form(@PathVariable String code, Model model) {
        ReferralPartner partner = referralService.byPublicCode(code);
        if (partner == null) return "public/link-expired";
        model.addAttribute("partner", partner);
        model.addAttribute("agency", agencyRepository.findById(partner.getTenantId()).orElse(null));
        model.addAttribute("loanTypes", LoanType.values());
        return "public/refer";
    }

    @PostMapping("/refer/{code}")
    public String submit(@PathVariable String code,
                         @RequestParam String name, @RequestParam String mobile,
                         @RequestParam(required = false) String city,
                         @RequestParam(required = false) LoanType loanType,
                         @RequestParam(required = false) BigDecimal amount,
                         @RequestParam(required = false) String note,
                         @RequestParam(name = "website", required = false) String honeypot,
                         Model model) {
        ReferralPartner partner = referralService.byPublicCode(code);
        if (partner == null) return "public/link-expired";
        Agency agency = agencyRepository.findById(partner.getTenantId()).orElse(null);
        model.addAttribute("partner", partner);
        model.addAttribute("agency", agency);
        String digits = mobile == null ? "" : mobile.replaceAll("[^0-9]", "");
        if (honeypot == null || honeypot.isBlank()) {
            if (name == null || name.isBlank() || digits.length() < 10) {
                model.addAttribute("loanTypes", LoanType.values());
                model.addAttribute("error", "Please enter the name and a 10-digit mobile number.");
                return "public/refer";
            }
            leadService.captureReferral(partner, cut(name, 120), digits.length() == 10 ? "+91" + digits : "+" + digits,
                    cut(city, 80), loanType, amount, cut(note, 900));
        }
        model.addAttribute("submittedName", name);
        return "public/refer-thanks";
    }

    private static String cut(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
