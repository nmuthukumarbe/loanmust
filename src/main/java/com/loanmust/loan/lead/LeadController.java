package com.loanmust.loan.lead;

import com.loanmust.loan.advisor.LoanAdvisorService;
import com.loanmust.loan.audit.AuditService;
import com.loanmust.loan.builder.BuilderService;
import com.loanmust.loan.common.Channel;
import com.loanmust.loan.common.IdCodec;
import com.loanmust.loan.common.WhatsApp;
import com.loanmust.loan.customer.Customer;
import com.loanmust.loan.customer.EmploymentType;
import com.loanmust.loan.lender.LoanType;
import com.loanmust.loan.referral.ReferralService;
import com.loanmust.loan.reports.ExcelExporter;
import com.loanmust.loan.reports.ReportService;
import com.loanmust.loan.security.SecurityUtils;
import com.loanmust.loan.user.UserRepository;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
public class LeadController {

    private static final int PAGE_SIZE = 15;

    private final LeadService leadService;
    private final BuilderService builderService;
    private final ReferralService referralService;
    private final LoanAdvisorService advisorService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ReportService reportService;
    private final ExcelExporter excelExporter;

    public LeadController(LeadService leadService, BuilderService builderService, ReferralService referralService,
                          LoanAdvisorService advisorService, UserRepository userRepository, AuditService auditService,
                          ReportService reportService, ExcelExporter excelExporter) {
        this.leadService = leadService;
        this.builderService = builderService;
        this.referralService = referralService;
        this.advisorService = advisorService;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.reportService = reportService;
        this.excelExporter = excelExporter;
    }

