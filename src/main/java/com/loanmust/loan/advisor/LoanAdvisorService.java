package com.loanmust.loan.advisor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanmust.loan.audit.AuditService;
import com.loanmust.loan.common.CodeSequenceService;
import com.loanmust.loan.lender.LenderService;
import com.loanmust.loan.lender.LoanProduct;
import com.loanmust.loan.security.CustomUserDetails;
import com.loanmust.loan.security.SecurityUtils;
import com.loanmust.loan.tenant.Agency;
import com.loanmust.loan.tenant.AgencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Runs comparisons against the tenant's live rate card and saves / serves proposals. */
@Service
public class LoanAdvisorService {

    private static final Logger log = LoggerFactory.getLogger(LoanAdvisorService.class);
    private static final String TOKEN_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final LenderService lenderService;
    private final AgencyRepository agencyRepository;
    private final ProposalRepository proposalRepository;
    private final CodeSequenceService codeSequenceService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public LoanAdvisorService(LenderService lenderService, AgencyRepository agencyRepository,
                              ProposalRepository proposalRepository, CodeSequenceService codeSequenceService,
                              AuditService auditService, ObjectMapper objectMapper) {
        this.lenderService = lenderService;
        this.agencyRepository = agencyRepository;
        this.proposalRepository = proposalRepository;
        this.codeSequenceService = codeSequenceService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    public AdvisorOutcome compare(Long tenantId, AdvisorRequest request) {
        Agency agency = agencyRepository.findById(tenantId).orElse(null);
        BigDecimal gst = agency == null ? null : agency.getProcessingFeeGstPercent();
        List<LoanProduct> products = request.getLoanType() == null ? List.of()
                : lenderService.activeProducts(tenantId, request.getLoanType());
        return new LoanAdvisor(gst).compare(request, products, p -> lenderService.payoutPercentFor(tenantId, p));
    }

    // ------------------------------------------------------------------ proposals

    public Page<Proposal> search(Long tenantId, String q, int page, int size) {
        return proposalRepository.search(tenantId, q == null ? "" : q.trim(), PageRequest.of(Math.max(0, page), size));
    }

    public Proposal get(Long tenantId, Long id) {
        return id == null ? null : proposalRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId).orElse(null);
    }

    public List<Proposal> forCustomer(Long tenantId, Long customerId) {
        return proposalRepository.findByTenantIdAndCustomerIdAndDeletedFalseOrderByIdDesc(tenantId, customerId);
    }

    public List<Proposal> forLead(Long tenantId, Long leadId) {
        return proposalRepository.findByTenantIdAndLeadIdAndDeletedFalseOrderByIdDesc(tenantId, leadId);
    }

    /** Re-runs the comparison server-side (never trusts posted figures) and freezes it. */
    @Transactional
    public Proposal saveProposal(Long tenantId, AdvisorRequest request, String remarks) {
        AdvisorOutcome outcome = compare(tenantId, request);
        Agency agency = agencyRepository.findById(tenantId).orElse(null);
        CustomUserDetails user = SecurityUtils.currentUser();

        Proposal p = new Proposal();
        p.setTenantId(tenantId);
        p.setCode(codeSequenceService.next(tenantId, CodeSequenceService.Type.PROPOSAL,
                proposalRepository.countByTenantId(tenantId)));
        p.setToken(newToken());
        p.setCustomerId(request.getCustomerId());
        p.setLeadId(request.getLeadId());
        p.setCustomerName(request.getCustomerName());
        p.setCustomerMobile(request.getCustomerMobile());
        p.setLoanType(request.getLoanType());
        p.setLoanAmount(request.getLoanAmount());
        p.setTenureYears(request.getTenureYears());
        p.setEligibleCount(outcome.getEligibleCount());
        LoanOffer best = outcome.getRecommended();
        if (best != null) {
            p.setRecommendedProductId(best.getProductId());
            p.setRecommendedLenderName(best.getLenderName());
            p.setRecommendedProductName(best.getProductName());
            p.setRecommendedRate(best.getApplicableRate());
            p.setRecommendedEmi(best.getEmi());
        }
        try {
            p.setOutcomeJson(objectMapper.writeValueAsString(outcome));
        } catch (Exception e) {
            throw new IllegalStateException("Could not save the comparison", e);
        }
        p.setValidUntil(LocalDate.now().plusDays(agency == null ? 15 : agency.getProposalValidityDays()));
        p.setPreparedByName(user == null ? null : user.getFullName());
        p.setRemarks(remarks);
        p.setCreatedBy(SecurityUtils.currentUserLogin());
        Proposal saved = proposalRepository.save(p);
        auditService.log("CREATE", "Proposal", saved.getId(), saved.getCode() + " · " + nz(saved.getCustomerName()),
                best == null ? "Comparison saved - no eligible lender"
                        : "Recommended " + best.getLenderName() + " at " + best.getApplicableRate() + "%");
        return saved;
    }

    /** The frozen outcome of a proposal; null if it cannot be read. */
    public AdvisorOutcome outcomeOf(Proposal p) {
        if (p == null || p.getOutcomeJson() == null) return null;
        try {
            return objectMapper.readValue(p.getOutcomeJson(), AdvisorOutcome.class);
        } catch (Exception e) {
            log.warn("Unreadable proposal {}: {}", p.getId(), e.getMessage());
            return null;
        }
    }

    /** Public view by token - counts the view. Returns null for unknown tokens. */
    @Transactional
    public Proposal openPublic(String token, boolean countView) {
        if (token == null || token.length() < 16) return null;
        Proposal p = proposalRepository.findByTokenAndDeletedFalse(token).orElse(null);
        if (p == null || !countView) return p;
        p.setViewCount((p.getViewCount() == null ? 0 : p.getViewCount()) + 1);
        p.setLastViewedAt(LocalDateTime.now());
        return proposalRepository.save(p);
    }

    @Transactional
    public void linkApplication(Long tenantId, Long proposalId, Long applicationId) {
        Proposal p = get(tenantId, proposalId);
        if (p == null) return;
        p.setApplicationId(applicationId);
        proposalRepository.save(p);
    }

    @Transactional
    public void archive(Long tenantId, Long id) {
        Proposal p = get(tenantId, id);
        if (p == null) return;
        p.setDeleted(true);
        p.setModifiedBy(SecurityUtils.currentUserLogin());
        proposalRepository.save(p);
        auditService.log("ARCHIVE", "Proposal", id, p.getCode(), "Proposal withdrawn - public link disabled");
    }

    public Agency agency(Long tenantId) {
        return agencyRepository.findById(tenantId).orElse(null);
    }

    private static String newToken() {
        StringBuilder sb = new StringBuilder(24);
        for (int i = 0; i < 24; i++) sb.append(TOKEN_ALPHABET.charAt(RANDOM.nextInt(TOKEN_ALPHABET.length())));
        return sb.toString();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
