package com.loanmust.loan.modd;

import com.loanmust.loan.application.LoanApplication;
import com.loanmust.loan.audit.AuditService;
import com.loanmust.loan.common.CodeSequenceService;
import com.loanmust.loan.security.SecurityUtils;
import com.loanmust.loan.tenant.Agency;
import com.loanmust.loan.tenant.AgencyRepository;
import com.loanmust.loan.user.User;
import com.loanmust.loan.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * MODD processing: one job per secured loan file - collect originals, prepare the
 * memorandum and stamp duty, book the sub-registrar, register, deposit originals with the
 * lender and file the acknowledgement.
 */
@Service
public class ModdService {

    public static final String DEFAULT_ORIGINALS = String.join("\n",
            "Original sale deed / title deed",
            "Mother deed & previous chain documents",
            "Encumbrance certificate",
            "Khata certificate & extract",
            "Latest property tax paid receipt",
            "Approved building plan");

    private final ModdRepository repository;
    private final AgencyRepository agencyRepository;
    private final UserRepository userRepository;
    private final CodeSequenceService codeSequenceService;
    private final AuditService auditService;

    public ModdService(ModdRepository repository, AgencyRepository agencyRepository, UserRepository userRepository,
                       CodeSequenceService codeSequenceService, AuditService auditService) {
        this.repository = repository;
        this.agencyRepository = agencyRepository;
        this.userRepository = userRepository;
        this.codeSequenceService = codeSequenceService;
        this.auditService = auditService;
    }

    public Page<ModdRecord> search(Long tenantId, String q, ModdStatus status, int page, int size) {
        return repository.search(tenantId, q == null ? "" : q.trim(), status, PageRequest.of(Math.max(0, page), size));
    }

    public ModdRecord get(Long tenantId, Long id) {
        return id == null ? null : repository.findByIdAndTenantIdAndDeletedFalse(id, tenantId).orElse(null);
    }

    public ModdRecord forApplication(Long tenantId, Long applicationId) {
        return repository.findFirstByTenantIdAndApplicationIdAndDeletedFalse(tenantId, applicationId).orElse(null);
    }

    public List<ModdRecord> upcomingAppointments(Long tenantId, int days) {
        LocalDateTime now = LocalDateTime.now().toLocalDate().atStartOfDay();
        return repository.appointmentsBetween(tenantId, now, now.plusDays(days + 1L));
    }

