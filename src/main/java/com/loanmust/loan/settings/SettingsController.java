package com.loanmust.loan.settings;

import com.loanmust.loan.audit.AuditService;
import com.loanmust.loan.security.SecurityUtils;
import com.loanmust.loan.tenant.Agency;
import com.loanmust.loan.tenant.AgencyRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Agency profile, code prefixes, referral / MODD defaults and the workspace accent. */
@Controller
public class SettingsController {

    private final AgencyRepository agencyRepository;
    private final AuditService auditService;

    public SettingsController(AgencyRepository agencyRepository, AuditService auditService) {
        this.agencyRepository = agencyRepository;
        this.auditService = auditService;
    }

    @GetMapping("/settings")
    public String page(Model model) {
        Agency agency = agencyRepository.findById(SecurityUtils.currentTenantId()).orElse(null);
        if (agency == null) return "redirect:/dashboard";
        model.addAttribute("activeMenu", "settings");
        model.addAttribute("agency", agency);
        model.addAttribute("palettes", AccentPalette.all());
        return "settings/index";
    }

    @PostMapping("/settings/save")
    public String save(@ModelAttribute("agency") Agency form, RedirectAttributes ra) {
        Agency a = agencyRepository.findById(SecurityUtils.currentTenantId()).orElse(null);
        if (a == null) return "redirect:/dashboard";
        AuditService.Diff diff = new AuditService.Diff()
                .compare("Name", a.getName(), form.getName())
                .compare("DSA code", a.getDsaCode(), form.getDsaCode())
                .compare("Default referral %", a.getDefaultReferralPercent(), form.getDefaultReferralPercent())
                .compare("Referral TDS %", a.getReferralTdsPercent(), form.getReferralTdsPercent())
                .compare("MODD stamp duty %", a.getModdStampDutyPercent(), form.getModdStampDutyPercent())
                .compare("MODD stamp duty cap", a.getModdStampDutyCap(), form.getModdStampDutyCap())
                .compare("MODD service charge", a.getModdServiceCharge(), form.getModdServiceCharge())
                .compare("Accent", a.getAccentColor(), form.getAccentColor());
        a.setName(form.getName());
        a.setTradeName(form.getTradeName());
        a.setOwnerName(form.getOwnerName());
        a.setEmail(form.getEmail());
        a.setPhone(form.getPhone());
        a.setDsaCode(form.getDsaCode());
        a.setGstin(form.getGstin());
        a.setPan(form.getPan());
        a.setAddress(form.getAddress());
        a.setCity(form.getCity());
        a.setState(form.getState());
        a.setPincode(form.getPincode());
        a.setLeadPrefix(form.getLeadPrefix());
        a.setCustomerPrefix(form.getCustomerPrefix());
        a.setApplicationPrefix(form.getApplicationPrefix());
        a.setReferralPrefix(form.getReferralPrefix());
        a.setProposalPrefix(form.getProposalPrefix());
        a.setReceiptPrefix(form.getReceiptPrefix());
        a.setAccentColor(AccentPalette.fromId(form.getAccentColor()).getId());
        a.setDefaultReferralPercent(form.getDefaultReferralPercent());
        a.setReferralTdsPercent(form.getReferralTdsPercent());
        a.setModdStampDutyPercent(form.getModdStampDutyPercent());
        a.setModdStampDutyCap(form.getModdStampDutyCap());
        a.setModdServiceCharge(form.getModdServiceCharge());
        a.setProcessingFeeGstPercent(form.getProcessingFeeGstPercent());
        a.setProposalValidityDays(form.getProposalValidityDays());
        a.setModifiedBy(SecurityUtils.currentUserLogin());
        agencyRepository.save(a);
        auditService.logDiff("Agency", a.getId(), a.getName(), diff);
        ra.addFlashAttribute("toast", "Settings saved.");
        return "redirect:/settings";
    }
}
