package com.loanmust.loan.commission;

import com.loanmust.loan.common.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BankPayoutRepository extends JpaRepository<BankPayout, Long> {

    Optional<BankPayout> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    Optional<BankPayout> findFirstByTenantIdAndDisbursementIdAndDeletedFalse(Long tenantId, Long disbursementId);

    List<BankPayout> findByTenantIdAndApplicationIdAndDeletedFalseOrderByIdDesc(Long tenantId, Long applicationId);

    List<BankPayout> findByTenantIdAndDeletedFalseOrderByIdDesc(Long tenantId);

    @Query("""
            select coalesce(sum(b.expectedAmount), 0) from BankPayout b
            where b.tenantId = :tenantId and b.deleted = false and b.status in :statuses
            """)
    BigDecimal sumExpectedByStatuses(@Param("tenantId") Long tenantId, @Param("statuses") List<PaymentStatus> statuses);

    @Query("""
            select coalesce(sum(b.expectedAmount - coalesce(b.receivedAmount, 0)), 0) from BankPayout b
            where b.tenantId = :tenantId and b.deleted = false and b.status in :statuses
            """)
    BigDecimal sumOutstanding(@Param("tenantId") Long tenantId, @Param("statuses") List<PaymentStatus> statuses);

    @Query("""
            select coalesce(sum(b.receivedAmount), 0) from BankPayout b
            where b.tenantId = :tenantId and b.deleted = false
            """)
    BigDecimal sumReceived(@Param("tenantId") Long tenantId);

    @Query("""
            select coalesce(sum(b.receivedAmount), 0) from BankPayout b
            where b.tenantId = :tenantId and b.deleted = false and b.receivedOn between :from and :to
            """)
    BigDecimal sumReceivedBetween(@Param("tenantId") Long tenantId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select coalesce(sum(b.expectedAmount), 0) from BankPayout b
            where b.tenantId = :tenantId and b.deleted = false and b.disbursedOn between :from and :to
            """)
    BigDecimal sumEarnedBetween(@Param("tenantId") Long tenantId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select b from BankPayout b
            where b.tenantId = :tenantId and b.deleted = false
              and (:q = '' or lower(coalesce(b.customerName, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(b.applicationCode, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(b.invoiceNumber, '')) like lower(concat('%', :q, '%')))
              and (:status is null or b.status = :status)
              and (:lenderId is null or b.lenderId = :lenderId)
            order by b.id desc
            """)
    Page<BankPayout> search(@Param("tenantId") Long tenantId, @Param("q") String q,
                            @Param("status") PaymentStatus status, @Param("lenderId") Long lenderId,
                            Pageable pageable);
}
