package com.loanmust.loan.admin;

import com.loanmust.loan.audit.AuditService;
import com.loanmust.loan.security.SecurityUtils;
import com.loanmust.loan.user.Role;
import com.loanmust.loan.user.User;
import com.loanmust.loan.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Administration: staff accounts and their roles. */
@Controller
public class UserAdminController {

    private static final int PAGE_SIZE = 10;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserAdminController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                               AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @GetMapping("/admin/users")
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        model.addAttribute("activeMenu", "admin");
        model.addAttribute("userPage", userRepository.search(tenantId, q == null ? "" : q.trim(),
                PageRequest.of(Math.max(0, page), PAGE_SIZE)));
        model.addAttribute("q", q);
        return "admin/users";
    }

    @GetMapping("/admin/users/new")
    public String createForm(Model model) {
        User user = new User();
        user.setRole(Role.LOAN_EXECUTIVE);
        user.setEnabled(true);
        user.setLoginEnabled(true);
        model.addAttribute("activeMenu", "admin");
        model.addAttribute("user", user);
        model.addAttribute("roles", Role.values());
        return "admin/user-form";
    }

    @GetMapping("/admin/users/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        User user = userRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId).orElse(null);
        if (user == null) return "redirect:/admin/users";
        model.addAttribute("activeMenu", "admin");
        model.addAttribute("user", user);
        model.addAttribute("roles", Role.values());
        return "admin/user-form";
    }

    @PostMapping("/admin/users/save")
    public String save(@ModelAttribute User form,
                       @RequestParam(required = false) String password,
                       RedirectAttributes ra) {
        Long tenantId = SecurityUtils.currentTenantId();
        boolean isNew = form.getId() == null;
        User entity;
        if (isNew) {
            if (userRepository.existsByEmail(form.getEmail())) {
                ra.addFlashAttribute("toast", "That email address is already in use.");
                return "redirect:/admin/users/new";
            }
            entity = new User();
            entity.setTenantId(tenantId);
            entity.setCreatedBy(SecurityUtils.currentUserLogin());
            // A new account starts on a temporary password and must change it at first sign-in.
            String initial = password == null || password.isBlank() ? "welcome123" : password;
            entity.setPassword(passwordEncoder.encode(initial));
            entity.setMustChangePassword(true);
        } else {
            entity = userRepository.findByIdAndTenantIdAndDeletedFalse(form.getId(), tenantId).orElse(null);
            if (entity == null) return "redirect:/admin/users";
            if (password != null && !password.isBlank()) {
                entity.setPassword(passwordEncoder.encode(password));
                entity.setMustChangePassword(true);
            }
        }

        AuditService.Diff diff = new AuditService.Diff()
                .compare("Full name", entity.getFullName(), form.getFullName())
                .compare("Role", entity.getRole(), form.getRole())
                .compare("Account active", entity.isEnabled(), form.isEnabled())
                .compare("Login allowed", entity.isLoginEnabled(), form.isLoginEnabled());

        entity.setEmail(form.getEmail());
        entity.setUsername(form.getUsername() == null || form.getUsername().isBlank() ? null : form.getUsername());
        entity.setFullName(form.getFullName());
        entity.setPhone(form.getPhone());
        entity.setDesignation(form.getDesignation());
        entity.setBranch(form.getBranch());
        entity.setRole(form.getRole() == null ? Role.LOAN_EXECUTIVE : form.getRole());
        entity.setEnabled(form.isEnabled());
        entity.setLoginEnabled(form.isLoginEnabled());
        entity.setModifiedBy(SecurityUtils.currentUserLogin());

        User saved = userRepository.save(entity);
        if (isNew) {
            auditService.log("CREATE", "User", saved.getId(), saved.getFullName(),
                    "Account created with role " + saved.getRoleLabel());
            ra.addFlashAttribute("toast", "Account created. Temporary password: "
                    + (password == null || password.isBlank() ? "welcome123" : password));
        } else {
            auditService.logDiff("User", saved.getId(), saved.getFullName(), diff);
            ra.addFlashAttribute("toast", "Account saved.");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{id}/archive")
    public String archive(@PathVariable Long id, RedirectAttributes ra) {
        Long tenantId = SecurityUtils.currentTenantId();
        User user = userRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId).orElse(null);
        if (user == null) return "redirect:/admin/users";
        if (user.getId().equals(SecurityUtils.currentUserId())) {
            ra.addFlashAttribute("toast", "You cannot archive your own account.");
            return "redirect:/admin/users";
        }
        user.setDeleted(true);
        user.setLoginEnabled(false);
        userRepository.save(user);
        auditService.log("ARCHIVE", "User", id, user.getFullName(), "Account archived");
        ra.addFlashAttribute("toast", "Account archived.");
        return "redirect:/admin/users";
    }
}