    /** Stamp duty at the agency's configured rate and cap (state-specific; set in Settings). */
    public BigDecimal stampDutyFor(Long tenantId, BigDecimal securedAmount) {
        Agency agency = agencyRepository.findById(tenantId).orElse(null);
        if (agency == null || securedAmount == null || agency.getModdStampDutyPercent() == null) return null;
        BigDecimal duty = securedAmount.multiply(agency.getModdStampDutyPercent()).divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);
        if (agency.getModdStampDutyCap() != null && agency.getModdStampDutyCap().signum() > 0) {
            duty = duty.min(agency.getModdStampDutyCap());
        }
        return duty;
    }

    /** Creates the MODD job for a file if it has none. Idempotent. */
    @Transactional
    public ModdRecord ensureFor(Long tenantId, LoanApplication a) {
        ModdRecord existing = forApplication(tenantId, a.getId());
        if (existing != null) return existing;
        Agency agency = agencyRepository.findById(tenantId).orElse(null);
        ModdRecord m = new ModdRecord();
        m.setTenantId(tenantId);
        m.setCode(codeSequenceService.next(tenantId, CodeSequenceService.Type.MODD, repository.countByTenantId(tenantId)));
        m.setApplicationId(a.getId());
        m.setApplicationCode(a.getCode());
        m.setCustomerId(a.getCustomerId());
        m.setCustomerName(a.getCustomerName());
        m.setCustomerMobile(a.getCustomerMobile());
        m.setLenderName(a.getLenderName());
        m.setPropertyAddress(a.getPropertyAddress());
        m.setSecuredAmount(a.getLoanAmount());
        m.setStampDuty(stampDutyFor(tenantId, a.getLoanAmount()));
        m.setServiceCharge(agency == null ? null : agency.getModdServiceCharge());
        m.setOriginalDocuments(DEFAULT_ORIGINALS);
        m.setStatus(ModdStatus.NOT_STARTED);
        m.setAssignedUserId(a.getProcessorUserId());
        m.setAssignedUserName(a.getProcessorName());
        m.setCreatedBy(SecurityUtils.currentUserLogin());
        ModdRecord saved = repository.save(m);
        auditService.log("CREATE", "Modd", saved.getId(), saved.getCode() + " · " + saved.getCustomerName(),
                "MODD job opened for " + a.getCode());
        return saved;
    }

    @Transactional
    public ModdRecord save(Long tenantId, ModdRecord form) {
        ModdRecord m = get(tenantId, form.getId());
        if (m == null) return null;
        AuditService.Diff diff = new AuditService.Diff()
                .compare("Status", m.getStatus(), form.getStatus())
                .compare("Sub-registrar", m.getSubRegistrarOffice(), form.getSubRegistrarOffice())
                .compare("Appointment", m.getAppointmentAt(), form.getAppointmentAt())
                .compare("Registration no", m.getRegistrationNumber(), form.getRegistrationNumber())
                .compare("Stamp duty", m.getStampDuty(), form.getStampDuty())
                .compare("Charges collected", m.isChargesCollected(), form.isChargesCollected())
                .compare("Deposited on", m.getDepositedOn(), form.getDepositedOn());

        m.setPropertyAddress(form.getPropertyAddress());
        m.setSecuredAmount(form.getSecuredAmount());
        m.setSubRegistrarOffice(form.getSubRegistrarOffice());
        m.setAppointmentAt(form.getAppointmentAt());
        m.setRegisteredOn(form.getRegisteredOn());
        m.setRegistrationNumber(form.getRegistrationNumber());
        m.setStampDuty(form.getStampDuty());
        m.setRegistrationFee(form.getRegistrationFee());
        m.setOtherCharges(form.getOtherCharges());
        m.setServiceCharge(form.getServiceCharge());
        m.setChargesCollected(form.isChargesCollected());
        m.setOriginalDocuments(form.getOriginalDocuments());
        m.setDepositedOn(form.getDepositedOn());
        m.setDepositedWith(form.getDepositedWith());
        m.setAcknowledgementNumber(form.getAcknowledgementNumber());
        m.setRemarks(form.getRemarks());
        User u = form.getAssignedUserId() == null ? null
                : userRepository.findByIdAndTenantIdAndDeletedFalse(form.getAssignedUserId(), tenantId).orElse(null);
        m.setAssignedUserId(u == null ? null : u.getId());
        m.setAssignedUserName(u == null ? null : u.getFullName());

        // Status follows the facts when they are entered, so the board never lags reality.
        ModdStatus status = form.getStatus() == null ? m.getStatus() : form.getStatus();
        if (status != ModdStatus.CANCELLED) {
            if (m.getDepositedOn() != null && status.ordinal() < ModdStatus.DEPOSITED_WITH_BANK.ordinal()) {
                status = ModdStatus.DEPOSITED_WITH_BANK;
            } else if (m.getRegistrationNumber() != null && !m.getRegistrationNumber().isBlank()
                    && status.ordinal() < ModdStatus.REGISTERED.ordinal()) {
                status = ModdStatus.REGISTERED;
            } else if (m.getAppointmentAt() != null && status.ordinal() < ModdStatus.APPOINTMENT_BOOKED.ordinal()) {
                status = ModdStatus.APPOINTMENT_BOOKED;
            }
        }
        m.setStatus(status);
        m.setModifiedBy(SecurityUtils.currentUserLogin());
        ModdRecord saved = repository.save(m);
        auditService.logDiff("Modd", saved.getId(), saved.getCode() + " · " + saved.getCustomerName(), diff);
        return saved;
    }

    @Transactional
    public void archive(Long tenantId, Long id) {
        ModdRecord m = get(tenantId, id);
        if (m == null) return;
        m.setDeleted(true);
        m.setModifiedBy(SecurityUtils.currentUserLogin());
        repository.save(m);
        auditService.log("ARCHIVE", "Modd", id, m.getCode(), "MODD job archived");
    }
}
