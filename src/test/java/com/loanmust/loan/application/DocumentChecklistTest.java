package com.loanmust.loan.application;

import com.loanmust.loan.customer.EmploymentType;
import com.loanmust.loan.lender.LoanType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentChecklistTest {

    private static boolean has(List<DocumentChecklist.Item> items, String fragment) {
        return items.stream().anyMatch(i -> i.name().contains(fragment));
    }

    @Test
    void salariedHomeLoanNeedsSlipsAndTitle() {
        List<DocumentChecklist.Item> items = DocumentChecklist.forProfile(EmploymentType.SALARIED, LoanType.HOME_LOAN, false, false);
        assertTrue(has(items, "Salary slips"));
        assertTrue(has(items, "Title deed"));
        assertTrue(has(items, "Encumbrance"));
        assertFalse(has(items, "ITR"));
    }

    @Test
    void builderProjectSwapsTitleChainForAllotment() {
        List<DocumentChecklist.Item> items = DocumentChecklist.forProfile(EmploymentType.SALARIED, LoanType.HOME_LOAN, true, false);
        assertTrue(has(items, "Allotment letter"));
        assertFalse(has(items, "mother-deed"));
    }

    @Test
    void selfEmployedBalanceTransferNeedsItrAndForeclosure() {
        List<DocumentChecklist.Item> items = DocumentChecklist.forProfile(EmploymentType.SELF_EMPLOYED_BUSINESS, LoanType.BALANCE_TRANSFER, false, true);
        assertTrue(has(items, "ITR"));
        assertTrue(has(items, "Foreclosure"));
        assertTrue(has(items, "Co-applicant"));
    }

    @Test
    void personalLoanHasNoPropertyPapers() {
        List<DocumentChecklist.Item> items = DocumentChecklist.forProfile(EmploymentType.SALARIED, LoanType.PERSONAL_LOAN, false, false);
        assertFalse(items.stream().anyMatch(i -> i.category() == DocumentCategory.PROPERTY));
    }
}
