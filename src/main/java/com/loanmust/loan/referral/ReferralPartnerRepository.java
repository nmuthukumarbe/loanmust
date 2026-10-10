package com.loanmust.loan.referral;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReferralPartnerRepository extends JpaRepository<ReferralPartner, Long> {

    Optional<ReferralPartner> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    /** Public referral links resolve the partner (and through it the tenant) by code. */
    Optional<ReferralPartner> findFirstByCodeIgnoreCaseAndActiveTrueAndDeletedFalse(String code);

    Optional<ReferralPartner> findFirstByTenantIdAndMobileAndDeletedFalse(Long tenantId, String mobile);

    List<ReferralPartner> findByTenantIdAndDeletedFalseOrderByNameAsc(Long tenantId);

    List<ReferralPartner> findByTenantIdAndActiveTrueAndDeletedFalseOrderByNameAsc(Long tenantId);

    long countByTenantId(Long tenantId);

    @Query("""
            select r from ReferralPartner r
            where r.tenantId = :tenantId and r.deleted = false
              and (:q = '' or lower(r.name) like lower(concat('%', :q, '%'))
                   or lower(r.mobile) like lower(concat('%', :q, '%'))
                   or lower(r.code) like lower(concat('%', :q, '%')))
              and (:partnerType is null or r.partnerType = :partnerType)
            order by r.name asc
            """)
    Page<ReferralPartner> search(@Param("tenantId") Long tenantId, @Param("q") String q,
                                 @Param("partnerType") PartnerType partnerType, Pageable pageable);
}
