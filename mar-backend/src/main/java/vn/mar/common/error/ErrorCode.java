package vn.mar.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    UNAUTHENTICATED("UNAUTHENTICATED", HttpStatus.UNAUTHORIZED, "Authentication is required"),
    PERMISSION_DENIED("PERMISSION_DENIED", HttpStatus.FORBIDDEN, "Permission denied"),
    VALIDATION_ERROR("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Validation failed"),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "Resource not found"),
    DUPLICATE_RESOURCE("DUPLICATE_RESOURCE", HttpStatus.CONFLICT, "Resource already exists"),
    CONFLICT("CONFLICT", HttpStatus.CONFLICT, "State conflict"),
    TENANT_INACTIVE("TENANT_INACTIVE", HttpStatus.CONFLICT, "Tenant is inactive"),
    DUPLICATE_TENANT_CODE("DUPLICATE_TENANT_CODE", HttpStatus.CONFLICT, "Tenant code already exists"),
    DUPLICATE_ACTIVE_BRANCH("DUPLICATE_ACTIVE_BRANCH", HttpStatus.CONFLICT, "Active branch already exists"),
    DUPLICATE_USER_EMAIL("DUPLICATE_USER_EMAIL", HttpStatus.CONFLICT, "User email already exists"),
    INVALID_PARENT_STATUS("INVALID_PARENT_STATUS", HttpStatus.UNPROCESSABLE_ENTITY, "Parent status is invalid"),
    NEGATIVE_TUITION("NEGATIVE_TUITION", HttpStatus.BAD_REQUEST, "Tuition must be non-negative"),
    INVALID_PERMISSION_GUARDRAIL("INVALID_PERMISSION_GUARDRAIL", HttpStatus.UNPROCESSABLE_ENTITY, "Permission guardrail is invalid"),
    IMPORT_BATCH_NOT_FOUND("IMPORT_BATCH_NOT_FOUND", HttpStatus.NOT_FOUND, "Import batch not found"),
    IMPORT_FILE_INVALID("IMPORT_FILE_INVALID", HttpStatus.BAD_REQUEST, "Import file is invalid"),
    IMPORT_ROW_VALIDATION_ERROR("IMPORT_ROW_VALIDATION_ERROR", HttpStatus.UNPROCESSABLE_ENTITY, "Import row validation failed"),
    UNMERGE_NOT_ALLOWED("UNMERGE_NOT_ALLOWED", HttpStatus.UNPROCESSABLE_ENTITY, "Unmerge is not allowed"),
    BUSINESS_RULE_VIOLATION("BUSINESS_RULE_VIOLATION", HttpStatus.UNPROCESSABLE_ENTITY, "Business rule violation"),
    INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public String code() {
        return code;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
