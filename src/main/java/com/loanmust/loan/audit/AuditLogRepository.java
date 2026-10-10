package com.loanmust.loan.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByTenantIdAndEntityTypeAndEntityIdAndDeletedFalseOrderByIdDesc(
            Long tenantId, String entityType, Long entityId);

    @Query("""
            select a from AuditLog a
            where a.tenantId = :tenantId and a.deleted = false
              and (:q = '' or lower(coalesce(a.entityLabel, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(a.userEmail, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(a.remarks, '')) like lower(concat('%', :q, '%')))
              and (:entityType = '' or a.entityType = :entityType)
              and (:action = '' or a.action = :action)
              and (:from is null or a.createdAt >= :from)
              and (:to is null or a.createdAt <= :to)
            order by a.id desc
            """)
    Page<AuditLog> search(@Param("tenantId") Long tenantId,
                          @Param("q") String q,
                          @Param("entityType") String entityType,
                          @Param("action") String action,
                          @Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to,
                          Pageable pageable);

    @Query("select distinct a.entityType from AuditLog a where a.tenantId = :tenantId and a.deleted = false order by a.entityType")
    List<String> distinctEntityTypes(@Param("tenantId") Long tenantId);

    /** Records created per user and record type in a period - the staff productivity report. */
    @Query("""
            select coalesce(a.userName, a.userEmail), a.entityType, count(a) from AuditLog a
            where a.tenantId = :tenantId and a.deleted = false and a.action = 'CREATE'
              and a.createdAt between :from and :to
            group by coalesce(a.userName, a.userEmail), a.entityType
            order by coalesce(a.userName, a.userEmail), a.entityType
            """)
    List<Object[]> createdByUser(@Param("tenantId") Long tenantId,
                                 @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            select a from AuditLog a
            where a.tenantId = :tenantId and a.deleted = false
            order by a.id desc
            """)
    List<AuditLog> recent(@Param("tenantId") Long tenantId, org.springframework.data.domain.Pageable pageable);
}
