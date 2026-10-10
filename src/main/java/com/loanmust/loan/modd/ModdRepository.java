package com.loanmust.loan.modd;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ModdRepository extends JpaRepository<ModdRecord, Long> {

    Optional<ModdRecord> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    Optional<ModdRecord> findFirstByTenantIdAndApplicationIdAndDeletedFalse(Long tenantId, Long applicationId);

    List<ModdRecord> findByTenantIdAndDeletedFalseOrderByIdDesc(Long tenantId);

    long countByTenantId(Long tenantId);

    long countByTenantIdAndStatusInAndDeletedFalse(Long tenantId, List<ModdStatus> statuses);

    @Query("""
            select m from ModdRecord m
            where m.tenantId = :tenantId and m.deleted = false
              and m.appointmentAt is not null and m.appointmentAt between :from and :to
              and m.status = com.loanmust.loan.modd.ModdStatus.APPOINTMENT_BOOKED
            order by m.appointmentAt asc
            """)
    List<ModdRecord> appointmentsBetween(@Param("tenantId") Long tenantId,
                                         @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            select m from ModdRecord m
            where m.tenantId = :tenantId and m.deleted = false
              and (:q = '' or lower(coalesce(m.customerName, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(m.applicationCode, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(m.registrationNumber, '')) like lower(concat('%', :q, '%'))
                   or lower(coalesce(m.code, '')) like lower(concat('%', :q, '%')))
              and (:status is null or m.status = :status)
            order by m.id desc
            """)
    Page<ModdRecord> search(@Param("tenantId") Long tenantId, @Param("q") String q,
                            @Param("status") ModdStatus status, Pageable pageable);
}
