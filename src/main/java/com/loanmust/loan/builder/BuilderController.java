package com.loanmust.loan.builder;

import com.loanmust.loan.application.LoanApplication;
import com.loanmust.loan.application.LoanApplicationService;
import com.loanmust.loan.audit.AuditService;
import com.loanmust.loan.common.IdCodec;
import com.loanmust.loan.lead.LeadRepository;
import com.loanmust.loan.lender.LenderService;
import com.loanmust.loan.security.SecurityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builder tie-ups: the builder, its projects, and the business each one brings. */
@Controller
public class BuilderController {

    private final BuilderService builderService;
    private final LoanApplicationService applicationService;
    private final LeadRepository leadRepository;
    private final LenderService lenderService;
    private final AuditService auditService;

    public BuilderController(BuilderService builderService, LoanApplicationService applicationService,
                             LeadRepository leadRepository, LenderService lenderService, AuditService auditService) {
        this.builderService = builderService;
        this.applicationService = applicationService;
        this.leadRepository = leadRepository;
        this.lenderService = lenderService;
        this.auditService = auditService;
    }

    @GetMapping("/builders")
    public String list(Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        List<Builder> builders = builderService.builders(tenantId);
        Map<Long, Long> projectCounts = new LinkedHashMap<>();
        Map<Long, Integer> fileCounts = new LinkedHashMap<>();
        Map<Long, BigDecimal> disbursed = new LinkedHashMap<>();
        for (Builder b : builders) {
            projectCounts.put(b.getId(), builderService.projectCount(tenantId, b.getId()));
            List<LoanApplication> files = applicationService.forBuilder(tenantId, b.getId());
            fileCounts.put(b.getId(), files.size());
            disbursed.put(b.getId(), files.stream().map(a -> a.getTotalDisbursed() == null ? BigDecimal.ZERO : a.getTotalDisbursed())
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        model.addAttribute("activeMenu", "builders");
        model.addAttribute("builders", builders);
        model.addAttribute("projectCounts", projectCounts);
        model.addAttribute("fileCounts", fileCounts);
        model.addAttribute("disbursed", disbursed);
        return "builder/list";
    }

    @GetMapping("/builders/new")
    public String createForm(Model model) {
        Builder b = new Builder();
        b.setActive(true);
        b.setTieUpSince(java.time.LocalDate.now());
        model.addAttribute("activeMenu", "builders");
        model.addAttribute("builder", b);
        return "builder/form";
    }

    @GetMapping("/builders/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Builder b = builderService.get(SecurityUtils.currentTenantId(), id);
        if (b == null) return "redirect:/builders";
        model.addAttribute("activeMenu", "builders");
        model.addAttribute("builder", b);
        return "builder/form";
    }

    @GetMapping("/builders/{id}")
    public String view(@PathVariable Long id, Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        Builder b = builderService.get(tenantId, id);
        if (b == null) return "redirect:/builders";
        model.addAttribute("activeMenu", "builders");
        model.addAttribute("builder", b);
        model.addAttribute("projects", builderService.projects(tenantId, id));
        model.addAttribute("leads", leadRepository.findByTenantIdAndBuilderIdAndDeletedFalseOrderByIdDesc(tenantId, id));
        model.addAttribute("applications", applicationService.forBuilder(tenantId, id));
        model.addAttribute("auditTrail", auditService.historyFor(tenantId, "Builder", id));
        return "builder/view";
    }

    @PostMapping("/builders/save")
    public String save(@ModelAttribute Builder builder, RedirectAttributes ra) {
        Builder saved = builderService.save(SecurityUtils.currentTenantId(), builder);
        if (saved == null) return "redirect:/builders";
        ra.addFlashAttribute("toast", "Builder saved.");
        return "redirect:/builders/" + IdCodec.tok(saved.getId());
    }

    @PostMapping("/builders/{id}/archive")
    public String archive(@PathVariable Long id, RedirectAttributes ra) {
        builderService.archive(SecurityUtils.currentTenantId(), id);
        ra.addFlashAttribute("toast", "Builder archived.");
        return "redirect:/builders";
    }

    // ------------------------------------------------------------------ projects

    @GetMapping("/builders/{id}/projects/new")
    public String newProject(@PathVariable Long id, Model model) {
        Builder b = builderService.get(SecurityUtils.currentTenantId(), id);
        if (b == null) return "redirect:/builders";
        Project p = new Project();
        p.setBuilderId(b.getId());
        p.setCity(b.getCity());
        p.setStatus(ProjectStatus.UNDER_CONSTRUCTION);
        return projectForm(p, b, model);
    }

    @GetMapping("/builders/projects/{projectId}/edit")
    public String editProject(@PathVariable Long projectId, Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        Project p = builderService.getProject(tenantId, projectId);
        if (p == null) return "redirect:/builders";
        return projectForm(p, builderService.get(tenantId, p.getBuilderId()), model);
    }

    private String projectForm(Project p, Builder b, Model model) {
        model.addAttribute("activeMenu", "builders");
        model.addAttribute("project", p);
        model.addAttribute("builder", b);
        model.addAttribute("statuses", ProjectStatus.values());
        model.addAttribute("lenders", lenderService.activeLenders(SecurityUtils.currentTenantId()));
        return "builder/project-form";
    }

    @PostMapping("/builders/projects/save")
    public String saveProject(@ModelAttribute Project project,
                              @RequestParam(name = "approvedLender", required = false) List<String> approvedLender,
                              RedirectAttributes ra) {
        if (approvedLender != null) project.setApprovedLenders(String.join(", ", approvedLender));
        Project saved = builderService.saveProject(SecurityUtils.currentTenantId(), project);
        if (saved == null) return "redirect:/builders";
        ra.addFlashAttribute("toast", "Project saved.");
        return "redirect:/builders/" + IdCodec.tok(saved.getBuilderId());
    }

    @PostMapping("/builders/projects/{projectId}/archive")
    public String archiveProject(@PathVariable Long projectId, RedirectAttributes ra) {
        Long tenantId = SecurityUtils.currentTenantId();
        Project p = builderService.getProject(tenantId, projectId);
        builderService.archiveProject(tenantId, projectId);
        ra.addFlashAttribute("toast", "Project archived.");
        return p == null ? "redirect:/builders" : "redirect:/builders/" + IdCodec.tok(p.getBuilderId());
    }
}
