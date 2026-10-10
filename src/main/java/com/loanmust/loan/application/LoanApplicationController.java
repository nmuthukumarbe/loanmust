package com.loanmust.loan.application;

import com.loanmust.loan.advisor.LoanAdvisorService;
import com.loanmust.loan.advisor.Proposal;
import com.loanmust.loan.audit.AuditService;
import com.loanmust.loan.builder.BuilderService;
import com.loanmust.loan.commission.CommissionService;
import com.loanmust.loan.common.IdCodec;
import com.loanmust.loan.common.WhatsApp;
import com.loanmust.loan.customer.Customer;
import com.loanmust.loan.customer.CustomerService;
import com.loanmust.loan.finance.FinanceService;
import com.loanmust.loan.lead.Lead;
import com.loanmust.loan.lead.LeadService;
import com.loanmust.loan.lead.LeadSource;
import com.loanmust.loan.lender.LenderService;
import com.loanmust.loan.lender.LoanProduct;
import com.loanmust.loan.lender.LoanType;
import com.loanmust.loan.modd.ModdRecord;
import com.loanmust.loan.modd.ModdService;
import com.loanmust.loan.referral.ReferralPayoutRepository;
import com.loanmust.loan.referral.ReferralService;
import com.loanmust.loan.reports.ExcelExporter;
import com.loanmust.loan.reports.ReportService;
import com.loanmust.loan.security.SecurityUtils;
import com.loanmust.loan.storage.StorageService;
import com.loanmust.loan.user.Role;
import com.loanmust.loan.user.UserRepository;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Loan files: list, pipeline board, the file screen and every workflow action on it. */
@Controller
public class LoanApplicationController {

    private static final int PAGE_SIZE = 15;

    private final LoanApplicationService service;
    private final CustomerService customerService;
    private final LenderService lenderService;
    private final BuilderService builderService;
    private final ReferralService referralService;
    private final ReferralPayoutRepository referralPayoutRepository;
    private final CommissionService commissionService;
    private final ModdService moddService;
    private final FinanceService financeService;
    private final LeadService leadService;
    private final LoanAdvisorService advisorService;
    private final StorageService storageService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ReportService reportService;
    private final ExcelExporter excelExporter;

    public LoanApplicationController(LoanApplicationService service, CustomerService customerService,
                                     LenderService lenderService, BuilderService builderService,
                                     ReferralService referralService, ReferralPayoutRepository referralPayoutRepository,
                                     CommissionService commissionService, ModdService moddService,
                                     FinanceService financeService, LeadService leadService,
                                     LoanAdvisorService advisorService, StorageService storageService,
                                     UserRepository userRepository, AuditService auditService,
                                     ReportService reportService, ExcelExporter excelExporter) {
        this.service = service;
        this.customerService = customerService;
        this.lenderService = lenderService;
        this.builderService = builderService;
        this.referralService = referralService;
        this.referralPayoutRepository = referralPayoutRepository;
        this.commissionService = commissionService;
        this.moddService = moddService;
        this.financeService = financeService;
        this.leadService = leadService;
        this.advisorService = advisorService;
        this.storageService = storageService;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.reportService = reportService;
        this.excelExporter = excelExporter;
    }

    // ------------------------------------------------------------------ lists

