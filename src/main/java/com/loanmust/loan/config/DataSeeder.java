package com.loanmust.loan.config;

import com.loanmust.loan.access.PermissionService;
import com.loanmust.loan.lender.Lender;
import com.loanmust.loan.lender.LenderRepository;
import com.loanmust.loan.lender.LenderType;
import com.loanmust.loan.lender.LoanProduct;
import com.loanmust.loan.lender.LoanProductRepository;
import com.loanmust.loan.lender.LoanType;
import com.loanmust.loan.lender.RateType;
import com.loanmust.loan.tenant.Agency;
import com.loanmust.loan.tenant.AgencyRepository;
import com.loanmust.loan.user.Role;
import com.loanmust.loan.user.User;
import com.loanmust.loan.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seeds a working agency on first boot so the application is usable immediately.
 * Idempotent - guarded by an existence check on the owner account.
 *
 * Demo sign-ins (change these before any real deployment):
 *   owner@shreefinserv.in       / owner123       - Owner
 *   admin@shreefinserv.in       / admin123       - Admin
 *   manager@shreefinserv.in     / manager123     - Manager
 *   executive@shreefinserv.in   / executive123   - Loan Executive
 *   processing@shreefinserv.in  / processing123  - Processing
 *   dataentry@shreefinserv.in   / dataentry123   - Data Entry
 *   finance@shreefinserv.in     / finance123     - Finance
 *
 * The lender panel is seeded with ILLUSTRATIVE rate cards so Rate Compare works out of the
 * box. They are not live rates: confirm each scheme with the lender and update it under
 * Banks & Rates before advising a customer.
 */
