package com.loanmust.loan.application;

import com.loanmust.loan.advisor.LoanAdvisorService;
import com.loanmust.loan.advisor.LoanMath;
import com.loanmust.loan.audit.AuditService;
import com.loanmust.loan.builder.Builder;
import com.loanmust.loan.builder.BuilderService;
import com.loanmust.loan.builder.Project;
import com.loanmust.loan.commission.CommissionService;
import com.loanmust.loan.common.CodeSequenceService;
import com.loanmust.loan.common.Labels;
import com.loanmust.loan.customer.Customer;
import com.loanmust.loan.customer.CustomerService;
import com.loanmust.loan.lead.LeadService;
import com.loanmust.loan.lead.LeadSource;
import com.loanmust.loan.lender.Lender;
import com.loanmust.loan.lender.LenderService;
import com.loanmust.loan.lender.LoanProduct;
import com.loanmust.loan.lender.LoanType;
import com.loanmust.loan.modd.ModdService;
import com.loanmust.loan.referral.ReferralPartner;
import com.loanmust.loan.referral.ReferralService;
import com.loanmust.loan.security.CustomUserDetails;
import com.loanmust.loan.security.SecurityUtils;
import com.loanmust.loan.storage.StorageService;
import com.loanmust.loan.user.User;
import com.loanmust.loan.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The loan file workflow: open → collect documents → log in with the bank → sanction →
 * agreement & MODD → disbursement. Recording a disbursement is the money event: it raises
 * the agency's payout receivable from the lender and the referrer's payout in one step.
 */
@Service
public class LoanApplicationService {

    private final LoanApplicationRepository repository;
    private final ApplicationDocumentRepository documentRepository;
    private final ApplicationEventRepository eventRepository;
    private final DisbursementRepository disbursementRepository;
    private final CustomerService customerService;
    private final LenderService lenderService;
    private final BuilderService builderService;
    private final ReferralService referralService;
    private final CommissionService commissionService;
    private final ModdService moddService;
    private final LeadService leadService;
    private final LoanAdvisorService advisorService;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final CodeSequenceService codeSequenceService;
    private final AuditService auditService;

