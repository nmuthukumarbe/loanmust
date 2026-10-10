package com.loanmust.loan.config;

import com.loanmust.loan.advisor.AdvisorRequest;
import com.loanmust.loan.advisor.LoanAdvisorService;
import com.loanmust.loan.application.ApplicationDocument;
import com.loanmust.loan.application.ApplicationStage;
import com.loanmust.loan.application.Disbursement;
import com.loanmust.loan.application.DocumentStatus;
import com.loanmust.loan.application.LoanApplication;
import com.loanmust.loan.application.LoanApplicationService;
import com.loanmust.loan.builder.Builder;
import com.loanmust.loan.builder.BuilderService;
import com.loanmust.loan.builder.Project;
import com.loanmust.loan.builder.ProjectStatus;
import com.loanmust.loan.commission.BankPayout;
import com.loanmust.loan.commission.CommissionService;
import com.loanmust.loan.common.Channel;
import com.loanmust.loan.common.PaymentMode;
import com.loanmust.loan.customer.Customer;
import com.loanmust.loan.customer.CustomerService;
import com.loanmust.loan.customer.EmploymentType;
import com.loanmust.loan.customer.Gender;
import com.loanmust.loan.finance.FeeReceipt;
import com.loanmust.loan.finance.FeeType;
import com.loanmust.loan.finance.FinanceService;
import com.loanmust.loan.hr.AttendanceStatus;
import com.loanmust.loan.hr.Employee;
import com.loanmust.loan.hr.EmployeeService;
import com.loanmust.loan.lead.Lead;
import com.loanmust.loan.lead.LeadActivity;
import com.loanmust.loan.lead.LeadService;
import com.loanmust.loan.lead.LeadSource;
import com.loanmust.loan.lead.LeadStatus;
import com.loanmust.loan.lender.LenderService;
import com.loanmust.loan.lender.LoanProduct;
import com.loanmust.loan.lender.LoanType;
import com.loanmust.loan.modd.ModdRecord;
import com.loanmust.loan.modd.ModdService;
import com.loanmust.loan.modd.ModdStatus;
import com.loanmust.loan.referral.PartnerType;
import com.loanmust.loan.referral.PayoutBasis;
import com.loanmust.loan.referral.ReferralPartner;
import com.loanmust.loan.referral.ReferralPayout;
import com.loanmust.loan.referral.ReferralService;
import com.loanmust.loan.security.CustomUserDetails;
import com.loanmust.loan.user.User;
import com.loanmust.loan.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * A realistic demo book - builders, referral partners, leads, customers, loan files at every
 * stage, disbursements, payouts, MODD jobs, fee receipts and attendance - so every screen
 * has something on it. Runs once, on an empty book, when app.demo.seed-transactions=true.
 *
 * Everything is created through the real services while signed in as the demo owner, so
 * the demo data is exactly what the application itself would have produced, audit included.
 */
