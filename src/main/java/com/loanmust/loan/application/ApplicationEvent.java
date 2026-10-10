package com.loanmust.loan.application;

import com.loanmust.loan.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** The loan file's timeline: stage moves, bank queries, customer calls, notes. */
@Entity
@Table(name = "application_events")
public class ApplicationEvent extends BaseEntity {

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private Long applicationId;

    /** STAGE, NOTE, BANK_QUERY, DISBURSEMENT, DOCUMENT. */
    @Column(nullable = false, length = 20)
    private String eventType = "NOTE";

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 40)
    private ApplicationStage fromStage;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 40)
    private ApplicationStage toStage;

    @Column(length = 1000)
    private String remarks;

    private String userName;

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public ApplicationStage getFromStage() { return fromStage; }
    public void setFromStage(ApplicationStage fromStage) { this.fromStage = fromStage; }

    public ApplicationStage getToStage() { return toStage; }
    public void setToStage(ApplicationStage toStage) { this.toStage = toStage; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    @jakarta.persistence.Transient
    public String getIcon() {
        return switch (eventType == null ? "NOTE" : eventType) {
            case "STAGE" -> toStage == null ? "bi-arrow-right-circle" : toStage.getIcon();
            case "BANK_QUERY" -> "bi-question-circle";
            case "DISBURSEMENT" -> "bi-cash-coin";
            case "DOCUMENT" -> "bi-file-earmark-check";
            default -> "bi-chat-left-text";
        };
    }

    @jakarta.persistence.Transient
    public String getTitle() {
        return switch (eventType == null ? "NOTE" : eventType) {
            case "STAGE" -> toStage == null ? "Stage changed" : "Moved to " + toStage.getLabel();
            case "BANK_QUERY" -> "Bank query";
            case "DISBURSEMENT" -> "Disbursement";
            case "DOCUMENT" -> "Documents";
            default -> "Note";
        };
    }
}
