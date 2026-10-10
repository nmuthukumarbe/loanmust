package com.loanmust.loan.common;

import com.loanmust.loan.tenant.Agency;
import com.loanmust.loan.tenant.AgencyRepository;
import org.springframework.stereotype.Service;

/**
 * Generates the human-friendly codes shown on screen (LD-0001, LA-0007, RF-0003 ...).
 *
 * The count-based sequence keeps codes readable and gap-free for an agency's volume; the
 * prefix is configurable per tenant in settings. Callers pass the count of ALL rows for the
 * tenant (archived ones included) so an archived record's code is never handed out again.
 */
@Service
public class CodeSequenceService {

    private final AgencyRepository agencyRepository;

    public CodeSequenceService(AgencyRepository agencyRepository) {
        this.agencyRepository = agencyRepository;
    }

    public String next(Long tenantId, Type type, long existingCount) {
        return prefixFor(tenantId, type) + String.format("%04d", existingCount + 1);
    }

    private String prefixFor(Long tenantId, Type type) {
        Agency agency = tenantId == null ? null : agencyRepository.findById(tenantId).orElse(null);
        if (agency == null) {
            return type.fallback;
        }
        String configured = switch (type) {
            case LEAD -> agency.getLeadPrefix();
            case CUSTOMER -> agency.getCustomerPrefix();
            case APPLICATION -> agency.getApplicationPrefix();
            case REFERRAL -> agency.getReferralPrefix();
            case PROPOSAL -> agency.getProposalPrefix();
            case RECEIPT -> agency.getReceiptPrefix();
            case EMPLOYEE, MODD, PAYOUT -> type.fallback;
        };
        return configured == null || configured.isBlank() ? type.fallback : configured;
    }

    public enum Type {
        LEAD("LD-"),
        CUSTOMER("CU-"),
        APPLICATION("LA-"),
        REFERRAL("RF-"),
        PROPOSAL("PR-"),
        RECEIPT("FR-"),
        MODD("MD-"),
        PAYOUT("RP-"),
        EMPLOYEE("EMP-");

        private final String fallback;

        Type(String fallback) {
            this.fallback = fallback;
        }
    }
}
