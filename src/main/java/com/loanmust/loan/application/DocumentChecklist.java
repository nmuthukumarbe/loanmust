package com.loanmust.loan.application;

import com.loanmust.loan.customer.EmploymentType;
import com.loanmust.loan.lender.LoanType;

import java.util.ArrayList;
import java.util.List;

/**
 * The standard document list Indian lenders ask for, assembled for the borrower's profile:
 * KYC for everyone, income proof by employment type, property papers for secured loans and
 * existing-loan papers for a balance transfer. Staff can add or mark lines "not applicable"
 * on the file - this is only the starting checklist.
 */
public final class DocumentChecklist {

    private DocumentChecklist() {
    }

    public record Item(String name, DocumentCategory category, boolean mandatory) {
    }

    public static List<Item> forProfile(EmploymentType employment, LoanType loanType, boolean builderProject,
                                        boolean hasCoApplicant) {
        List<Item> items = new ArrayList<>();
        // KYC
        items.add(new Item("PAN card", DocumentCategory.KYC, true));
        items.add(new Item("Aadhaar card", DocumentCategory.KYC, true));
        items.add(new Item("Passport-size photographs", DocumentCategory.KYC, true));
        items.add(new Item("Current address proof", DocumentCategory.KYC, false));
        if (hasCoApplicant) {
            items.add(new Item("Co-applicant PAN, Aadhaar & photo", DocumentCategory.KYC, true));
        }

        // Income
        EmploymentType emp = employment == null ? EmploymentType.SALARIED : employment;
        switch (emp) {
            case SALARIED -> {
                items.add(new Item("Salary slips - last 3 months", DocumentCategory.INCOME, true));
                items.add(new Item("Form 16 - last 2 years", DocumentCategory.INCOME, true));
                items.add(new Item("Employment / appointment letter or employee ID", DocumentCategory.INCOME, false));
                items.add(new Item("Salary account statement - last 6 months", DocumentCategory.BANKING, true));
            }
            case SELF_EMPLOYED_PROFESSIONAL, SELF_EMPLOYED_BUSINESS -> {
                items.add(new Item("ITR with computation - last 3 years", DocumentCategory.INCOME, true));
                items.add(new Item("CA-certified P&L and balance sheet - last 3 years", DocumentCategory.INCOME, true));
                items.add(new Item(emp == EmploymentType.SELF_EMPLOYED_PROFESSIONAL
                        ? "Professional qualification / practice certificate"
                        : "Business proof (GST registration / Udyam / shop licence)", DocumentCategory.INCOME, true));
                items.add(new Item("GST returns - last 12 months", DocumentCategory.INCOME, false));
                items.add(new Item("Current & savings account statements - last 12 months", DocumentCategory.BANKING, true));
            }
            case PENSIONER -> {
                items.add(new Item("Pension payment order (PPO)", DocumentCategory.INCOME, true));
                items.add(new Item("Pension account statement - last 6 months", DocumentCategory.BANKING, true));
            }
            case NRI -> {
                items.add(new Item("Passport & valid visa / work permit", DocumentCategory.KYC, true));
                items.add(new Item("Overseas salary slips - last 3 months & employment contract", DocumentCategory.INCOME, true));
                items.add(new Item("NRE / NRO account statement - last 6 months", DocumentCategory.BANKING, true));
                items.add(new Item("Power of attorney (if applicable)", DocumentCategory.OTHER, false));
            }
        }

        // Property
        LoanType type = loanType == null ? LoanType.HOME_LOAN : loanType;
        if (type.isSecured()) {
            if (builderProject) {
                items.add(new Item("Allotment letter / builder-buyer agreement", DocumentCategory.PROPERTY, true));
                items.add(new Item("Builder NOC & project approval (APF) letter", DocumentCategory.PROPERTY, true));
                items.add(new Item("Own-contribution payment receipts", DocumentCategory.PROPERTY, true));
            } else {
                items.add(new Item("Sale agreement / agreement to sell", DocumentCategory.PROPERTY, true));
                items.add(new Item("Title deed & mother-deed chain", DocumentCategory.PROPERTY, true));
                items.add(new Item("Encumbrance certificate (EC) - 13 to 30 years", DocumentCategory.PROPERTY, true));
                items.add(new Item("Khata certificate & extract / property tax receipts", DocumentCategory.PROPERTY, true));
            }
            if (type == LoanType.CONSTRUCTION_LOAN || type == LoanType.HOME_IMPROVEMENT || type == LoanType.PLOT_LOAN) {
                items.add(new Item("Approved building plan & licence", DocumentCategory.PROPERTY, type != LoanType.PLOT_LOAN));
                items.add(new Item("Engineer's cost estimate", DocumentCategory.PROPERTY, type != LoanType.PLOT_LOAN));
            } else {
                items.add(new Item("Approved building plan / occupancy certificate", DocumentCategory.PROPERTY, false));
            }
        }
        if (type == LoanType.BALANCE_TRANSFER || type == LoanType.TOP_UP) {
            items.add(new Item("Existing loan sanction letter", DocumentCategory.EXISTING_LOAN, true));
            items.add(new Item("Loan account statement - last 12 months", DocumentCategory.EXISTING_LOAN, true));
            items.add(new Item("List of documents (LOD) held by existing lender", DocumentCategory.EXISTING_LOAN, true));
            items.add(new Item("Foreclosure letter", DocumentCategory.EXISTING_LOAN, true));
        }

        items.add(new Item("Signed application form", DocumentCategory.OTHER, true));
        items.add(new Item("Processing fee cheque / payment proof", DocumentCategory.OTHER, false));
        return items;
    }
}
