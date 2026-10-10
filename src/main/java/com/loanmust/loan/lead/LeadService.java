package com.loanmust.loan.lead;

import com.loanmust.loan.audit.AuditService;
import com.loanmust.loan.builder.Builder;
import com.loanmust.loan.builder.BuilderRepository;
import com.loanmust.loan.builder.Project;
import com.loanmust.loan.builder.ProjectRepository;
import com.loanmust.loan.common.Channel;
import com.loanmust.loan.common.CodeSequenceService;
import com.loanmust.loan.customer.Customer;
import com.loanmust.loan.customer.CustomerRepository;
import com.loanmust.loan.lender.LoanType;
import com.loanmust.loan.referral.ReferralPartner;
import com.loanmust.loan.referral.ReferralPartnerRepository;
import com.loanmust.loan.security.CustomUserDetails;
import com.loanmust.loan.security.SecurityUtils;
import com.loanmust.loan.user.User;
import com.loanmust.loan.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Lead capture, channel tagging, follow-ups, scoring and conversion to a customer. */
@Service
public class LeadService {

    private static final List<LeadStatus> OPEN = List.of(LeadStatus.NEW, LeadStatus.CONTACTED,
            LeadStatus.INTERESTED, LeadStatus.FOLLOW_UP, LeadStatus.DOCS_REQUESTED);

    private final LeadRepository leadRepository;
    private final LeadActivityRepository activityRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final BuilderRepository builderRepository;
    private final ProjectRepository projectRepository;
    private final ReferralPartnerRepository partnerRepository;
    private final LeadScoringService scoringService;
    private final CodeSequenceService codeSequenceService;
    private final AuditService auditService;

    public LeadService(LeadRepository leadRepository, LeadActivityRepository activityRepository,
                       CustomerRepository customerRepository, UserRepository userRepository,
                       BuilderRepository builderRepository, ProjectRepository projectRepository,
                       ReferralPartnerRepository partnerRepository, LeadScoringService scoringService,
                       CodeSequenceService codeSequenceService, AuditService auditService) {
        this.leadRepository = leadRepository;
        this.activityRepository = activityRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.builderRepository = builderRepository;
        this.projectRepository = projectRepository;
        this.partnerRepository = partnerRepository;
        this.scoringService = scoringService;
        this.codeSequenceService = codeSequenceService;
        this.auditService = auditService;
    }

    public static List<LeadStatus> openStatuses() {
        return OPEN;
    }

    // ------------------------------------------------------------------ reading

    public Page<Lead> search(Long tenantId, String q, LeadStatus status, LeadSource source, LeadPriority priority,
                             LoanType loanType, Long assignedUserId, LocalDate from, LocalDate to, int page, int size) {
        return leadRepository.search(tenantId, q == null ? "" : q.trim(), status, source, priority, loanType,
                assignedUserId,
                from == null ? null : from.atStartOfDay(),
                to == null ? null : to.atTime(23, 59, 59),
                PageRequest.of(Math.max(0, page), size));
    }

