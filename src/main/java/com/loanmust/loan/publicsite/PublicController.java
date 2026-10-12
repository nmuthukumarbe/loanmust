package com.loanmust.loan.publicsite;

import com.loanmust.loan.lead.LeadService;
import com.loanmust.loan.lender.LoanType;
import com.loanmust.loan.security.SecurityUtils;
import com.loanmust.loan.tenant.Agency;
import com.loanmust.loan.tenant.AgencyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/**
 * The public website at / (with its enquiry form) and the staff sign-in page. A website
 * enquiry lands as a NEW lead with source WEBSITE. A hidden honeypot field turns away
 * simple form-filling bots.
 */
@Controller
public class PublicController {

    private final AgencyRepository agencyRepository;
    private final LeadService leadService;
    private final boolean demoLogins;

    public PublicController(AgencyRepository agencyRepository, LeadService leadService,
                            @Value("${app.demo.seed-transactions:false}") boolean demoLogins) {
        this.agencyRepository = agencyRepository;
        this.leadService = leadService;
        this.demoLogins = demoLogins;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("loanTypes", LoanType.values());
        model.addAttribute("signedIn", SecurityUtils.currentUser() != null);
        return "public/home";
    }

    @PostMapping("/enquiry")
    public String enquiry(@RequestParam String name, @RequestParam String mobile,
                          @RequestParam(required = false) String city,
                          @RequestParam(required = false) LoanType loanType,
                          @RequestParam(required = false) BigDecimal amount,
                          @RequestParam(required = false) String note,
                          @RequestParam(name = "website", required = false) String honeypot,
                          Model model) {
        String digits = mobile == null ? "" : mobile.replaceAll("[^0-9]", "");
        if (honeypot == null || honeypot.isBlank()) {
            if (name == null || name.isBlank() || digits.length() < 10) {
                model.addAttribute("loanTypes", LoanType.values());
                model.addAttribute("signedIn", SecurityUtils.currentUser() != null);
                model.addAttribute("error", "Please enter your name and a 10-digit mobile number.");
                return "public/home";
            }
            Agency agency = agencyRepository.findFirstByDeletedFalseOrderByIdAsc().orElse(null);
            if (agency != null) {
                leadService.captureWebsite(agency.getId(), cut(name, 120),
                        digits.length() == 10 ? "+91" + digits : "+" + digits,
                        cut(city, 80), loanType, amount, cut(note, 900));
            }
        }
        return "redirect:/?thanks#enquiry";
    }

    @GetMapping("/login")
    public String login(Model model) {
        // The demo desk picker pre-fills demo passwords, so it only shows with demo data on.
        model.addAttribute("demoLogins", demoLogins);
        return SecurityUtils.currentUser() == null ? "login" : "redirect:/dashboard";
    }

    private static String cut(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
