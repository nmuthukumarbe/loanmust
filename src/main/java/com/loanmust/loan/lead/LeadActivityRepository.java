package com.loanmust.loan.lead;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeadActivityRepository extends JpaRepository<LeadActivity, Long> {

    List<LeadActivity> findByTenantIdAndLeadIdAndDeletedFalseOrderByIdDesc(Long tenantId, Long leadId);

    long countByTenantIdAndLeadIdAndDeletedFalse(Long tenantId, Long leadId);
}
