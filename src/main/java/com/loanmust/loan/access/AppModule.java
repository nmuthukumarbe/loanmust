package com.loanmust.loan.access;

import java.util.List;

/**
 * The rows of the permission matrix — every screen access can be granted on. Each carries
 * the URL prefix used by {@link PermissionInterceptor} to map a request to a module.
 */
public enum AppModule {
    DASHBOARD("dashboard", "Dashboard", "bi-grid-1x2-fill", "/dashboard"),
    LEADS("leads", "Leads", "bi-funnel-fill", "/leads"),
    CUSTOMERS("customers", "Customers", "bi-people-fill", "/customers"),
    ADVISOR("advisor", "Rate Compare & Advisor", "bi-bank", "/advisor"),
    LENDERS("lenders", "Banks & Rates", "bi-building-columns", "/lenders"),
    APPLICATIONS("applications", "Loan Files", "bi-folder2-open", "/applications"),
    MODD("modd", "MODD Processing", "bi-file-earmark-lock", "/modd"),
    BUILDERS("builders", "Builders & Projects", "bi-buildings", "/builders"),
    REFERRALS("referrals", "Referral Partners", "bi-people", "/referrals"),
    REFERRAL_PAYOUTS("referral_payouts", "Referral Payouts", "bi-send-check", "/referral-payouts"),
    COMMISSION("commission", "Bank Payouts", "bi-percent", "/commission"),
    FINANCE("finance", "Finance & Fees", "bi-receipt", "/finance"),
    REPORTS("reports", "Reports", "bi-graph-up-arrow", "/reports"),
    EMPLOYEES("employees", "Employees", "bi-person-vcard", "/employees"),
    ATTENDANCE("attendance", "Attendance", "bi-calendar-check", "/attendance"),
    USERS("users", "Users & Roles", "bi-person-badge", "/admin"),
    AUDIT("audit", "Audit Log", "bi-shield-lock", "/audit"),
    SETTINGS("settings", "Settings", "bi-gear-fill", "/settings");

    private final String key;
    private final String label;
    private final String icon;
    private final String urlPrefix;

    AppModule(String key, String label, String icon, String urlPrefix) {
        this.key = key;
        this.label = label;
        this.icon = icon;
        this.urlPrefix = urlPrefix;
    }

    public String getKey() { return key; }
    public String getLabel() { return label; }
    public String getIcon() { return icon; }
    public String getUrlPrefix() { return urlPrefix; }

    public static AppModule fromKey(String key) {
        for (AppModule m : values()) {
            if (m.key.equalsIgnoreCase(key)) return m;
        }
        return null;
    }

    /** The module a request path belongs to, or null if it is not a guarded screen. */
    public static AppModule forPath(String path) {
        if (path == null) return null;
        AppModule best = null;
        for (AppModule m : values()) {
            if (path.equals(m.urlPrefix) || path.startsWith(m.urlPrefix + "/")) {
                if (best == null || m.urlPrefix.length() > best.urlPrefix.length()) best = m;
            }
        }
        return best;
    }

    public static List<AppModule> all() {
        return List.of(values());
    }
}
