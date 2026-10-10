package com.loanmust.loan.dashboard;

import com.loanmust.loan.finance.FinanceService;
import com.loanmust.loan.security.SecurityUtils;
import com.loanmust.loan.user.Role;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        Long ownerId = SecurityUtils.ownerFilterUserId();
        Role role = SecurityUtils.currentRole();
        boolean money = role != null && role.isMoney();

        model.addAttribute("activeMenu", "dashboard");
        model.addAttribute("kpi", dashboardService.kpis(tenantId, ownerId));
        model.addAttribute("ownBookOnly", ownerId != null);
        model.addAttribute("showMoney", money);
        model.addAttribute("followUps", dashboardService.followUps(tenantId, ownerId));
        model.addAttribute("sanctionsExpiring", dashboardService.sanctionsExpiring(tenantId));
        model.addAttribute("moddAppointments", dashboardService.moddAppointments(tenantId));
        java.util.Map<com.loanmust.loan.application.ApplicationStage, Long> stageCounts = dashboardService.stageCounts(tenantId);
        model.addAttribute("stageCounts", stageCounts);
        model.addAttribute("stageMax", Math.max(1L, stageCounts.values().stream().mapToLong(Long::longValue).max().orElse(1L)));
        model.addAttribute("recentActivity", dashboardService.recentActivity(tenantId));

        List<FinanceService.MonthPoint> trend = dashboardService.trend(tenantId);
        List<BigDecimal> disbursed = new ArrayList<>(), earned = new ArrayList<>();
        for (FinanceService.MonthPoint p : trend) {
            disbursed.add(p.disbursed());
            earned.add(p.earned());
        }
        model.addAttribute("trendLabels", DashboardService.labels(trend));
        model.addAttribute("trendDisbursed", disbursed);
        model.addAttribute("trendEarned", earned);
        return "dashboard/index";
    }
}
