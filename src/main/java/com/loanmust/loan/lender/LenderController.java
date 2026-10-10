package com.loanmust.loan.lender;

import com.loanmust.loan.audit.AuditService;
import com.loanmust.loan.common.IdCodec;
import com.loanmust.loan.reports.ExcelExporter;
import com.loanmust.loan.reports.ReportTable;
import com.loanmust.loan.security.SecurityUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Banks & Rates: the lender panel, each lender's schemes and the consolidated rate card. */
@Controller
public class LenderController {

    private final LenderService lenderService;
    private final AuditService auditService;
    private final ExcelExporter excelExporter;

    public LenderController(LenderService lenderService, AuditService auditService, ExcelExporter excelExporter) {
        this.lenderService = lenderService;
        this.auditService = auditService;
        this.excelExporter = excelExporter;
    }

    @GetMapping("/lenders")
    public String list(@RequestParam(required = false) LoanType loanType, Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        List<Lender> lenders = lenderService.lenders(tenantId);
        Map<Long, Long> productCounts = new LinkedHashMap<>();
        for (Lender l : lenders) productCounts.put(l.getId(), lenderService.productCount(tenantId, l.getId()));
        model.addAttribute("activeMenu", "lenders");
        model.addAttribute("lenders", lenders);
        model.addAttribute("productCounts", productCounts);
        model.addAttribute("rateCard", lenderService.rateCard(tenantId, loanType));
        model.addAttribute("loanTypes", LoanType.values());
        model.addAttribute("loanType", loanType);
        return "lender/list";
    }

    @GetMapping("/lenders/rates/export")
    public ResponseEntity<ByteArrayResource> exportRates(@RequestParam(required = false) LoanType loanType) {
        Long tenantId = SecurityUtils.currentTenantId();
        List<String> headers = List.of("Lender", "Scheme", "Loan type", "Rate from %", "Rate to %", "Rate type",
                "Women concession", "Processing fee %", "Max tenure (yrs)", "Max LTV %", "Max FOIR %",
                "Min CIBIL", "Payout %", "Rate confirmed on", "Active");
        List<List<Object>> rows = new ArrayList<>();
        for (LoanProduct p : lenderService.rateCard(tenantId, loanType)) {
            rows.add(java.util.Arrays.asList(p.getLenderName(), p.getName(), p.getLoanTypeLabel(), p.getRateMin(),
                    p.getRateMax(), p.getRateType() == null ? "" : p.getRateType().getLabel(), p.getWomenConcession(),
                    p.getProcessingFeePercent(), p.getMaxTenureYears(), p.getMaxLtvPercent(), p.getMaxFoirPercent(),
                    p.getMinCibil(), lenderService.payoutPercentFor(tenantId, p), p.getRateUpdatedOn(),
                    p.isActive() ? "Yes" : "No"));
        }
        auditService.log("EXPORT", "LoanProduct", 0L, "Rate card", "Rate card exported");
        return excelExporter.download(new ReportTable("Rate card", headers, rows), "loanmust-rate-card.xlsx");
    }

    @GetMapping("/lenders/new")
    public String createForm(Model model) {
        Lender lender = new Lender();
        lender.setActive(true);
        lender.setLenderType(LenderType.PRIVATE_BANK);
        model.addAttribute("activeMenu", "lenders");
        model.addAttribute("lender", lender);
        model.addAttribute("lenderTypes", LenderType.values());
        return "lender/form";
    }

    @GetMapping("/lenders/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Lender lender = lenderService.get(SecurityUtils.currentTenantId(), id);
        if (lender == null) return "redirect:/lenders";
        model.addAttribute("activeMenu", "lenders");
        model.addAttribute("lender", lender);
        model.addAttribute("lenderTypes", LenderType.values());
        return "lender/form";
    }

    @GetMapping("/lenders/{id}")
    public String view(@PathVariable Long id, Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        Lender lender = lenderService.get(tenantId, id);
        if (lender == null) return "redirect:/lenders";
        model.addAttribute("activeMenu", "lenders");
        model.addAttribute("lender", lender);
        model.addAttribute("products", lenderService.products(tenantId, id));
        model.addAttribute("auditTrail", auditService.historyFor(tenantId, "Lender", id));
        return "lender/view";
    }

