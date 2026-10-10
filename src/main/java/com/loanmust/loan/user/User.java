package com.loanmust.loan.user;

import com.loanmust.loan.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** A staff account belonging to a loan agency. */
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String email;

    /** Optional login name. Sign-in accepts either this or the email address. */
    @Column(unique = true)
    private String username;

    @Column(nullable = false)
    private String password;      // BCrypt hash

    private String fullName;
    private String phone;
    private String designation;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private Long tenantId;

    /** Branch or office the user works from; used in reports and filters. */
    private String branch;

    /** Account status. An inactive account cannot authenticate. */
    @Column(nullable = false)
    private boolean enabled = true;

    /** "Allow login" toggle, independent of account status. */
    @org.hibernate.annotations.ColumnDefault("1")
    @Column(nullable = false)
    private boolean loginEnabled = true;

    /** When true the user must set a new password before using the app. */
    @org.hibernate.annotations.ColumnDefault("0")
    @Column(nullable = false)
    private boolean mustChangePassword = false;

    private java.time.LocalDateTime lastLoginAt;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isLoginEnabled() { return loginEnabled; }
    public void setLoginEnabled(boolean loginEnabled) { this.loginEnabled = loginEnabled; }

    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }

    public java.time.LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(java.time.LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    @jakarta.persistence.Transient
    public String getRoleLabel() {
        return role == null ? "" : role.getLabel();
    }

    @jakarta.persistence.Transient
    public String getLoginName() {
        return username != null && !username.isBlank() ? username : email;
    }

    /** Up-to-two-letter avatar initials derived from the full name (falls back to email). */
    @jakarta.persistence.Transient
    public String getInitials() {
        String source = fullName != null && !fullName.isBlank() ? fullName.trim() : email;
        if (source == null || source.isBlank()) {
            return "?";
        }
        String[] parts = source.trim().split("\\s+");
        if (parts.length == 1) {
            String p = parts[0];
            return p.substring(0, Math.min(2, p.length())).toUpperCase();
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
