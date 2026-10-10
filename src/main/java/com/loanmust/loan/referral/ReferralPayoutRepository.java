package com.loanmust.loan.referral;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReferralPayoutRepository extends JpaRepository<ReferralPayout, Long> {

    Optional<ReferralPayout> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    Optional<ReferralPayout> findFirstByTenantIdAndDisbursementIdAndDeletedFalse(Long tenantId, Long disbursementId);

    List<ReferralPayout> findByTenantIdAndPartnerIdAndDeletedFalseOrderByIdDesc(Long tenantId, Long partnerId);

    List<ReferralPayout> findByTenantIdAndApplicationIdAndDeletedFalseOrderByIdDesc(Long tenantId, Long applicationId);

    long countByTenantId(Long tenantId);

    long countByTenantIdAndStatusAndDeletedFalse(Long tenantId, PayoutStatus status);

    @Query("""
            select coalesce(sum(p.netAmount), 0) from ReferralPayout p
            where p.tenantId = :tenantId and p.deleted = false and p.status in :statuses
            """)
    BigDecimal sumNetByStatuses(@Param("tenantId") Long tenantId, @Param("statuses") List<PayoutStatus> statuses);

    @Query("""
            select coalesce(sum(p.netAmount), 0) from ReferralPayout p
            where p.tenantId = :tenantId and p.deleted = false and p.partnerId = :partnerId and p.status in :statuses
            """)
    BigDecimal sumNetForPartner(@Param("tenantId") Long tenantId, @Param("partnerId") Long partnerId,
                                @Param("statuses") List<PayoutStatus> statuses);

    @Query("""
            select coalesce(sum(p.netAmount), 0) from ReferralPayout p
            where p.tenantId = :tenantId and p.deleted = false and p.status = com.loanmust.loan.referral.PayoutStatus.PAID
              and p.paidOn between :from and :to
            """)
    BigDecimal sumPaidBetween(@Param("tenantId") Long tenantId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select p from ReferralPayout p
            where p.tenantId = :tenantId and p.deleted = false
              and (:q = '' or lower(coalesce(p.partnerName, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(p.customerName, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(p.applicationCode, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(p.code, '')) like lower(concat('%', :q, '%')))
              and (:status is null or p.status = :status)
              and (:partnerId is null or p.partnerId = :partnerId)
            order by p.id desc
            """)
    Page<ReferralPayout> search(@Param("tenantId") Long tenantId, @Param("q") String q,
                                @Param("status") PayoutStatus status, @Param("partnerId") Long partnerId,
                                Pageable pageable);

    List<ReferralPayout> findByTenantIdAndDeletedFalseOrderByIdDesc(Long tenantId);
}
