package com.loanmust.loan.reports;

import com.loanmust.loan.audit.AuditService;
import com.loanmust.loan.security.SecurityUtils;
import com.loanmust.loan.user.Role;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Controller
public class ReportsController {

    private final ReportService reportService;
    private final ExcelExporter excelExporter;
    private final AuditService auditService;

    public ReportsController(ReportService reportService, ExcelExporter excelExporter, AuditService auditService) {
        this.reportService = reportService;
        this.excelExporter = excelExporter;
        this.auditService = auditService;
    }

    @GetMapping("/reports")
    public String index(Model model) {
        boolean money = canSeeMoney();
        List<ReportService.Report> reports = Arrays.stream(ReportService.Report.values())
                .filter(r -> money || !r.isMoney()).toList();
        model.addAttribute("activeMenu", "reports");
        model.addAttribute("reports", reports);
        return "reports/index";
    }

    @GetMapping("/reports/{key}")
    public String view(@PathVariable String key,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       Model model) {
        ReportService.Report report = ReportService.Report.fromKey(key);
        if (report == null) return "redirect:/reports";
        if (report.isMoney() && !canSeeMoney()) return "redirect:/no-access?m=reports";
        if (report.isDated() && from == null && to == null) {
            from = LocalDate.now().withDayOfMonth(1);
            to = LocalDate.now();
        }
        model.addAttribute("activeMenu", "reports");
        model.addAttribute("report", report);
        model.addAttribute("table", reportService.build(report, SecurityUtils.currentTenantId(), from, to));
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "reports/view";
    }

    @GetMapping("/reports/{key}/export")
    public ResponseEntity<ByteArrayResource> export(@PathVariable String key,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        ReportService.Report report = ReportService.Report.fromKey(key);
        if (report == null || (report.isMoney() && !canSeeMoney())) {
            return ResponseEntity.notFound().build();
        }
        ReportTable table = reportService.build(report, SecurityUtils.currentTenantId(), from, to);
        auditService.log("EXPORT", "Report", 0L, report.getTitle(), "Exported " + table.rows().size() + " rows");
        return excelExporter.download(table, "loanmust-" + report.getKey() + "-" + LocalDate.now() + ".xlsx");
    }

    private boolean canSeeMoney() {
        Role role = SecurityUtils.currentRole();
        return role != null && role.isMoney();
    }
}
