package com.loanmust.loan.access;

import com.loanmust.loan.common.BaseEntity;
import com.loanmust.loan.user.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** One row of the permission matrix: what {@link Role} may do on one {@link AppModule}. */
@Entity
@Table(name = "role_permissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenantId", "role", "moduleKey"}))
public class RolePermission extends BaseEntity {

    @Column(nullable = false)
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false, length = 40)
    private String moduleKey;

    private boolean canView;
    private boolean canAdd;
    private boolean canEdit;
    private boolean canDelete;
    private boolean canExport;
    private boolean canPrint;
    private boolean canApprove;

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getModuleKey() { return moduleKey; }
    public void setModuleKey(String moduleKey) { this.moduleKey = moduleKey; }

    public boolean isCanView() { return canView; }
    public void setCanView(boolean canView) { this.canView = canView; }

    public boolean isCanAdd() { return canAdd; }
    public void setCanAdd(boolean canAdd) { this.canAdd = canAdd; }

    public boolean isCanEdit() { return canEdit; }
    public void setCanEdit(boolean canEdit) { this.canEdit = canEdit; }

    public boolean isCanDelete() { return canDelete; }
    public void setCanDelete(boolean canDelete) { this.canDelete = canDelete; }

    public boolean isCanExport() { return canExport; }
    public void setCanExport(boolean canExport) { this.canExport = canExport; }

    public boolean isCanPrint() { return canPrint; }
    public void setCanPrint(boolean canPrint) { this.canPrint = canPrint; }

    public boolean isCanApprove() { return canApprove; }
    public void setCanApprove(boolean canApprove) { this.canApprove = canApprove; }

    public boolean allows(PermissionAction action) {
        return switch (action) {
            case VIEW -> canView;
            case ADD -> canAdd;
            case EDIT -> canEdit;
            case DELETE -> canDelete;
            case EXPORT -> canExport;
            case PRINT -> canPrint;
            case APPROVE -> canApprove;
        };
    }

    public void set(PermissionAction action, boolean value) {
        switch (action) {
            case VIEW -> canView = value;
            case ADD -> canAdd = value;
            case EDIT -> canEdit = value;
            case DELETE -> canDelete = value;
            case EXPORT -> canExport = value;
            case PRINT -> canPrint = value;
            case APPROVE -> canApprove = value;
        }
    }
}
