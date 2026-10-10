package com.loanmust.loan.admin;

import com.loanmust.loan.audit.AuditService;
import com.loanmust.loan.security.CustomUserDetails;
import com.loanmust.loan.security.SecurityUtils;
import com.loanmust.loan.user.User;
import com.loanmust.loan.user.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** The signed-in user's own account: change password. */
@Controller
public class AccountController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AccountController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                             AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @GetMapping("/account/password")
    public String form(@RequestParam(required = false) String required, Model model) {
        model.addAttribute("activeMenu", "account");
        model.addAttribute("required", required != null);
        return "admin/password";
    }

    @PostMapping("/account/password")
    public String change(@RequestParam String currentPassword,
                         @RequestParam String newPassword,
                         @RequestParam String confirmPassword,
                         RedirectAttributes ra) {
        CustomUserDetails principal = SecurityUtils.currentUser();
        if (principal == null) return "redirect:/login";

        User user = userRepository.findById(principal.getId()).orElse(null);
        if (user == null) return "redirect:/login";

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            ra.addFlashAttribute("error", "Your current password is not correct.");
            return "redirect:/account/password";
        }
        if (newPassword == null || newPassword.length() < 8) {
            ra.addFlashAttribute("error", "Choose a password of at least 8 characters.");
            return "redirect:/account/password";
        }
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "The two new passwords do not match.");
            return "redirect:/account/password";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.setModifiedBy(SecurityUtils.currentUserLogin());
        userRepository.save(user);
        auditService.log("UPDATE", "User", user.getId(), user.getFullName(), "Password changed");

        // Refresh the principal so the force-change interceptor stops redirecting.
        SecurityContextHolder.clearContext();
        ra.addFlashAttribute("toast", "Password changed. Sign in again with your new password.");
        return "redirect:/login?passwordChanged";
    }
}
