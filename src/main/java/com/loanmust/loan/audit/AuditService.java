package com.loanmust.loan.audit;

import com.loanmust.loan.security.CustomUserDetails;
import com.loanmust.loan.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Writes the audit trail. Auditing must never break the action being audited, so every
 * failure is swallowed with a debug log.
 *
 * The {@link Diff} helper is how modules record field-level before/after values without
 * each of them hand-rolling comparison code.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void log(String action, String entityType, Long entityId, String entityLabel, String remarks) {
        write(action, entityType, entityId, entityLabel, null, null, null, remarks);
    }

    public void logChange(String entityType, Long entityId, String entityLabel,
                          String fieldName, String oldValue, String newValue) {
        write("UPDATE", entityType, entityId, entityLabel, fieldName, oldValue, newValue, null);
    }

    /** Records one row per changed field. Nothing is written when nothing changed. */
    public void logDiff(String entityType, Long entityId, String entityLabel, Diff diff) {
        for (Diff.Change change : diff.changes()) {
            write("UPDATE", entityType, entityId, entityLabel,
                    change.field(), change.oldValue(), change.newValue(), null);
        }
    }

    public void logStatus(String entityType, Long entityId, String entityLabel,
                          String oldStatus, String newStatus, String remarks) {
        write("STATUS", entityType, entityId, entityLabel, "Status", oldStatus, newStatus, remarks);
    }

    /**
     * The most recent entries of one kind for a tenant, newest first (dashboard activity feed);
     * tenant-scoped like every other read here.
     */
    public List<AuditLog> recentFor(Long tenantId, String entityType, int limit) {
        if (tenantId == null) return List.of();
        try {
            return repository.search(tenantId, "", entityType, "", null, null,
                    org.springframework.data.domain.PageRequest.of(0, Math.max(1, limit))).getContent();
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<AuditLog> historyFor(Long tenantId, String entityType, Long entityId) {
        try {
            return repository.findByTenantIdAndEntityTypeAndEntityIdAndDeletedFalseOrderByIdDesc(
                    tenantId, entityType, entityId);
        } catch (Exception e) {
            log.debug("Audit read skipped: {}", e.getMessage());
            return List.of();
        }
    }

    /** One field change recorded under a specific action (e.g. ASSIGN). */
    public void write(String action, String entityType, Long entityId, String entityLabel,
                      String fieldName, String oldValue, String newValue) {
        write(action, entityType, entityId, entityLabel, fieldName, oldValue, newValue, null);
    }

    /**
     * For actions with no signed-in user - the public referral form - where the tenant
     * cannot be read from the security context and must be given explicitly.
     */
    public void logFor(Long tenantId, String action, String entityType, Long entityId, String entityLabel, String remarks) {
        writeFor(tenantId, action, entityType, entityId, entityLabel, null, null, null, remarks);
    }

    private void write(String action, String entityType, Long entityId, String entityLabel,
                       String fieldName, String oldValue, String newValue, String remarks) {
        writeFor(SecurityUtils.currentTenantId(), action, entityType, entityId, entityLabel, fieldName, oldValue, newValue, remarks);
    }

    private void writeFor(Long tenantId, String action, String entityType, Long entityId, String entityLabel,
                          String fieldName, String oldValue, String newValue, String remarks) {
        try {
            AuditLog entry = new AuditLog();
            entry.setTenantId(tenantId);
            CustomUserDetails user = SecurityUtils.currentUser();
            entry.setUserEmail(user != null ? user.getUsername() : "system");
            entry.setUserName(user != null ? user.getFullName() : "System");
            entry.setAction(action);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId);
            entry.setEntityLabel(cut(entityLabel, 255));
            entry.setFieldName(fieldName);
            entry.setOldValue(cut(oldValue, 500));
            entry.setNewValue(cut(newValue, 500));
            entry.setRemarks(cut(remarks, 500));
            repository.save(entry);
        } catch (Exception e) {
            log.debug("Audit write skipped: {}", e.getMessage());
        }
    }

    private String cut(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    /**
     * Collects field-level changes before a save.
     *
     * <pre>
     * Diff diff = new Diff();
     * diff.compare("Status", entity.getStatus(), form.getStatus());
     * // ... apply the form to the entity, save, then:
     * auditService.logDiff("Lead", entity.getId(), entity.getName(), diff);
     * </pre>
     */
    public static class Diff {

        private final List<Change> changes = new ArrayList<>();

        public Diff compare(String field, Object oldValue, Object newValue) {
            String before = str(oldValue);
            String after = str(newValue);
            if (!Objects.equals(before, after)) {
                changes.add(new Change(field, before, after));
            }
            return this;
        }

        public List<Change> changes() {
            return changes;
        }

        public boolean isEmpty() {
            return changes.isEmpty();
        }

        private String str(Object value) {
            if (value == null) return "";
            if (value instanceof Enum<?> e) return e.name();
            return String.valueOf(value);
        }

        public record Change(String field, String oldValue, String newValue) {
        }
    }
}
