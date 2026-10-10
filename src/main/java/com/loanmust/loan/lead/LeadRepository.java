package com.loanmust.loan.lead;

import com.loanmust.loan.lender.LoanType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    Optional<Lead> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    List<Lead> findByTenantIdAndMobileAndDeletedFalse(Long tenantId, String mobile);

    List<Lead> findByTenantIdAndReferralPartnerIdAndDeletedFalseOrderByIdDesc(Long tenantId, Long partnerId);

    List<Lead> findByTenantIdAndBuilderIdAndDeletedFalseOrderByIdDesc(Long tenantId, Long builderId);

    List<Lead> findByTenantIdAndDeletedFalseOrderByIdDesc(Long tenantId);

    long countByTenantId(Long tenantId);

    long countByTenantIdAndStatusInAndDeletedFalse(Long tenantId, List<LeadStatus> statuses);

    long countByTenantIdAndAssignedUserIdAndStatusInAndDeletedFalse(Long tenantId, Long userId, List<LeadStatus> statuses);

    long countByTenantIdAndCreatedAtAfterAndDeletedFalse(Long tenantId, LocalDateTime after);

    long countByTenantIdAndConvertedAtAfterAndDeletedFalse(Long tenantId, LocalDateTime after);

    @Query("""
            select l from Lead l
            where l.tenantId = :tenantId and l.deleted = false
              and l.status in :openStatuses
              and l.nextFollowUpDate is not null and l.nextFollowUpDate <= :until
              and (:ownerId is null or l.assignedUserId = :ownerId)
            order by l.nextFollowUpDate asc, l.score desc
            """)
    List<Lead> dueFollowUps(@Param("tenantId") Long tenantId, @Param("until") LocalDate until,
                            @Param("openStatuses") List<LeadStatus> openStatuses, @Param("ownerId") Long ownerId);

    @Query("""
            select l from Lead l
            where l.tenantId = :tenantId and l.deleted = false
              and (:q = '' or lower(l.name) like lower(concat('%', :q, '%'))
                   or lower(l.mobile) like lower(concat('%', :q, '%'))
                   or lower(coalesce(l.code, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(l.email, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(l.builderName, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(l.referralPartnerName, '')) like lower(concat('%', :q, '%')))
              and (:status is null or l.status = :status)
              and (:source is null or l.source = :source)
              and (:priority is null or l.priority = :priority)
              and (:loanType is null or l.loanType = :loanType)
              and (:assignedUserId is null or l.assignedUserId = :assignedUserId)
              and (:from is null or l.createdAt >= :from)
              and (:to is null or l.createdAt <= :to)
            order by l.id desc
            """)
    Page<Lead> search(@Param("tenantId") Long tenantId,
                      @Param("q") String q,
                      @Param("status") LeadStatus status,
                      @Param("source") LeadSource source,
                      @Param("priority") LeadPriority priority,
                      @Param("loanType") LoanType loanType,
                      @Param("assignedUserId") Long assignedUserId,
                      @Param("from") LocalDateTime from,
                      @Param("to") LocalDateTime to,
                      Pageable pageable);

    @Query("""
            select l.source, count(l),
                   sum(case when l.status = com.loanmust.loan.lead.LeadStatus.CONVERTED then 1 else 0 end)
            from Lead l
            where l.tenantId = :tenantId and l.deleted = false
              and (:from is null or l.createdAt >= :from)
              and (:to is null or l.createdAt <= :to)
            group by l.source
            order by count(l) desc
            """)
    List<Object[]> conversionBySource(@Param("tenantId") Long tenantId,
                                      @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
