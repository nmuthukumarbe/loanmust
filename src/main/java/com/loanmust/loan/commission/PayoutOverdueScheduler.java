package com.loanmust.loan.commission;

import com.loanmust.loan.tenant.Agency;
import com.loanmust.loan.tenant.AgencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Nightly: flags lender payouts still unpaid 60 days after disbursal. Single-instance by
 * design - on more than one node, run it on one or add a distributed lock (ShedLock).
 */
@Component
public class PayoutOverdueScheduler {

    private static final Logger log = LoggerFactory.getLogger(PayoutOverdueScheduler.class);

    private final AgencyRepository agencyRepository;
    private final CommissionService commissionService;

    public PayoutOverdueScheduler(AgencyRepository agencyRepository, CommissionService commissionService) {
        this.agencyRepository = agencyRepository;
        this.commissionService = commissionService;
    }

    @Scheduled(cron = "${app.payout.overdue-cron:0 30 6 * * *}")
    public void run() {
        for (Agency agency : agencyRepository.findByDeletedFalseOrderByNameAsc()) {
            try {
                int n = commissionService.flagOverdue(agency.getId());
                if (n > 0) log.info("{}: {} lender payout(s) flagged overdue", agency.getName(), n);
            } catch (Exception e) {
                log.warn("Overdue check failed for agency {}: {}", agency.getId(), e.getMessage());
            }
        }
    }
}
