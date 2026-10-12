package com.loanmust.loan;

import com.loanmust.loan.advisor.Proposal;
import com.loanmust.loan.advisor.ProposalRepository;
import com.loanmust.loan.application.ApplicationDocument;
import com.loanmust.loan.application.ApplicationDocumentRepository;
import com.loanmust.loan.application.ApplicationStage;
import com.loanmust.loan.application.LoanApplication;
import com.loanmust.loan.application.LoanApplicationRepository;
import com.loanmust.loan.builder.Builder;
import com.loanmust.loan.builder.BuilderRepository;
import com.loanmust.loan.builder.ProjectRepository;
import com.loanmust.loan.commission.BankPayoutRepository;
import com.loanmust.loan.common.IdCodec;
import com.loanmust.loan.config.DataSeeder;
import com.loanmust.loan.customer.Customer;
import com.loanmust.loan.customer.CustomerRepository;
import com.loanmust.loan.finance.FeeReceiptRepository;
import com.loanmust.loan.hr.EmployeeRepository;
import com.loanmust.loan.lead.Lead;
import com.loanmust.loan.lead.LeadRepository;
import com.loanmust.loan.lender.Lender;
import com.loanmust.loan.lender.LenderRepository;
import com.loanmust.loan.lender.LoanProduct;
import com.loanmust.loan.lender.LoanProductRepository;
import com.loanmust.loan.modd.ModdRecord;
import com.loanmust.loan.modd.ModdRepository;
import com.loanmust.loan.referral.ReferralPartner;
import com.loanmust.loan.referral.ReferralPartnerRepository;
import com.loanmust.loan.referral.ReferralPayout;
import com.loanmust.loan.referral.ReferralPayoutRepository;
import com.loanmust.loan.reports.ReportService;
import com.loanmust.loan.security.CustomUserDetails;
import com.loanmust.loan.user.User;
import com.loanmust.loan.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the whole application on H2 with the demo book seeded, renders every screen as the
 * owner (so every template is evaluated against real data), checks role restrictions, and
 * walks a loan from enquiry to disbursement through the real HTTP endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationSmokeTest {

    @Autowired MockMvc mvc;
    @Autowired IdCodec ids;
    @Autowired UserRepository users;
    @Autowired LeadRepository leads;
    @Autowired CustomerRepository customers;
    @Autowired LoanApplicationRepository applications;
    @Autowired ApplicationDocumentRepository documents;
    @Autowired LenderRepository lenders;
    @Autowired LoanProductRepository products;
    @Autowired BuilderRepository builders;
    @Autowired ProjectRepository projects;
    @Autowired ReferralPartnerRepository partners;
    @Autowired ReferralPayoutRepository referralPayouts;
    @Autowired BankPayoutRepository bankPayouts;
    @Autowired ModdRepository modds;
    @Autowired ProposalRepository proposals;
    @Autowired FeeReceiptRepository receipts;
    @Autowired EmployeeRepository employees;

    private Long tenantId;

    @BeforeEach
    void tenant() {
        tenantId = users.findByEmailAndDeletedFalse(DataSeeder.OWNER_EMAIL).orElseThrow().getTenantId();
    }

    private RequestPostProcessor as(String email) {
        User u = users.findByEmailAndDeletedFalse(email).orElseThrow();
        return user(new CustomUserDetails(u));
    }

    private RequestPostProcessor owner() {
        return as(DataSeeder.OWNER_EMAIL);
    }

    private String enc(Long id) {
        return ids.encode(id);
    }

    private void ok(String url, RequestPostProcessor who) throws Exception {
        MvcResult r = mvc.perform(get(url).with(who)).andReturn();
        assertThat(r.getResponse().getStatus()).as("GET " + url).isEqualTo(200);
        assertThat(r.getResponse().getContentAsString()).as("GET " + url).doesNotContain("Something went wrong");
    }

    @Test
    void demoBookWasSeeded() {
        assertThat(leads.findByTenantIdAndDeletedFalseOrderByIdDesc(tenantId)).isNotEmpty();
        assertThat(applications.findByTenantIdAndDeletedFalseOrderByIdDesc(tenantId)).hasSizeGreaterThanOrEqualTo(8);
        assertThat(bankPayouts.findByTenantIdAndDeletedFalseOrderByIdDesc(tenantId)).isNotEmpty();
        assertThat(referralPayouts.findByTenantIdAndDeletedFalseOrderByIdDesc(tenantId)).isNotEmpty();
        assertThat(modds.findByTenantIdAndDeletedFalseOrderByIdDesc(tenantId)).isNotEmpty();
        assertThat(employees.findByTenantIdAndDeletedFalseOrderByNameAsc(tenantId)).isNotEmpty();
    }

    @Test
    void everyListScreenRendersForTheOwner() throws Exception {
        for (String url : List.of("/dashboard", "/leads", "/leads/new", "/customers", "/customers/new",
                "/advisor", "/advisor/proposals", "/lenders", "/lenders/new", "/applications", "/applications/board",
                "/applications/new", "/modd", "/builders", "/builders/new", "/referrals", "/referrals/new",
                "/referral-payouts", "/commission", "/finance", "/finance/receipts", "/finance/receipts/new",
                "/reports", "/employees", "/employees/new", "/attendance", "/admin/users", "/admin/users/new",
                "/admin/roles", "/audit", "/settings", "/account/password")) {
            ok(url, owner());
        }
        for (ReportService.Report r : ReportService.Report.values()) {
            ok("/reports/" + r.getKey(), owner());
        }
    }

    @Test
    void everyDetailScreenRendersForTheOwner() throws Exception {
        List<String> urls = new ArrayList<>();
        for (Lead l : leads.findByTenantIdAndDeletedFalseOrderByIdDesc(tenantId)) {
            urls.add("/leads/" + enc(l.getId()));
            urls.add("/leads/" + enc(l.getId()) + "/edit");
            urls.add("/advisor?leadId=" + enc(l.getId()));
        }
        for (Customer c : customers.findByTenantIdAndDeletedFalseOrderByNameAsc(tenantId)) {
            urls.add("/customers/" + enc(c.getId()));
            urls.add("/customers/" + enc(c.getId()) + "/edit");
            urls.add("/advisor?customerId=" + enc(c.getId()) + "&loanAmount=5000000&tenureYears=20&loanType=HOME_LOAN&propertyValue=7000000");
        }
        for (LoanApplication a : applications.findByTenantIdAndDeletedFalseOrderByIdDesc(tenantId)) {
            urls.add("/applications/" + enc(a.getId()));
            urls.add("/applications/" + enc(a.getId()) + "/edit");
        }
        for (Lender l : lenders.findByTenantIdAndDeletedFalseOrderByNameAsc(tenantId)) {
            urls.add("/lenders/" + enc(l.getId()));
            urls.add("/lenders/" + enc(l.getId()) + "/edit");
            urls.add("/lenders/" + enc(l.getId()) + "/products/new");
        }
        for (LoanProduct p : products.findByTenantIdAndDeletedFalseOrderByLoanTypeAscRateMinAsc(tenantId)) {
            urls.add("/lenders/products/" + enc(p.getId()) + "/edit");
        }
        for (Builder b : builders.findByTenantIdAndDeletedFalseOrderByNameAsc(tenantId)) {
            urls.add("/builders/" + enc(b.getId()));
            urls.add("/builders/" + enc(b.getId()) + "/edit");
            urls.add("/builders/" + enc(b.getId()) + "/projects/new");
        }
        projects.findByTenantIdAndDeletedFalseOrderByBuilderNameAscNameAsc(tenantId)
                .forEach(p -> urls.add("/builders/projects/" + enc(p.getId()) + "/edit"));
        for (ReferralPartner p : partners.findByTenantIdAndDeletedFalseOrderByNameAsc(tenantId)) {
            urls.add("/referrals/" + enc(p.getId()));
            urls.add("/referrals/" + enc(p.getId()) + "/edit");
        }
        referralPayouts.findByTenantIdAndDeletedFalseOrderByIdDesc(tenantId)
                .forEach(p -> urls.add("/referral-payouts/" + enc(p.getId())));
        modds.findByTenantIdAndDeletedFalseOrderByIdDesc(tenantId).forEach(m -> urls.add("/modd/" + enc(m.getId())));
        proposals.search(tenantId, "", org.springframework.data.domain.PageRequest.of(0, 50))
                .forEach(p -> urls.add("/advisor/proposals/" + enc(p.getId())));
        receipts.findByTenantIdAndReceivedOnBetweenAndDeletedFalseOrderByReceivedOnAsc(tenantId, LocalDate.of(2000, 1, 1), LocalDate.now())
                .forEach(r -> urls.add("/finance/receipts/" + enc(r.getId())));
        employees.findByTenantIdAndDeletedFalseOrderByNameAsc(tenantId)
                .forEach(e -> urls.add("/employees/" + enc(e.getId()) + "/edit"));
        for (String url : urls) ok(url, owner());
    }

    @Test
    void publicPagesRenderWithoutSignIn() throws Exception {
        Proposal p = proposals.search(tenantId, "", org.springframework.data.domain.PageRequest.of(0, 1)).getContent().get(0);
        MvcResult r = mvc.perform(get("/p/" + p.getToken())).andExpect(status().isOk()).andReturn();
        assertThat(r.getResponse().getContentAsString()).contains(p.getCustomerName());

        ReferralPartner partner = partners.findByTenantIdAndActiveTrueAndDeletedFalseOrderByNameAsc(tenantId).get(0);
        mvc.perform(get("/refer/" + partner.getCode())).andExpect(status().isOk());
        long before = leads.countByTenantId(tenantId);
        mvc.perform(post("/refer/" + partner.getCode()).with(csrf())
                        .param("name", "Public Visitor").param("mobile", "9876543210").param("loanType", "HOME_LOAN"))
                .andExpect(status().isOk());
        assertThat(leads.countByTenantId(tenantId)).isEqualTo(before + 1);

        mvc.perform(get("/p/not-a-real-token-at-all")).andExpect(status().isOk());
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/login")).andExpect(status().isOk());

        // Public website and its enquiry form.
        mvc.perform(get("/")).andExpect(status().isOk());
        long beforeWeb = leads.countByTenantId(tenantId);
        mvc.perform(post("/enquiry").with(csrf())
                        .param("name", "Website Visitor").param("mobile", "9123456789").param("loanType", "LAP"))
                .andExpect(status().is3xxRedirection());
        assertThat(leads.countByTenantId(tenantId)).isEqualTo(beforeWeb + 1);
        mvc.perform(get("/").param("thanks", "")).andExpect(status().isOk());
    }

    @Test
    void rolesAreRestricted() throws Exception {
        // Data entry: captures leads, never sees money or admin screens.
        ok("/leads", as("dataentry@shreefinserv.in"));
        ok("/applications", as("dataentry@shreefinserv.in"));
        for (String url : List.of("/finance", "/commission", "/referral-payouts", "/admin/users", "/settings", "/reports")) {
            MvcResult r = mvc.perform(get(url).with(as("dataentry@shreefinserv.in"))).andReturn();
            assertThat(r.getResponse().getStatus()).as(url).isBetween(300, 399);
        }
        // Finance: money screens yes, admin no.
        ok("/finance", as("finance@shreefinserv.in"));
        ok("/referral-payouts", as("finance@shreefinserv.in"));
        // A loan executive only sees their own files.
        User exec = users.findByEmailAndDeletedFalse("executive@shreefinserv.in").orElseThrow();
        LoanApplication someoneElses = applications.findByTenantIdAndDeletedFalseOrderByIdDesc(tenantId).stream()
                .filter(a -> !exec.getId().equals(a.getAssignedUserId())).findFirst().orElseThrow();
        mvc.perform(get("/applications/" + enc(someoneElses.getId())).with(as("executive@shreefinserv.in")))
                .andExpect(redirectedUrlPattern("/applications*"));
        ok("/dashboard", as("executive@shreefinserv.in"));
        ok("/dashboard", as("processing@shreefinserv.in"));
    }

    @Test
    void enquiryToDisbursementRaisesBothPayouts() throws Exception {
        ReferralPartner partner = partners.findByTenantIdAndActiveTrueAndDeletedFalseOrderByNameAsc(tenantId).get(0);
        LoanProduct product = products.findByTenantIdAndLoanTypeAndActiveTrueAndDeletedFalseOrderByRateMinAsc(
                tenantId, com.loanmust.loan.lender.LoanType.HOME_LOAN).get(0);

        // 1. lead through a referral partner
        mvc.perform(post("/leads/save").with(owner()).with(csrf())
                        .param("name", "Workflow Test").param("mobile", "+919000000001")
                        .param("source", "REFERRAL").param("referralPartnerId", String.valueOf(partner.getId()))
                        .param("loanType", "HOME_LOAN").param("loanAmountRequired", "4000000")
                        .param("monthlyIncome", "120000").param("cibilScore", "790").param("employmentType", "SALARIED"))
                .andExpect(status().is3xxRedirection());
        Lead lead = leads.findByTenantIdAndMobileAndDeletedFalse(tenantId, "+919000000001").get(0);
        assertThat(lead.getReferralPartnerId()).isEqualTo(partner.getId());

        // 2. convert to a customer
        mvc.perform(post("/leads/" + enc(lead.getId()) + "/convert").with(owner()).with(csrf()))
                .andExpect(status().is3xxRedirection());
        Customer customer = customers.findFirstByTenantIdAndMobileAndDeletedFalse(tenantId, "+919000000001").orElseThrow();
        assertThat(customer.getReferralPartnerId()).isEqualTo(partner.getId());

        // 3. open the loan file - checklist is seeded
        mvc.perform(post("/applications/save").with(owner()).with(csrf())
                        .param("customerId", String.valueOf(customer.getId())).param("leadId", String.valueOf(lead.getId()))
                        .param("loanType", "HOME_LOAN").param("productId", String.valueOf(product.getId()))
                        .param("requestedAmount", "4000000").param("tenureYears", "20").param("propertyValue", "5500000"))
                .andExpect(status().is3xxRedirection());
        LoanApplication app = applications.findByTenantIdAndCustomerIdAndDeletedFalseOrderByIdDesc(tenantId, customer.getId()).get(0);
        assertThat(app.getStage()).isEqualTo(ApplicationStage.DOCS_PENDING);
        assertThat(app.getReferralPartnerId()).isEqualTo(partner.getId());
        assertThat(app.getPayoutPercent()).isNotNull();
        List<ApplicationDocument> docs = documents.findByTenantIdAndApplicationIdAndDeletedFalseOrderBySortOrderAscIdAsc(tenantId, app.getId());
        assertThat(docs).isNotEmpty();
        assertThat(leads.findById(lead.getId()).orElseThrow().getStatus()).isEqualTo(com.loanmust.loan.lead.LeadStatus.CONVERTED);

        String file = "/applications/" + enc(app.getId());
        // 4. bank login, sanction, MODD
        mvc.perform(post(file + "/login").with(owner()).with(csrf()).param("bankReference", "TEST/123"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post(file + "/sanction").with(owner()).with(csrf())
                        .param("sanctionedAmount", "4000000").param("sanctionedRate", "8.4").param("sanctionedTenureYears", "20"))
                .andExpect(status().is3xxRedirection());
        assertThat(applications.findById(app.getId()).orElseThrow().getSanctionedEmi()).isNotNull();
        mvc.perform(post(file + "/stage").with(owner()).with(csrf()).param("stage", "MODD_AGREEMENT"))
                .andExpect(status().is3xxRedirection());
        ModdRecord modd = modds.findFirstByTenantIdAndApplicationIdAndDeletedFalse(tenantId, app.getId()).orElseThrow();
        assertThat(modd.getStampDuty()).isNotNull();

        // 5. disbursement raises the lender payout and the referral payout
        mvc.perform(post(file + "/disburse").with(owner()).with(csrf())
                        .param("amount", "4000000").param("disbursedOn", LocalDate.now().toString()).param("paidTo", "Seller"))
                .andExpect(status().is3xxRedirection());
        LoanApplication done = applications.findById(app.getId()).orElseThrow();
        assertThat(done.getStage()).isEqualTo(ApplicationStage.DISBURSED);
        assertThat(bankPayouts.findByTenantIdAndApplicationIdAndDeletedFalseOrderByIdDesc(tenantId, app.getId())).hasSize(1);
        List<ReferralPayout> rp = referralPayouts.findByTenantIdAndApplicationIdAndDeletedFalseOrderByIdDesc(tenantId, app.getId());
        assertThat(rp).hasSize(1);

        // 6. a disbursement beyond the sanction is refused
        mvc.perform(post(file + "/disburse").with(owner()).with(csrf()).param("amount", "1"))
                .andExpect(status().is3xxRedirection());
        assertThat(bankPayouts.findByTenantIdAndApplicationIdAndDeletedFalseOrderByIdDesc(tenantId, app.getId())).hasSize(1);

        // 7. approve and pay the referrer
        String payout = "/referral-payouts/" + enc(rp.get(0).getId());
        mvc.perform(post(payout + "/approve").with(owner()).with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(post(payout + "/pay").with(as("finance@shreefinserv.in")).with(csrf())
                        .param("paymentMode", "UPI").param("paymentReference", "UTR-TEST-1"))
                .andExpect(status().is3xxRedirection());
        assertThat(referralPayouts.findById(rp.get(0).getId()).orElseThrow().getStatus())
                .isEqualTo(com.loanmust.loan.referral.PayoutStatus.PAID);
        ok(file, owner());
        ok(payout, owner());
    }

    @Test
    void savingAProposalFreezesTheComparison() throws Exception {
        Customer c = customers.findByTenantIdAndDeletedFalseOrderByNameAsc(tenantId).get(0);
        long before = proposals.countByTenantId(tenantId);
        mvc.perform(post("/advisor/save").with(owner()).with(csrf())
                        .param("customerId", String.valueOf(c.getId())).param("customerName", c.getName())
                        .param("customerMobile", c.getMobile()).param("loanType", "HOME_LOAN")
                        .param("loanAmount", "5000000").param("tenureYears", "20").param("propertyValue", "7000000")
                        .param("monthlyIncome", "150000").param("cibilScore", "800").param("employmentType", "SALARIED"))
                .andExpect(status().is3xxRedirection());
        assertThat(proposals.countByTenantId(tenantId)).isEqualTo(before + 1);
    }

    @Test
    void excelExportsDownload() throws Exception {
        for (String url : List.of("/leads/export", "/applications/export", "/lenders/rates/export",
                "/reports/disbursements/export", "/reports/referral-payouts/export", "/audit/export")) {
            MvcResult r = mvc.perform(get(url).with(owner())).andExpect(status().isOk()).andReturn();
            assertThat(r.getResponse().getContentAsByteArray().length).as(url).isGreaterThan(100);
        }
    }
}
