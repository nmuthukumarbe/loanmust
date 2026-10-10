package com.loanmust.loan.access;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Shown when a role is denied a screen by the permission matrix. */
@Controller
public class NoAccessController {

    @GetMapping("/no-access")
    public String noAccess(@RequestParam(name = "m", required = false) String moduleKey, Model model) {
        AppModule module = AppModule.fromKey(moduleKey);
        model.addAttribute("moduleLabel", module == null ? "this screen" : module.getLabel());
        model.addAttribute("activeMenu", "");
        return "error/no-access";
    }
}
