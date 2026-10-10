package com.loanmust.loan.application;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DisbursementRepository extends JpaRepository<Disbursement, Long> {

    Optional<Disbursement> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    List<Disbursement> findByTenantIdAndApplicationIdAndDeletedFalseOrderByDisbursedOnAscIdAsc(Long tenantId, Long applicationId);

    List<Disbursement> findByTenantIdAndDisbursedOnBetweenAndDeletedFalseOrderByDisbursedOnAsc(Long tenantId, LocalDate from, LocalDate to);

    long countByTenantIdAndApplicationIdAndDeletedFalse(Long tenantId, Long applicationId);

    @Query("""
            select coalesce(sum(d.amount), 0) from Disbursement d
            where d.tenantId = :tenantId and d.deleted = false and d.applicationId = :applicationId
            """)
    BigDecimal totalFor(@Param("tenantId") Long tenantId, @Param("applicationId") Long applicationId);

    @Query("""
            select coalesce(sum(d.amount), 0) from Disbursement d
            where d.tenantId = :tenantId and d.deleted = false and d.disbursedOn between :from and :to
            """)
    BigDecimal sumBetween(@Param("tenantId") Long tenantId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select count(distinct d.applicationId) from Disbursement d
            where d.tenantId = :tenantId and d.deleted = false and d.disbursedOn between :from and :to
            """)
    long filesBetween(@Param("tenantId") Long tenantId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
