package com.loanmust.loan.access;

import com.loanmust.loan.security.CustomUserDetails;
import com.loanmust.loan.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * Enforces the Roles & Permissions matrix on every request, so what a signed-in user can
 * do always matches the configured settings — not just what the menu shows, but direct
 * POSTs and typed URLs too.
 *
 * The required action is derived from the HTTP method and the path:
 *   GET list/detail            -> VIEW
 *   GET  .../new               -> ADD          GET .../{id}/edit -> EDIT
 *   POST .../save|/create      -> ADD or EDIT  (either grants it)
 *   POST .../delete|/archive   -> DELETE       POST .../approve  -> APPROVE
 *   any  .../export|download   -> EXPORT       any .../print|pdf -> PRINT
 *   other writes (POST/PUT...) -> EDIT
 *
 * OWNER (super admin) always passes; unmapped paths (login, portal, public, static) are
 * never blocked. A denial redirects to the branded /no-access screen.
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private final PermissionService permissionService;

    public PermissionInterceptor(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        CustomUserDetails user = SecurityUtils.currentUser();
        if (user == null || user.getRole() == null) {
            return true;   // not authenticated yet - Spring Security decides
        }
        if (permissionService.isSuperAdmin(user.getRole())) {
            return true;
        }
        String path = request.getRequestURI();
        AppModule module = AppModule.forPath(path);
        if (module == null) {
            return true;   // not a guarded screen
        }
        Long tenantId = user.getTenantId();
        if (tenantId == null) {
            return true;
        }

        List<PermissionAction> required = requiredActions(request.getMethod(), path);
        boolean allowed = required.stream()
                .anyMatch(a -> permissionService.can(tenantId, user.getRole(), module, a));
        if (!allowed) {
            response.sendRedirect(request.getContextPath() + "/no-access?m=" + module.getKey());
            return false;
        }
        return true;
    }

    /** The permission(s) a request needs — the user must hold at least one of them. */
    private List<PermissionAction> requiredActions(String method, String path) {
        String p = path.toLowerCase();
        boolean write = !"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method);

        if (p.contains("/delete") || p.contains("/archive") || p.contains("/remove")) {
            return List.of(PermissionAction.DELETE);
        }
        if (p.contains("/approve") || p.contains("/reject")) {
            return List.of(PermissionAction.APPROVE);
        }
        if (p.contains("export") || p.contains("download") || p.endsWith(".csv") || p.contains(".xls")) {
            return List.of(PermissionAction.EXPORT);
        }
        if (p.contains("/print") || p.endsWith(".pdf")) {
            return List.of(PermissionAction.PRINT);
        }
        if (!write) {
            if (p.endsWith("/new")) return List.of(PermissionAction.ADD);
            if (p.endsWith("/edit")) return List.of(PermissionAction.EDIT);
            return List.of(PermissionAction.VIEW);
        }
        // Writes: creating or updating through a shared save endpoint needs add OR edit.
        if (p.endsWith("/save") || p.endsWith("/create") || p.endsWith("/new")) {
            return List.of(PermissionAction.ADD, PermissionAction.EDIT);
        }
        return List.of(PermissionAction.EDIT);
    }
}
