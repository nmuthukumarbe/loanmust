package com.loanmust.loan.lender;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LenderRepository extends JpaRepository<Lender, Long> {

    Optional<Lender> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    List<Lender> findByTenantIdAndDeletedFalseOrderByNameAsc(Long tenantId);

    List<Lender> findByTenantIdAndActiveTrueAndDeletedFalseOrderByNameAsc(Long tenantId);

    long countByTenantIdAndDeletedFalse(Long tenantId);
}