@Component
@Order(20)
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final boolean enabled;
    private final UserRepository userRepository;
    private final BuilderService builderService;
    private final ReferralService referralService;
    private final LeadService leadService;
    private final CustomerService customerService;
    private final LenderService lenderService;
    private final LoanApplicationService applicationService;
    private final ModdService moddService;
    private final CommissionService commissionService;
    private final FinanceService financeService;
    private final LoanAdvisorService advisorService;
    private final EmployeeService employeeService;

    public DemoDataSeeder(@Value("${app.demo.seed-transactions:false}") boolean enabled,
                          UserRepository userRepository, BuilderService builderService, ReferralService referralService,
                          LeadService leadService, CustomerService customerService, LenderService lenderService,
                          LoanApplicationService applicationService, ModdService moddService,
                          CommissionService commissionService, FinanceService financeService,
                          LoanAdvisorService advisorService, EmployeeService employeeService) {
        this.enabled = enabled;
        this.userRepository = userRepository;
        this.builderService = builderService;
        this.referralService = referralService;
        this.leadService = leadService;
        this.customerService = customerService;
        this.lenderService = lenderService;
        this.applicationService = applicationService;
        this.moddService = moddService;
        this.commissionService = commissionService;
        this.financeService = financeService;
        this.advisorService = advisorService;
        this.employeeService = employeeService;
    }

    @Override
    public void run(String... args) {
        if (!enabled) return;
        User owner = userRepository.findByEmailAndDeletedFalse(DataSeeder.OWNER_EMAIL).orElse(null);
        if (owner == null) return;
        Long t = owner.getTenantId();
        if (!builderService.builders(t).isEmpty()) return;   // already has data

        log.info("Seeding the demo loan book...");
        CustomUserDetails principal = new CustomUserDetails(owner);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        try {
            seed(t);
            log.info("Demo loan book ready.");
        } catch (Exception e) {
            log.warn("Demo data seeding stopped early: {}", e.getMessage(), e);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void seed(Long t) {
        Long exec1 = userId("executive@shreefinserv.in");
        Long exec2 = userId("executive2@shreefinserv.in");
        Long processor = userId("processing@shreefinserv.in");
        LocalDate today = LocalDate.now();

        // ---- builder tie-ups ----
        Builder nandi = builder(t, "Nandi Developers", "Suresh Nandi", "+919886012345", "Whitefield",
                "Site desk on weekends; buyer referrals shared with us first.");
        Builder skyline = builder(t, "Skyline Constructions", "Farhan Ali", "+919845098765", "Sarjapur Road",
                "Approved-project buyers referred for home loans.");
        Builder greenfield = builder(t, "Greenfield Realty", "Asha Menon", "+919900112233", "Devanahalli",
                "Plotted development - plot and composite loans.");
        Project nandiHeights = project(t, nandi, "Nandi Heights", "Whitefield", "Apartment", ProjectStatus.UNDER_CONSTRUCTION,
                "6500000", "12500000", "SBI, HDFC Bank, LIC HFL");
        Project nandiGrove = project(t, nandi, "Nandi Grove Villas", "Varthur", "Villa", ProjectStatus.READY_TO_MOVE,
                "18000000", "26000000", "HDFC Bank, ICICI Bank");
        Project skylineOne = project(t, skyline, "Skyline One", "Sarjapur Road", "Apartment", ProjectStatus.UNDER_CONSTRUCTION,
                "5500000", "9800000", "SBI, Axis Bank, Bajaj Housing");
        Project greenfieldMeadows = project(t, greenfield, "Greenfield Meadows", "Devanahalli", "Plot", ProjectStatus.READY_TO_MOVE,
                "2500000", "6000000", "SBI, LIC HFL");

        // ---- referral partners ----
        ReferralPartner licAgent = partner(t, "Manjunath S", "+919880011122", PartnerType.LIC_AGENT, PayoutBasis.PERCENT_OF_DISBURSAL, "0.25", "manjunath@okaxis");
        ReferralPartner ca = partner(t, "CA Ramesh Bhat", "+919844055566", PartnerType.CA_PROFESSIONAL, PayoutBasis.PERCENT_OF_DISBURSAL, "0.20", "rameshbhat@oksbi");
        ReferralPartner oldCustomer = partner(t, "Geetha Krishnan", "+919901077788", PartnerType.CUSTOMER, PayoutBasis.FLAT_PER_FILE, "5000", "geetha.k@okicici");
        ReferralPartner subDsa = partner(t, "Prakash Finance Point", "+919845333444", PartnerType.CHANNEL_PARTNER, PayoutBasis.PERCENT_OF_DISBURSAL, "0.35", null);
        partner(t, "Sowmya Rao", "+919886777999", PartnerType.INDIVIDUAL, PayoutBasis.FLAT_PER_FILE, "3000", "sowmya@ybl");

        // ---- open leads at every status ----
        lead(t, "Arvind Kulkarni", "+919845100001", LeadSource.BUILDER, nandi, nandiHeights, null, LoanType.HOME_LOAN,
                "7500000", "9500000", "145000", "0", 782, EmploymentType.SALARIED, LeadStatus.INTERESTED, exec1, today);
        lead(t, "Shilpa Desai", "+919845100002", LeadSource.LIC, null, null, null, LoanType.HOME_LOAN,
                "4500000", "6000000", "85000", "12000", 745, EmploymentType.SALARIED, LeadStatus.CONTACTED, exec1, today.minusDays(1));
        lead(t, "Mohammed Irfan", "+919845100003", LeadSource.REFERRAL, null, null, licAgent, LoanType.LAP,
                "3000000", "9000000", "220000", "35000", 721, EmploymentType.SELF_EMPLOYED_BUSINESS, LeadStatus.FOLLOW_UP, exec2, today.plusDays(2));
        lead(t, "Pooja Iyer", "+919845100004", LeadSource.WEBSITE, null, null, null, LoanType.BALANCE_TRANSFER,
                "3800000", null, "120000", "41000", 801, EmploymentType.SALARIED, LeadStatus.NEW, null, today);
        lead(t, "Naveen Shetty", "+919845100005", LeadSource.WALK_IN, null, null, null, LoanType.PLOT_LOAN,
                "2000000", "3200000", "60000", "8000", 690, EmploymentType.SELF_EMPLOYED_PROFESSIONAL, LeadStatus.DOCS_REQUESTED, exec2, today.minusDays(2));
        lead(t, "Lalitha Murthy", "+919845100006", LeadSource.REFERRAL, null, null, ca, LoanType.HOME_LOAN,
                "6000000", "8000000", "110000", "0", null, EmploymentType.SELF_EMPLOYED_PROFESSIONAL, LeadStatus.NEW, exec1, today);
        Lead lost = lead(t, "Deepak Jain", "+919845100007", LeadSource.DIRECT, null, null, null, LoanType.PERSONAL_LOAN,
                "800000", null, "40000", "22000", 640, EmploymentType.SALARIED, LeadStatus.CONTACTED, exec2, null);
        leadService.close(t, lost.getId(), LeadStatus.NOT_ELIGIBLE, "CIBIL 640 below every lender's cut-off; advised credit repair");

        // ---- loan files at every stage ----
        Customer c1 = customer(t, "Rajesh Kumar", "+919845200001", Gender.MALE, EmploymentType.SALARIED, "Infosys Ltd",
                "165000", "8000", 805, 38, licAgent, exec1);
        Customer c2 = customer(t, "Anitha Prakash", "+919845200002", Gender.FEMALE, EmploymentType.SALARIED, "Bosch India",
                "120000", "0", 776, 34, null, exec1);
        Customer c3 = customer(t, "Vinod Patil", "+919845200003", Gender.MALE, EmploymentType.SELF_EMPLOYED_BUSINESS, "Patil Traders",
                "250000", "30000", 742, 45, subDsa, exec2);
        Customer c4 = customer(t, "Kavitha Ramesh", "+919845200004", Gender.FEMALE, EmploymentType.SALARIED, "Wipro Ltd",
                "98000", "6500", 768, 31, oldCustomer, exec2);
        Customer c5 = customer(t, "Harish Gowda", "+919845200005", Gender.MALE, EmploymentType.SALARIED, "Karnataka Govt",
                "88000", "0", 812, 49, null, exec1);
        Customer c6 = customer(t, "Sneha Kulkarni", "+919845200006", Gender.FEMALE, EmploymentType.SELF_EMPLOYED_PROFESSIONAL, "Sneha Dental Clinic",
                "180000", "15000", 790, 36, ca, exec1);

        // 1. Fully disbursed 4 months ago, payout received, referral paid
        LoanApplication a1 = file(t, c1, LoanType.HOME_LOAN, "HDFC Bank Home Loan", nandi, nandiHeights, "9200000", "7200000", 20, exec1, processor);
        complete(t, a1, "HDFC/HL/2026/118842", today.minusMonths(5).minusDays(12), "7200000", "8.40", 20, today.minusMonths(4).minusDays(20));
        tranche(t, a1, today.minusMonths(4).minusDays(5), "7200000", "Nandi Developers");
        receive(t, a1, today.minusMonths(3).minusDays(10));
        approveAndPay(t, a1, today.minusMonths(3).minusDays(5), "UTR226014587712");
        receipt(t, a1, FeeType.LOGIN_FEE, "5000", today.minusMonths(5).minusDays(10));

        // 2. Construction-linked, part disbursed in two tranches
        LoanApplication a2 = file(t, c2, LoanType.HOME_LOAN, "SBI Regular Home Loan", skyline, skylineOne, "8200000", "6000000", 25, exec1, processor);
        complete(t, a2, "SBI/HL/57120934", today.minusMonths(3).minusDays(2), "6000000", "8.20", 25, today.minusMonths(2).minusDays(18));
        tranche(t, a2, today.minusMonths(2).minusDays(5), "2500000", "Skyline Constructions");
        tranche(t, a2, today.minusDays(12), "1500000", "Skyline Constructions");
        receive(t, a2, today.minusMonths(1).minusDays(3));

        // 3. LAP disbursed last month, payout not yet received, referral approved
        LoanApplication a3 = file(t, c3, LoanType.LAP, "HDFC Bank LAP", null, null, "12000000", "5000000", 15, exec2, processor);
        complete(t, a3, "HDFC/LAP/2026/77120", today.minusMonths(2), "5000000", "9.75", 15, today.minusMonths(1).minusDays(15));
        tranche(t, a3, today.minusMonths(1).minusDays(8), "5000000", "Customer account");
        approveOnly(t, a3);
        receipt(t, a3, FeeType.MODD_CHARGES, "12500", today.minusMonths(1).minusDays(16));

        // 4. Sanctioned, MODD appointment booked this week
        LoanApplication a4 = file(t, c4, LoanType.HOME_LOAN, "LIC HFL Home Loan", nandi, nandiGrove, "6500000", "5000000", 25, exec2, processor);
        applicationService.recordLogin(t, a4.getId(), "LICHFL/BLR/2026/3391", today.minusDays(40), null);
        markAllDocs(t, a4, DocumentStatus.VERIFIED);
        applicationService.moveStage(t, a4.getId(), ApplicationStage.CREDIT_REVIEW, "PD done at office");
        applicationService.moveStage(t, a4.getId(), ApplicationStage.TECHNICAL_LEGAL, "Valuation & legal initiated");
        applicationService.recordSanction(t, a4.getId(), new BigDecimal("5000000"), new BigDecimal("8.45"), 25,
                today.minusDays(10), today.plusDays(10), "Sanction letter collected");
        applicationService.moveStage(t, a4.getId(), ApplicationStage.MODD_AGREEMENT, "Agreement signing fixed");
        ModdRecord m4 = moddService.forApplication(t, a4.getId());
        if (m4 != null) {
            m4.setSubRegistrarOffice("Sub-Registrar Office, Varthur");
            m4.setAppointmentAt(today.plusDays(3).atTime(11, 0));
            m4.setStatus(ModdStatus.APPOINTMENT_BOOKED);
            m4.setRegistrationFee(new BigDecimal("2000"));
            moddService.save(t, m4);
        }

        // 5. Technical & legal
        LoanApplication a5 = file(t, c5, LoanType.BALANCE_TRANSFER, "SBI Home Loan Takeover", null, null, "7000000", "3500000", 15, exec1, processor);
        applicationService.recordLogin(t, a5.getId(), "SBI/BT/88123401", today.minusDays(14), "Takeover from private bank at 9.6%");
        markAllDocs(t, a5, DocumentStatus.RECEIVED);
        applicationService.moveStage(t, a5.getId(), ApplicationStage.CREDIT_REVIEW, null);
        applicationService.moveStage(t, a5.getId(), ApplicationStage.TECHNICAL_LEGAL, "Valuer visit on Saturday");

        // 6. Logged in, bank query open
        LoanApplication a6 = file(t, c6, LoanType.HOME_LOAN, "ICICI Bank Home Loan", null, null, "11000000", "7500000", 20, exec1, processor);
        markSomeDocs(t, a6);
        applicationService.recordLogin(t, a6.getId(), "ICICI/HL/BLR/556021", today.minusDays(5), null);
        applicationService.addNote(t, a6.getId(), "BANK_QUERY", "Bank wants clinic registration certificate and 2 years' GST returns.");

        // 7. Collecting documents
        Customer c7 = customer(t, "Ganesh Bhat", "+919845200007", Gender.MALE, EmploymentType.SALARIED, "Accenture",
                "92000", "11000", 758, 29, null, exec2);
        LoanApplication a7 = file(t, c7, LoanType.PLOT_LOAN, "SBI Realty (plot loan)", greenfield, greenfieldMeadows, "3600000", "2700000", 15, exec2, processor);
        markSomeDocs(t, a7);

        // 8. Rejected
        Customer c8 = customer(t, "Sunil Verma", "+919845200008", Gender.MALE, EmploymentType.SELF_EMPLOYED_BUSINESS, "Verma Electricals",
                "70000", "28000", 702, 52, null, exec2);
        LoanApplication a8 = file(t, c8, LoanType.HOME_LOAN, "Axis Bank Home Loan", null, null, "5000000", "4000000", 15, exec2, processor);
        applicationService.recordLogin(t, a8.getId(), "AXIS/HL/22931", today.minusDays(30), null);
        applicationService.moveStage(t, a8.getId(), ApplicationStage.REJECTED, "Income on ITR insufficient for asked amount; re-pitch at lower ticket with co-applicant");

        // ---- saved proposals ----
        proposal(t, c2, LoanType.HOME_LOAN, "6000000", "8200000", 25, true);
        proposal(t, c6, LoanType.HOME_LOAN, "7500000", "11000000", 20, true);

        // ---- staff & attendance ----
        for (User u : userRepository.findByTenantIdAndDeletedFalseOrderByFullNameAsc(t)) {
            Employee e = new Employee();
            e.setName(u.getFullName());
            e.setMobile(u.getPhone());
            e.setEmail(u.getEmail());
            e.setDesignation(u.getDesignation());
            e.setDepartment(switch (u.getRole()) {
                case DATA_ENTRY -> "Data Entry";
                case LOAN_EXECUTIVE, MANAGER -> "Sales";
                case PROCESSING -> "Processing";
                case FINANCE -> "Accounts";
                default -> "Administration";
            });
            e.setJoiningDate(today.minusYears(1).minusDays(u.getId() * 17));
            e.setMonthlySalary(new BigDecimal(switch (u.getRole()) {
                case DATA_ENTRY -> "18000";
                case LOAN_EXECUTIVE -> "25000";
                case PROCESSING -> "28000";
                case FINANCE -> "30000";
                default -> "45000";
            }));
            e.setUserId(u.getId());
            e.setActive(true);
            Employee saved = employeeService.save(t, e);
            for (int d = 1; d <= 12; d++) {
                LocalDate day = today.minusDays(d);
                if (day.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
                AttendanceStatus s = (d + saved.getId()) % 11 == 0 ? AttendanceStatus.LEAVE
                        : (d + saved.getId()) % 7 == 0 ? AttendanceStatus.HALF_DAY
                        : "Sales".equals(saved.getDepartment()) && d % 3 == 0 ? AttendanceStatus.FIELD_WORK
                        : AttendanceStatus.PRESENT;
                employeeService.mark(t, saved.getId(), day, s, LocalTime.of(9, 30), LocalTime.of(18, 15), null);
            }
        }
    }

    // ------------------------------------------------------------------ builders

    private Long userId(String email) {
        return userRepository.findByEmailAndDeletedFalse(email).map(User::getId).orElse(null);
    }

    private Builder builder(Long t, String name, String contact, String mobile, String area, String arrangement) {
        Builder b = new Builder();
        b.setName(name);
        b.setContactPerson(contact);
        b.setMobile(mobile);
        b.setCity("Bengaluru");
        b.setAddress(area + ", Bengaluru");
        b.setTieUpSince(LocalDate.now().minusMonths(14));
        b.setArrangement(arrangement);
        b.setActive(true);
        return builderService.save(t, b);
    }

    private Project project(Long t, Builder b, String name, String location, String type, ProjectStatus status,
                            String from, String to, String lenders) {
        Project p = new Project();
        p.setBuilderId(b.getId());
        p.setName(name);
        p.setLocation(location);
        p.setCity("Bengaluru");
        p.setProjectType(type);
        p.setStatus(status);
        p.setReraNumber("PRM/KA/RERA/1251/" + (300 + name.length()) + "/PR/" + (200000 + name.hashCode() % 1000 + 1000));
        p.setTotalUnits(80 + name.length() * 3);
        p.setPriceFrom(new BigDecimal(from));
        p.setPriceTo(new BigDecimal(to));
        p.setPossessionDate(LocalDate.now().plusMonths(status == ProjectStatus.READY_TO_MOVE ? 0 : 18));
        p.setApprovedLenders(lenders);
        return builderService.saveProject(t, p);
    }

    private ReferralPartner partner(Long t, String name, String mobile, PartnerType type, PayoutBasis basis, String value, String upi) {
        ReferralPartner p = new ReferralPartner();
        p.setName(name);
        p.setMobile(mobile);
        p.setCity("Bengaluru");
        p.setPartnerType(type);
        p.setPayoutBasis(basis);
        p.setPayoutValue(new BigDecimal(value));
        p.setUpiId(upi);
        if (upi == null) {
            p.setBankAccountName(name);
            p.setBankAccountNumber("50100" + Math.abs(mobile.hashCode()) % 100000000);
            p.setIfsc("HDFC0001234");
            p.setBankName("HDFC Bank");
        }
        p.setActive(true);
        return referralService.save(t, p);
    }

    // ------------------------------------------------------------------ leads & customers

    private Lead lead(Long t, String name, String mobile, LeadSource source, Builder builder, Project project,
                      ReferralPartner partner, LoanType type, String amount, String propertyValue, String income,
                      String emi, Integer cibil, EmploymentType emp, LeadStatus status, Long assignee, LocalDate followUp) {
        Lead l = new Lead();
        l.setName(name);
        l.setMobile(mobile);
        l.setCity("Bengaluru");
        l.setSource(source);
        if (source == LeadSource.LIC) l.setSourceDetail("LIC branch 7B, Jayanagar");
        if (source == LeadSource.WEBSITE) l.setSourceDetail("Balance-transfer enquiry form");
        l.setBuilderId(builder == null ? null : builder.getId());
        l.setProjectId(project == null ? null : project.getId());
        l.setReferralPartnerId(partner == null ? null : partner.getId());
        l.setLoanType(type);
        l.setLoanAmountRequired(new BigDecimal(amount));
        l.setPropertyValue(propertyValue == null ? null : new BigDecimal(propertyValue));
        l.setMonthlyIncome(new BigDecimal(income));
        l.setExistingEmi(new BigDecimal(emi));
        l.setCibilScore(cibil);
        l.setEmploymentType(emp);
        l.setStatus(status);
        l.setAssignedUserId(assignee);
        l.setNextFollowUpDate(followUp);
        l.setRequirement(type.getLabel() + " - looking for the lowest EMI");
        Lead saved = leadService.save(t, l);
        if (status != LeadStatus.NEW) {
            LeadActivity a = new LeadActivity();
            a.setActivityDate(LocalDate.now().minusDays(3));
            a.setChannel(Channel.CALL);
            a.setRemarks("Explained the process and documents; customer comparing banks.");
            a.setOutcome("Interested");
            a.setNextFollowUpDate(followUp);
            leadService.addActivity(t, saved.getId(), a, status);
        }
        return saved;
    }

    private Customer customer(Long t, String name, String mobile, Gender gender, EmploymentType emp, String employer,
                              String income, String emi, int cibil, int age, ReferralPartner partner, Long owner) {
        Customer c = new Customer();
        c.setName(name);
        c.setMobile(mobile);
        c.setEmail(name.toLowerCase().replace(" ", ".") + "@example.com");
        c.setGender(gender);
        c.setDateOfBirth(LocalDate.now().minusYears(age).minusDays(age * 7L));
        c.setPan("ABCPK" + (1000 + Math.abs(name.hashCode()) % 9000) + "L");
        c.setAadhaarLast4(String.valueOf(1000 + Math.abs(mobile.hashCode()) % 9000));
        c.setAddress("No. " + (10 + age) + ", 4th Cross, Bengaluru");
        c.setCity("Bengaluru");
        c.setState("Karnataka");
        c.setPincode("5600" + (10 + age));
        c.setEmploymentType(emp);
        c.setEmployerName(employer);
        c.setWorkExperienceYears(Math.max(2, age - 24));
        c.setMonthlyIncome(new BigDecimal(income));
        c.setExistingEmi(new BigDecimal(emi));
        c.setCibilScore(cibil);
        c.setCibilCheckedOn(LocalDate.now().minusDays(20));
        c.setReferralPartnerId(partner == null ? null : partner.getId());
        c.setOwnerUserId(owner);
        return customerService.save(t, c);
    }

    // ------------------------------------------------------------------ loan files

    private LoanApplication file(Long t, Customer c, LoanType type, String productName, Builder builder, Project project,
                                 String propertyValue, String amount, int tenure, Long exec, Long processor) {
        LoanProduct product = lenderService.activeProducts(t, type).stream()
                .filter(p -> p.getName().equals(productName)).findFirst().orElse(null);
        LoanApplication a = new LoanApplication();
        a.setCustomerId(c.getId());
        a.setLoanType(type);
        a.setProductId(product == null ? null : product.getId());
        a.setLenderId(product == null ? null : product.getLenderId());
        a.setBuilderId(builder == null ? null : builder.getId());
        a.setProjectId(project == null ? null : project.getId());
        a.setSource(builder != null ? LeadSource.BUILDER : c.getReferralPartnerId() != null ? LeadSource.REFERRAL : LeadSource.DIRECT);
        a.setPropertyAddress(project != null ? project.getName() + ", " + project.getLocation() + ", Bengaluru" : c.getAddress());
        a.setPropertyType(project != null ? project.getProjectType() : "Apartment");
        a.setPropertyValue(new BigDecimal(propertyValue));
        a.setRequestedAmount(new BigDecimal(amount));
        a.setTenureYears(tenure);
        a.setAssignedUserId(exec);
        a.setProcessorUserId(processor);
        return applicationService.save(t, a);
    }

    private void complete(Long t, LoanApplication a, String bankRef, LocalDate loginDate, String sanctioned, String rate,
                          int tenure, LocalDate sanctionDate) {
        applicationService.recordLogin(t, a.getId(), bankRef, loginDate, null);
        markAllDocs(t, a, DocumentStatus.VERIFIED);
        applicationService.moveStage(t, a.getId(), ApplicationStage.CREDIT_REVIEW, "PD completed");
        applicationService.moveStage(t, a.getId(), ApplicationStage.TECHNICAL_LEGAL, "Valuation and legal clear");
        applicationService.recordSanction(t, a.getId(), new BigDecimal(sanctioned), new BigDecimal(rate), tenure,
                sanctionDate, sanctionDate.plusMonths(6), null);
        applicationService.moveStage(t, a.getId(), ApplicationStage.MODD_AGREEMENT, "Agreement signed");
        LoanApplication fresh = applicationService.get(t, a.getId());
        ModdRecord m = moddService.ensureFor(t, fresh);
        m.setSubRegistrarOffice("Sub-Registrar Office, Shivajinagar");
        m.setAppointmentAt(sanctionDate.plusDays(7).atTime(10, 30));
        m.setRegistrationNumber("SVN-1-0" + (4000 + a.getId()) + "-2026-27");
        m.setRegisteredOn(sanctionDate.plusDays(7));
        m.setRegistrationFee(new BigDecimal("2000"));
        m.setDepositedOn(sanctionDate.plusDays(9));
        m.setDepositedWith(fresh.getLenderName() + " - Bengaluru branch");
        m.setAcknowledgementNumber("ACK/" + (88000 + a.getId()));
        m.setChargesCollected(true);
        m.setStatus(ModdStatus.COMPLETED);
        moddService.save(t, m);
    }

    private void tranche(Long t, LoanApplication a, LocalDate on, String amount, String paidTo) {
        Disbursement d = new Disbursement();
        d.setDisbursedOn(on);
        d.setAmount(new BigDecimal(amount));
        d.setPaidTo(paidTo);
        d.setReference("UTR" + on.toString().replace("-", "") + (a.getId() * 7));
        applicationService.addDisbursement(t, a.getId(), d);
    }

    private void receive(Long t, LoanApplication a, LocalDate on) {
        for (BankPayout p : commissionService.forApplication(t, a.getId())) {
            commissionService.recordInvoice(t, p.getId(), "SF/" + on.getYear() + "/" + (100 + p.getId()), on.minusDays(15));
            commissionService.recordReceipt(t, p.getId(), p.getExpectedAmount(), on, "NEFT" + (700000 + p.getId()));
        }
    }

    private void approveAndPay(Long t, LoanApplication a, LocalDate on, String utr) {
        for (ReferralPayout p : referralService.searchPayouts(t, a.getCode(), null, null, 0, 20).getContent()) {
            referralService.approve(t, p.getId());
            referralService.markPaid(t, p.getId(), on, PaymentMode.UPI, utr, null);
        }
    }

    private void approveOnly(Long t, LoanApplication a) {
        for (ReferralPayout p : referralService.searchPayouts(t, a.getCode(), null, null, 0, 20).getContent()) {
            referralService.approve(t, p.getId());
        }
    }

    private void receipt(Long t, LoanApplication a, FeeType type, String amount, LocalDate on) {
        FeeReceipt r = new FeeReceipt();
        r.setApplicationId(a.getId());
        r.setFeeType(type);
        r.setAmount(new BigDecimal(amount));
        r.setReceivedOn(on);
        r.setPaymentMode(PaymentMode.UPI);
        r.setReference("UPI" + (500000 + a.getId()));
        financeService.saveReceipt(t, r);
    }

    private void markAllDocs(Long t, LoanApplication a, DocumentStatus status) {
        for (ApplicationDocument d : applicationService.documents(t, a.getId())) {
            applicationService.setDocumentStatus(t, d.getId(), d.isMandatory() ? status : DocumentStatus.NOT_APPLICABLE, null);
        }
    }

    private void markSomeDocs(Long t, LoanApplication a) {
        List<ApplicationDocument> docs = applicationService.documents(t, a.getId());
        for (int i = 0; i < docs.size(); i++) {
            if (i % 3 != 2) applicationService.setDocumentStatus(t, docs.get(i).getId(), DocumentStatus.RECEIVED, null);
        }
    }

    private void proposal(Long t, Customer c, LoanType type, String amount, String propertyValue, int tenure, boolean woman) {
        AdvisorRequest r = new AdvisorRequest();
        r.setCustomerId(c.getId());
        r.setCustomerName(c.getName());
        r.setCustomerMobile(c.getMobile());
        r.setLoanType(type);
        r.setLoanAmount(new BigDecimal(amount));
        r.setPropertyValue(new BigDecimal(propertyValue));
        r.setTenureYears(tenure);
        r.setMonthlyIncome(c.getMonthlyIncome());
        r.setExistingEmi(c.getExistingEmi());
        r.setCibilScore(c.getCibilScore());
        r.setEmploymentType(c.getEmploymentType());
        r.setApplicantAge(c.getAge());
        r.setWomanApplicant(woman && c.getGender() == Gender.FEMALE);
        advisorService.saveProposal(t, r, "Shared after first meeting");
    }
}