    @GetMapping("/applications")
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) ApplicationStage stage,
                       @RequestParam(required = false) LoanType loanType,
                       @RequestParam(required = false) Long lenderId,
                       @RequestParam(required = false) Long builderId,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        model.addAttribute("activeMenu", "applications");
        model.addAttribute("appPage", service.search(tenantId, q, stage, loanType, lenderId, builderId,
                SecurityUtils.ownerFilterUserId(), page, PAGE_SIZE));
        model.addAttribute("q", q);
        model.addAttribute("stage", stage);
        model.addAttribute("loanType", loanType);
        model.addAttribute("lenderId", lenderId);
        model.addAttribute("builderId", builderId);
        model.addAttribute("stages", ApplicationStage.values());
        model.addAttribute("loanTypes", LoanType.values());
        model.addAttribute("lenders", lenderService.lenders(tenantId));
        model.addAttribute("builders", builderService.builders(tenantId));
        model.addAttribute("lockedToOwn", SecurityUtils.ownerFilterUserId() != null);
        return "application/list";
    }

    @GetMapping("/applications/board")
    public String board(Model model) {
        model.addAttribute("activeMenu", "pipeline");
        model.addAttribute("board", service.board(SecurityUtils.currentTenantId(), SecurityUtils.ownerFilterUserId(), 12));
        model.addAttribute("lockedToOwn", SecurityUtils.ownerFilterUserId() != null);
        return "application/board";
    }

    @GetMapping("/applications/export")
    public ResponseEntity<ByteArrayResource> export() {
        auditService.log("EXPORT", "LoanApplication", 0L, "Pipeline", "Loan files exported");
        return excelExporter.download(reportService.build(ReportService.Report.PIPELINE, SecurityUtils.currentTenantId(), null, null),
                "loanmust-loan-files-" + LocalDate.now() + ".xlsx");
    }

    // ------------------------------------------------------------------ open / edit

    @GetMapping("/applications/new")
    public String createForm(@RequestParam(required = false) Long customerId,
                             @RequestParam(required = false) Long leadId,
                             @RequestParam(required = false) Long proposalId,
                             Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        LoanApplication a = new LoanApplication();
        a.setLoanType(LoanType.HOME_LOAN);
        a.setTenureYears(20);
        a.setSource(LeadSource.DIRECT);

        Proposal proposal = advisorService.get(tenantId, proposalId);
        if (proposal != null) {
            a.setProposalId(proposal.getId());
            if (customerId == null) customerId = proposal.getCustomerId();
            if (leadId == null) leadId = proposal.getLeadId();
            a.setLoanType(proposal.getLoanType());
            a.setRequestedAmount(proposal.getLoanAmount());
            a.setTenureYears(proposal.getTenureYears());
            LoanProduct product = lenderService.getProduct(tenantId, proposal.getRecommendedProductId());
            if (product != null) {
                a.setProductId(product.getId());
                a.setLenderId(product.getLenderId());
            }
        }
        Lead lead = leadService.get(tenantId, leadId);
        if (lead != null) {
            a.setLeadId(lead.getId());
            a.setSource(lead.getSource());
            a.setBuilderId(lead.getBuilderId());
            a.setProjectId(lead.getProjectId());
            a.setReferralPartnerId(lead.getReferralPartnerId());
            if (a.getRequestedAmount() == null) a.setRequestedAmount(lead.getLoanAmountRequired());
            if (lead.getLoanType() != null && proposal == null) a.setLoanType(lead.getLoanType());
            a.setPropertyValue(lead.getPropertyValue());
            a.setAssignedUserId(lead.getAssignedUserId());
            if (customerId == null) customerId = lead.getConvertedCustomerId();
        }
        Customer customer = customerService.get(tenantId, customerId);
        if (customer != null) {
            a.setCustomerId(customer.getId());
            if (a.getReferralPartnerId() == null) a.setReferralPartnerId(customer.getReferralPartnerId());
            if (a.getAssignedUserId() == null) a.setAssignedUserId(customer.getOwnerUserId());
        }
        return form(a, customer, model);
    }

    @GetMapping("/applications/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        LoanApplication a = service.getVisible(tenantId, id);
        if (a == null) return "redirect:/applications";
        return form(a, customerService.get(tenantId, a.getCustomerId()), model);
    }

    private String form(LoanApplication a, Customer customer, Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        model.addAttribute("activeMenu", "applications");
        model.addAttribute("app", a);
        model.addAttribute("customer", customer);
        model.addAttribute("customers", customer == null ? customerService.all(tenantId) : List.of(customer));
        model.addAttribute("loanTypes", LoanType.values());
        model.addAttribute("sources", LeadSource.values());
        model.addAttribute("lenders", lenderService.activeLenders(tenantId));
        model.addAttribute("products", lenderService.activeProducts(tenantId));
        model.addAttribute("builders", builderService.activeBuilders(tenantId));
        model.addAttribute("projects", builderService.allProjects(tenantId));
        model.addAttribute("partners", referralService.activePartners(tenantId));
        model.addAttribute("users", userRepository.findByTenantIdAndEnabledTrueAndDeletedFalseOrderByFullNameAsc(tenantId));
        model.addAttribute("showPayout", isMoney());
        return "application/form";
    }

    @PostMapping("/applications/save")
    public String save(@ModelAttribute LoanApplication form, RedirectAttributes ra) {
        Long tenantId = SecurityUtils.currentTenantId();
        if (form.getId() != null && service.getVisible(tenantId, form.getId()) == null) return "redirect:/applications";
        if (!isMoney()) form.setPayoutPercent(null);   // only money roles may set the payout
        LoanApplication saved = service.save(tenantId, form);
        if (saved == null) {
            ra.addFlashAttribute("error", "Pick the customer this loan file is for.");
            return "redirect:/applications/new";
        }
        ra.addFlashAttribute("toast", form.getId() == null
                ? "Loan file " + saved.getCode() + " opened with its document checklist." : "Loan file saved.");
        return "redirect:/applications/" + IdCodec.tok(saved.getId());
    }

    // ------------------------------------------------------------------ the file

    @GetMapping("/applications/{id}")
    public String view(@PathVariable Long id, Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        LoanApplication a = service.getVisible(tenantId, id);
        if (a == null) return "redirect:/applications";
        List<ApplicationDocument> docs = service.documents(tenantId, id);
        boolean money = isMoney();
        model.addAttribute("activeMenu", "applications");
        model.addAttribute("app", a);
        model.addAttribute("customer", customerService.get(tenantId, a.getCustomerId()));
        model.addAttribute("documents", docs);
        model.addAttribute("docProgress", service.documentProgress(docs));
        model.addAttribute("events", service.events(tenantId, id));
        model.addAttribute("disbursements", service.disbursements(tenantId, id));
        model.addAttribute("modd", moddService.forApplication(tenantId, id));
        model.addAttribute("showMoney", money);
        model.addAttribute("bankPayouts", money ? commissionService.forApplication(tenantId, id) : List.of());
        model.addAttribute("referralPayouts", money
                ? referralPayoutRepository.findByTenantIdAndApplicationIdAndDeletedFalseOrderByIdDesc(tenantId, id) : List.of());
        model.addAttribute("receipts", money ? financeService.receiptsForApplication(tenantId, id) : List.of());
        model.addAttribute("path", ApplicationStage.path());
        model.addAttribute("stages", ApplicationStage.values());
        model.addAttribute("docStatuses", DocumentStatus.values());
        model.addAttribute("docCategories", DocumentCategory.values());
        model.addAttribute("auditTrail", auditService.historyFor(tenantId, "LoanApplication", id));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("whatsappLink", WhatsApp.link(a.getCustomerMobile(), statusMessage(a, docs)));
        return "application/view";
    }

    @PostMapping("/applications/{id}/stage")
    public String stage(@PathVariable Long id, @RequestParam ApplicationStage stage,
                        @RequestParam(required = false) String remarks, RedirectAttributes ra) {
        if (!canTouch(id)) return "redirect:/applications";
        String error = service.moveStage(SecurityUtils.currentTenantId(), id, stage, remarks);
        if (error != null) ra.addFlashAttribute("error", error);
        else ra.addFlashAttribute("toast", "File moved to " + stage.getLabel() + ".");
        return back(id);
    }

    @PostMapping("/applications/{id}/login")
    public String login(@PathVariable Long id, @RequestParam String bankReference,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate loginDate,
                        @RequestParam(required = false) String remarks, RedirectAttributes ra) {
        if (!canTouch(id)) return "redirect:/applications";
        service.recordLogin(SecurityUtils.currentTenantId(), id, bankReference, loginDate, remarks);
        ra.addFlashAttribute("toast", "Bank login recorded.");
        return back(id);
    }

    @PostMapping("/applications/{id}/sanction")
    public String sanction(@PathVariable Long id, @RequestParam BigDecimal sanctionedAmount,
                           @RequestParam(required = false) BigDecimal sanctionedRate,
                           @RequestParam(required = false) Integer sanctionedTenureYears,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sanctionDate,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sanctionValidTill,
                           @RequestParam(required = false) String remarks, RedirectAttributes ra) {
        if (!canTouch(id)) return "redirect:/applications";
        service.recordSanction(SecurityUtils.currentTenantId(), id, sanctionedAmount, sanctionedRate,
                sanctionedTenureYears, sanctionDate, sanctionValidTill, remarks);
        ra.addFlashAttribute("toast", "Sanction recorded.");
        return back(id);
    }

    @PostMapping("/applications/{id}/disburse")
    public String disburse(@PathVariable Long id, @ModelAttribute Disbursement form, RedirectAttributes ra) {
        if (!canTouch(id)) return "redirect:/applications";
        String error = service.addDisbursement(SecurityUtils.currentTenantId(), id, form);
        if (error != null) ra.addFlashAttribute("error", error);
        else ra.addFlashAttribute("toast", "Disbursement recorded. Bank payout and referral payout raised.");
        return back(id);
    }

    @PostMapping("/applications/{id}/note")
    public String note(@PathVariable Long id, @RequestParam(defaultValue = "NOTE") String type,
                       @RequestParam String remarks, RedirectAttributes ra) {
        if (!canTouch(id)) return "redirect:/applications";
        service.addNote(SecurityUtils.currentTenantId(), id, type, remarks);
        ra.addFlashAttribute("toast", "Added to the timeline.");
        return back(id);
    }

    @PostMapping("/applications/{id}/modd")
    public String openModd(@PathVariable Long id, RedirectAttributes ra) {
        Long tenantId = SecurityUtils.currentTenantId();
        LoanApplication a = service.getVisible(tenantId, id);
        if (a == null) return "redirect:/applications";
        ModdRecord m = moddService.ensureFor(tenantId, a);
        ra.addFlashAttribute("toast", "MODD job " + m.getCode() + " ready.");
        return "redirect:/modd/" + IdCodec.tok(m.getId());
    }

    @PostMapping("/applications/{id}/archive")
    public String archive(@PathVariable Long id, RedirectAttributes ra) {
        if (!canTouch(id)) return "redirect:/applications";
        service.archive(SecurityUtils.currentTenantId(), id);
        ra.addFlashAttribute("toast", "Loan file archived.");
        return "redirect:/applications";
    }

    // ------------------------------------------------------------------ documents

    @PostMapping("/applications/{id}/documents/add")
    public String addDocument(@PathVariable Long id, @RequestParam String docName,
                              @RequestParam(required = false) DocumentCategory category,
                              @RequestParam(defaultValue = "false") boolean mandatory, RedirectAttributes ra) {
        if (!canTouch(id)) return "redirect:/applications";
        service.addDocument(SecurityUtils.currentTenantId(), id, docName, category, mandatory);
        ra.addFlashAttribute("toast", "Checklist line added.");
        return back(id) + "#documents";
    }

    @PostMapping("/applications/documents/{docId}/status")
    public String documentStatus(@PathVariable Long docId, @RequestParam DocumentStatus status,
                                 @RequestParam(required = false) String remarks, RedirectAttributes ra) {
        ApplicationDocument d = service.getDocument(SecurityUtils.currentTenantId(), docId);
        if (d == null || !canTouch(d.getApplicationId())) return "redirect:/applications";
        service.setDocumentStatus(SecurityUtils.currentTenantId(), docId, status, remarks);
        ra.addFlashAttribute("toast", d.getDocName() + ": " + status.getLabel());
        return back(d.getApplicationId()) + "#documents";
    }

    @PostMapping("/applications/documents/{docId}/upload")
    public String upload(@PathVariable Long docId, @RequestParam("file") MultipartFile file, RedirectAttributes ra) throws IOException {
        Long tenantId = SecurityUtils.currentTenantId();
        ApplicationDocument d = service.getDocument(tenantId, docId);
        if (d == null || !canTouch(d.getApplicationId())) return "redirect:/applications";
        if (file == null || file.isEmpty()) {
            ra.addFlashAttribute("error", "Choose a file to upload.");
        } else {
            String error = service.uploadDocument(tenantId, docId, file.getOriginalFilename(), file.getContentType(), file.getBytes());
            if (error != null) ra.addFlashAttribute("error", error);
            else ra.addFlashAttribute("toast", d.getDocName() + " uploaded.");
        }
        return back(d.getApplicationId()) + "#documents";
    }

    /** Streams a stored document - tenant-checked before a single byte is read. */
    @GetMapping("/applications/documents/{docId}/file")
    public ResponseEntity<ByteArrayResource> file(@PathVariable Long docId) {
        Long tenantId = SecurityUtils.currentTenantId();
        ApplicationDocument d = service.getDocument(tenantId, docId);
        if (d == null || !d.isHasFile() || !storageService.belongsTo(d.getFileKey(), tenantId) || !canTouch(d.getApplicationId())) {
            return ResponseEntity.notFound().build();
        }
        MediaType type;
        try {
            type = d.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(d.getContentType());
        } catch (Exception e) {
            type = MediaType.APPLICATION_OCTET_STREAM;
        }
        String name = d.getFileName() == null ? "document" : d.getFileName().replace("\"", "");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + name + "\"")
                .contentType(type)
                .body(new ByteArrayResource(storageService.download(d.getFileKey())));
    }

    @PostMapping("/applications/documents/{docId}/remove")
    public String removeDocument(@PathVariable Long docId, RedirectAttributes ra) {
        Long tenantId = SecurityUtils.currentTenantId();
        ApplicationDocument d = service.getDocument(tenantId, docId);
        if (d == null || !canTouch(d.getApplicationId())) return "redirect:/applications";
        service.removeDocument(tenantId, docId);
        ra.addFlashAttribute("toast", "Checklist line removed.");
        return back(d.getApplicationId()) + "#documents";
    }

    // ------------------------------------------------------------------ helpers

    private boolean canTouch(Long applicationId) {
        return service.getVisible(SecurityUtils.currentTenantId(), applicationId) != null;
    }

    private boolean isMoney() {
        Role role = SecurityUtils.currentRole();
        return role != null && role.isMoney();
    }

    private String back(Long id) {
        return "redirect:/applications/" + IdCodec.tok(id);
    }

    /** A status update the executive can send the customer on WhatsApp in one tap. */
    private String statusMessage(LoanApplication a, List<ApplicationDocument> docs) {
        StringBuilder sb = new StringBuilder("Dear ").append(a.getCustomerName()).append(", update on your ")
                .append(a.getLoanTypeLabel().toLowerCase())
                .append(a.getLenderName() == null ? "" : " with " + a.getLenderName())
                .append(" (file ").append(a.getCode()).append("): ").append(a.getStageLabel()).append(".");
        if (a.getStage() == ApplicationStage.DOCS_PENDING) {
            List<String> pending = docs.stream().filter(d -> d.isMandatory() && d.getStatus() == DocumentStatus.PENDING)
                    .map(ApplicationDocument::getDocName).limit(8).toList();
            if (!pending.isEmpty()) sb.append(" Documents still needed: ").append(String.join(", ", pending)).append(".");
        }
        return sb.toString();
    }
}
