package com.loanmust.loan.referral;

import com.loanmust.loan.common.PaymentMode;
import com.loanmust.loan.security.SecurityUtils;
import com.loanmust.loan.tenant.Agency;
import com.loanmust.loan.tenant.AgencyRepository;
import com.loanmust.loan.user.Role;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

/**
 * The referral payout desk: approve (Owner / Manager), pay and record the UTR (Finance),
 * then send the partner a thank-you on WhatsApp.
 */
@Controller
public class ReferralPayoutController {

    private final ReferralService referralService;
    private final ReferralPayoutRepository payoutRepository;
    private final AgencyRepository agencyRepository;

    public ReferralPayoutController(ReferralService referralService, ReferralPayoutRepository payoutRepository,
                                    AgencyRepository agencyRepository) {
        this.referralService = referralService;
        this.payoutRepository = payoutRepository;
        this.agencyRepository = agencyRepository;
    }

    @GetMapping("/referral-payouts")
    public String list(@RequestParam(required = false) String q, @RequestParam(required = false) PayoutStatus status,
                       @RequestParam(required = false) Long partnerId,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        model.addAttribute("activeMenu", "referral_payouts");
        model.addAttribute("payoutPage", referralService.searchPayouts(tenantId, q, status, partnerId, page, 15));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("statuses", PayoutStatus.values());
        model.addAttribute("paymentModes", PaymentMode.values());
        model.addAttribute("pendingTotal", payoutRepository.sumNetByStatuses(tenantId, List.of(PayoutStatus.PENDING)));
        model.addAttribute("approvedTotal", payoutRepository.sumNetByStatuses(tenantId, List.of(PayoutStatus.APPROVED)));
        model.addAttribute("paidTotal", payoutRepository.sumNetByStatuses(tenantId, List.of(PayoutStatus.PAID)));
        model.addAttribute("canApprove", canApprove());
        model.addAttribute("today", LocalDate.now());
        return "referral/payouts";
    }

    @GetMapping("/referral-payouts/{id}")
    public String view(@PathVariable Long id, Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        ReferralPayout p = referralService.getPayout(tenantId, id);
        if (p == null) return "redirect:/referral-payouts";
        Agency agency = agencyRepository.findById(tenantId).orElse(null);
        model.addAttribute("activeMenu", "referral_payouts");
        model.addAttribute("payout", p);
        model.addAttribute("partner", referralService.get(tenantId, p.getPartnerId()));
        model.addAttribute("paymentModes", PaymentMode.values());
        model.addAttribute("canApprove", canApprove());
        model.addAttribute("today", LocalDate.now());
        if (p.getStatus() == PayoutStatus.PAID) {
            model.addAttribute("thankYouLink", referralService.paidMessageLink(p, agency == null ? "" : agency.getDisplayName()));
        }
        return "referral/payout-view";
    }

    @PostMapping("/referral-payouts/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes ra) {
        if (!canApprove()) {
            ra.addFlashAttribute("error", "Only the owner or a manager can approve referral payouts.");
        } else if (referralService.approve(SecurityUtils.currentTenantId(), id)) {
            ra.addFlashAttribute("toast", "Payout approved - finance can pay it now.");
        } else {
            ra.addFlashAttribute("error", "Only a pending or on-hold payout can be approved.");
        }
        return "redirect:/referral-payouts/" + com.loanmust.loan.common.IdCodec.tok(id);
    }

    @PostMapping("/referral-payouts/{id}/pay")
    public String pay(@PathVariable Long id,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paidOn,
                      @RequestParam(required = false) PaymentMode paymentMode,
                      @RequestParam(required = false) String paymentReference,
                      @RequestParam(required = false) String remarks, RedirectAttributes ra) {
        if (referralService.markPaid(SecurityUtils.currentTenantId(), id, paidOn, paymentMode, paymentReference, remarks)) {
            ra.addFlashAttribute("toast", "Marked paid. Send the partner a thank-you on WhatsApp.");
        } else {
            ra.addFlashAttribute("error", "A payout must be approved before it is paid.");
        }
        return "redirect:/referral-payouts/" + com.loanmust.loan.common.IdCodec.tok(id);
    }

    @PostMapping("/referral-payouts/{id}/status")
    public String status(@PathVariable Long id, @RequestParam PayoutStatus status,
                         @RequestParam(required = false) String remarks, RedirectAttributes ra) {
        if (!canApprove()) {
            ra.addFlashAttribute("error", "Only the owner or a manager can hold or cancel payouts.");
        } else if (referralService.setStatus(SecurityUtils.currentTenantId(), id, status, remarks)) {
            ra.addFlashAttribute("toast", "Payout " + status.getLabel().toLowerCase() + ".");
        } else {
            ra.addFlashAttribute("error", "A paid payout cannot be changed.");
        }
        return "redirect:/referral-payouts/" + com.loanmust.loan.common.IdCodec.tok(id);
    }

    private boolean canApprove() {
        Role role = SecurityUtils.currentRole();
        return role == Role.OWNER || role == Role.MANAGER || role == Role.ADMIN;
    }
}
