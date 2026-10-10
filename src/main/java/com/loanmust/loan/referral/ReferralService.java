package com.loanmust.loan.referral;

import com.loanmust.loan.audit.AuditService;
import com.loanmust.loan.common.CodeSequenceService;
import com.loanmust.loan.common.Labels;
import com.loanmust.loan.common.PaymentMode;
import com.loanmust.loan.common.WhatsApp;
import com.loanmust.loan.security.CustomUserDetails;
import com.loanmust.loan.security.SecurityUtils;
import com.loanmust.loan.tenant.Agency;
import com.loanmust.loan.tenant.AgencyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The referral programme: partners, their personal links, and the payout ledger.
 *
 * Payout workflow - PENDING (raised automatically on disbursal) → APPROVED (manager / owner
 * checks the file really disbursed) → PAID (finance records UTR). The rule in force at
 * disbursal is snapshotted onto the payout, and TDS is deducted at the agency's rate.
 */
@Service
public class ReferralService {

    private static final List<PayoutStatus> OWED = List.of(PayoutStatus.PENDING, PayoutStatus.APPROVED, PayoutStatus.ON_HOLD);

    private final ReferralPartnerRepository partnerRepository;
    private final ReferralPayoutRepository payoutRepository;
    private final AgencyRepository agencyRepository;
    private final CodeSequenceService codeSequenceService;
    private final AuditService auditService;

    public ReferralService(ReferralPartnerRepository partnerRepository, ReferralPayoutRepository payoutRepository,
                           AgencyRepository agencyRepository, CodeSequenceService codeSequenceService,
                           AuditService auditService) {
        this.partnerRepository = partnerRepository;
        this.payoutRepository = payoutRepository;
        this.agencyRepository = agencyRepository;
        this.codeSequenceService = codeSequenceService;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------------ partners

    public Page<ReferralPartner> search(Long tenantId, String q, PartnerType type, int page, int size) {
        return partnerRepository.search(tenantId, q == null ? "" : q.trim(), type, PageRequest.of(Math.max(0, page), size));
    }

    public List<ReferralPartner> activePartners(Long tenantId) {
        return partnerRepository.findByTenantIdAndActiveTrueAndDeletedFalseOrderByNameAsc(tenantId);
    }

    public ReferralPartner get(Long tenantId, Long id) {
        return id == null ? null : partnerRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId).orElse(null);
    }

    /** Public link lookup - no tenant in context, the code identifies the partner. */
    public ReferralPartner byPublicCode(String code) {
        if (code == null || code.isBlank() || code.length() > 20) return null;
        return partnerRepository.findFirstByCodeIgnoreCaseAndActiveTrueAndDeletedFalse(code.trim()).orElse(null);
    }

    public BigDecimal owedTo(Long tenantId, Long partnerId) {
        return payoutRepository.sumNetForPartner(tenantId, partnerId, OWED);
    }

    public BigDecimal paidTo(Long tenantId, Long partnerId) {
        return payoutRepository.sumNetForPartner(tenantId, partnerId, List.of(PayoutStatus.PAID));
    }

    public List<ReferralPayout> payoutsFor(Long tenantId, Long partnerId) {
        return payoutRepository.findByTenantIdAndPartnerIdAndDeletedFalseOrderByIdDesc(tenantId, partnerId);
    }

