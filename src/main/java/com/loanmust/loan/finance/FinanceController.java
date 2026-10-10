package com.loanmust.loan.finance;

import com.loanmust.loan.application.LoanApplication;
import com.loanmust.loan.application.LoanApplicationService;
import com.loanmust.loan.common.IdCodec;
import com.loanmust.loan.common.PaymentMode;
import com.loanmust.loan.customer.CustomerService;
import com.loanmust.loan.security.SecurityUtils;
import com.loanmust.loan.tenant.AgencyRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** The money picture and customer fee receipts. */
@Controller
public class FinanceController {

    private final FinanceService financeService;
    private final CustomerService customerService;
    private final LoanApplicationService applicationService;
    private final AgencyRepository agencyRepository;

    public FinanceController(FinanceService financeService, CustomerService customerService,
                             LoanApplicationService applicationService, AgencyRepository agencyRepository) {
        this.financeService = financeService;
        this.customerService = customerService;
        this.applicationService = applicationService;
        this.agencyRepository = agencyRepository;
    }

    @GetMapping("/finance")
    public String index(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                        Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to == null) to = LocalDate.now();
        List<FinanceService.MonthPoint> trend = financeService.trend(tenantId, 6);
        List<String> labels = new ArrayList<>();
        List<BigDecimal> earned = new ArrayList<>(), received = new ArrayList<>();
        for (FinanceService.MonthPoint p : trend) {
            labels.add(p.label());
            earned.add(p.earned());
            received.add(p.received());
        }
        model.addAttribute("activeMenu", "finance");
        model.addAttribute("summary", financeService.summary(tenantId, from, to));
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("trendLabels", labels);
        model.addAttribute("trendEarned", earned);
        model.addAttribute("trendReceived", received);
        model.addAttribute("recentReceipts", financeService.receipts(tenantId, null, null, null, 0, 6).getContent());
        return "finance/index";
    }

    @GetMapping("/finance/receipts")
    public String receipts(@RequestParam(required = false) String q,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                           @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("activeMenu", "finance");
        model.addAttribute("receiptPage", financeService.receipts(SecurityUtils.currentTenantId(), q, from, to, page, 15));
        model.addAttribute("q", q);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "finance/receipts";
    }

    @GetMapping("/finance/receipts/new")
    public String newReceipt(@RequestParam(required = false) Long customerId,
                             @RequestParam(required = false) Long applicationId,
                             @RequestParam(required = false) FeeType feeType,
                             @RequestParam(required = false) BigDecimal amount, Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        FeeReceipt r = new FeeReceipt();
        r.setReceivedOn(LocalDate.now());
        r.setFeeType(feeType == null ? FeeType.LOGIN_FEE : feeType);
        r.setAmount(amount);
        r.setPaymentMode(PaymentMode.UPI);
        LoanApplication app = applicationService.get(tenantId, applicationId);
        if (app != null) {
            r.setApplicationId(app.getId());
            r.setApplicationCode(app.getCode());
            customerId = app.getCustomerId();
        }
        r.setCustomerId(customerId);
        model.addAttribute("activeMenu", "finance");
        model.addAttribute("receipt", r);
        model.addAttribute("customers", customerService.all(tenantId));
        model.addAttribute("feeTypes", FeeType.values());
        model.addAttribute("paymentModes", PaymentMode.values());
        return "finance/receipt-form";
    }

    @PostMapping("/finance/receipts/save")
    public String save(@ModelAttribute FeeReceipt form, RedirectAttributes ra) {
        FeeReceipt saved = financeService.saveReceipt(SecurityUtils.currentTenantId(), form);
        if (saved == null) {
            ra.addFlashAttribute("error", "Pick the customer and enter an amount.");
            return "redirect:/finance/receipts/new";
        }
        ra.addFlashAttribute("toast", "Receipt " + saved.getReceiptNo() + " recorded.");
        return "redirect:/finance/receipts/" + IdCodec.tok(saved.getId());
    }

    @GetMapping("/finance/receipts/{id}")
    public String receipt(@PathVariable Long id, Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        FeeReceipt r = financeService.getReceipt(tenantId, id);
        if (r == null) return "redirect:/finance/receipts";
        model.addAttribute("activeMenu", "finance");
        model.addAttribute("receipt", r);
        model.addAttribute("agency", agencyRepository.findById(tenantId).orElse(null));
        return "finance/receipt-view";
    }

    @PostMapping("/finance/receipts/{id}/archive")
    public String cancel(@PathVariable Long id, RedirectAttributes ra) {
        financeService.cancelReceipt(SecurityUtils.currentTenantId(), id);
        ra.addFlashAttribute("toast", "Receipt cancelled.");
        return "redirect:/finance/receipts";
    }
}
