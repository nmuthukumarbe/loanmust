package com.loanmust.loan.builder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    List<Project> findByTenantIdAndBuilderIdAndDeletedFalseOrderByNameAsc(Long tenantId, Long builderId);

    List<Project> findByTenantIdAndDeletedFalseOrderByBuilderNameAscNameAsc(Long tenantId);

    long countByTenantIdAndBuilderIdAndDeletedFalse(Long tenantId, Long builderId);
}
