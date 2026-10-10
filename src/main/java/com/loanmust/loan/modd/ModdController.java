package com.loanmust.loan.modd;

import com.loanmust.loan.audit.AuditService;
import com.loanmust.loan.common.IdCodec;
import com.loanmust.loan.common.Labels;
import com.loanmust.loan.common.WhatsApp;
import com.loanmust.loan.security.SecurityUtils;
import com.loanmust.loan.user.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.format.DateTimeFormatter;

/** MODD processing desk. */
@Controller
public class ModdController {

    private final ModdService moddService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ModdController(ModdService moddService, UserRepository userRepository, AuditService auditService) {
        this.moddService = moddService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @GetMapping("/modd")
    public String list(@RequestParam(required = false) String q, @RequestParam(required = false) ModdStatus status,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        model.addAttribute("activeMenu", "modd");
        model.addAttribute("moddPage", moddService.search(tenantId, q, status, page, 15));
        model.addAttribute("appointments", moddService.upcomingAppointments(tenantId, 7));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("statuses", ModdStatus.values());
        return "modd/list";
    }

    @GetMapping("/modd/{id}")
    public String view(@PathVariable Long id, Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        ModdRecord m = moddService.get(tenantId, id);
        if (m == null) return "redirect:/modd";
        model.addAttribute("activeMenu", "modd");
        model.addAttribute("modd", m);
        model.addAttribute("statuses", ModdStatus.values());
        model.addAttribute("path", ModdStatus.path());
        model.addAttribute("suggestedStampDuty", moddService.stampDutyFor(tenantId, m.getSecuredAmount()));
        model.addAttribute("users", userRepository.findByTenantIdAndEnabledTrueAndDeletedFalseOrderByFullNameAsc(tenantId));
        model.addAttribute("auditTrail", auditService.historyFor(tenantId, "Modd", id));
        String msg = "Dear " + m.getCustomerName() + ", your MODD (mortgage deposit) for loan file " + m.getApplicationCode()
                + (m.getAppointmentAt() == null ? " is being prepared."
                : " is scheduled at " + (m.getSubRegistrarOffice() == null ? "the sub-registrar office" : m.getSubRegistrarOffice())
                + " on " + m.getAppointmentAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
                + ". Please bring original ID proof and the title documents.")
                + " Charges to be paid: Rs." + Labels.inr(m.getTotalToCollect()) + ".";
        model.addAttribute("whatsappLink", WhatsApp.link(m.getCustomerMobile(), msg));
        return "modd/view";
    }

    @PostMapping("/modd/save")
    public String save(@ModelAttribute ModdRecord form, RedirectAttributes ra) {
        ModdRecord saved = moddService.save(SecurityUtils.currentTenantId(), form);
        if (saved == null) return "redirect:/modd";
        ra.addFlashAttribute("toast", "MODD updated - " + saved.getStatusLabel() + ".");
        return "redirect:/modd/" + IdCodec.tok(saved.getId());
    }

    @PostMapping("/modd/{id}/archive")
    public String archive(@PathVariable Long id, RedirectAttributes ra) {
        moddService.archive(SecurityUtils.currentTenantId(), id);
        ra.addFlashAttribute("toast", "MODD job archived.");
        return "redirect:/modd";
    }
}
