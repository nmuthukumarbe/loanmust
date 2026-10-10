package com.loanmust.loan.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndDeletedFalse(String email);

    Optional<User> findByUsernameAndDeletedFalse(String username);

    Optional<User> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    boolean existsByEmail(String email);

    List<User> findByTenantIdAndDeletedFalseOrderByFullNameAsc(Long tenantId);

    List<User> findByTenantIdAndEnabledTrueAndDeletedFalseOrderByFullNameAsc(Long tenantId);

    long countByTenantIdAndDeletedFalse(Long tenantId);

    @Query("""
            select u from User u
            where u.tenantId = :tenantId and u.deleted = false
              and (:q = '' or lower(u.fullName) like lower(concat('%', :q, '%'))
                   or lower(u.email) like lower(concat('%', :q, '%'))
                   or lower(coalesce(u.phone, '')) like lower(concat('%', :q, '%')))
            order by u.fullName asc
            """)
    Page<User> search(@Param("tenantId") Long tenantId, @Param("q") String q, Pageable pageable);
}
