package com.loanmust.loan.application;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationEventRepository extends JpaRepository<ApplicationEvent, Long> {

    List<ApplicationEvent> findByTenantIdAndApplicationIdAndDeletedFalseOrderByIdDesc(Long tenantId, Long applicationId);
}
