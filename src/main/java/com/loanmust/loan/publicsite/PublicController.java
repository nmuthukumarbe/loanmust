package com.loanmust.loan.publicsite;

import com.loanmust.loan.security.SecurityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Sign-in page and the root redirect. */
@Controller
public class PublicController {

    @GetMapping("/")
    public String root() {
        return SecurityUtils.currentUser() == null ? "redirect:/login" : "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return SecurityUtils.currentUser() == null ? "login" : "redirect:/dashboard";
    }
}
