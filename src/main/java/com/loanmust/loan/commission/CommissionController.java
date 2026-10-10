package com.loanmust.loan.commission;

import com.loanmust.loan.common.PaymentStatus;
import com.loanmust.loan.lender.LenderService;
import com.loanmust.loan.security.SecurityUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Bank payouts: what each lender owes the agency, invoicing and receipt reconciliation. */
@Controller
public class CommissionController {

    private final CommissionService commissionService;
    private final BankPayoutRepository repository;
    private final LenderService lenderService;

    public CommissionController(CommissionService commissionService, BankPayoutRepository repository, LenderService lenderService) {
        this.commissionService = commissionService;
        this.repository = repository;
        this.lenderService = lenderService;
    }

    @GetMapping("/commission")
    public String list(@RequestParam(required = false) String q, @RequestParam(required = false) PaymentStatus status,
                       @RequestParam(required = false) Long lenderId, @RequestParam(defaultValue = "0") int page, Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        model.addAttribute("activeMenu", "commission");
        model.addAttribute("payoutPage", commissionService.search(tenantId, q, status, lenderId, page, 15));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("lenderId", lenderId);
        model.addAttribute("statuses", PaymentStatus.values());
        model.addAttribute("lenders", lenderService.lenders(tenantId));
        model.addAttribute("outstanding", commissionService.outstanding(tenantId));
        model.addAttribute("overdue", repository.sumOutstanding(tenantId, List.of(PaymentStatus.OVERDUE)));
        model.addAttribute("received", repository.sumReceived(tenantId));
        model.addAttribute("today", LocalDate.now());
        return "commission/list";
    }

    @PostMapping("/commission/{id}/invoice")
    public String invoice(@PathVariable Long id, @RequestParam String invoiceNumber,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate invoiceDate,
                          RedirectAttributes ra) {
        commissionService.recordInvoice(SecurityUtils.currentTenantId(), id, invoiceNumber, invoiceDate);
        ra.addFlashAttribute("toast", "Invoice " + invoiceNumber + " recorded.");
        return "redirect:/commission";
    }

    @PostMapping("/commission/{id}/receipt")
    public String receipt(@PathVariable Long id, @RequestParam BigDecimal amount,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receivedOn,
                          @RequestParam(required = false) String reference, RedirectAttributes ra) {
        commissionService.recordReceipt(SecurityUtils.currentTenantId(), id, amount, receivedOn, reference);
        BankPayout p = commissionService.get(SecurityUtils.currentTenantId(), id);
        if (p != null && p.isMismatch() && p.getStatus() == PaymentStatus.PAID) {
            ra.addFlashAttribute("error", "Received amount differs from expected by more than ₹1 - check with the lender.");
        } else {
            ra.addFlashAttribute("toast", "Receipt recorded.");
        }
        return "redirect:/commission";
    }

    @PostMapping("/commission/{id}/archive")
    public String writeOff(@PathVariable Long id, @RequestParam(required = false) String remarks, RedirectAttributes ra) {
        commissionService.writeOff(SecurityUtils.currentTenantId(), id, remarks);
        ra.addFlashAttribute("toast", "Payout written off.");
        return "redirect:/commission";
    }
}
