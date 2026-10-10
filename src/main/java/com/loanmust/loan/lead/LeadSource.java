package com.loanmust.loan.lead;

/** Where the enquiry came from - drives builder-wise and referral-wise reporting. */
public enum LeadSource {
    DIRECT("Direct client"),
    WALK_IN("Walk-in"),
    BUILDER("Builder tie-up"),
    LIC("LIC tie-up"),
    REFERRAL("Referral partner"),
    EXISTING_CUSTOMER("Existing customer"),
    WEBSITE("Website / online"),
    SOCIAL_MEDIA("Social media"),
    CAMPAIGN("Campaign / event");

    private final String label;

    LeadSource(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
