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
    public static final String LANGUAGE_CREATED = "LANGUAGE_CREATED";
    public static final String LANGUAGE_UPDATED = "LANGUAGE_UPDATED";
    public static final String LANGUAGE_STATUS_CHANGED = "LANGUAGE_STATUS_CHANGED";
    public static final String PROGRAM_CREATED = "PROGRAM_CREATED";
    public static final String PROGRAM_UPDATED = "PROGRAM_UPDATED";
    public static final String PROGRAM_STATUS_CHANGED = "PROGRAM_STATUS_CHANGED";
    public static final String COURSE_CREATED = "COURSE_CREATED";
    public static final String COURSE_UPDATED = "COURSE_UPDATED";
    public static final String COURSE_STATUS_CHANGED = "COURSE_STATUS_CHANGED";
    public static final String IMPORT_BATCH_CREATED = "IMPORT_BATCH_CREATED";
    public static final String DUPLICATE_CASE_CREATED = "DUPLICATE_CASE_CREATED";
    public static final String DUPLICATE_CASE_RESOLVED = "DUPLICATE_CASE_RESOLVED";

    private AuditActions() {
    }
}
