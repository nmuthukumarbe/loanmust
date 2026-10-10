package com.loanmust.loan.access;

import com.loanmust.loan.user.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central authority for "may this role do this on this screen?".
 *
 * Model: OWNER is the super admin and always has every permission (so an administrator can
 * never lock everyone out). For every other role a saved {@link RolePermission} row wins;
 * when no row exists the answer defaults to ALLOW, so the application behaves exactly as it
 * did before any restrictions were configured — administrators untick to lock things down.
 */
@Service
public class PermissionService {

    private final RolePermissionRepository repository;

    public PermissionService(RolePermissionRepository repository) {
        this.repository = repository;
    }

    /** The super admin role that can never be restricted. */
    public boolean isSuperAdmin(Role role) {
        return role == Role.OWNER;
    }

    /** Core check used by the interceptor and the templates. */
    public boolean can(Long tenantId, Role role, String moduleKey, PermissionAction action) {
        if (role == null || tenantId == null) return false;
        if (isSuperAdmin(role)) return true;
        return repository.findByTenantIdAndRoleAndModuleKeyAndDeletedFalse(tenantId, role, moduleKey)
                .map(rp -> rp.allows(action))
                .orElse(true);   // default-allow until an administrator restricts it
    }

    public boolean can(Long tenantId, Role role, AppModule module, PermissionAction action) {
        return can(tenantId, role, module.getKey(), action);
    }

    /**
     * The full matrix for one role, keyed by module then action, for rendering the grid.
     * Missing rows render as all-allowed (matching {@link #can}).
     */
    public Map<String, Map<PermissionAction, Boolean>> matrix(Long tenantId, Role role) {
        Map<String, RolePermission> saved = new LinkedHashMap<>();
        for (RolePermission rp : repository.findByTenantIdAndRoleAndDeletedFalse(tenantId, role)) {
            saved.put(rp.getModuleKey(), rp);
        }
        Map<String, Map<PermissionAction, Boolean>> out = new LinkedHashMap<>();
        boolean sa = isSuperAdmin(role);
        for (AppModule module : AppModule.values()) {
            Map<PermissionAction, Boolean> row = new EnumMap<>(PermissionAction.class);
            RolePermission rp = saved.get(module.getKey());
            for (PermissionAction action : PermissionAction.values()) {
                row.put(action, sa || rp == null || rp.allows(action));
            }
            out.put(module.getKey(), row);
        }
        return out;
    }

    /** Persists the grid for one role. Never stores the super admin (always all-allowed). */
    @Transactional
    public void save(Long tenantId, Role role, Map<String, List<String>> grantedByModule, String actor) {
        if (isSuperAdmin(role)) return;
        for (AppModule module : AppModule.values()) {
            List<String> granted = grantedByModule.getOrDefault(module.getKey(), List.of());
            RolePermission rp = repository
                    .findByTenantIdAndRoleAndModuleKeyAndDeletedFalse(tenantId, role, module.getKey())
                    .orElseGet(() -> {
                        RolePermission n = new RolePermission();
                        n.setTenantId(tenantId);
                        n.setRole(role);
                        n.setModuleKey(module.getKey());
                        n.setCreatedBy(actor);
                        return n;
                    });
            for (PermissionAction action : PermissionAction.values()) {
                rp.set(action, granted.contains(action.name()));
            }
            rp.setModifiedBy(actor);
            repository.save(rp);
        }
    }
}
