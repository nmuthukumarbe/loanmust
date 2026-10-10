package com.loanmust.loan.security;

import com.loanmust.loan.user.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Convenience accessors for the authenticated principal and its tenant. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static CustomUserDetails currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details) {
            return details;
        }
        return null;
    }

    public static Long currentTenantId() {
        CustomUserDetails user = currentUser();
        return user == null ? null : user.getTenantId();
    }

    public static Long currentUserId() {
        CustomUserDetails user = currentUser();
        return user == null ? null : user.getId();
    }

    public static String currentUserLogin() {
        CustomUserDetails user = currentUser();
        return user == null ? "system" : user.getUsername();
    }

    public static Role currentRole() {
        CustomUserDetails user = currentUser();
        return user == null ? null : user.getRole();
    }

    /**
     * A sales executive only sees their own leads; every other role sees the whole
     * tenant. Returns null when no ownership filter should be applied.
     */
    public static Long ownerFilterUserId() {
        CustomUserDetails user = currentUser();
        if (user == null) return null;
        return user.getRole() == Role.LOAN_EXECUTIVE ? user.getId() : null;
    }
}
