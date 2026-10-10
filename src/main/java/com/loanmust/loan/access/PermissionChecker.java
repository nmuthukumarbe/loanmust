package com.loanmust.loan.access;

import com.loanmust.loan.user.Role;

/**
 * Per-request view helper exposed to Thymeleaf as {@code perms}, so templates can hide
 * menu entries and action buttons the current user's role may not use, e.g.
 * {@code th:if="${perms.canView('leads')}"} or {@code ${perms.can('applications','DELETE')}}.
 */
public class PermissionChecker {

    private final PermissionService service;
    private final Long tenantId;
    private final Role role;

    public PermissionChecker(PermissionService service, Long tenantId, Role role) {
        this.service = service;
        this.tenantId = tenantId;
        this.role = role;
    }

    public boolean can(String moduleKey, String action) {
        try {
            return service.can(tenantId, role, moduleKey, PermissionAction.valueOf(action));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean canView(String moduleKey)   { return can(moduleKey, "VIEW"); }
    public boolean canAdd(String moduleKey)    { return can(moduleKey, "ADD"); }
    public boolean canEdit(String moduleKey)   { return can(moduleKey, "EDIT"); }
    public boolean canDelete(String moduleKey) { return can(moduleKey, "DELETE"); }
    public boolean canExport(String moduleKey) { return can(moduleKey, "EXPORT"); }
    public boolean canPrint(String moduleKey)  { return can(moduleKey, "PRINT"); }
    public boolean canApprove(String moduleKey){ return can(moduleKey, "APPROVE"); }
}
