package com.loanmust.loan.audit;

import com.loanmust.loan.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * An immutable record of a significant action (PRD section 15). Old and new values are
 * captured so any record's history answers "what changed, who changed it, and when".
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    private Long tenantId;
    private String userEmail;
    private String userName;

    /** CREATE, UPDATE, ARCHIVE, STATUS, ASSIGN, SEND, PAYMENT, LOGIN, EXPORT. */
    private String action;

    private String entityType;    // Lead, LoanApplication, Payout, Modd, ...
    private Long entityId;
    private String entityLabel;   // human-readable name shown in the audit list

    private String fieldName;

    @Column(length = 500)
    private String oldValue;

    @Column(length = 500)
    private String newValue;

    @Column(length = 500)
    private String remarks;

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public String getEntityLabel() { return entityLabel; }
    public void setEntityLabel(String entityLabel) { this.entityLabel = entityLabel; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    /** Colour token used by the audit timeline in the UI. */
    @jakarta.persistence.Transient
    public String getTone() {
        if (action == null) return "slate";
        return switch (action) {
            case "CREATE" -> "emerald";
            case "UPDATE", "ASSIGN" -> "brand";
            case "ARCHIVE" -> "rose";
            case "STATUS" -> "amber";
            case "PAYMENT" -> "emerald";
            case "SEND" -> "indigo";
            default -> "slate";
        };
    }
}
