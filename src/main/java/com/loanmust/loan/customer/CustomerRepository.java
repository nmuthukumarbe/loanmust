package com.loanmust.loan.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    Optional<Customer> findFirstByTenantIdAndMobileAndDeletedFalse(Long tenantId, String mobile);

    List<Customer> findByTenantIdAndDeletedFalseOrderByNameAsc(Long tenantId);

    List<Customer> findByTenantIdAndReferralPartnerIdAndDeletedFalseOrderByIdDesc(Long tenantId, Long referralPartnerId);

    long countByTenantId(Long tenantId);

    long countByTenantIdAndDeletedFalse(Long tenantId);

    @Query("""
            select c from Customer c
            where c.tenantId = :tenantId and c.deleted = false
              and (:q = '' or lower(c.name) like lower(concat('%', :q, '%'))
                   or lower(c.mobile) like lower(concat('%', :q, '%'))
                   or lower(coalesce(c.code, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(c.pan, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(c.email, '')) like lower(concat('%', :q, '%')))
              and (:employmentType is null or c.employmentType = :employmentType)
              and (:ownerUserId is null or c.ownerUserId = :ownerUserId)
            order by c.id desc
            """)
    Page<Customer> search(@Param("tenantId") Long tenantId,
                          @Param("q") String q,
                          @Param("employmentType") EmploymentType employmentType,
                          @Param("ownerUserId") Long ownerUserId,
                          Pageable pageable);
}
