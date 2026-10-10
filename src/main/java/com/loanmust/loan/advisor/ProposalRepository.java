package com.loanmust.loan.advisor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {

    Optional<Proposal> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    Optional<Proposal> findByTokenAndDeletedFalse(String token);

    List<Proposal> findByTenantIdAndCustomerIdAndDeletedFalseOrderByIdDesc(Long tenantId, Long customerId);

    List<Proposal> findByTenantIdAndLeadIdAndDeletedFalseOrderByIdDesc(Long tenantId, Long leadId);

    long countByTenantId(Long tenantId);

    @Query("""
            select p from Proposal p
            where p.tenantId = :tenantId and p.deleted = false
              and (:q = '' or lower(coalesce(p.customerName, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(p.customerMobile, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(p.code, '')) like lower(concat('%', :q, '%')))
            order by p.id desc
            """)
    Page<Proposal> search(@Param("tenantId") Long tenantId, @Param("q") String q, Pageable pageable);
}
