package com.loanmust.loan.reports;

import java.util.List;

/**
 * A report as plain data: one shape feeds both the on-screen table and the Excel export,
 * so what a user sees and what they download can never disagree.
 */
public record ReportTable(String title, List<String> headers, List<List<Object>> rows) {

    public boolean isEmpty() {
        return rows == null || rows.isEmpty();
    }
}