    @Transactional
    public ReferralPartner save(Long tenantId, ReferralPartner form) {
        boolean isNew = form.getId() == null;
        ReferralPartner entity = isNew ? new ReferralPartner() : get(tenantId, form.getId());
        if (entity == null) return null;
        if (isNew) {
            entity.setTenantId(tenantId);
            entity.setCreatedBy(SecurityUtils.currentUserLogin());
            entity.setCode(codeSequenceService.next(tenantId, CodeSequenceService.Type.REFERRAL,
                    partnerRepository.countByTenantId(tenantId)) + "-" + tenantId);
        }
        AuditService.Diff diff = new AuditService.Diff()
                .compare("Name", entity.getName(), form.getName())
                .compare("Mobile", entity.getMobile(), form.getMobile())
                .compare("Payout basis", entity.getPayoutBasis(), form.getPayoutBasis())
                .compare("Payout value", entity.getPayoutValue(), form.getPayoutValue())
                .compare("UPI", entity.getUpiId(), form.getUpiId())
                .compare("Account", entity.getBankAccountNumber(), form.getBankAccountNumber())
                .compare("Active", entity.isActive(), form.isActive());

        entity.setName(form.getName());
        entity.setMobile(form.getMobile());
        entity.setEmail(form.getEmail());
        entity.setCity(form.getCity());
        entity.setPartnerType(form.getPartnerType() == null ? PartnerType.INDIVIDUAL : form.getPartnerType());
        entity.setPayoutBasis(form.getPayoutBasis() == null ? PayoutBasis.PERCENT_OF_DISBURSAL : form.getPayoutBasis());
        BigDecimal value = form.getPayoutValue();
        if (value == null && entity.getPayoutBasis() == PayoutBasis.PERCENT_OF_DISBURSAL) {
            Agency agency = agencyRepository.findById(tenantId).orElse(null);
            value = agency == null ? null : agency.getDefaultReferralPercent();
        }
        entity.setPayoutValue(value);
        entity.setUpiId(form.getUpiId());
        entity.setBankAccountName(form.getBankAccountName());
        entity.setBankAccountNumber(form.getBankAccountNumber());
        entity.setIfsc(form.getIfsc() == null ? null : form.getIfsc().trim().toUpperCase());
        entity.setBankName(form.getBankName());
        entity.setPan(form.getPan() == null ? null : form.getPan().trim().toUpperCase());
        entity.setActive(form.isActive());
        entity.setNotes(form.getNotes());
        entity.setModifiedBy(SecurityUtils.currentUserLogin());
        ReferralPartner saved = partnerRepository.save(entity);
        if (isNew) {
            auditService.log("CREATE", "ReferralPartner", saved.getId(), saved.getName(),
                    "Referral partner enrolled · " + saved.getPayoutRule());
        } else {
            auditService.logDiff("ReferralPartner", saved.getId(), saved.getName(), diff);
        }
        return saved;
    }

    @Transactional
    public void archive(Long tenantId, Long id) {
        ReferralPartner p = get(tenantId, id);
        if (p == null) return;
        p.setDeleted(true);
        p.setActive(false);
        p.setModifiedBy(SecurityUtils.currentUserLogin());
        partnerRepository.save(p);
        auditService.log("ARCHIVE", "ReferralPartner", id, p.getName(), "Partner archived - referral link disabled");
    }

    // ------------------------------------------------------------------ payouts

    public Page<ReferralPayout> searchPayouts(Long tenantId, String q, PayoutStatus status, Long partnerId, int page, int size) {
        return payoutRepository.search(tenantId, q == null ? "" : q.trim(), status, partnerId,
                PageRequest.of(Math.max(0, page), size));
    }

