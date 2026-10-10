package com.loanmust.loan.hr;

import com.loanmust.loan.audit.AuditService;
import com.loanmust.loan.common.CodeSequenceService;
import com.loanmust.loan.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Staff records and daily attendance. */
@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final CodeSequenceService codeSequenceService;
    private final AuditService auditService;

    public EmployeeService(EmployeeRepository employeeRepository, AttendanceRepository attendanceRepository,
                           CodeSequenceService codeSequenceService, AuditService auditService) {
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.codeSequenceService = codeSequenceService;
        this.auditService = auditService;
    }

    public List<Employee> all(Long tenantId) {
        return employeeRepository.findByTenantIdAndDeletedFalseOrderByNameAsc(tenantId);
    }

    public List<Employee> active(Long tenantId) {
        return employeeRepository.findByTenantIdAndActiveTrueAndDeletedFalseOrderByNameAsc(tenantId);
    }

    public Employee get(Long tenantId, Long id) {
        return id == null ? null : employeeRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId).orElse(null);
    }

    @Transactional
    public Employee save(Long tenantId, Employee form) {
        boolean isNew = form.getId() == null;
        Employee e = isNew ? new Employee() : get(tenantId, form.getId());
        if (e == null) return null;
        if (isNew) {
            e.setTenantId(tenantId);
            e.setCode(codeSequenceService.next(tenantId, CodeSequenceService.Type.EMPLOYEE, employeeRepository.countByTenantId(tenantId)));
            e.setCreatedBy(SecurityUtils.currentUserLogin());
        }
        AuditService.Diff diff = new AuditService.Diff()
                .compare("Name", e.getName(), form.getName())
                .compare("Designation", e.getDesignation(), form.getDesignation())
                .compare("Department", e.getDepartment(), form.getDepartment())
                .compare("Salary", e.getMonthlySalary(), form.getMonthlySalary())
                .compare("Active", e.isActive(), form.isActive());
        e.setName(form.getName());
        e.setMobile(form.getMobile());
        e.setEmail(form.getEmail());
        e.setDesignation(form.getDesignation());
        e.setDepartment(form.getDepartment());
        e.setJoiningDate(form.getJoiningDate());
        e.setDateOfBirth(form.getDateOfBirth());
        e.setMonthlySalary(form.getMonthlySalary());
        e.setUserId(form.getUserId());
        e.setAddress(form.getAddress());
        e.setEmergencyContact(form.getEmergencyContact());
        e.setActive(form.isActive());
        e.setNotes(form.getNotes());
        e.setModifiedBy(SecurityUtils.currentUserLogin());
        Employee saved = employeeRepository.save(e);
        if (isNew) auditService.log("CREATE", "Employee", saved.getId(), saved.getName(), "Employee added");
        else auditService.logDiff("Employee", saved.getId(), saved.getName(), diff);
        return saved;
    }

    @Transactional
    public void archive(Long tenantId, Long id) {
        Employee e = get(tenantId, id);
        if (e == null) return;
        e.setDeleted(true);
        e.setActive(false);
        e.setModifiedBy(SecurityUtils.currentUserLogin());
        employeeRepository.save(e);
        auditService.log("ARCHIVE", "Employee", id, e.getName(), "Employee archived");
    }

    // ------------------------------------------------------------------ attendance

    /** employeeId → that day's attendance row (absent from the map = not marked yet). */
    public Map<Long, Attendance> day(Long tenantId, LocalDate date) {
        Map<Long, Attendance> map = new HashMap<>();
        for (Attendance a : attendanceRepository.findByTenantIdAndWorkDateAndDeletedFalse(tenantId, date)) {
            map.put(a.getEmployeeId(), a);
        }
        return map;
    }

    @Transactional
    public void mark(Long tenantId, Long employeeId, LocalDate date, AttendanceStatus status,
                     LocalTime checkIn, LocalTime checkOut, String remarks) {
        Employee e = get(tenantId, employeeId);
        if (e == null || date == null || status == null) return;
        Attendance a = attendanceRepository.findFirstByTenantIdAndEmployeeIdAndWorkDateAndDeletedFalse(tenantId, employeeId, date)
                .orElseGet(() -> {
                    Attendance n = new Attendance();
                    n.setTenantId(tenantId);
                    n.setEmployeeId(employeeId);
                    n.setWorkDate(date);
                    n.setCreatedBy(SecurityUtils.currentUserLogin());
                    return n;
                });
        a.setEmployeeName(e.getName());
        a.setStatus(status);
        a.setCheckIn(checkIn);
        a.setCheckOut(checkOut);
        a.setRemarks(remarks == null || remarks.isBlank() ? null : remarks.trim());
        a.setModifiedBy(SecurityUtils.currentUserLogin());
        attendanceRepository.save(a);
    }

    /** Month summary row per employee: paid days and counts by status. */
    public record MonthRow(Employee employee, Map<AttendanceStatus, Integer> counts, double paidDays, Map<Integer, String> days) {
    }

    public List<MonthRow> month(Long tenantId, YearMonth ym) {
        List<Attendance> rows = attendanceRepository.findByTenantIdAndWorkDateBetweenAndDeletedFalse(tenantId, ym.atDay(1), ym.atEndOfMonth());
        List<MonthRow> out = new java.util.ArrayList<>();
        for (Employee e : all(tenantId)) {
            Map<AttendanceStatus, Integer> counts = new LinkedHashMap<>();
            for (AttendanceStatus s : AttendanceStatus.values()) counts.put(s, 0);
            Map<Integer, String> days = new HashMap<>();
            double paid = 0;
            for (Attendance a : rows) {
                if (!a.getEmployeeId().equals(e.getId())) continue;
                counts.merge(a.getStatus(), 1, Integer::sum);
                paid += a.getStatus().getDayValue();
                days.put(a.getWorkDate().getDayOfMonth(), a.getStatus().getShortCode());
            }
            if (!e.isActive() && counts.values().stream().allMatch(c -> c == 0)) continue;
            out.add(new MonthRow(e, counts, paid, days));
        }
        return out;
    }
}
