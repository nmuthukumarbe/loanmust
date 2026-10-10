package com.loanmust.loan.lender;

import com.loanmust.loan.audit.AuditService;
import com.loanmust.loan.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Lender panel and the rate card of every scheme. */
@Service
public class LenderService {

    private final LenderRepository lenderRepository;
    private final LoanProductRepository productRepository;
    private final AuditService auditService;

    public LenderService(LenderRepository lenderRepository, LoanProductRepository productRepository,
                         AuditService auditService) {
        this.lenderRepository = lenderRepository;
        this.productRepository = productRepository;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------------ reading

    public List<Lender> lenders(Long tenantId) {
        return lenderRepository.findByTenantIdAndDeletedFalseOrderByNameAsc(tenantId);
    }

    public List<Lender> activeLenders(Long tenantId) {
        return lenderRepository.findByTenantIdAndActiveTrueAndDeletedFalseOrderByNameAsc(tenantId);
    }

    public Lender get(Long tenantId, Long id) {
        return id == null ? null : lenderRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId).orElse(null);
    }

    public LoanProduct getProduct(Long tenantId, Long id) {
        return id == null ? null : productRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId).orElse(null);
    }

    public List<LoanProduct> products(Long tenantId, Long lenderId) {
        return productRepository.findByTenantIdAndLenderIdAndDeletedFalseOrderByLoanTypeAscNameAsc(tenantId, lenderId);
    }

    public List<LoanProduct> rateCard(Long tenantId, LoanType loanType) {
        return loanType == null
                ? productRepository.findByTenantIdAndDeletedFalseOrderByLoanTypeAscRateMinAsc(tenantId)
                : productRepository.findByTenantIdAndLoanTypeAndDeletedFalseOrderByRateMinAsc(tenantId, loanType);
    }

    public List<LoanProduct> activeProducts(Long tenantId) {
        return productRepository.findByTenantIdAndActiveTrueAndDeletedFalseOrderByLenderNameAscNameAsc(tenantId);
    }

    public List<LoanProduct> activeProducts(Long tenantId, LoanType loanType) {
        return productRepository.findByTenantIdAndLoanTypeAndActiveTrueAndDeletedFalseOrderByRateMinAsc(tenantId, loanType);
    }

    public long productCount(Long tenantId, Long lenderId) {
        return productRepository.countByTenantIdAndLenderIdAndDeletedFalse(tenantId, lenderId);
    }

    /** Payout percent that applies to a product: its own, else its lender's default. */
    public BigDecimal payoutPercentFor(Long tenantId, LoanProduct product) {
        if (product == null) return null;
        if (product.getPayoutPercent() != null) return product.getPayoutPercent();
        Lender lender = get(tenantId, product.getLenderId());
        return lender == null ? null : lender.getPayoutPercent();
    }

    // ------------------------------------------------------------------ writing

    @Transactional
    public Lender save(Long tenantId, Lender form) {
        boolean isNew = form.getId() == null;
        Lender entity = isNew ? new Lender() : get(tenantId, form.getId());
        if (entity == null) return null;
        if (isNew) {
            entity.setTenantId(tenantId);
            entity.setCreatedBy(SecurityUtils.currentUserLogin());
        }
        AuditService.Diff diff = new AuditService.Diff()
                .compare("Name", entity.getName(), form.getName())
                .compare("Payout %", entity.getPayoutPercent(), form.getPayoutPercent())
                .compare("RM", entity.getRmName(), form.getRmName())
                .compare("Active", entity.isActive(), form.isActive());

        entity.setName(form.getName());
        entity.setShortName(form.getShortName());
        entity.setLenderType(form.getLenderType());
        entity.setRmName(form.getRmName());
        entity.setRmMobile(form.getRmMobile());
        entity.setRmEmail(form.getRmEmail());
        entity.setBranch(form.getBranch());
        entity.setPayoutPercent(form.getPayoutPercent());
        entity.setPayoutTerms(form.getPayoutTerms());
        entity.setBrandColor(form.getBrandColor());
        entity.setActive(form.isActive());
        entity.setNotes(form.getNotes());
        entity.setModifiedBy(SecurityUtils.currentUserLogin());
        Lender saved = lenderRepository.save(entity);

        if (isNew) {
            auditService.log("CREATE", "Lender", saved.getId(), saved.getName(), "Lender added to panel");
        } else {
            auditService.logDiff("Lender", saved.getId(), saved.getName(), diff);
            // keep the denormalised lender name on its schemes in step
            for (LoanProduct p : products(tenantId, saved.getId())) {
                if (!saved.getDisplayName().equals(p.getLenderName())) {
                    p.setLenderName(saved.getDisplayName());
                    productRepository.save(p);
                }
            }
        }
        return saved;
    }

    @Transactional
    public void archive(Long tenantId, Long id) {
        Lender lender = get(tenantId, id);
        if (lender == null) return;
        lender.setDeleted(true);
        lender.setActive(false);
        lender.setModifiedBy(SecurityUtils.currentUserLogin());
        lenderRepository.save(lender);
        for (LoanProduct p : products(tenantId, id)) {
            p.setActive(false);
            productRepository.save(p);
        }
        auditService.log("ARCHIVE", "Lender", id, lender.getName(), "Lender archived; its schemes deactivated");
    }

    @Transactional
    public LoanProduct saveProduct(Long tenantId, LoanProduct form) {
        Lender lender = get(tenantId, form.getLenderId());
        if (lender == null) return null;
        boolean isNew = form.getId() == null;
        LoanProduct entity = isNew ? new LoanProduct() : getProduct(tenantId, form.getId());
        if (entity == null) return null;
        if (isNew) {
            entity.setTenantId(tenantId);
            entity.setCreatedBy(SecurityUtils.currentUserLogin());
        }
        BigDecimal rateMin = form.getRateMin();
        BigDecimal rateMax = form.getRateMax() == null ? rateMin : form.getRateMax();
        if (rateMin != null && rateMax != null && rateMax.compareTo(rateMin) < 0) {
            BigDecimal t = rateMin; rateMin = rateMax; rateMax = t;   // keep min <= max
        }
        boolean rateChanged = isNew
                || !java.util.Objects.equals(entity.getRateMin(), rateMin)
                || !java.util.Objects.equals(entity.getRateMax(), rateMax);

        AuditService.Diff diff = new AuditService.Diff()
                .compare("Rate from", entity.getRateMin(), rateMin)
                .compare("Rate to", entity.getRateMax(), rateMax)
                .compare("Processing fee %", entity.getProcessingFeePercent(), form.getProcessingFeePercent())
                .compare("Max LTV %", entity.getMaxLtvPercent(), form.getMaxLtvPercent())
                .compare("Max FOIR %", entity.getMaxFoirPercent(), form.getMaxFoirPercent())
                .compare("Min CIBIL", entity.getMinCibil(), form.getMinCibil())
                .compare("Payout %", entity.getPayoutPercent(), form.getPayoutPercent())
                .compare("Active", entity.isActive(), form.isActive());

        entity.setLenderId(lender.getId());
        entity.setLenderName(lender.getDisplayName());
        entity.setName(form.getName());
        entity.setLoanType(form.getLoanType() == null ? LoanType.HOME_LOAN : form.getLoanType());
        entity.setRateType(form.getRateType());
        entity.setBenchmark(form.getBenchmark());
        entity.setRateMin(rateMin);
        entity.setRateMax(rateMax);
        entity.setWomenConcession(form.getWomenConcession());
        entity.setProcessingFeePercent(form.getProcessingFeePercent());
        entity.setProcessingFeeMin(form.getProcessingFeeMin());
        entity.setProcessingFeeMax(form.getProcessingFeeMax());
        entity.setMaxTenureYears(form.getMaxTenureYears());
        entity.setMaxLtvPercent(form.getMaxLtvPercent());
        entity.setMaxFoirPercent(form.getMaxFoirPercent());
        entity.setMinCibil(form.getMinCibil());
        entity.setBestRateCibil(form.getBestRateCibil());
        entity.setMinMonthlyIncome(form.getMinMonthlyIncome());
        entity.setMinLoanAmount(form.getMinLoanAmount());
        entity.setMaxLoanAmount(form.getMaxLoanAmount());
        entity.setMaxAgeAtMaturity(form.getMaxAgeAtMaturity());
        entity.setSalariedAllowed(form.isSalariedAllowed());
        entity.setSelfEmployedAllowed(form.isSelfEmployedAllowed());
        entity.setPensionerAllowed(form.isPensionerAllowed());
        entity.setNriAllowed(form.isNriAllowed());
        entity.setPrepaymentCharges(form.getPrepaymentCharges());
        entity.setKeyBenefits(form.getKeyBenefits());
        entity.setPayoutPercent(form.getPayoutPercent());
        entity.setActive(form.isActive());
        entity.setNotes(form.getNotes());
        if (rateChanged || form.getRateUpdatedOn() != null) {
            entity.setRateUpdatedOn(form.getRateUpdatedOn() != null ? form.getRateUpdatedOn() : LocalDate.now());
        }
        entity.setModifiedBy(SecurityUtils.currentUserLogin());
        LoanProduct saved = productRepository.save(entity);

        if (isNew) {
            auditService.log("CREATE", "LoanProduct", saved.getId(), saved.getLenderName() + " · " + saved.getName(),
                    "Scheme added at " + saved.getRateRange());
        } else {
            auditService.logDiff("LoanProduct", saved.getId(), saved.getLenderName() + " · " + saved.getName(), diff);
        }
        return saved;
    }

    /** Re-confirms the current rate card with the lender without changing it. */
    @Transactional
    public void confirmRate(Long tenantId, Long productId) {
        LoanProduct p = getProduct(tenantId, productId);
        if (p == null) return;
        p.setRateUpdatedOn(LocalDate.now());
        p.setModifiedBy(SecurityUtils.currentUserLogin());
        productRepository.save(p);
        auditService.log("UPDATE", "LoanProduct", productId, p.getLenderName() + " · " + p.getName(),
                "Rate re-confirmed at " + p.getRateRange());
    }

    @Transactional
    public void archiveProduct(Long tenantId, Long productId) {
        LoanProduct p = getProduct(tenantId, productId);
        if (p == null) return;
        p.setDeleted(true);
        p.setActive(false);
        p.setModifiedBy(SecurityUtils.currentUserLogin());
        productRepository.save(p);
        auditService.log("ARCHIVE", "LoanProduct", productId, p.getLenderName() + " · " + p.getName(), "Scheme archived");
    }
}
