package com.loanmust.loan.finance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FeeReceiptRepository extends JpaRepository<FeeReceipt, Long> {

    Optional<FeeReceipt> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    List<FeeReceipt> findByTenantIdAndCustomerIdAndDeletedFalseOrderByReceivedOnDesc(Long tenantId, Long customerId);

    List<FeeReceipt> findByTenantIdAndApplicationIdAndDeletedFalseOrderByReceivedOnDesc(Long tenantId, Long applicationId);

    List<FeeReceipt> findByTenantIdAndReceivedOnBetweenAndDeletedFalseOrderByReceivedOnAsc(Long tenantId, LocalDate from, LocalDate to);

    long countByTenantId(Long tenantId);

    @Query("""
            select coalesce(sum(f.amount), 0) from FeeReceipt f
            where f.tenantId = :tenantId and f.deleted = false and f.receivedOn between :from and :to
            """)
    BigDecimal sumBetween(@Param("tenantId") Long tenantId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select f from FeeReceipt f
            where f.tenantId = :tenantId and f.deleted = false
              and (:q = '' or lower(f.customerName) like lower(concat('%', :q, '%'))
                   or lower(coalesce(f.receiptNo, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(f.applicationCode, '')) like lower(concat('%', :q, '%')))
              and (:from is null or f.receivedOn >= :from)
              and (:to is null or f.receivedOn <= :to)
            order by f.receivedOn desc, f.id desc
            """)
    Page<FeeReceipt> search(@Param("tenantId") Long tenantId, @Param("q") String q,
                            @Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);
}