    public LoanApplicationService(LoanApplicationRepository repository,
                                  ApplicationDocumentRepository documentRepository,
                                  ApplicationEventRepository eventRepository,
                                  DisbursementRepository disbursementRepository,
                                  CustomerService customerService, LenderService lenderService,
                                  BuilderService builderService, ReferralService referralService,
                                  CommissionService commissionService, ModdService moddService,
                                  LeadService leadService, LoanAdvisorService advisorService,
                                  UserRepository userRepository, StorageService storageService,
                                  CodeSequenceService codeSequenceService, AuditService auditService) {
        this.repository = repository;
        this.documentRepository = documentRepository;
        this.eventRepository = eventRepository;
        this.disbursementRepository = disbursementRepository;
        this.customerService = customerService;
        this.lenderService = lenderService;
        this.builderService = builderService;
        this.referralService = referralService;
        this.commissionService = commissionService;
        this.moddService = moddService;
        this.leadService = leadService;
        this.advisorService = advisorService;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.codeSequenceService = codeSequenceService;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------------ reading

    public Page<LoanApplication> search(Long tenantId, String q, ApplicationStage stage, LoanType loanType,
                                        Long lenderId, Long builderId, Long ownerId, int page, int size) {
        return repository.search(tenantId, q == null ? "" : q.trim(), stage, loanType, lenderId, builderId, ownerId,
                PageRequest.of(Math.max(0, page), size));
    }

    public LoanApplication get(Long tenantId, Long id) {
        return id == null ? null : repository.findByIdAndTenantIdAndDeletedFalse(id, tenantId).orElse(null);
    }

    /** A loan executive may only open their own files. */
    public LoanApplication getVisible(Long tenantId, Long id) {
        LoanApplication a = get(tenantId, id);
        Long owner = SecurityUtils.ownerFilterUserId();
        if (a != null && owner != null && !owner.equals(a.getAssignedUserId())) return null;
        return a;
    }

    public List<ApplicationDocument> documents(Long tenantId, Long applicationId) {
        return documentRepository.findByTenantIdAndApplicationIdAndDeletedFalseOrderBySortOrderAscIdAsc(tenantId, applicationId);
    }

    public ApplicationDocument getDocument(Long tenantId, Long docId) {
        return docId == null ? null : documentRepository.findByIdAndTenantIdAndDeletedFalse(docId, tenantId).orElse(null);
    }

    public List<ApplicationEvent> events(Long tenantId, Long applicationId) {
        return eventRepository.findByTenantIdAndApplicationIdAndDeletedFalseOrderByIdDesc(tenantId, applicationId);
    }

    public List<Disbursement> disbursements(Long tenantId, Long applicationId) {
        return disbursementRepository.findByTenantIdAndApplicationIdAndDeletedFalseOrderByDisbursedOnAscIdAsc(tenantId, applicationId);
    }

    public List<LoanApplication> forCustomer(Long tenantId, Long customerId) {
        return repository.findByTenantIdAndCustomerIdAndDeletedFalseOrderByIdDesc(tenantId, customerId);
    }

    public List<LoanApplication> forPartner(Long tenantId, Long partnerId) {
        return repository.findByTenantIdAndReferralPartnerIdAndDeletedFalseOrderByIdDesc(tenantId, partnerId);
    }

    public List<LoanApplication> forBuilder(Long tenantId, Long builderId) {
        return repository.findByTenantIdAndBuilderIdAndDeletedFalseOrderByIdDesc(tenantId, builderId);
    }

    public List<LoanApplication> forLender(Long tenantId, Long lenderId) {
        return repository.findByTenantIdAndLenderIdAndDeletedFalseOrderByIdDesc(tenantId, lenderId);
    }

    /** Pipeline board: each open stage with its top files, count and value. */
    public Map<ApplicationStage, BoardColumn> board(Long tenantId, Long ownerId, int perColumn) {
        Map<ApplicationStage, BoardColumn> board = new LinkedHashMap<>();
        for (ApplicationStage stage : ApplicationStage.path()) {
            List<LoanApplication> files = repository.byStage(tenantId, stage, ownerId, PageRequest.of(0, perColumn));
            board.put(stage, new BoardColumn(stage, files,
                    ownerId == null ? repository.countByTenantIdAndStageAndDeletedFalse(tenantId, stage) : files.size(),
                    repository.valueInStage(tenantId, stage, ownerId)));
        }
        return board;
    }

    public record BoardColumn(ApplicationStage stage, List<LoanApplication> files, long count, BigDecimal value) {
    }

    /** Document completeness for the progress chip: done / total mandatory. */
    public int[] documentProgress(List<ApplicationDocument> docs) {
        int total = 0, done = 0;
        for (ApplicationDocument d : docs) {
            if (!d.isMandatory()) continue;
            total++;
            if (d.getStatus() != null && d.getStatus().isDone()) done++;
        }
        return new int[]{done, total};
    }

    // ------------------------------------------------------------------ open / edit

    @Transactional
    public LoanApplication save(Long tenantId, LoanApplication form) {
        boolean isNew = form.getId() == null;
        Customer customer = customerService.get(tenantId, form.getCustomerId());
        if (customer == null) return null;
        LoanApplication entity = isNew ? new LoanApplication() : get(tenantId, form.getId());
        if (entity == null) return null;

        AuditService.Diff diff = new AuditService.Diff()
                .compare("Requested amount", entity.getRequestedAmount(), form.getRequestedAmount())
                .compare("Tenure", entity.getTenureYears(), form.getTenureYears())
                .compare("Property value", entity.getPropertyValue(), form.getPropertyValue())
                .compare("Executive", entity.getAssignedUserId(), form.getAssignedUserId());

        if (isNew) {
            entity.setTenantId(tenantId);
            entity.setCreatedBy(SecurityUtils.currentUserLogin());
            entity.setCode(codeSequenceService.next(tenantId, CodeSequenceService.Type.APPLICATION, repository.countByTenantId(tenantId)));
            entity.setStage(ApplicationStage.DOCS_PENDING);
            entity.setStageChangedAt(LocalDateTime.now());
            entity.setDisbursementStatus(DisbursementStatus.NOT_STARTED);
            entity.setTotalDisbursed(BigDecimal.ZERO);
            entity.setLeadId(form.getLeadId());
            entity.setProposalId(form.getProposalId());
        }
        entity.setCustomerId(customer.getId());
        entity.setCustomerName(customer.getName());
        entity.setCustomerMobile(customer.getMobile());
        entity.setLoanType(form.getLoanType() == null ? LoanType.HOME_LOAN : form.getLoanType());

        LoanProduct product = lenderService.getProduct(tenantId, form.getProductId());
        Lender lender = lenderService.get(tenantId, product != null ? product.getLenderId() : form.getLenderId());
        String previousLender = entity.getLenderName();
        entity.setLenderId(lender == null ? null : lender.getId());
        entity.setLenderName(lender == null ? null : lender.getDisplayName());
        entity.setProductId(product == null ? null : product.getId());
        entity.setProductName(product == null ? null : product.getName());
        if (form.getPayoutPercent() != null) {
            entity.setPayoutPercent(form.getPayoutPercent());
        } else if (isNew || entity.getPayoutPercent() == null) {
            entity.setPayoutPercent(product != null ? lenderService.payoutPercentFor(tenantId, product)
                    : lender == null ? null : lender.getPayoutPercent());
        }

        entity.setSource(form.getSource() == null ? LeadSource.DIRECT : form.getSource());
        Project project = builderService.getProject(tenantId, form.getProjectId());
        Builder builder = builderService.get(tenantId, project != null ? project.getBuilderId() : form.getBuilderId());
        entity.setBuilderId(builder == null ? null : builder.getId());
        entity.setBuilderName(builder == null ? null : builder.getName());
        entity.setProjectId(project == null ? null : project.getId());
        entity.setProjectName(project == null ? null : project.getName());
        if (builder != null && entity.getSource() == LeadSource.DIRECT) entity.setSource(LeadSource.BUILDER);

        Long partnerId = form.getReferralPartnerId() != null ? form.getReferralPartnerId() : (isNew ? customer.getReferralPartnerId() : null);
        ReferralPartner partner = referralService.get(tenantId, partnerId);
        entity.setReferralPartnerId(partner == null ? null : partner.getId());
        entity.setReferralPartnerName(partner == null ? null : partner.getName());
        if (partner != null && entity.getSource() == LeadSource.DIRECT) entity.setSource(LeadSource.REFERRAL);

        entity.setPropertyAddress(form.getPropertyAddress());
        entity.setPropertyType(form.getPropertyType());
        entity.setPropertyValue(form.getPropertyValue());
        entity.setRequestedAmount(form.getRequestedAmount());
        entity.setTenureYears(form.getTenureYears());
        entity.setCoApplicantName(form.getCoApplicantName());
        entity.setCoApplicantRelation(form.getCoApplicantRelation());
        entity.setCoApplicantIncome(form.getCoApplicantIncome());
        entity.setExpectedDisbursementDate(form.getExpectedDisbursementDate());
        entity.setNotes(form.getNotes());

        User exec = userFor(tenantId, form.getAssignedUserId());
        if (exec != null) {
            entity.setAssignedUserId(exec.getId());
            entity.setAssignedUserName(exec.getFullName());
        } else if (isNew) {
            CustomUserDetails me = SecurityUtils.currentUser();
            Long ownerId = customer.getOwnerUserId() != null ? customer.getOwnerUserId() : me == null ? null : me.getId();
            User owner = userFor(tenantId, ownerId);
            entity.setAssignedUserId(owner == null ? null : owner.getId());
            entity.setAssignedUserName(owner == null ? null : owner.getFullName());
        }
        User processor = userFor(tenantId, form.getProcessorUserId());
        entity.setProcessorUserId(processor == null ? null : processor.getId());
        entity.setProcessorName(processor == null ? null : processor.getFullName());
        entity.setModifiedBy(SecurityUtils.currentUserLogin());

        LoanApplication saved = repository.save(entity);
        if (isNew) {
            seedChecklist(tenantId, saved, customer);
            event(tenantId, saved.getId(), "STAGE", null, ApplicationStage.DOCS_PENDING,
                    "File opened" + (saved.getLenderName() == null ? "" : " for " + saved.getLenderName())
                            + (saved.getRequestedAmount() == null ? "" : " · ₹" + Labels.inr(saved.getRequestedAmount())));
            auditService.log("CREATE", "LoanApplication", saved.getId(), saved.getCode() + " · " + saved.getCustomerName(),
                    "Loan file opened");
            if (saved.getLeadId() != null) leadService.markConverted(tenantId, saved.getLeadId(), saved.getId());
            if (saved.getProposalId() != null) advisorService.linkApplication(tenantId, saved.getProposalId(), saved.getId());
        } else {
            diff.compare("Lender", previousLender, saved.getLenderName());
            auditService.logDiff("LoanApplication", saved.getId(), saved.getCode() + " · " + saved.getCustomerName(), diff);
        }
        return saved;
    }

    // ------------------------------------------------------------------ workflow

    @Transactional
    public String moveStage(Long tenantId, Long id, ApplicationStage stage, String remarks) {
        LoanApplication a = get(tenantId, id);
        if (a == null || stage == null) return "File not found.";
        if (a.getStage() == stage) return null;
        if (stage == ApplicationStage.DISBURSED) {
            return "Record a disbursement to mark the file disbursed - that also raises the bank and referral payouts.";
        }
        if (stage == ApplicationStage.SANCTIONED && a.getSanctionedAmount() == null) {
            return "Record the sanction (amount, rate, tenure) to move the file to Sanctioned.";
        }
        if (stage == ApplicationStage.LOGGED_IN && (a.getBankReference() == null || a.getBankReference().isBlank())) {
            return "Record the bank login (lender's application number) first.";
        }
        if (stage == ApplicationStage.REJECTED && (remarks == null || remarks.isBlank())) {
            return "Give the reason the lender rejected the file.";
        }
        if (a.getStage() == ApplicationStage.DISBURSED && stage != ApplicationStage.WITHDRAWN) {
            return "A disbursed file cannot move back.";
        }
        ApplicationStage from = a.getStage();
        applyStage(tenantId, a, stage, remarks);
        if (stage == ApplicationStage.REJECTED) a.setRejectionReason(remarks);
        repository.save(a);
        if (stage == ApplicationStage.MODD_AGREEMENT && a.getLoanType() != null && a.getLoanType().isSecured()) {
            moddService.ensureFor(tenantId, a);
        }
        auditService.logStatus("LoanApplication", id, a.getCode() + " · " + a.getCustomerName(),
                from.getLabel(), stage.getLabel(), remarks);
        return null;
    }

    @Transactional
    public void recordLogin(Long tenantId, Long id, String bankReference, LocalDate loginDate, String remarks) {
        LoanApplication a = get(tenantId, id);
        if (a == null || bankReference == null || bankReference.isBlank()) return;
        a.setBankReference(bankReference.trim());
        a.setLoginDate(loginDate == null ? LocalDate.now() : loginDate);
        if (a.getStage() == ApplicationStage.DOCS_PENDING) {
            ApplicationStage from = a.getStage();
            applyStage(tenantId, a, ApplicationStage.LOGGED_IN,
                    "Logged in with " + nz(a.getLenderName()) + " · ref " + a.getBankReference()
                            + (remarks == null || remarks.isBlank() ? "" : " · " + remarks));
            auditService.logStatus("LoanApplication", id, a.getCode(), from.getLabel(), ApplicationStage.LOGGED_IN.getLabel(), null);
        } else {
            event(tenantId, id, "NOTE", null, null, "Bank reference updated to " + a.getBankReference());
        }
        a.setModifiedBy(SecurityUtils.currentUserLogin());
        repository.save(a);
    }

    @Transactional
    public void recordSanction(Long tenantId, Long id, BigDecimal amount, BigDecimal rate, Integer tenureYears,
                               LocalDate sanctionDate, LocalDate validTill, String remarks) {
        LoanApplication a = get(tenantId, id);
        if (a == null || amount == null || amount.signum() <= 0) return;
        AuditService.Diff diff = new AuditService.Diff()
                .compare("Sanctioned amount", a.getSanctionedAmount(), amount)
                .compare("Sanctioned rate", a.getSanctionedRate(), rate)
                .compare("Sanctioned tenure", a.getSanctionedTenureYears(), tenureYears);
        a.setSanctionedAmount(amount);
        a.setSanctionedRate(rate);
        a.setSanctionedTenureYears(tenureYears == null ? a.getTenureYears() : tenureYears);
        a.setSanctionDate(sanctionDate == null ? LocalDate.now() : sanctionDate);
        a.setSanctionValidTill(validTill == null ? a.getSanctionDate().plusMonths(6) : validTill);
        if (rate != null && a.getSanctionedTenureYears() != null) {
            a.setSanctionedEmi(LoanMath.emi(amount, rate, a.getSanctionedTenureYears() * 12));
        }
        String note = "Sanctioned ₹" + Labels.inr(amount) + (rate == null ? "" : " @ " + rate.stripTrailingZeros().toPlainString() + "%")
                + (a.getSanctionedTenureYears() == null ? "" : " for " + a.getSanctionedTenureYears() + " yrs")
                + (a.getSanctionedEmi() == null ? "" : " · EMI ₹" + Labels.inr(a.getSanctionedEmi()))
                + (remarks == null || remarks.isBlank() ? "" : " · " + remarks);
        if (a.getStage().ordinal() < ApplicationStage.SANCTIONED.ordinal()) {
            ApplicationStage from = a.getStage();
            applyStage(tenantId, a, ApplicationStage.SANCTIONED, note);
            auditService.logStatus("LoanApplication", id, a.getCode(), from.getLabel(), ApplicationStage.SANCTIONED.getLabel(), null);
        } else {
            event(tenantId, id, "NOTE", null, null, "Sanction revised: " + note);
        }
        a.setModifiedBy(SecurityUtils.currentUserLogin());
        repository.save(a);
        auditService.logDiff("LoanApplication", id, a.getCode() + " · " + a.getCustomerName(), diff);
    }

    /**
     * Records a tranche. Updates the file's disbursed total and status, moves it to
     * DISBURSED, and raises the lender payout and the referrer payout for this tranche.
     */
    @Transactional
    public String addDisbursement(Long tenantId, Long id, Disbursement form) {
        LoanApplication a = get(tenantId, id);
        if (a == null) return "File not found.";
        if (form.getAmount() == null || form.getAmount().signum() <= 0) return "Enter the amount disbursed.";
        if (a.getStage().isTerminalFailure()) return "This file is " + a.getStage().getLabel().toLowerCase() + ".";
        BigDecimal already = disbursementRepository.totalFor(tenantId, id);
        BigDecimal cap = a.getSanctionedAmount() != null ? a.getSanctionedAmount() : a.getRequestedAmount();
        if (cap != null && already.add(form.getAmount()).compareTo(cap) > 0) {
            return "Total disbursed would exceed the sanctioned amount of ₹" + Labels.inr(cap) + ".";
        }
        long existing = disbursementRepository.countByTenantIdAndApplicationIdAndDeletedFalse(tenantId, id);

        Disbursement d = new Disbursement();
        d.setTenantId(tenantId);
        d.setApplicationId(id);
        d.setTrancheNo((int) existing + 1);
        d.setDisbursedOn(form.getDisbursedOn() == null ? LocalDate.now() : form.getDisbursedOn());
        d.setAmount(form.getAmount().setScale(2, RoundingMode.HALF_UP));
        d.setPaidTo(form.getPaidTo());
        d.setReference(form.getReference());
        d.setRemarks(form.getRemarks());
        d.setCreatedBy(SecurityUtils.currentUserLogin());
        Disbursement saved = disbursementRepository.save(d);

        BigDecimal total = already.add(saved.getAmount());
        a.setTotalDisbursed(total);
        if (a.getFirstDisbursementDate() == null) a.setFirstDisbursementDate(saved.getDisbursedOn());
        a.setDisbursementStatus(cap != null && total.compareTo(cap) >= 0 ? DisbursementStatus.FULL : DisbursementStatus.PARTIAL);
        String note = "Tranche " + saved.getTrancheNo() + ": ₹" + Labels.inr(saved.getAmount())
                + (saved.getPaidTo() == null ? "" : " to " + saved.getPaidTo())
                + (saved.getReference() == null ? "" : " · ref " + saved.getReference());
        if (a.getStage() != ApplicationStage.DISBURSED) {
            ApplicationStage from = a.getStage();
            applyStage(tenantId, a, ApplicationStage.DISBURSED, note);
            auditService.logStatus("LoanApplication", id, a.getCode(), from.getLabel(), ApplicationStage.DISBURSED.getLabel(), null);
        } else {
            event(tenantId, id, "DISBURSEMENT", null, null, note);
        }
        a.setModifiedBy(SecurityUtils.currentUserLogin());
        repository.save(a);
        auditService.write("PAYMENT", "LoanApplication", id, a.getCode() + " · " + a.getCustomerName(),
                "Disbursed", null, note);

        commissionService.raiseForDisbursement(tenantId, a, saved);
        if (a.getReferralPartnerId() != null) {
            referralService.raiseForDisbursement(tenantId, a.getReferralPartnerId(), a.getId(), a.getCode(), saved.getId(),
                    saved.getTrancheNo() == 1, a.getCustomerName(), a.getLenderName(), saved.getAmount());
        }
        return null;
    }

    @Transactional
    public void addNote(Long tenantId, Long id, String type, String remarks) {
        LoanApplication a = get(tenantId, id);
        if (a == null || remarks == null || remarks.isBlank()) return;
        String t = "BANK_QUERY".equals(type) ? "BANK_QUERY" : "NOTE";
        event(tenantId, id, t, null, null, remarks.trim());
        auditService.log("UPDATE", "LoanApplication", id, a.getCode(), ("BANK_QUERY".equals(t) ? "Bank query: " : "Note: ") + remarks.trim());
    }

    // ------------------------------------------------------------------ documents

    @Transactional
    public void addDocument(Long tenantId, Long applicationId, String name, DocumentCategory category, boolean mandatory) {
        LoanApplication a = get(tenantId, applicationId);
        if (a == null || name == null || name.isBlank()) return;
        ApplicationDocument d = new ApplicationDocument();
        d.setTenantId(tenantId);
        d.setApplicationId(applicationId);
        d.setDocName(name.trim());
        d.setCategory(category == null ? DocumentCategory.OTHER : category);
        d.setMandatory(mandatory);
        d.setStatus(DocumentStatus.PENDING);
        d.setSortOrder((int) documentRepository.countByTenantIdAndApplicationIdAndDeletedFalse(tenantId, applicationId) + 1);
        d.setCreatedBy(SecurityUtils.currentUserLogin());
        documentRepository.save(d);
    }

    @Transactional
    public void setDocumentStatus(Long tenantId, Long docId, DocumentStatus status, String remarks) {
        ApplicationDocument d = getDocument(tenantId, docId);
        if (d == null || status == null) return;
        String before = d.getStatus().getLabel();
        d.setStatus(status);
        if (remarks != null) d.setRemarks(remarks.isBlank() ? null : remarks.trim());
        if ((status == DocumentStatus.RECEIVED || status == DocumentStatus.VERIFIED) && d.getReceivedOn() == null) {
            d.setReceivedOn(LocalDate.now());
        }
        d.setModifiedBy(SecurityUtils.currentUserLogin());
        documentRepository.save(d);
        auditService.write("UPDATE", "LoanApplication", d.getApplicationId(), d.getDocName(), "Document", before, status.getLabel());
    }

    @Transactional
    public String uploadDocument(Long tenantId, Long docId, String filename, String contentType, byte[] bytes) {
        ApplicationDocument d = getDocument(tenantId, docId);
        if (d == null) return "Document line not found.";
        if (bytes == null || bytes.length == 0) return "Choose a file to upload.";
        String old = d.getFileKey();
        String key = storageService.upload(tenantId, "applications/" + d.getApplicationId(), filename, bytes);
        d.setFileKey(key);
        d.setFileName(filename);
        d.setContentType(contentType);
        d.setFileSize((long) bytes.length);
        if (d.getStatus() == DocumentStatus.PENDING || d.getStatus() == DocumentStatus.REJECTED) {
            d.setStatus(DocumentStatus.RECEIVED);
        }
        d.setReceivedOn(LocalDate.now());
        CustomUserDetails me = SecurityUtils.currentUser();
        d.setUploadedByName(me == null ? null : me.getFullName());
        d.setModifiedBy(SecurityUtils.currentUserLogin());
        documentRepository.save(d);
        if (old != null) storageService.delete(old);
        auditService.log("UPDATE", "LoanApplication", d.getApplicationId(), d.getDocName(), "Document uploaded: " + filename);
        return null;
    }

    @Transactional
    public void removeDocument(Long tenantId, Long docId) {
        ApplicationDocument d = getDocument(tenantId, docId);
        if (d == null) return;
        d.setDeleted(true);
        d.setModifiedBy(SecurityUtils.currentUserLogin());
        documentRepository.save(d);
        auditService.log("ARCHIVE", "LoanApplication", d.getApplicationId(), d.getDocName(), "Checklist line removed");
    }

    @Transactional
    public void archive(Long tenantId, Long id) {
        LoanApplication a = get(tenantId, id);
        if (a == null) return;
        a.setDeleted(true);
        a.setModifiedBy(SecurityUtils.currentUserLogin());
        repository.save(a);
        auditService.log("ARCHIVE", "LoanApplication", id, a.getCode() + " · " + a.getCustomerName(), "Loan file archived");
    }

    // ------------------------------------------------------------------ internals

    private void seedChecklist(Long tenantId, LoanApplication a, Customer customer) {
        int order = 1;
        for (DocumentChecklist.Item item : DocumentChecklist.forProfile(customer.getEmploymentType(), a.getLoanType(),
                a.getProjectId() != null, a.getCoApplicantName() != null && !a.getCoApplicantName().isBlank())) {
            ApplicationDocument d = new ApplicationDocument();
            d.setTenantId(tenantId);
            d.setApplicationId(a.getId());
            d.setDocName(item.name());
            d.setCategory(item.category());
            d.setMandatory(item.mandatory());
            d.setStatus(DocumentStatus.PENDING);
            d.setSortOrder(order++);
            d.setCreatedBy(SecurityUtils.currentUserLogin());
            documentRepository.save(d);
        }
    }

    private void applyStage(Long tenantId, LoanApplication a, ApplicationStage to, String remarks) {
        ApplicationStage from = a.getStage();
        a.setStage(to);
        a.setStageChangedAt(LocalDateTime.now());
        event(tenantId, a.getId(), "STAGE", from, to, remarks);
    }

    private void event(Long tenantId, Long applicationId, String type, ApplicationStage from, ApplicationStage to, String remarks) {
        ApplicationEvent e = new ApplicationEvent();
        e.setTenantId(tenantId);
        e.setApplicationId(applicationId);
        e.setEventType(type);
        e.setFromStage(from);
        e.setToStage(to);
        e.setRemarks(remarks);
        CustomUserDetails me = SecurityUtils.currentUser();
        e.setUserName(me == null ? "System" : me.getFullName());
        e.setCreatedBy(SecurityUtils.currentUserLogin());
        eventRepository.save(e);
    }

    private User userFor(Long tenantId, Long userId) {
        return userId == null ? null : userRepository.findByIdAndTenantIdAndDeletedFalse(userId, tenantId).orElse(null);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