    public ReferralPayout getPayout(Long tenantId, Long id) {
        return id == null ? null : payoutRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId).orElse(null);
    }

    /**
     * Raises the referrer's payout for one disbursement. Idempotent per disbursement.
     * A flat-per-file rule pays once per loan file (on the first tranche only).
     */
    @Transactional
    public ReferralPayout raiseForDisbursement(Long tenantId, Long partnerId, Long applicationId, String applicationCode,
                                               Long disbursementId, boolean firstTranche, String customerName,
                                               String lenderName, BigDecimal disbursedAmount) {
        ReferralPartner partner = get(tenantId, partnerId);
        if (partner == null || disbursedAmount == null || disbursedAmount.signum() <= 0) return null;
        if (payoutRepository.findFirstByTenantIdAndDisbursementIdAndDeletedFalse(tenantId, disbursementId).isPresent()) return null;
        PayoutBasis basis = partner.getPayoutBasis() == null ? PayoutBasis.PERCENT_OF_DISBURSAL : partner.getPayoutBasis();
        if (basis == PayoutBasis.FLAT_PER_FILE && !firstTranche) return null;

        Agency agency = agencyRepository.findById(tenantId).orElse(null);
        BigDecimal value = partner.getPayoutValue() != null ? partner.getPayoutValue()
                : agency == null ? null : agency.getDefaultReferralPercent();
        if (value == null || value.signum() <= 0) return null;

        BigDecimal gross = basis == PayoutBasis.FLAT_PER_FILE
                ? value.setScale(2, RoundingMode.HALF_UP)
                : disbursedAmount.multiply(value).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal tdsPct = agency == null || agency.getReferralTdsPercent() == null ? BigDecimal.ZERO : agency.getReferralTdsPercent();
        BigDecimal tds = gross.multiply(tdsPct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        ReferralPayout p = new ReferralPayout();
        p.setTenantId(tenantId);
        p.setCode(codeSequenceService.next(tenantId, CodeSequenceService.Type.PAYOUT, payoutRepository.countByTenantId(tenantId)));
        p.setPartnerId(partner.getId());
        p.setPartnerName(partner.getName());
        p.setPartnerMobile(partner.getMobile());
        p.setApplicationId(applicationId);
        p.setApplicationCode(applicationCode);
        p.setDisbursementId(disbursementId);
        p.setCustomerName(customerName);
        p.setLenderName(lenderName);
        p.setDisbursedAmount(disbursedAmount);
        p.setPayoutBasis(basis);
        p.setPayoutValue(value);
        p.setGrossAmount(gross);
        p.setTdsPercent(tdsPct);
        p.setTdsAmount(tds);
        p.setNetAmount(gross.subtract(tds));
        p.setStatus(PayoutStatus.PENDING);
        p.setCreatedBy(SecurityUtils.currentUserLogin());
        ReferralPayout saved = payoutRepository.save(p);
        auditService.log("CREATE", "ReferralPayout", saved.getId(), partner.getName(),
                "₹" + Labels.inr(saved.getNetAmount()) + " raised for " + applicationCode + " (" + partner.getPayoutRule() + ")");
        return saved;
    }

    @Transactional
    public boolean approve(Long tenantId, Long id) {
        ReferralPayout p = getPayout(tenantId, id);
        if (p == null || (p.getStatus() != PayoutStatus.PENDING && p.getStatus() != PayoutStatus.ON_HOLD)) return false;
        CustomUserDetails user = SecurityUtils.currentUser();
        String before = p.getStatus().getLabel();
        p.setStatus(PayoutStatus.APPROVED);
        p.setApprovedByName(user == null ? "System" : user.getFullName());
        p.setApprovedAt(LocalDateTime.now());
        p.setModifiedBy(SecurityUtils.currentUserLogin());
        payoutRepository.save(p);
        auditService.logStatus("ReferralPayout", id, p.getPartnerName(), before, p.getStatus().getLabel(), null);
        return true;
    }

    @Transactional
    public boolean markPaid(Long tenantId, Long id, LocalDate paidOn, PaymentMode mode, String reference, String remarks) {
        ReferralPayout p = getPayout(tenantId, id);
        if (p == null || p.getStatus() != PayoutStatus.APPROVED) return false;
        p.setStatus(PayoutStatus.PAID);
        p.setPaidOn(paidOn == null ? LocalDate.now() : paidOn);
        p.setPaymentMode(mode);
        p.setPaymentReference(reference);
        if (remarks != null && !remarks.isBlank()) p.setRemarks(remarks);
        p.setModifiedBy(SecurityUtils.currentUserLogin());
        payoutRepository.save(p);
        auditService.write("PAYMENT", "ReferralPayout", id, p.getPartnerName(), "Paid",
                null, "₹" + Labels.inr(p.getNetAmount()) + (reference == null ? "" : " · " + reference));
        return true;
    }

    @Transactional
    public boolean setStatus(Long tenantId, Long id, PayoutStatus status, String remarks) {
        ReferralPayout p = getPayout(tenantId, id);
        if (p == null || p.getStatus() == PayoutStatus.PAID || status == PayoutStatus.PAID) return false;
        String before = p.getStatus().getLabel();
        p.setStatus(status);
        if (remarks != null && !remarks.isBlank()) p.setRemarks(remarks);
        p.setModifiedBy(SecurityUtils.currentUserLogin());
        payoutRepository.save(p);
        auditService.logStatus("ReferralPayout", id, p.getPartnerName(), before, status.getLabel(), remarks);
        return true;
    }

    /** Thank-you message with payment details, opened in WhatsApp by the staff member. */
    public String paidMessageLink(ReferralPayout p, String agencyName) {
        String msg = "Dear " + p.getPartnerName() + ", thank you for referring " + p.getCustomerName()
                + ". Your referral reward of Rs." + Labels.inr(p.getNetAmount())
                + (p.getTdsAmount() != null && p.getTdsAmount().signum() > 0 ? " (after TDS of Rs." + Labels.inr(p.getTdsAmount()) + ")" : "")
                + " has been paid" + (p.getPaymentReference() == null ? "" : " - ref " + p.getPaymentReference())
                + ". Keep referring! - " + agencyName;
        return WhatsApp.link(p.getPartnerMobile(), msg);
    }

    /** Invitation carrying the partner's personal referral link. */
    public String inviteLink(ReferralPartner partner, String baseUrl, String agencyName) {
        String msg = "Hi " + partner.getName() + ", here is your personal referral link for " + agencyName
                + ". Share it with anyone looking for a home loan or loan against property - you earn "
                + partner.getPayoutRule() + " when their loan is disbursed: " + baseUrl + "/refer/" + partner.getCode();
        return WhatsApp.link(partner.getMobile(), msg);
    }
}
