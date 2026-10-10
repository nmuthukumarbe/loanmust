package com.loanmust.loan.customer;

import com.loanmust.loan.audit.AuditService;
import com.loanmust.loan.common.CodeSequenceService;
import com.loanmust.loan.referral.ReferralPartner;
import com.loanmust.loan.referral.ReferralPartnerRepository;
import com.loanmust.loan.security.SecurityUtils;
import com.loanmust.loan.user.User;
import com.loanmust.loan.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** The borrower book: profile, credit details, ownership. */
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final ReferralPartnerRepository partnerRepository;
    private final CodeSequenceService codeSequenceService;
    private final AuditService auditService;

    public CustomerService(CustomerRepository customerRepository, UserRepository userRepository,
                           ReferralPartnerRepository partnerRepository,
                           CodeSequenceService codeSequenceService, AuditService auditService) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.partnerRepository = partnerRepository;
        this.codeSequenceService = codeSequenceService;
        this.auditService = auditService;
    }

    public Page<Customer> search(Long tenantId, String q, EmploymentType employmentType, Long ownerUserId, int page, int size) {
        return customerRepository.search(tenantId, q == null ? "" : q.trim(), employmentType, ownerUserId,
                PageRequest.of(Math.max(0, page), size));
    }

    public Customer get(Long tenantId, Long id) {
        return id == null ? null : customerRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId).orElse(null);
    }

    public List<Customer> all(Long tenantId) {
        return customerRepository.findByTenantIdAndDeletedFalseOrderByNameAsc(tenantId);
    }

    /** Another customer already holding this mobile number, if any. */
    public Customer duplicateOf(Long tenantId, Customer form) {
        if (form.getMobile() == null || form.getMobile().isBlank()) return null;
        Customer other = customerRepository.findFirstByTenantIdAndMobileAndDeletedFalse(tenantId, form.getMobile().trim()).orElse(null);
        return other != null && !other.getId().equals(form.getId()) ? other : null;
    }

    @Transactional
    public Customer save(Long tenantId, Customer form) {
        boolean isNew = form.getId() == null;
        Customer entity = isNew ? new Customer() : get(tenantId, form.getId());
        if (entity == null) return null;
        if (isNew) {
            entity.setTenantId(tenantId);
            entity.setCreatedBy(SecurityUtils.currentUserLogin());
            entity.setCode(codeSequenceService.next(tenantId, CodeSequenceService.Type.CUSTOMER,
                    customerRepository.countByTenantId(tenantId)));
        }
        AuditService.Diff diff = new AuditService.Diff()
                .compare("Name", entity.getName(), form.getName())
                .compare("Mobile", entity.getMobile(), form.getMobile())
                .compare("PAN", entity.getPan(), form.getPan())
                .compare("Employment", entity.getEmploymentType(), form.getEmploymentType())
                .compare("Monthly income", entity.getMonthlyIncome(), form.getMonthlyIncome())
                .compare("Existing EMI", entity.getExistingEmi(), form.getExistingEmi())
                .compare("CIBIL", entity.getCibilScore(), form.getCibilScore());

        if (form.getCibilScore() != null && !form.getCibilScore().equals(entity.getCibilScore())
                && form.getCibilCheckedOn() == null) {
            entity.setCibilCheckedOn(java.time.LocalDate.now());
        } else {
            entity.setCibilCheckedOn(form.getCibilCheckedOn());
        }
        entity.setName(form.getName() == null ? null : form.getName().trim());
        entity.setMobile(form.getMobile() == null ? null : form.getMobile().trim());
        entity.setAlternateMobile(form.getAlternateMobile());
        entity.setEmail(form.getEmail());
        entity.setDateOfBirth(form.getDateOfBirth());
        entity.setGender(form.getGender());
        entity.setPan(form.getPan() == null ? null : form.getPan().trim().toUpperCase());
        String aadhaar = form.getAadhaarLast4() == null ? null : form.getAadhaarLast4().replaceAll("[^0-9]", "");
        entity.setAadhaarLast4(aadhaar == null || aadhaar.isEmpty() ? null
                : aadhaar.substring(Math.max(0, aadhaar.length() - 4)));
        entity.setAddress(form.getAddress());
        entity.setCity(form.getCity());
        entity.setState(form.getState());
        entity.setPincode(form.getPincode());
        entity.setEmploymentType(form.getEmploymentType());
        entity.setEmployerName(form.getEmployerName());
        entity.setWorkExperienceYears(form.getWorkExperienceYears());
        entity.setMonthlyIncome(form.getMonthlyIncome());
        entity.setExistingEmi(form.getExistingEmi());
        entity.setCibilScore(form.getCibilScore());
        entity.setNotes(form.getNotes());

        User owner = form.getOwnerUserId() == null ? null
                : userRepository.findByIdAndTenantIdAndDeletedFalse(form.getOwnerUserId(), tenantId).orElse(null);
        if (owner != null) {
            entity.setOwnerUserId(owner.getId());
            entity.setOwnerName(owner.getFullName());
        } else if (isNew) {
            entity.setOwnerUserId(SecurityUtils.currentUserId());
            entity.setOwnerName(SecurityUtils.currentUser() == null ? null : SecurityUtils.currentUser().getFullName());
        }
        ReferralPartner partner = form.getReferralPartnerId() == null ? null
                : partnerRepository.findByIdAndTenantIdAndDeletedFalse(form.getReferralPartnerId(), tenantId).orElse(null);
        entity.setReferralPartnerId(partner == null ? null : partner.getId());
        entity.setReferralPartnerName(partner == null ? null : partner.getName());
        entity.setModifiedBy(SecurityUtils.currentUserLogin());

        Customer saved = customerRepository.save(entity);
        if (isNew) {
            auditService.log("CREATE", "Customer", saved.getId(), saved.getName(), "Customer added");
        } else {
            auditService.logDiff("Customer", saved.getId(), saved.getName(), diff);
        }
        return saved;
    }

    @Transactional
    public void archive(Long tenantId, Long id) {
        Customer c = get(tenantId, id);
        if (c == null) return;
        c.setDeleted(true);
        c.setModifiedBy(SecurityUtils.currentUserLogin());
        customerRepository.save(c);
        auditService.log("ARCHIVE", "Customer", id, c.getName(), "Customer archived");
    }
}
