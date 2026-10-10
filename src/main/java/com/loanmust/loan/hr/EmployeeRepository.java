package com.loanmust.loan.hr;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    List<Employee> findByTenantIdAndDeletedFalseOrderByNameAsc(Long tenantId);

    List<Employee> findByTenantIdAndActiveTrueAndDeletedFalseOrderByNameAsc(Long tenantId);

    long countByTenantId(Long tenantId);
}