    @PostMapping("/lenders/save")
    public String save(@ModelAttribute Lender lender, RedirectAttributes ra) {
        if (!isMoney()) {
            // only money roles may set what a lender pays the agency
            Lender existing = lenderService.get(SecurityUtils.currentTenantId(), lender.getId());
            lender.setPayoutPercent(existing == null ? null : existing.getPayoutPercent());
            lender.setPayoutTerms(existing == null ? null : existing.getPayoutTerms());
        }
        Lender saved = lenderService.save(SecurityUtils.currentTenantId(), lender);
        if (saved == null) return "redirect:/lenders";
        ra.addFlashAttribute("toast", "Lender saved.");
        return "redirect:/lenders/" + IdCodec.tok(saved.getId());
    }

    @PostMapping("/lenders/{id}/archive")
    public String archive(@PathVariable Long id, RedirectAttributes ra) {
        lenderService.archive(SecurityUtils.currentTenantId(), id);
        ra.addFlashAttribute("toast", "Lender archived.");
        return "redirect:/lenders";
    }

    // ------------------------------------------------------------------ schemes

    @GetMapping("/lenders/{id}/products/new")
    public String newProduct(@PathVariable Long id, Model model) {
        Lender lender = lenderService.get(SecurityUtils.currentTenantId(), id);
        if (lender == null) return "redirect:/lenders";
        LoanProduct p = new LoanProduct();
        p.setLenderId(lender.getId());
        p.setLenderName(lender.getDisplayName());
        p.setActive(true);
        p.setMaxLtvPercent(new BigDecimal("80"));
        p.setMaxFoirPercent(new BigDecimal("55"));
        p.setProcessingFeePercent(new BigDecimal("0.50"));
        return productForm(p, lender, model);
    }

    @GetMapping("/lenders/products/{productId}/edit")
    public String editProduct(@PathVariable Long productId, Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        LoanProduct p = lenderService.getProduct(tenantId, productId);
        if (p == null) return "redirect:/lenders";
        return productForm(p, lenderService.get(tenantId, p.getLenderId()), model);
    }

    private String productForm(LoanProduct product, Lender lender, Model model) {
        model.addAttribute("activeMenu", "lenders");
        model.addAttribute("product", product);
        model.addAttribute("lender", lender);
        model.addAttribute("loanTypes", LoanType.values());
        model.addAttribute("rateTypes", RateType.values());
        return "lender/product-form";
    }

    @PostMapping("/lenders/products/save")
    public String saveProduct(@ModelAttribute LoanProduct product, RedirectAttributes ra) {
        if (!isMoney()) {
            LoanProduct existing = lenderService.getProduct(SecurityUtils.currentTenantId(), product.getId());
            product.setPayoutPercent(existing == null ? null : existing.getPayoutPercent());
        }
        LoanProduct saved = lenderService.saveProduct(SecurityUtils.currentTenantId(), product);
        if (saved == null) {
            ra.addFlashAttribute("error", "Choose the lender this scheme belongs to.");
            return "redirect:/lenders";
        }
        ra.addFlashAttribute("toast", "Scheme saved. Rate card updated.");
        return "redirect:/lenders/" + IdCodec.tok(saved.getLenderId());
    }

    @PostMapping("/lenders/products/{productId}/confirm-rate")
    public String confirmRate(@PathVariable Long productId, RedirectAttributes ra) {
        Long tenantId = SecurityUtils.currentTenantId();
        lenderService.confirmRate(tenantId, productId);
        LoanProduct p = lenderService.getProduct(tenantId, productId);
        ra.addFlashAttribute("toast", "Rate re-confirmed for today.");
        return p == null ? "redirect:/lenders" : "redirect:/lenders/" + IdCodec.tok(p.getLenderId());
    }

    @PostMapping("/lenders/products/{productId}/archive")
    public String archiveProduct(@PathVariable Long productId, RedirectAttributes ra) {
        Long tenantId = SecurityUtils.currentTenantId();
        LoanProduct p = lenderService.getProduct(tenantId, productId);
        lenderService.archiveProduct(tenantId, productId);
        ra.addFlashAttribute("toast", "Scheme archived.");
        return p == null ? "redirect:/lenders" : "redirect:/lenders/" + IdCodec.tok(p.getLenderId());
    }

    private boolean isMoney() {
        com.loanmust.loan.user.Role role = SecurityUtils.currentRole();
        return role != null && role.isMoney();
    }
}
