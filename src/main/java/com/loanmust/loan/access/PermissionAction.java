package com.loanmust.loan.access;

/** The columns of the permission matrix — what a role may do on a screen. */
public enum PermissionAction {
    VIEW("View"),
    ADD("Add"),
    EDIT("Edit"),
    DELETE("Delete"),
    EXPORT("Export"),
    PRINT("Print"),
    APPROVE("Approve");

    private final String label;

    PermissionAction(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