    @GetMapping("/leads")
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) LeadStatus status,
                       @RequestParam(required = false) LeadSource source,
                       @RequestParam(required = false) LeadPriority priority,
                       @RequestParam(required = false) LoanType loanType,
                       @RequestParam(required = false) Long assignedUserId,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        Long ownerFilter = SecurityUtils.ownerFilterUserId();
        model.addAttribute("activeMenu", "leads");
        model.addAttribute("leadPage", leadService.search(tenantId, q, status, source, priority, loanType,
                ownerFilter != null ? ownerFilter : assignedUserId, from, to, page, PAGE_SIZE));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("source", source);
        model.addAttribute("priority", priority);
        model.addAttribute("loanType", loanType);
        model.addAttribute("assignedUserId", assignedUserId);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("lockedToOwnLeads", ownerFilter != null);
        model.addAttribute("statuses", LeadStatus.values());
        model.addAttribute("sources", LeadSource.values());
        model.addAttribute("priorities", LeadPriority.values());
        model.addAttribute("loanTypes", LoanType.values());
        model.addAttribute("users", userRepository.findByTenantIdAndEnabledTrueAndDeletedFalseOrderByFullNameAsc(tenantId));
        model.addAttribute("dueToday", leadService.dueFollowUps(tenantId, LocalDate.now(), ownerFilter).size());
        return "lead/list";
    }

    @GetMapping("/leads/export")
    public ResponseEntity<ByteArrayResource> export() {
        auditService.log("EXPORT", "Lead", 0L, "Lead register", "Leads exported");
        return excelExporter.download(reportService.build(ReportService.Report.LEADS, SecurityUtils.currentTenantId(), null, null),
                "loanmust-leads-" + LocalDate.now() + ".xlsx");
    }

    @GetMapping("/leads/new")
    public String createForm(@RequestParam(required = false) Long builderId,
                             @RequestParam(required = false) Long projectId,
                             @RequestParam(required = false) Long partnerId,
                             Model model) {
        Lead lead = new Lead();
        lead.setStatus(LeadStatus.NEW);
        lead.setSource(LeadSource.DIRECT);
        lead.setLoanType(LoanType.HOME_LOAN);
        lead.setNextFollowUpDate(LocalDate.now().plusDays(1));
        if (projectId != null || builderId != null) {
            lead.setSource(LeadSource.BUILDER);
            lead.setBuilderId(builderId);
            lead.setProjectId(projectId);
        }
        if (partnerId != null) {
            lead.setSource(LeadSource.REFERRAL);
            lead.setReferralPartnerId(partnerId);
        }
        return form(lead, model);
    }

    @GetMapping("/leads/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Lead lead = visible(id);
        if (lead == null) return "redirect:/leads";
        return form(lead, model);
    }

    private String form(Lead lead, Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        model.addAttribute("activeMenu", "leads");
        model.addAttribute("lead", lead);
        model.addAttribute("statuses", LeadStatus.values());
        model.addAttribute("sources", LeadSource.values());
        model.addAttribute("loanTypes", LoanType.values());
        model.addAttribute("employmentTypes", EmploymentType.values());
        model.addAttribute("builders", builderService.activeBuilders(tenantId));
        model.addAttribute("projects", builderService.allProjects(tenantId));
        model.addAttribute("partners", referralService.activePartners(tenantId));
        model.addAttribute("users", userRepository.findByTenantIdAndEnabledTrueAndDeletedFalseOrderByFullNameAsc(tenantId));
        return "lead/form";
    }

    @GetMapping("/leads/{id}")
    public String view(@PathVariable Long id, Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        Lead lead = visible(id);
        if (lead == null) return "redirect:/leads";
        model.addAttribute("activeMenu", "leads");
        model.addAttribute("lead", lead);
        model.addAttribute("timeline", leadService.timeline(tenantId, id));
        model.addAttribute("assessment", leadService.assess(tenantId, lead));
        model.addAttribute("proposals", advisorService.forLead(tenantId, id));
        model.addAttribute("auditTrail", auditService.historyFor(tenantId, "Lead", id));
        model.addAttribute("users", userRepository.findByTenantIdAndEnabledTrueAndDeletedFalseOrderByFullNameAsc(tenantId));
        model.addAttribute("channels", Channel.values());
        model.addAttribute("statuses", LeadStatus.values());
        model.addAttribute("whatsappLink", WhatsApp.link(lead.getMobile(),
                "Hello " + lead.getName() + ", thank you for your loan enquiry. "));
        if (lead.getDuplicateOfLeadId() != null) {
            model.addAttribute("duplicateOf", leadService.get(tenantId, lead.getDuplicateOfLeadId()));
        }
        return "lead/view";
    }

    @PostMapping("/leads/save")
    public String save(@ModelAttribute Lead lead, RedirectAttributes ra) {
        if (lead.getId() != null && visible(lead.getId()) == null) return "redirect:/leads";
        Lead saved = leadService.save(SecurityUtils.currentTenantId(), lead);
        if (saved == null) return "redirect:/leads";
        ra.addFlashAttribute("toast", "Lead saved.");
        return "redirect:/leads/" + IdCodec.tok(saved.getId());
    }

    @PostMapping("/leads/{id}/follow-up")
    public String followUp(@PathVariable Long id, @ModelAttribute LeadActivity activity,
                           @RequestParam(required = false) LeadStatus newStatus, RedirectAttributes ra) {
        if (visible(id) == null) return "redirect:/leads";
        leadService.addActivity(SecurityUtils.currentTenantId(), id, activity, newStatus);
        ra.addFlashAttribute("toast", "Follow-up recorded.");
        return "redirect:/leads/" + IdCodec.tok(id);
    }

    @PostMapping("/leads/{id}/assign")
    public String assign(@PathVariable Long id, @RequestParam(required = false) Long userId, RedirectAttributes ra) {
        if (visible(id) == null) return "redirect:/leads";
        leadService.assign(SecurityUtils.currentTenantId(), id, userId);
        ra.addFlashAttribute("toast", "Assignment updated.");
        return "redirect:/leads/" + IdCodec.tok(id);
    }

    @PostMapping("/leads/{id}/close")
    public String close(@PathVariable Long id, @RequestParam LeadStatus status,
                        @RequestParam(required = false) String reason, RedirectAttributes ra) {
        if (visible(id) == null) return "redirect:/leads";
        if (status != LeadStatus.LOST && status != LeadStatus.NOT_ELIGIBLE) return "redirect:/leads/" + IdCodec.tok(id);
        leadService.close(SecurityUtils.currentTenantId(), id, status, reason);
        ra.addFlashAttribute("toast", "Lead closed as " + status.getLabel().toLowerCase() + ".");
        return "redirect:/leads/" + IdCodec.tok(id);
    }

    /** Lead → customer, then straight into Rate Compare with the profile pre-filled. */
    @PostMapping("/leads/{id}/convert")
    public String convert(@PathVariable Long id, @RequestParam(defaultValue = "customer") String next, RedirectAttributes ra) {
        if (visible(id) == null) return "redirect:/leads";
        Customer customer = leadService.convertToCustomer(SecurityUtils.currentTenantId(), id);
        if (customer == null) return "redirect:/leads";
        ra.addFlashAttribute("toast", "Customer " + customer.getCode() + " ready.");
        if ("file".equals(next)) {
            return "redirect:/applications/new?customerId=" + IdCodec.tok(customer.getId()) + "&leadId=" + IdCodec.tok(id);
        }
        return "redirect:/customers/" + IdCodec.tok(customer.getId());
    }

    @PostMapping("/leads/{id}/archive")
    public String archive(@PathVariable Long id, RedirectAttributes ra) {
        if (visible(id) == null) return "redirect:/leads";
        leadService.archive(SecurityUtils.currentTenantId(), id);
        ra.addFlashAttribute("toast", "Lead archived.");
        return "redirect:/leads";
    }

    /** Loan executives only reach their own leads, whatever URL they type. */
    private Lead visible(Long id) {
        Lead lead = leadService.get(SecurityUtils.currentTenantId(), id);
        Long owner = SecurityUtils.ownerFilterUserId();
        if (lead != null && owner != null && !owner.equals(lead.getAssignedUserId())) return null;
        return lead;
    }
}
