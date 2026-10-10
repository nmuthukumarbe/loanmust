package com.loanmust.loan.access;

import com.loanmust.loan.audit.AuditService;
import com.loanmust.loan.security.SecurityUtils;
import com.loanmust.loan.user.Role;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The Roles & Permissions matrix — tick what each role may do on each screen. */
@Controller
public class RolesController {

    private final PermissionService permissionService;
    private final AuditService auditService;

    public RolesController(PermissionService permissionService, AuditService auditService) {
        this.permissionService = permissionService;
        this.auditService = auditService;
    }

    @GetMapping("/admin/roles")
    public String roles(@RequestParam(required = false) String role, Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        Role selected = parse(role);
        model.addAttribute("activeMenu", "roles");
        model.addAttribute("roles", Role.values());
        model.addAttribute("modules", AppModule.values());
        model.addAttribute("actions", PermissionAction.values());
        model.addAttribute("selectedRole", selected);
        model.addAttribute("isSuperAdmin", permissionService.isSuperAdmin(selected));
        model.addAttribute("matrix", permissionService.matrix(tenantId, selected));
        return "admin/roles";
    }

    @PostMapping("/admin/roles/save")
    public String save(@RequestParam String role,
                       @RequestParam(name = "p", required = false) List<String> granted,
                       RedirectAttributes ra) {
        Long tenantId = SecurityUtils.currentTenantId();
        Role selected = parse(role);
        if (permissionService.isSuperAdmin(selected)) {
            ra.addFlashAttribute("error", "Super Admin cannot be restricted.");
            return "redirect:/admin/roles?role=" + selected.name();
        }
        // Each ticked box arrives as "<moduleKey>:<ACTION>".
        Map<String, List<String>> grantedByModule = new LinkedHashMap<>();
        if (granted != null) {
            for (String token : granted) {
                int i = token.indexOf(':');
                if (i <= 0) continue;
                grantedByModule.computeIfAbsent(token.substring(0, i), k -> new ArrayList<>())
                        .add(token.substring(i + 1));
            }
        }
        permissionService.save(tenantId, selected, grantedByModule, SecurityUtils.currentUserLogin());
        auditService.log("UPDATE", "RolePermission", 0L, selected.getLabel(),
                "Permissions updated for " + selected.getLabel());
        ra.addFlashAttribute("toast", selected.getLabel() + " permissions saved.");
        return "redirect:/admin/roles?role=" + selected.name();
    }

    private Role parse(String role) {
        if (role != null) {
            try {
                return Role.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Role.ADMIN;
    }
}
