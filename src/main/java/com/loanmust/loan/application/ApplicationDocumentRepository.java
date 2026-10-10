package com.loanmust.loan.application;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ApplicationDocumentRepository extends JpaRepository<ApplicationDocument, Long> {

    Optional<ApplicationDocument> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    List<ApplicationDocument> findByTenantIdAndApplicationIdAndDeletedFalseOrderBySortOrderAscIdAsc(Long tenantId, Long applicationId);

    long countByTenantIdAndApplicationIdAndDeletedFalse(Long tenantId, Long applicationId);

    long countByTenantIdAndApplicationIdAndStatusAndMandatoryTrueAndDeletedFalse(Long tenantId, Long applicationId, DocumentStatus status);

    @Query("""
            select count(d) from ApplicationDocument d
            where d.tenantId = :tenantId and d.deleted = false and d.fileKey is not null
              and d.createdAt >= :from
            """)
    long countUploadsSince(@Param("tenantId") Long tenantId, @Param("from") LocalDateTime from);
}