    public Lead get(Long tenantId, Long id) {
        return id == null ? null : leadRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId).orElse(null);
    }

    public List<LeadActivity> timeline(Long tenantId, Long leadId) {
        return activityRepository.findByTenantIdAndLeadIdAndDeletedFalseOrderByIdDesc(tenantId, leadId);
    }

    public LeadScoringService.Assessment assess(Long tenantId, Lead lead) {
        return scoringService.assess(lead, activityRepository.countByTenantIdAndLeadIdAndDeletedFalse(tenantId, lead.getId()));
    }

    public List<Lead> dueFollowUps(Long tenantId, LocalDate until, Long ownerId) {
        return leadRepository.dueFollowUps(tenantId, until, OPEN, ownerId);
    }

    // ------------------------------------------------------------------ writing

    @Transactional
    public Lead save(Long tenantId, Lead form) {
        boolean isNew = form.getId() == null;
        Lead entity;
        if (isNew) {
            entity = new Lead();
            entity.setTenantId(tenantId);
            entity.setCreatedBy(SecurityUtils.currentUserLogin());
            entity.setCode(codeSequenceService.next(tenantId, CodeSequenceService.Type.LEAD,
                    leadRepository.countByTenantId(tenantId)));
            entity.setFollowUpCount(0);
        } else {
            entity = get(tenantId, form.getId());
            if (entity == null) return null;
        }

        AuditService.Diff diff = new AuditService.Diff()
                .compare("Name", entity.getName(), form.getName())
                .compare("Mobile", entity.getMobile(), form.getMobile())
                .compare("Status", entity.getStatus(), form.getStatus())
                .compare("Source", entity.getSource(), form.getSource())
                .compare("Loan type", entity.getLoanType(), form.getLoanType())
                .compare("Loan amount", entity.getLoanAmountRequired(), form.getLoanAmountRequired())
                .compare("CIBIL", entity.getCibilScore(), form.getCibilScore())
                .compare("Next follow-up", entity.getNextFollowUpDate(), form.getNextFollowUpDate());

        entity.setName(trim(form.getName()));
        entity.setMobile(trim(form.getMobile()));
        entity.setEmail(trim(form.getEmail()));
        entity.setCity(trim(form.getCity()));
        entity.setSource(form.getSource() == null ? LeadSource.DIRECT : form.getSource());
        entity.setSourceDetail(trim(form.getSourceDetail()));
        entity.setLoanType(form.getLoanType());
        entity.setLoanAmountRequired(form.getLoanAmountRequired());
        entity.setPropertyValue(form.getPropertyValue());
        entity.setMonthlyIncome(form.getMonthlyIncome());
        entity.setExistingEmi(form.getExistingEmi());
        entity.setCibilScore(form.getCibilScore());
        entity.setEmploymentType(form.getEmploymentType());
        entity.setStatus(form.getStatus() == null ? LeadStatus.NEW : form.getStatus());
        entity.setNextFollowUpDate(form.getNextFollowUpDate());
        entity.setRequirement(form.getRequirement());
        entity.setNotes(form.getNotes());
        if (entity.getStatus() == LeadStatus.LOST || entity.getStatus() == LeadStatus.NOT_ELIGIBLE) {
            entity.setLostReason(form.getLostReason());
        }
        // Channel tags follow the source: a builder only on a builder lead, a partner only on a referral.
        boolean builderSource = entity.getSource() == LeadSource.BUILDER;
        boolean referralSource = entity.getSource() == LeadSource.REFERRAL;
        applyChannel(tenantId, entity,
                builderSource ? form.getBuilderId() : null,
                builderSource ? form.getProjectId() : null,
                referralSource ? form.getReferralPartnerId() : null);
        entity.setModifiedBy(SecurityUtils.currentUserLogin());

        if (form.getAssignedUserId() != null && !form.getAssignedUserId().equals(entity.getAssignedUserId())) {
            applyAssignment(tenantId, entity, form.getAssignedUserId());
        } else if (isNew && form.getAssignedUserId() == null) {
            // A lead entered by a sales user belongs to them unless reassigned.
            CustomUserDetails current = SecurityUtils.currentUser();
            if (current != null && current.getRole() != null && current.getRole().isSales()) {
                applyAssignment(tenantId, entity, current.getId());
            }
        }

        detectDuplicate(tenantId, entity);
        Lead saved = leadRepository.save(entity);
        rescore(tenantId, saved);
        saved = leadRepository.save(saved);

        if (isNew) {
            auditService.log("CREATE", "Lead", saved.getId(), saved.getName(), "Lead captured · " + saved.getChannelLine());
        } else {
            auditService.logDiff("Lead", saved.getId(), saved.getName(), diff);
        }
        return saved;
    }

    /** Enquiry submitted through a referral partner's public link. */
    @Transactional
    public Lead captureReferral(ReferralPartner partner, String name, String mobile, String city,
                                LoanType loanType, BigDecimal amount, String note) {
        Long tenantId = partner.getTenantId();
        Lead lead = new Lead();
        lead.setTenantId(tenantId);
        lead.setCode(codeSequenceService.next(tenantId, CodeSequenceService.Type.LEAD, leadRepository.countByTenantId(tenantId)));
        lead.setCreatedBy("referral-link:" + partner.getCode());
        lead.setName(trim(name));
        lead.setMobile(trim(mobile));
        lead.setCity(trim(city));
        lead.setLoanType(loanType == null ? LoanType.HOME_LOAN : loanType);
        lead.setLoanAmountRequired(amount);
        lead.setRequirement(note);
        lead.setSource(LeadSource.REFERRAL);
        lead.setReferralPartnerId(partner.getId());
        lead.setReferralPartnerName(partner.getName());
        lead.setStatus(LeadStatus.NEW);
        lead.setNextFollowUpDate(LocalDate.now());
        lead.setFollowUpCount(0);
        detectDuplicate(tenantId, lead);
        Lead saved = leadRepository.save(lead);
        rescore(tenantId, saved);
        saved = leadRepository.save(saved);
        auditService.logFor(tenantId, "CREATE", "Lead", saved.getId(), saved.getName(),
                "Submitted through referral link of " + partner.getName() + " (" + partner.getCode() + ")");
        return saved;
    }

    @Transactional
    public LeadActivity addActivity(Long tenantId, Long leadId, LeadActivity form, LeadStatus newStatus) {
        Lead lead = get(tenantId, leadId);
        if (lead == null) return null;
        CustomUserDetails current = SecurityUtils.currentUser();

        LeadActivity a = new LeadActivity();
        a.setTenantId(tenantId);
        a.setLeadId(leadId);
        a.setActivityDate(form.getActivityDate() == null ? LocalDate.now() : form.getActivityDate());
        a.setChannel(form.getChannel() == null ? Channel.CALL : form.getChannel());
        a.setRemarks(form.getRemarks());
        a.setOutcome(form.getOutcome());
        a.setNextFollowUpDate(form.getNextFollowUpDate());
        a.setUserId(current == null ? null : current.getId());
        a.setUserName(current == null ? "System" : current.getFullName());
        a.setCreatedBy(SecurityUtils.currentUserLogin());
        LeadActivity saved = activityRepository.save(a);

        LeadStatus before = lead.getStatus();
        lead.setLastContactedAt(LocalDateTime.now());
        lead.setNextFollowUpDate(form.getNextFollowUpDate());
        lead.setFollowUpCount((lead.getFollowUpCount() == null ? 0 : lead.getFollowUpCount()) + 1);
        if (newStatus != null) {
            lead.setStatus(newStatus);
        } else if (lead.getStatus() == LeadStatus.NEW) {
            lead.setStatus(LeadStatus.CONTACTED);
        }
        lead.setModifiedBy(SecurityUtils.currentUserLogin());
        rescore(tenantId, lead);
        leadRepository.save(lead);

        auditService.log("UPDATE", "Lead", leadId, lead.getName(),
                "Follow-up via " + a.getChannelLabel() + (form.getOutcome() == null ? "" : " - " + form.getOutcome()));
        if (before != lead.getStatus()) {
            auditService.logStatus("Lead", leadId, lead.getName(), before.getLabel(), lead.getStatus().getLabel(), null);
        }
        return saved;
    }

    @Transactional
    public void assign(Long tenantId, Long leadId, Long userId) {
        Lead lead = get(tenantId, leadId);
        if (lead == null) return;
        String previous = lead.getAssignedUserName();
        applyAssignment(tenantId, lead, userId);
        lead.setModifiedBy(SecurityUtils.currentUserLogin());
        leadRepository.save(lead);
        auditService.write("ASSIGN", "Lead", leadId, lead.getName(), "Assigned to", previous, lead.getAssignedUserName());
    }

    @Transactional
    public void close(Long tenantId, Long leadId, LeadStatus status, String reason) {
        Lead lead = get(tenantId, leadId);
        if (lead == null || status == null) return;
        String before = lead.getStatus().getLabel();
        lead.setStatus(status);
        lead.setLostReason(reason);
        lead.setNextFollowUpDate(null);
        lead.setModifiedBy(SecurityUtils.currentUserLogin());
        rescore(tenantId, lead);
        leadRepository.save(lead);
        auditService.logStatus("Lead", leadId, lead.getName(), before, status.getLabel(), reason);
    }

    /**
     * Turns a lead into a customer, carrying across the credit profile. Idempotent and
     * mobile-matched: an existing customer with the same mobile is reused, never duplicated.
     */
    @Transactional
    public Customer convertToCustomer(Long tenantId, Long leadId) {
        Lead lead = get(tenantId, leadId);
        if (lead == null) return null;
        if (lead.getConvertedCustomerId() != null) {
            Customer existing = customerRepository.findByIdAndTenantIdAndDeletedFalse(lead.getConvertedCustomerId(), tenantId).orElse(null);
            if (existing != null) return existing;
        }
        Customer customer = customerRepository.findFirstByTenantIdAndMobileAndDeletedFalse(tenantId, lead.getMobile()).orElse(null);
        boolean created = customer == null;
        if (created) {
            customer = new Customer();
            customer.setTenantId(tenantId);
            customer.setCode(codeSequenceService.next(tenantId, CodeSequenceService.Type.CUSTOMER,
                    customerRepository.countByTenantId(tenantId)));
            customer.setCreatedBy(SecurityUtils.currentUserLogin());
            customer.setName(lead.getName());
            customer.setMobile(lead.getMobile());
            customer.setEmail(lead.getEmail());
            customer.setCity(lead.getCity());
            customer.setOwnerUserId(lead.getAssignedUserId());
            customer.setOwnerName(lead.getAssignedUserName());
            customer.setSourceLeadId(lead.getId());
        }
        // Fill gaps in the credit profile from the lead without overwriting what is known.
        if (customer.getEmploymentType() == null) customer.setEmploymentType(lead.getEmploymentType());
        if (customer.getMonthlyIncome() == null) customer.setMonthlyIncome(lead.getMonthlyIncome());
        if (customer.getExistingEmi() == null) customer.setExistingEmi(lead.getExistingEmi());
        if (customer.getCibilScore() == null) customer.setCibilScore(lead.getCibilScore());
        if (customer.getReferralPartnerId() == null && lead.getReferralPartnerId() != null) {
            customer.setReferralPartnerId(lead.getReferralPartnerId());
            customer.setReferralPartnerName(lead.getReferralPartnerName());
        }
        customer.setModifiedBy(SecurityUtils.currentUserLogin());
        Customer saved = customerRepository.save(customer);

        lead.setConvertedCustomerId(saved.getId());
        lead.setModifiedBy(SecurityUtils.currentUserLogin());
        leadRepository.save(lead);

        if (created) {
            auditService.log("CREATE", "Customer", saved.getId(), saved.getName(), "Created from lead " + lead.getCode());
        }
        auditService.log("UPDATE", "Lead", leadId, lead.getName(), "Linked to customer " + saved.getCode());
        return saved;
    }

    /** Marks the lead converted once a loan file exists for it. */
    @Transactional
    public void markConverted(Long tenantId, Long leadId, Long applicationId) {
        Lead lead = get(tenantId, leadId);
        if (lead == null) return;
        String before = lead.getStatus().getLabel();
        lead.setStatus(LeadStatus.CONVERTED);
        lead.setConvertedApplicationId(applicationId);
        lead.setConvertedAt(LocalDateTime.now());
        lead.setNextFollowUpDate(null);
        lead.setModifiedBy(SecurityUtils.currentUserLogin());
        rescore(tenantId, lead);
        leadRepository.save(lead);
        auditService.logStatus("Lead", leadId, lead.getName(), before, LeadStatus.CONVERTED.getLabel(), "Loan file opened");
    }

    @Transactional
    public void archive(Long tenantId, Long leadId) {
        Lead lead = get(tenantId, leadId);
        if (lead == null) return;
        lead.setDeleted(true);
        lead.setModifiedBy(SecurityUtils.currentUserLogin());
        leadRepository.save(lead);
        auditService.log("ARCHIVE", "Lead", leadId, lead.getName(), "Lead archived");
    }

    // ------------------------------------------------------------------ internals

    private void rescore(Long tenantId, Lead lead) {
        long activities = lead.getId() == null ? 0 : activityRepository.countByTenantIdAndLeadIdAndDeletedFalse(tenantId, lead.getId());
        LeadScoringService.Assessment a = scoringService.assess(lead, activities);
        lead.setScore(a.score());
        lead.setPriority(a.priority());
        lead.setSuggestedAction(a.suggestedAction());
    }

    private void detectDuplicate(Long tenantId, Lead lead) {
        if (lead.getMobile() == null || lead.getMobile().isBlank()) return;
        Long duplicateOf = null;
        for (Lead other : leadRepository.findByTenantIdAndMobileAndDeletedFalse(tenantId, lead.getMobile())) {
            if (other.getId() == null || other.getId().equals(lead.getId())) continue;
            if (other.isOpen()) {
                duplicateOf = other.getId();
                break;
            }
        }
        lead.setDuplicateOfLeadId(duplicateOf);
    }

    private void applyChannel(Long tenantId, Lead lead, Long builderId, Long projectId, Long partnerId) {
        Project project = projectId == null ? null : projectRepository.findByIdAndTenantIdAndDeletedFalse(projectId, tenantId).orElse(null);
        if (project != null && builderId == null) builderId = project.getBuilderId();
        Builder builder = builderId == null ? null : builderRepository.findByIdAndTenantIdAndDeletedFalse(builderId, tenantId).orElse(null);
        lead.setBuilderId(builder == null ? null : builder.getId());
        lead.setBuilderName(builder == null ? null : builder.getName());
        lead.setProjectId(project == null ? null : project.getId());
        lead.setProjectName(project == null ? null : project.getName());
        ReferralPartner partner = partnerId == null ? null : partnerRepository.findByIdAndTenantIdAndDeletedFalse(partnerId, tenantId).orElse(null);
        lead.setReferralPartnerId(partner == null ? null : partner.getId());
        lead.setReferralPartnerName(partner == null ? null : partner.getName());
    }

    private void applyAssignment(Long tenantId, Lead lead, Long userId) {
        User user = userId == null ? null : userRepository.findByIdAndTenantIdAndDeletedFalse(userId, tenantId).orElse(null);
        lead.setAssignedUserId(user == null ? null : user.getId());
        lead.setAssignedUserName(user == null ? null : user.getFullName());
        lead.setAssignedAt(user == null ? null : LocalDateTime.now());
    }

    private static String trim(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
