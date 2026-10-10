package com.loanmust.loan.referral;

import com.loanmust.loan.application.LoanApplicationService;
import com.loanmust.loan.audit.AuditService;
import com.loanmust.loan.common.IdCodec;
import com.loanmust.loan.lead.LeadRepository;
import com.loanmust.loan.security.SecurityUtils;
import com.loanmust.loan.tenant.Agency;
import com.loanmust.loan.tenant.AgencyRepository;
import com.loanmust.loan.user.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** Referral partners: enrolment, personal links, their leads, files and payouts. */
@Controller
public class ReferralController {

    private final ReferralService referralService;
    private final LoanApplicationService applicationService;
    private final LeadRepository leadRepository;
    private final AgencyRepository agencyRepository;
    private final AuditService auditService;
    private final String publicBaseUrl;

    public ReferralController(ReferralService referralService, LoanApplicationService applicationService,
                              LeadRepository leadRepository, AgencyRepository agencyRepository, AuditService auditService,
                              @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        this.referralService = referralService;
        this.applicationService = applicationService;
        this.leadRepository = leadRepository;
        this.agencyRepository = agencyRepository;
        this.auditService = auditService;
        this.publicBaseUrl = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
    }

    @GetMapping("/referrals")
    public String list(@RequestParam(required = false) String q, @RequestParam(required = false) PartnerType partnerType,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("activeMenu", "referrals");
        model.addAttribute("partnerPage", referralService.search(SecurityUtils.currentTenantId(), q, partnerType, page, 15));
        model.addAttribute("q", q);
        model.addAttribute("partnerType", partnerType);
        model.addAttribute("partnerTypes", PartnerType.values());
        model.addAttribute("baseUrl", publicBaseUrl);
        return "referral/list";
    }

    @GetMapping("/referrals/new")
    public String createForm(Model model) {
        ReferralPartner p = new ReferralPartner();
        p.setActive(true);
        p.setPartnerType(PartnerType.INDIVIDUAL);
        p.setPayoutBasis(PayoutBasis.PERCENT_OF_DISBURSAL);
        Agency agency = agencyRepository.findById(SecurityUtils.currentTenantId()).orElse(null);
        p.setPayoutValue(agency == null ? null : agency.getDefaultReferralPercent());
        return form(p, model);
    }

    @GetMapping("/referrals/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        ReferralPartner p = referralService.get(SecurityUtils.currentTenantId(), id);
        if (p == null) return "redirect:/referrals";
        return form(p, model);
    }

    private String form(ReferralPartner p, Model model) {
        model.addAttribute("activeMenu", "referrals");
        model.addAttribute("partner", p);
        model.addAttribute("partnerTypes", PartnerType.values());
        model.addAttribute("payoutBases", PayoutBasis.values());
        model.addAttribute("canSetPayout", isMoney());
        return "referral/form";
    }

    @GetMapping("/referrals/{id}")
    public String view(@PathVariable Long id, Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        ReferralPartner p = referralService.get(tenantId, id);
        if (p == null) return "redirect:/referrals";
        Agency agency = agencyRepository.findById(tenantId).orElse(null);
        String agencyName = agency == null ? "us" : agency.getDisplayName();
        boolean money = isMoney();
        model.addAttribute("activeMenu", "referrals");
        model.addAttribute("partner", p);
        model.addAttribute("referralUrl", publicBaseUrl + "/refer/" + p.getCode());
        model.addAttribute("inviteLink", referralService.inviteLink(p, publicBaseUrl, agencyName));
        model.addAttribute("leads", leadRepository.findByTenantIdAndReferralPartnerIdAndDeletedFalseOrderByIdDesc(tenantId, id));
        model.addAttribute("applications", applicationService.forPartner(tenantId, id));
        model.addAttribute("showMoney", money);
        model.addAttribute("payouts", money ? referralService.payoutsFor(tenantId, id) : List.of());
        model.addAttribute("owed", money ? referralService.owedTo(tenantId, id) : null);
        model.addAttribute("paid", money ? referralService.paidTo(tenantId, id) : null);
        model.addAttribute("auditTrail", auditService.historyFor(tenantId, "ReferralPartner", id));
        return "referral/view";
    }

    @PostMapping("/referrals/save")
    public String save(@ModelAttribute("partner") ReferralPartner form, RedirectAttributes ra) {
        Long tenantId = SecurityUtils.currentTenantId();
        if (!isMoney()) {
            // only money roles may set or change what a partner is paid
            ReferralPartner existing = referralService.get(tenantId, form.getId());
            form.setPayoutBasis(existing == null ? null : existing.getPayoutBasis());
            form.setPayoutValue(existing == null ? null : existing.getPayoutValue());
        }
        ReferralPartner saved = referralService.save(tenantId, form);
        if (saved == null) return "redirect:/referrals";
        ra.addFlashAttribute("toast", "Referral partner saved. Share their personal link.");
        return "redirect:/referrals/" + IdCodec.tok(saved.getId());
    }

    @PostMapping("/referrals/{id}/archive")
    public String archive(@PathVariable Long id, RedirectAttributes ra) {
        referralService.archive(SecurityUtils.currentTenantId(), id);
        ra.addFlashAttribute("toast", "Partner archived; their link is disabled.");
        return "redirect:/referrals";
    }

    private boolean isMoney() {
        Role role = SecurityUtils.currentRole();
        return role != null && role.isMoney();
    }
}
