package com.loanmust.loan.builder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BuilderRepository extends JpaRepository<Builder, Long> {

    Optional<Builder> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    List<Builder> findByTenantIdAndDeletedFalseOrderByNameAsc(Long tenantId);

    List<Builder> findByTenantIdAndActiveTrueAndDeletedFalseOrderByNameAsc(Long tenantId);
}