@Component
@Order(10)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    public static final String OWNER_EMAIL = "owner@shreefinserv.in";

    private final AgencyRepository agencyRepository;
    private final UserRepository userRepository;
    private final LenderRepository lenderRepository;
    private final LoanProductRepository productRepository;
    private final PermissionService permissionService;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(AgencyRepository agencyRepository, UserRepository userRepository,
                      LenderRepository lenderRepository, LoanProductRepository productRepository,
                      PermissionService permissionService, PasswordEncoder passwordEncoder) {
        this.agencyRepository = agencyRepository;
        this.userRepository = userRepository;
        this.lenderRepository = lenderRepository;
        this.productRepository = productRepository;
        this.permissionService = permissionService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(OWNER_EMAIL)) {
            return;
        }
        log.info("Seeding the demo agency, staff accounts, lender panel and role permissions...");

        Agency agency = new Agency();
        agency.setName("Shree Finserv Loan Consultants");
        agency.setTradeName("Shree Finserv");
        agency.setOwnerName("Venkatesh Rao");
        agency.setEmail("hello@shreefinserv.in");
        agency.setPhone("+919845011223");
        agency.setDsaCode("DSA/BLR/0427");
        agency.setGstin("29ABCFS1234K1Z5");
        agency.setPan("ABCFS1234K");
        agency.setAddress("2nd Floor, Lakshmi Arcade, 80 Feet Road, Indiranagar");
        agency.setCity("Bengaluru");
        agency.setState("Karnataka");
        agency.setPincode("560038");
        agency.setCreatedBy("system");
        Long tenantId = agencyRepository.save(agency).getId();

        staff(tenantId, OWNER_EMAIL, "owner123", "Venkatesh Rao", Role.OWNER, "Proprietor");
        staff(tenantId, "admin@shreefinserv.in", "admin123", "Lakshmi Narayan", Role.ADMIN, "Office Administrator");
        staff(tenantId, "manager@shreefinserv.in", "manager123", "Pradeep Kumar", Role.MANAGER, "Sales & Operations Manager");
        staff(tenantId, "executive@shreefinserv.in", "executive123", "Ravi Shankar", Role.LOAN_EXECUTIVE, "Loan Executive");
        staff(tenantId, "executive2@shreefinserv.in", "executive123", "Divya Hegde", Role.LOAN_EXECUTIVE, "Loan Executive");
        staff(tenantId, "processing@shreefinserv.in", "processing123", "Sunitha Reddy", Role.PROCESSING, "Processing Officer");
        staff(tenantId, "dataentry@shreefinserv.in", "dataentry123", "Kiran Gowda", Role.DATA_ENTRY, "Data Entry Operator");
        staff(tenantId, "dataentry2@shreefinserv.in", "dataentry123", "Meena Kumari", Role.DATA_ENTRY, "Data Entry Operator");
        staff(tenantId, "finance@shreefinserv.in", "finance123", "Anand Joshi", Role.FINANCE, "Accounts");

        seedLenders(tenantId);
        seedPermissions(tenantId);
        log.info("Seed complete. Sign in as {} / owner123", OWNER_EMAIL);
    }

    private void staff(Long tenantId, String email, String password, String name, Role role, String designation) {
        User u = new User();
        u.setTenantId(tenantId);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(password));
        u.setFullName(name);
        u.setRole(role);
        u.setDesignation(designation);
        u.setBranch("Indiranagar");
        u.setPhone("+9198450" + String.format("%05d", Math.abs(email.hashCode()) % 100000));
        u.setEnabled(true);
        u.setLoginEnabled(true);
        u.setCreatedBy("system");
        userRepository.save(u);
    }

    // ------------------------------------------------------------------ lender panel

    private void seedLenders(Long tenantId) {
        Lender sbi = lender(tenantId, "State Bank of India", "SBI", LenderType.PSU_BANK, "0.30", "#1d4ed8");
        Lender hdfc = lender(tenantId, "HDFC Bank", "HDFC Bank", LenderType.PRIVATE_BANK, "0.45", "#1e3a8a");
        Lender icici = lender(tenantId, "ICICI Bank", "ICICI Bank", LenderType.PRIVATE_BANK, "0.45", "#b45309");
        Lender axis = lender(tenantId, "Axis Bank", "Axis Bank", LenderType.PRIVATE_BANK, "0.50", "#9f1239");
        Lender bob = lender(tenantId, "Bank of Baroda", "Bank of Baroda", LenderType.PSU_BANK, "0.30", "#ea580c");
        Lender lichfl = lender(tenantId, "LIC Housing Finance Ltd", "LIC HFL", LenderType.HFC, "0.50", "#0f766e");
        Lender bajaj = lender(tenantId, "Bajaj Housing Finance Ltd", "Bajaj Housing", LenderType.HFC, "0.60", "#0369a1");
        Lender kotak = lender(tenantId, "Kotak Mahindra Bank", "Kotak", LenderType.PRIVATE_BANK, "0.45", "#dc2626");

        // Home loans
        product(tenantId, sbi, "SBI Regular Home Loan", LoanType.HOME_LOAN, "8.25", "9.15", "0.05", "0.35", "2000", "10000",
                30, "90", "60", 650, 800, 70, true, true, "No prepayment charges on floating rate",
                "Interest on daily reducing balance\nNo hidden charges\nOverdraft (MaxGain) option available");
        product(tenantId, hdfc, "HDFC Bank Home Loan", LoanType.HOME_LOAN, "8.40", "9.60", "0.05", "0.50", "3000", null,
                30, "90", "55", 700, 800, 70, true, true, "Nil for individual floating-rate borrowers",
                "Doorstep service\nQuick sanction for approved projects");
        product(tenantId, icici, "ICICI Bank Home Loan", LoanType.HOME_LOAN, "8.50", "9.80", null, "0.50", "3000", null,
                30, "90", "55", 725, 800, 67, true, true, "Nil on floating rate",
                "Instant digital sanction letter\nTop-up eligibility after 6 EMIs");
        product(tenantId, axis, "Axis Bank Home Loan", LoanType.HOME_LOAN, "8.60", "9.90", null, "1.00", "10000", "10000",
                30, "90", "55", 700, 800, 65, true, true, "Nil on floating rate",
                "12 EMIs waived on select long-tenure loans (conditions apply)");
        product(tenantId, bob, "Baroda Home Loan", LoanType.HOME_LOAN, "8.30", "10.20", "0.15", "0.25", "2500", "25000",
                30, "90", "60", 701, 800, 70, true, true, "Nil on floating rate",
                "Concession for women borrowers\nFree accident insurance on select schemes");
        product(tenantId, lichfl, "LIC HFL Home Loan", LoanType.HOME_LOAN, "8.50", "10.25", null, "0.25", "10000", "15000",
                30, "90", "60", 650, 800, 75, true, true, "Nil on floating rate for individuals",
                "Loan up to age 75 at maturity\nPensioners eligible\nFlexible for self-employed");
        product(tenantId, bajaj, "Bajaj Housing Home Loan", LoanType.HOME_LOAN, "8.50", "11.00", null, "0.50", "5000", null,
                32, "80", "65", 700, 800, 70, true, true, "Nil on floating rate for individuals",
                "Higher FOIR for strong profiles\nLoans up to 32 years");
        product(tenantId, kotak, "Kotak Home Loan", LoanType.HOME_LOAN, "8.75", "9.75", null, "0.50", "10000", null,
                30, "90", "55", 725, 800, 65, true, false, "Nil on floating rate",
                "Fast-track processing for salaried with large employers");

        // Loan against property
        product(tenantId, sbi, "SBI Loan Against Property", LoanType.LAP, "9.80", "10.95", null, "1.00", "10000", "50000",
                15, "65", "60", 680, 800, 70, true, true, "Nil on floating rate", "Usable for business or personal needs");
        product(tenantId, hdfc, "HDFC Bank LAP", LoanType.LAP, "9.50", "11.00", null, "1.00", "5000", null,
                15, "60", "60", 700, 800, 70, true, true, "Up to 4% on part-prepayment from own funds - confirm",
                "Overdraft facility available");
        product(tenantId, icici, "ICICI Bank LAP", LoanType.LAP, "9.75", "11.25", null, "1.00", "10000", null,
                15, "65", "60", 700, 800, 70, true, true, "As per lender policy", "Commercial property accepted");
        product(tenantId, bajaj, "Bajaj Housing LAP", LoanType.LAP, "9.75", "12.50", null, "1.00", "10000", null,
                18, "70", "70", 700, 800, 70, true, true, "As per lender policy", "Higher LTV on residential property");

        // Balance transfer
        product(tenantId, sbi, "SBI Home Loan Takeover", LoanType.BALANCE_TRANSFER, "8.25", "9.15", "0.05", "0.35", "2000", "10000",
                30, "90", "60", 700, 800, 70, true, true, "Nil", "Top-up along with transfer");
        product(tenantId, hdfc, "HDFC Bank Balance Transfer", LoanType.BALANCE_TRANSFER, "8.40", "9.40", "0.05", "0.50", "3000", null,
                30, "90", "55", 725, 800, 70, true, true, "Nil", "Top-up along with transfer");
        product(tenantId, axis, "Axis Bank Balance Transfer", LoanType.BALANCE_TRANSFER, "8.60", "9.60", null, "0.50", "10000", "10000",
                30, "90", "55", 725, 800, 65, true, true, "Nil", "Processing fee waivers on select offers");

        // Plot loans
        product(tenantId, sbi, "SBI Realty (plot loan)", LoanType.PLOT_LOAN, "8.75", "9.65", null, "0.35", "2000", "10000",
                15, "75", "60", 700, 800, 65, true, true, "Nil", "Construction must start within 5 years");
        product(tenantId, lichfl, "LIC HFL Plot Loan", LoanType.PLOT_LOAN, "9.00", "10.50", null, "0.25", "10000", "15000",
                15, "75", "60", 650, 800, 70, true, true, "Nil", "Plot + construction composite option");

        // Construction
        product(tenantId, lichfl, "LIC HFL Construction Loan", LoanType.CONSTRUCTION_LOAN, "8.50", "10.25", null, "0.25", "10000", "15000",
                30, "90", "60", 650, 800, 75, true, true, "Nil", "Stage-wise disbursement");
        product(tenantId, bob, "Baroda Home Construction", LoanType.CONSTRUCTION_LOAN, "8.30", "10.20", "0.15", "0.25", "2500", "25000",
                30, "90", "60", 701, 800, 70, true, true, "Nil", "Stage-wise disbursement");

        // Personal loan (unsecured)
        product(tenantId, hdfc, "HDFC Bank Personal Loan", LoanType.PERSONAL_LOAN, "10.85", "21.00", null, "2.00", "4999", null,
                6, null, "50", 730, 800, 60, true, false, "4% if closed before 12 EMIs - confirm", "Disbursal in 24-48 hours");
        product(tenantId, icici, "ICICI Bank Personal Loan", LoanType.PERSONAL_LOAN, "10.85", "16.65", null, "2.00", "1999", null,
                6, null, "50", 730, 800, 60, true, false, "As per lender policy", "Paperless for existing customers");
        product(tenantId, bajaj, "Bajaj Finserv Personal Loan", LoanType.PERSONAL_LOAN, "11.00", "24.00", null, "3.00", "1999", null,
                7, null, "55", 700, 800, 58, true, true, "Up to 4% plus GST", "Flexi-loan option");
    }

    private Lender lender(Long tenantId, String name, String shortName, LenderType type, String payout, String color) {
        Lender l = new Lender();
        l.setTenantId(tenantId);
        l.setName(name);
        l.setShortName(shortName);
        l.setLenderType(type);
        l.setPayoutPercent(new BigDecimal(payout));
        l.setPayoutTerms("Payout on disbursed amount, invoiced monthly. Sample terms - update from your DSA agreement.");
        l.setBrandColor(color);
        l.setRmName("Relationship Manager");
        l.setBranch("Bengaluru");
        l.setActive(true);
        l.setCreatedBy("system");
        return lenderRepository.save(l);
    }

    private void product(Long tenantId, Lender lender, String name, LoanType type, String rateMin, String rateMax,
                         String womenConcession, String feePct, String feeMin, String feeMax, int tenure,
                         String ltv, String foir, int minCibil, int bestCibil, int maxAge,
                         boolean salaried, boolean selfEmployed, String prepayment, String benefits) {
        LoanProduct p = new LoanProduct();
        p.setTenantId(tenantId);
        p.setLenderId(lender.getId());
        p.setLenderName(lender.getDisplayName());
        p.setName(name);
        p.setLoanType(type);
        p.setRateType(RateType.FLOATING);
        p.setBenchmark(lender.getLenderType() == LenderType.HFC ? "PLR" : "RLLR / EBLR");
        p.setRateMin(new BigDecimal(rateMin));
        p.setRateMax(new BigDecimal(rateMax));
        p.setWomenConcession(womenConcession == null ? null : new BigDecimal(womenConcession));
        p.setProcessingFeePercent(new BigDecimal(feePct));
        p.setProcessingFeeMin(feeMin == null ? null : new BigDecimal(feeMin));
        p.setProcessingFeeMax(feeMax == null ? null : new BigDecimal(feeMax));
        p.setMaxTenureYears(tenure);
        p.setMaxLtvPercent(ltv == null ? null : new BigDecimal(ltv));
        p.setMaxFoirPercent(foir == null ? null : new BigDecimal(foir));
        p.setMinCibil(minCibil);
        p.setBestRateCibil(bestCibil);
        p.setMaxAgeAtMaturity(maxAge);
        p.setMinMonthlyIncome(type == LoanType.PERSONAL_LOAN ? new BigDecimal("25000") : new BigDecimal("15000"));
        p.setMinLoanAmount(type == LoanType.PERSONAL_LOAN ? new BigDecimal("50000") : new BigDecimal("300000"));
        p.setMaxLoanAmount(type == LoanType.PERSONAL_LOAN ? new BigDecimal("4000000") : new BigDecimal("150000000"));
        p.setSalariedAllowed(salaried);
        p.setSelfEmployedAllowed(selfEmployed);
        p.setPensionerAllowed(lender.getLenderType() == LenderType.HFC || lender.getLenderType() == LenderType.PSU_BANK);
        p.setNriAllowed(type == LoanType.HOME_LOAN && lender.getLenderType() != LenderType.HFC);
        p.setPrepaymentCharges(prepayment);
        p.setKeyBenefits(benefits);
        p.setRateUpdatedOn(LocalDate.now());
        p.setNotes("Illustrative sample rate card - confirm current rates with the lender before advising.");
        p.setActive(true);
        p.setCreatedBy("system");
        productRepository.save(p);
    }

    // ------------------------------------------------------------------ permissions

    /**
     * Sensible defaults per role. Unlisted modules are denied for that role; Owner is never
     * restricted, and Admin / Manager keep full access (the URL rules still gate admin pages).
     */
    private void seedPermissions(Long tenantId) {
        permissionService.save(tenantId, Role.DATA_ENTRY, grants(
                "dashboard", "VIEW",
                "leads", "VIEW,ADD,EDIT",
                "customers", "VIEW,ADD,EDIT",
                "advisor", "VIEW",
                "lenders", "VIEW",
                "applications", "VIEW,ADD,EDIT",
                "modd", "VIEW,EDIT",
                "builders", "VIEW,ADD,EDIT",
                "referrals", "VIEW,ADD,EDIT"), "system");
        permissionService.save(tenantId, Role.LOAN_EXECUTIVE, grants(
                "dashboard", "VIEW",
                "leads", "VIEW,ADD,EDIT",
                "customers", "VIEW,ADD,EDIT",
                "advisor", "VIEW,ADD,EDIT,PRINT",
                "lenders", "VIEW",
                "applications", "VIEW,ADD,EDIT,PRINT",
                "modd", "VIEW",
                "builders", "VIEW",
                "referrals", "VIEW,ADD,EDIT",
                "reports", "VIEW"), "system");
        permissionService.save(tenantId, Role.PROCESSING, grants(
                "dashboard", "VIEW",
                "leads", "VIEW,EDIT",
                "customers", "VIEW,ADD,EDIT",
                "advisor", "VIEW,ADD,EDIT,PRINT",
                "lenders", "VIEW,EDIT",
                "applications", "VIEW,ADD,EDIT,EXPORT,PRINT",
                "modd", "VIEW,ADD,EDIT,EXPORT,PRINT",
                "builders", "VIEW,EDIT",
                "referrals", "VIEW",
                "reports", "VIEW,EXPORT"), "system");
        permissionService.save(tenantId, Role.FINANCE, grants(
                "dashboard", "VIEW",
                "customers", "VIEW",
                "advisor", "VIEW",
                "lenders", "VIEW",
                "applications", "VIEW,EXPORT",
                "modd", "VIEW",
                "builders", "VIEW",
                "referrals", "VIEW",
                "referral_payouts", "VIEW,EDIT,EXPORT,PRINT",
                "commission", "VIEW,ADD,EDIT,DELETE,EXPORT,PRINT",
                "finance", "VIEW,ADD,EDIT,DELETE,EXPORT,PRINT",
                "reports", "VIEW,EXPORT,PRINT"), "system");
    }

    private static Map<String, List<String>> grants(String... pairs) {
        Map<String, List<String>> map = new HashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(pairs[i], List.of(pairs[i + 1].split(",")));
        }
        return map;
    }
}
