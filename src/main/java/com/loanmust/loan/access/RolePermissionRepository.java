package com.loanmust.loan.access;

import com.loanmust.loan.user.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findByTenantIdAndRoleAndDeletedFalse(Long tenantId, Role role);

    Optional<RolePermission> findByTenantIdAndRoleAndModuleKeyAndDeletedFalse(
            Long tenantId, Role role, String moduleKey);
}
