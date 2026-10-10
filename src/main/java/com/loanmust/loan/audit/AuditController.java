package com.loanmust.loan.audit;

import com.loanmust.loan.security.SecurityUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Tenant-wide audit log with the standard search / filter / paginate treatment. */
@Controller
public class AuditController {

    private static final int PAGE_SIZE = 25;

    private final AuditLogRepository repository;
    private final com.loanmust.loan.reports.ExcelExporter excelExporter;

    public AuditController(AuditLogRepository repository, com.loanmust.loan.reports.ExcelExporter excelExporter) {
        this.repository = repository;
        this.excelExporter = excelExporter;
    }

    /** The latest 5,000 audit entries as Excel, for an auditor or the owner's records. */
    @GetMapping("/audit/export")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.ByteArrayResource> export() {
        Long tenantId = SecurityUtils.currentTenantId();
        java.util.List<java.util.List<Object>> rows = new java.util.ArrayList<>();
        for (AuditLog a : repository.search(tenantId, "", "", "", null, null, PageRequest.of(0, 5000)).getContent()) {
            rows.add(java.util.Arrays.asList(a.getCreatedAt(), a.getUserName(), a.getUserEmail(), a.getAction(),
                    a.getEntityType(), a.getEntityLabel(), a.getFieldName(), a.getOldValue(), a.getNewValue(), a.getRemarks()));
        }
        return excelExporter.download(new com.loanmust.loan.reports.ReportTable("Audit log",
                java.util.List.of("When", "User", "Email", "Action", "Record type", "Record", "Field", "Old value", "New value", "Remarks"),
                rows), "loanmust-audit-log.xlsx");
    }

    @GetMapping("/audit")
    public String page(@RequestParam(required = false) String q,
                       @RequestParam(required = false) String entityType,
                       @RequestParam(required = false) String action,
                       @RequestParam(required = false) String from,
                       @RequestParam(required = false) String to,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        LocalDateTime fromAt = parse(from, true);
        LocalDateTime toAt = parse(to, false);

        model.addAttribute("activeMenu", "audit");
        model.addAttribute("logPage", repository.search(
                tenantId,
                q == null ? "" : q.trim(),
                entityType == null ? "" : entityType,
                action == null ? "" : action,
                fromAt, toAt,
                PageRequest.of(Math.max(0, page), PAGE_SIZE)));
        model.addAttribute("q", q);
        model.addAttribute("entityType", entityType);
        model.addAttribute("action", action);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("entityTypes", repository.distinctEntityTypes(tenantId));
        model.addAttribute("actions", java.util.List.of(
                "CREATE", "UPDATE", "STATUS", "ASSIGN", "ARCHIVE", "PAYMENT", "SEND", "EXPORT", "LOGIN"));
        return "admin/audit";
    }

    private LocalDateTime parse(String value, boolean startOfDay) {
        if (value == null || value.isBlank()) return null;
        try {
            LocalDate date = LocalDate.parse(value);
            return startOfDay ? date.atStartOfDay() : date.atTime(23, 59, 59);
        } catch (Exception e) {
            return null;
        }
    }
}
