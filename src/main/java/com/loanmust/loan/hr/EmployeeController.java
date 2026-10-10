package com.loanmust.loan.hr;

import com.loanmust.loan.audit.AuditService;
import com.loanmust.loan.common.IdCodec;
import com.loanmust.loan.security.SecurityUtils;
import com.loanmust.loan.user.UserRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/** Staff records and daily attendance. */
@Controller
public class EmployeeController {

    private final EmployeeService employeeService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public EmployeeController(EmployeeService employeeService, UserRepository userRepository, AuditService auditService) {
        this.employeeService = employeeService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @GetMapping("/employees")
    public String list(Model model) {
        model.addAttribute("activeMenu", "employees");
        model.addAttribute("employees", employeeService.all(SecurityUtils.currentTenantId()));
        return "hr/employee-list";
    }

    @GetMapping("/employees/new")
    public String createForm(Model model) {
        Employee e = new Employee();
        e.setActive(true);
        e.setJoiningDate(LocalDate.now());
        e.setDepartment("Data Entry");
        return form(e, model);
    }

    @GetMapping("/employees/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Employee e = employeeService.get(SecurityUtils.currentTenantId(), id);
        if (e == null) return "redirect:/employees";
        return form(e, model);
    }

    private String form(Employee e, Model model) {
        model.addAttribute("activeMenu", "employees");
        model.addAttribute("employee", e);
        model.addAttribute("users", userRepository.findByTenantIdAndDeletedFalseOrderByFullNameAsc(SecurityUtils.currentTenantId()));
        model.addAttribute("departments", List.of("Data Entry", "Sales", "Processing", "Accounts", "Administration"));
        return "hr/employee-form";
    }

    @PostMapping("/employees/save")
    public String save(@ModelAttribute Employee employee, RedirectAttributes ra) {
        Employee saved = employeeService.save(SecurityUtils.currentTenantId(), employee);
        if (saved == null) return "redirect:/employees";
        ra.addFlashAttribute("toast", "Employee saved.");
        return "redirect:/employees";
    }

    @PostMapping("/employees/{id}/archive")
    public String archive(@PathVariable Long id, RedirectAttributes ra) {
        employeeService.archive(SecurityUtils.currentTenantId(), id);
        ra.addFlashAttribute("toast", "Employee archived.");
        return "redirect:/employees";
    }

    // ------------------------------------------------------------------ attendance

    @GetMapping("/attendance")
    public String attendance(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                             @RequestParam(required = false) String month, Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        LocalDate day = date == null ? LocalDate.now() : date;
        YearMonth ym;
        try {
            ym = month == null || month.isBlank() ? YearMonth.from(day) : YearMonth.parse(month);
        } catch (Exception e) {
            ym = YearMonth.from(day);
        }
        Map<Long, Attendance> marks = employeeService.day(tenantId, day);
        model.addAttribute("activeMenu", "attendance");
        model.addAttribute("date", day);
        model.addAttribute("employees", employeeService.active(tenantId));
        model.addAttribute("marks", marks);
        model.addAttribute("statuses", AttendanceStatus.values());
        model.addAttribute("month", ym.toString());
        model.addAttribute("monthLabel", ym.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " " + ym.getYear());
        model.addAttribute("daysInMonth", ym.lengthOfMonth());
        model.addAttribute("monthRows", employeeService.month(tenantId, ym));
        return "hr/attendance";
    }

    /** Saves the whole day sheet: one status (and optional times) per employee. */
    @PostMapping("/attendance/save")
    public String saveDay(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                          @RequestParam Map<String, String> params, RedirectAttributes ra) {
        Long tenantId = SecurityUtils.currentTenantId();
        int n = 0;
        for (Employee e : employeeService.active(tenantId)) {
            String status = params.get("status_" + e.getId());
            if (status == null || status.isBlank()) continue;
            try {
                employeeService.mark(tenantId, e.getId(), date, AttendanceStatus.valueOf(status),
                        time(params.get("in_" + e.getId())), time(params.get("out_" + e.getId())),
                        params.get("remarks_" + e.getId()));
                n++;
            } catch (IllegalArgumentException ignored) {
                // unknown status value - skip that row
            }
        }
        auditService.log("UPDATE", "Attendance", 0L, date.toString(), "Attendance marked for " + n + " employee(s)");
        ra.addFlashAttribute("toast", "Attendance saved for " + n + " employee(s).");
        return "redirect:/attendance?date=" + date;
    }

    private static LocalTime time(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return LocalTime.parse(v);
        } catch (Exception e) {
            return null;
        }
    }
}
