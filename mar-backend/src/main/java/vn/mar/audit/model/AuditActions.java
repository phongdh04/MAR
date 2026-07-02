package vn.mar.audit.model;

public final class AuditActions {

    public static final String TENANT_CREATED = "TENANT_CREATED";
    public static final String TENANT_UPDATED = "TENANT_UPDATED";
    public static final String TENANT_STATUS_CHANGED = "TENANT_STATUS_CHANGED";
    public static final String BRANCH_CREATED = "BRANCH_CREATED";
    public static final String BRANCH_UPDATED = "BRANCH_UPDATED";
    public static final String BRANCH_STATUS_CHANGED = "BRANCH_STATUS_CHANGED";
    public static final String USER_CREATED = "USER_CREATED";
    public static final String USER_UPDATED = "USER_UPDATED";
    public static final String USER_STATUS_CHANGED = "USER_STATUS_CHANGED";
    public static final String USER_BRANCH_ASSIGNED = "USER_BRANCH_ASSIGNED";
    public static final String PERMISSION_MATRIX_UPDATED = "PERMISSION_MATRIX_UPDATED";

    private AuditActions() {
    }
}
