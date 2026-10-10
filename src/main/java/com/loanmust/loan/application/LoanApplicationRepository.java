package com.loanmust.loan.application;

import com.loanmust.loan.lender.LoanType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    Optional<LoanApplication> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    List<LoanApplication> findByTenantIdAndCustomerIdAndDeletedFalseOrderByIdDesc(Long tenantId, Long customerId);

    List<LoanApplication> findByTenantIdAndReferralPartnerIdAndDeletedFalseOrderByIdDesc(Long tenantId, Long partnerId);

    List<LoanApplication> findByTenantIdAndBuilderIdAndDeletedFalseOrderByIdDesc(Long tenantId, Long builderId);

    List<LoanApplication> findByTenantIdAndLenderIdAndDeletedFalseOrderByIdDesc(Long tenantId, Long lenderId);

    List<LoanApplication> findByTenantIdAndDeletedFalseOrderByIdDesc(Long tenantId);

    long countByTenantId(Long tenantId);

    long countByTenantIdAndStageAndDeletedFalse(Long tenantId, ApplicationStage stage);

    long countByTenantIdAndStageInAndDeletedFalse(Long tenantId, List<ApplicationStage> stages);

    long countByTenantIdAndStageInAndAssignedUserIdAndDeletedFalse(Long tenantId, List<ApplicationStage> stages, Long userId);

    /** Board column: newest-moved first, owner filter optional. */
    @Query("""
            select a from LoanApplication a
            where a.tenantId = :tenantId and a.deleted = false and a.stage = :stage
              and (:ownerId is null or a.assignedUserId = :ownerId)
            order by a.stageChangedAt desc, a.id desc
            """)
    List<LoanApplication> byStage(@Param("tenantId") Long tenantId, @Param("stage") ApplicationStage stage,
                                  @Param("ownerId") Long ownerId, Pageable pageable);

    @Query("""
            select coalesce(sum(coalesce(a.sanctionedAmount, a.requestedAmount)), 0) from LoanApplication a
            where a.tenantId = :tenantId and a.deleted = false and a.stage = :stage
              and (:ownerId is null or a.assignedUserId = :ownerId)
            """)
    BigDecimal valueInStage(@Param("tenantId") Long tenantId, @Param("stage") ApplicationStage stage,
                            @Param("ownerId") Long ownerId);

    @Query("""
            select a from LoanApplication a
            where a.tenantId = :tenantId and a.deleted = false
              and (:q = '' or lower(a.customerName) like lower(concat('%', :q, '%'))
                   or lower(coalesce(a.customerMobile, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(a.code, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(a.bankReference, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(a.lenderName, '')) like lower(concat('%', :q, '%')))
              and (:stage is null or a.stage = :stage)
              and (:loanType is null or a.loanType = :loanType)
              and (:lenderId is null or a.lenderId = :lenderId)
              and (:builderId is null or a.builderId = :builderId)
              and (:ownerId is null or a.assignedUserId = :ownerId)
            order by a.id desc
            """)
    Page<LoanApplication> search(@Param("tenantId") Long tenantId,
                                 @Param("q") String q,
                                 @Param("stage") ApplicationStage stage,
                                 @Param("loanType") LoanType loanType,
                                 @Param("lenderId") Long lenderId,
                                 @Param("builderId") Long builderId,
                                 @Param("ownerId") Long ownerId,
                                 Pageable pageable);

    /** Sanctioned but not yet disbursed files whose sanction letter lapses by {@code until}. */
    @Query("""
            select a from LoanApplication a
            where a.tenantId = :tenantId and a.deleted = false
              and a.stage in :stages and a.sanctionValidTill is not null and a.sanctionValidTill <= :until
              and (a.disbursementStatus is null or a.disbursementStatus = com.loanmust.loan.application.DisbursementStatus.NOT_STARTED)
            order by a.sanctionValidTill asc
            """)
    List<LoanApplication> sanctionsExpiring(@Param("tenantId") Long tenantId, @Param("until") LocalDate until,
                                            @Param("stages") List<ApplicationStage> stages);

    @Query("""
            select a.stage, count(a) from LoanApplication a
            where a.tenantId = :tenantId and a.deleted = false
            group by a.stage
            """)
    List<Object[]> countByStage(@Param("tenantId") Long tenantId);
}
