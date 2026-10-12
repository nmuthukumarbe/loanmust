package com.loanmust.loan.config;

import com.loanmust.loan.access.PermissionChecker;
import com.loanmust.loan.access.PermissionService;
import com.loanmust.loan.common.Fmt;
import com.loanmust.loan.common.IdCodec;
import com.loanmust.loan.security.CustomUserDetails;
import com.loanmust.loan.security.SecurityUtils;
import com.loanmust.loan.settings.AccentPalette;
import com.loanmust.loan.tenant.Agency;
import com.loanmust.loan.tenant.AgencyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Injects the signed-in user and their agency into every Thymeleaf model, so the shared
 * layout can render the sidebar, branding, role and currency on any page without each
 * controller repeating itself.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private static final Fmt FMT = new Fmt();

    private final AgencyRepository agencyRepository;
    private final PermissionService permissionService;
    private final IdCodec idCodec;
    private final String currencySymbol;
    private final String currencyCode;

    public GlobalModelAdvice(AgencyRepository agencyRepository,
                             PermissionService permissionService,
                             IdCodec idCodec,
                             @Value("${app.currency-symbol:₹}") String currencySymbol,
                             @Value("${app.currency-code:INR}") String currencyCode) {
        this.agencyRepository = agencyRepository;
        this.permissionService = permissionService;
        this.idCodec = idCodec;
        this.currencySymbol = currencySymbol;
        this.currencyCode = currencyCode;
    }

    /**
     * Resolve the CSRF token before the view renders. The token lives in the HTTP session;
     * on a long public page (the website) the response is already committed by the time
     * Thymeleaf reaches the form, and creating the session then fails mid-page.
     */
    @ModelAttribute
    public void loadCsrfToken(CsrfToken csrfToken) {
        if (csrfToken != null) csrfToken.getToken();
    }

    @ModelAttribute
    public void addGlobals(Model model) {
        model.addAttribute("enc", idCodec);   // ${enc.encode(x.id)} builds opaque URL ids
        model.addAttribute("fmt", FMT);       // ${fmt.inr(amount)} Indian number format
        model.addAttribute("currencySymbol", currencySymbol);
        model.addAttribute("currency", currencyCode);
        model.addAttribute("supportedLanguages", WebConfig.LANGUAGES);
        model.addAttribute("currentLang", LocaleContextHolder.getLocale().getLanguage());
        // Safe defaults so the layout never evaluates a null.
        model.addAttribute("currentAgencyName", "Sri Sai Associates");
        model.addAttribute("accentColor", "indigo");
        model.addAttribute("brandPalette", AccentPalette.INDIGO.shades());
        model.addAttribute("isAdministrator", Boolean.FALSE);
        model.addAttribute("canSeeMoney", Boolean.FALSE);

        CustomUserDetails user = SecurityUtils.currentUser();
        if (user == null) {
            return;
        }
        model.addAttribute("currentUserName", user.getFullName());
        model.addAttribute("currentUserRole", user.getRoleLabel());
        model.addAttribute("currentUserEmail", user.getUsername());
        model.addAttribute("currentUserInitials", initials(user.getFullName()));
        model.addAttribute("isAdministrator", user.getRole() != null && user.getRole().isAdministrative());
        model.addAttribute("canSeeMoney", user.getRole() != null && user.getRole().isMoney());

        Long tenantId = user.getTenantId();
        model.addAttribute("currentTenantId", tenantId);
        // Per-request permission helper for templates (menu + action-button gating).
        model.addAttribute("perms", new PermissionChecker(permissionService, tenantId, user.getRole()));
        if (tenantId == null) {
            return;
        }
        Agency agency = agencyRepository.findById(tenantId).orElse(null);
        if (agency != null) {
            model.addAttribute("currentAgencyName", agency.getDisplayName());
            model.addAttribute("currentAgencyDsaCode", agency.getDsaCode());
            AccentPalette accent = AccentPalette.fromId(agency.getAccentColor());
            model.addAttribute("accentColor", accent.getId());
            model.addAttribute("brandPalette", accent.shades());
        }
    }

    private String initials(String name) {
        if (name == null || name.isBlank()) return "U";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
