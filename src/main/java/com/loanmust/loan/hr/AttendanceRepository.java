package com.loanmust.loan.hr;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findFirstByTenantIdAndEmployeeIdAndWorkDateAndDeletedFalse(Long tenantId, Long employeeId, LocalDate workDate);

    List<Attendance> findByTenantIdAndWorkDateAndDeletedFalse(Long tenantId, LocalDate workDate);

    List<Attendance> findByTenantIdAndWorkDateBetweenAndDeletedFalse(Long tenantId, LocalDate from, LocalDate to);

    List<Attendance> findByTenantIdAndEmployeeIdAndWorkDateBetweenAndDeletedFalseOrderByWorkDateAsc(
            Long tenantId, Long employeeId, LocalDate from, LocalDate to);
}
