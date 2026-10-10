package com.loanmust.loan.dashboard;

import com.loanmust.loan.application.ApplicationStage;
import com.loanmust.loan.application.LoanApplication;
import com.loanmust.loan.application.LoanApplicationRepository;
import com.loanmust.loan.audit.AuditLog;
import com.loanmust.loan.audit.AuditLogRepository;
import com.loanmust.loan.finance.FinanceService;
import com.loanmust.loan.lead.Lead;
import com.loanmust.loan.lead.LeadRepository;
import com.loanmust.loan.lead.LeadService;
import com.loanmust.loan.modd.ModdRecord;
import com.loanmust.loan.modd.ModdRepository;
import com.loanmust.loan.modd.ModdService;
import com.loanmust.loan.modd.ModdStatus;
import com.loanmust.loan.referral.PayoutStatus;
import com.loanmust.loan.referral.ReferralPayoutRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Numbers for the dashboard. Every figure is tenant-scoped; sales users see their own book. */
@Service
public class DashboardService {

    private final LeadRepository leadRepository;
    private final LeadService leadService;
    private final LoanApplicationRepository applicationRepository;
    private final ModdRepository moddRepository;
    private final ModdService moddService;
    private final ReferralPayoutRepository referralPayoutRepository;
    private final FinanceService financeService;
    private final AuditLogRepository auditLogRepository;

    public DashboardService(LeadRepository leadRepository, LeadService leadService,
                            LoanApplicationRepository applicationRepository, ModdRepository moddRepository,
                            ModdService moddService, ReferralPayoutRepository referralPayoutRepository,
                            FinanceService financeService, AuditLogRepository auditLogRepository) {
        this.leadRepository = leadRepository;
        this.leadService = leadService;
        this.applicationRepository = applicationRepository;
        this.moddRepository = moddRepository;
        this.moddService = moddService;
        this.referralPayoutRepository = referralPayoutRepository;
        this.financeService = financeService;
        this.auditLogRepository = auditLogRepository;
    }

    public record Kpis(BigDecimal disbursedThisMonth, long filesDisbursedThisMonth, BigDecimal payoutEarnedThisMonth,
                       BigDecimal payoutOutstanding, BigDecimal referralOwed, long referralPendingApproval,
                       long openFiles, long openLeads, long followUpsDue, long sanctionsExpiring, long moddOpen,
                       long newLeadsThisMonth, long convertedThisMonth) {
    }

    public Kpis kpis(Long tenantId, Long ownerId) {
        YearMonth ym = YearMonth.now();
        FinanceService.Summary month = financeService.summary(tenantId, ym.atDay(1), ym.atEndOfMonth());
        long openFiles = ownerId == null
                ? applicationRepository.countByTenantIdAndStageInAndDeletedFalse(tenantId, ApplicationStage.openStages())
                : applicationRepository.countByTenantIdAndStageInAndAssignedUserIdAndDeletedFalse(tenantId, ApplicationStage.openStages(), ownerId);
        long openLeads = ownerId == null
                ? leadRepository.countByTenantIdAndStatusInAndDeletedFalse(tenantId, LeadService.openStatuses())
                : leadRepository.countByTenantIdAndAssignedUserIdAndStatusInAndDeletedFalse(tenantId, ownerId, LeadService.openStatuses());
        return new Kpis(
                month.disbursed(), month.filesDisbursed(), month.payoutEarned(), month.payoutOutstanding(),
                month.referralOwed(),
                referralPayoutRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, PayoutStatus.PENDING),
                openFiles, openLeads,
                leadService.dueFollowUps(tenantId, LocalDate.now(), ownerId).size(),
                sanctionsExpiring(tenantId).size(),
                moddRepository.countByTenantIdAndStatusInAndDeletedFalse(tenantId,
                        List.of(ModdStatus.NOT_STARTED, ModdStatus.DOCS_COLLECTED, ModdStatus.DRAFT_READY,
                                ModdStatus.APPOINTMENT_BOOKED, ModdStatus.REGISTERED)),
                leadRepository.countByTenantIdAndCreatedAtAfterAndDeletedFalse(tenantId, ym.atDay(1).atStartOfDay()),
                leadRepository.countByTenantIdAndConvertedAtAfterAndDeletedFalse(tenantId, ym.atDay(1).atStartOfDay()));
    }

    public List<Lead> followUps(Long tenantId, Long ownerId) {
        List<Lead> due = leadService.dueFollowUps(tenantId, LocalDate.now(), ownerId);
        return due.size() > 8 ? due.subList(0, 8) : due;
    }

    public List<LoanApplication> sanctionsExpiring(Long tenantId) {
        return applicationRepository.sanctionsExpiring(tenantId, LocalDate.now().plusDays(15),
                List.of(ApplicationStage.SANCTIONED, ApplicationStage.MODD_AGREEMENT));
    }

    public List<ModdRecord> moddAppointments(Long tenantId) {
        return moddService.upcomingAppointments(tenantId, 7);
    }

    /** Stage → count, in pipeline order, for the funnel bars. */
    public Map<ApplicationStage, Long> stageCounts(Long tenantId) {
        Map<ApplicationStage, Long> map = new LinkedHashMap<>();
        for (ApplicationStage s : ApplicationStage.values()) map.put(s, 0L);
        for (Object[] row : applicationRepository.countByStage(tenantId)) {
            map.put((ApplicationStage) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }

    public List<FinanceService.MonthPoint> trend(Long tenantId) {
        return financeService.trend(tenantId, 6);
    }

    public List<AuditLog> recentActivity(Long tenantId) {
        return auditLogRepository.recent(tenantId, PageRequest.of(0, 8));
    }

    public static List<String> labels(List<FinanceService.MonthPoint> points) {
        List<String> out = new ArrayList<>();
        for (FinanceService.MonthPoint p : points) out.add(p.label());
        return out;
    }
}
