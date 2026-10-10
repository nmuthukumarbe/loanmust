package com.loanmust.loan.customer;

import com.loanmust.loan.advisor.LoanAdvisorService;
import com.loanmust.loan.application.LoanApplicationService;
import com.loanmust.loan.audit.AuditService;
import com.loanmust.loan.common.IdCodec;
import com.loanmust.loan.common.WhatsApp;
import com.loanmust.loan.finance.FinanceService;
import com.loanmust.loan.referral.ReferralService;
import com.loanmust.loan.security.SecurityUtils;
import com.loanmust.loan.user.Role;
import com.loanmust.loan.user.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CustomerController {

    private static final int PAGE_SIZE = 15;

    private final CustomerService customerService;
    private final LoanApplicationService applicationService;
    private final LoanAdvisorService advisorService;
    private final FinanceService financeService;
    private final ReferralService referralService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public CustomerController(CustomerService customerService, LoanApplicationService applicationService,
                              LoanAdvisorService advisorService, FinanceService financeService,
                              ReferralService referralService, UserRepository userRepository, AuditService auditService) {
        this.customerService = customerService;
        this.applicationService = applicationService;
        this.advisorService = advisorService;
        this.financeService = financeService;
        this.referralService = referralService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @GetMapping("/customers")
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) EmploymentType employmentType,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        model.addAttribute("activeMenu", "customers");
        model.addAttribute("customerPage", customerService.search(tenantId, q, employmentType,
                SecurityUtils.ownerFilterUserId(), page, PAGE_SIZE));
        model.addAttribute("q", q);
        model.addAttribute("employmentType", employmentType);
        model.addAttribute("employmentTypes", EmploymentType.values());
        model.addAttribute("lockedToOwn", SecurityUtils.ownerFilterUserId() != null);
        return "customer/list";
    }

    @GetMapping("/customers/new")
    public String createForm(Model model) {
        Customer c = new Customer();
        c.setEmploymentType(EmploymentType.SALARIED);
        return form(c, model);
    }

    @GetMapping("/customers/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Customer c = visible(id);
        if (c == null) return "redirect:/customers";
        return form(c, model);
    }

    private String form(Customer c, Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        model.addAttribute("activeMenu", "customers");
        model.addAttribute("customer", c);
        model.addAttribute("employmentTypes", EmploymentType.values());
        model.addAttribute("genders", Gender.values());
        model.addAttribute("partners", referralService.activePartners(tenantId));
        model.addAttribute("users", userRepository.findByTenantIdAndEnabledTrueAndDeletedFalseOrderByFullNameAsc(tenantId));
        return "customer/form";
    }

    @GetMapping("/customers/{id}")
    public String view(@PathVariable Long id, Model model) {
        Long tenantId = SecurityUtils.currentTenantId();
        Customer c = visible(id);
        if (c == null) return "redirect:/customers";
        Role role = SecurityUtils.currentRole();
        model.addAttribute("activeMenu", "customers");
        model.addAttribute("customer", c);
        model.addAttribute("applications", applicationService.forCustomer(tenantId, id));
        model.addAttribute("proposals", advisorService.forCustomer(tenantId, id));
        model.addAttribute("receipts", role != null && role.isMoney() ? financeService.receiptsForCustomer(tenantId, id) : java.util.List.of());
        model.addAttribute("auditTrail", auditService.historyFor(tenantId, "Customer", id));
        model.addAttribute("whatsappLink", WhatsApp.link(c.getMobile(), "Hello " + c.getName() + ", "));
        return "customer/view";
    }

    @PostMapping("/customers/save")
    public String save(@ModelAttribute Customer customer, RedirectAttributes ra) {
        Long tenantId = SecurityUtils.currentTenantId();
        if (customer.getId() != null && visible(customer.getId()) == null) return "redirect:/customers";
        Customer dup = customerService.duplicateOf(tenantId, customer);
        if (dup != null) {
            ra.addFlashAttribute("error", "Mobile " + customer.getMobile() + " already belongs to " + dup.getName()
                    + " (" + dup.getCode() + "). Open that customer instead.");
            return customer.getId() == null ? "redirect:/customers/new" : "redirect:/customers/" + IdCodec.tok(customer.getId()) + "/edit";
        }
        Customer saved = customerService.save(tenantId, customer);
        if (saved == null) return "redirect:/customers";
        ra.addFlashAttribute("toast", "Customer saved.");
        return "redirect:/customers/" + IdCodec.tok(saved.getId());
    }

    @PostMapping("/customers/{id}/archive")
    public String archive(@PathVariable Long id, RedirectAttributes ra) {
        customerService.archive(SecurityUtils.currentTenantId(), id);
        ra.addFlashAttribute("toast", "Customer archived.");
        return "redirect:/customers";
    }

    private Customer visible(Long id) {
        Customer c = customerService.get(SecurityUtils.currentTenantId(), id);
        Long owner = SecurityUtils.ownerFilterUserId();
        if (c != null && owner != null && !owner.equals(c.getOwnerUserId())) return null;
        return c;
    }
}
