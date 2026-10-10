package com.loanmust.loan.advisor;

import com.loanmust.loan.common.IdCodec;
import com.loanmust.loan.common.Labels;
import com.loanmust.loan.common.WhatsApp;
import com.loanmust.loan.customer.Customer;
import com.loanmust.loan.customer.CustomerService;
import com.loanmust.loan.customer.EmploymentType;
import com.loanmust.loan.customer.Gender;
import com.loanmust.loan.lead.Lead;
import com.loanmust.loan.lead.LeadService;
import com.loanmust.loan.lender.LoanType;
import com.loanmust.loan.security.SecurityUtils;
import com.loanmust.loan.tenant.Agency;
import com.loanmust.loan.user.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Rate Compare & Advisor: run the customer's profile against every lender's live rate card,
 * show the ranked comparison with the recommendation, and save it as a shareable proposal.
 */
@Controller
public class AdvisorController {

    private final LoanAdvisorService advisorService;
    private final CustomerService customerService;
    private final LeadService leadService;
    private final String publicBaseUrl;

    public AdvisorController(LoanAdvisorService advisorService, CustomerService customerService, LeadService leadService,
                             @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        this.advisorService = advisorService;
        this.customerService = customerService;
        this.leadService = leadService;
        this.publicBaseUrl = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
    }

    @GetMapping("/advisor")
    public String advisor(@ModelAttribute("req") AdvisorRequest req, BindingResult binding, Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        if (req.getLoanAmount() == null) prefill(tenantId, req);
        model.addAttribute("activeMenu", "advisor");
        model.addAttribute("req", req);
        model.addAttribute("loanTypes", LoanType.values());
        model.addAttribute("employmentTypes", EmploymentType.values());
        model.addAttribute("showPayout", showPayout());
        if (req.isComplete()) {
            model.addAttribute("outcome", advisorService.compare(tenantId, req));
        }
        return "advisor/index";
    }

    @PostMapping("/advisor/save")
    public String save(@ModelAttribute("req") AdvisorRequest req, BindingResult binding, @RequestParam(required = false) String remarks,
                       RedirectAttributes ra) {
        if (!req.isComplete()) {
            ra.addFlashAttribute("error", "Enter the loan type, amount and tenure before saving.");
            return "redirect:/advisor";
        }
        Proposal p = advisorService.saveProposal(SecurityUtils.currentTenantId(), req, remarks);
        ra.addFlashAttribute("toast", "Proposal " + p.getCode() + " saved. Share it with the customer.");
        return "redirect:/advisor/proposals/" + IdCodec.tok(p.getId());
    }

    @GetMapping("/advisor/proposals")
    public String proposals(@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("activeMenu", "advisor");
        model.addAttribute("proposalPage", advisorService.search(SecurityUtils.currentTenantId(), q, page, 15));
        model.addAttribute("q", q);
        return "advisor/proposals";
    }

    @GetMapping("/advisor/proposals/{id}")
    public String proposal(@PathVariable Long id, Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        Proposal p = advisorService.get(tenantId, id);
        if (p == null) return "redirect:/advisor/proposals";
        Agency agency = advisorService.agency(tenantId);
        String shareUrl = publicBaseUrl + "/p/" + p.getToken();
        model.addAttribute("activeMenu", "advisor");
        model.addAttribute("proposal", p);
        model.addAttribute("outcome", advisorService.outcomeOf(p));
        model.addAttribute("shareUrl", shareUrl);
        model.addAttribute("showPayout", showPayout());
        String msg = "Dear " + (p.getCustomerName() == null ? "Customer" : p.getCustomerName())
                + ", we compared " + p.getLoanTypeLabel().toLowerCase() + " offers from all our partner banks for Rs."
                + Labels.inr(p.getLoanAmount())
                + (p.getRecommendedLenderName() == null || p.getRecommendedRate() == null ? "" : ". Best option: " + p.getRecommendedLenderName()
                + " at " + p.getRecommendedRate().stripTrailingZeros().toPlainString() + "% (EMI Rs." + Labels.inr(p.getRecommendedEmi()) + ")")
                + ". Full comparison: " + shareUrl + " - " + (agency == null ? "" : agency.getDisplayName());
        model.addAttribute("whatsappLink", p.getCustomerMobile() == null ? null : WhatsApp.link(p.getCustomerMobile(), msg));
        model.addAttribute("whatsappShare", "https://wa.me/?text=" + java.net.URLEncoder.encode(msg, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"));
        return "advisor/proposal";
    }

    @PostMapping("/advisor/proposals/{id}/archive")
    public String archive(@PathVariable Long id, RedirectAttributes ra) {
        advisorService.archive(SecurityUtils.currentTenantId(), id);
        ra.addFlashAttribute("toast", "Proposal withdrawn; its public link no longer opens.");
        return "redirect:/advisor/proposals";
    }

    private void prefill(Long tenantId, AdvisorRequest req) {
        Customer c = customerService.get(tenantId, req.getCustomerId());
        if (c != null) {
            req.setCustomerName(c.getName());
            req.setCustomerMobile(c.getMobile());
            req.setMonthlyIncome(c.getMonthlyIncome());
            req.setExistingEmi(c.getExistingEmi());
            req.setCibilScore(c.getCibilScore());
            if (c.getEmploymentType() != null) req.setEmploymentType(c.getEmploymentType());
            req.setApplicantAge(c.getAge());
            req.setWomanApplicant(c.getGender() == Gender.FEMALE);
            return;
        }
        Lead l = leadService.get(tenantId, req.getLeadId());
        if (l != null) {
            req.setCustomerName(l.getName());
            req.setCustomerMobile(l.getMobile());
            if (l.getLoanType() != null) req.setLoanType(l.getLoanType());
            req.setLoanAmount(l.getLoanAmountRequired());
            req.setPropertyValue(l.getPropertyValue());
            req.setMonthlyIncome(l.getMonthlyIncome());
            req.setExistingEmi(l.getExistingEmi());
            req.setCibilScore(l.getCibilScore());
            if (l.getEmploymentType() != null) req.setEmploymentType(l.getEmploymentType());
        }
    }

    private boolean showPayout() {
        Role role = SecurityUtils.currentRole();
        return role != null && role.isMoney();
    }
}
