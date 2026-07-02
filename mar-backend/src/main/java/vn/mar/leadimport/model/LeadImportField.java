package vn.mar.leadimport.model;

import java.util.Arrays;
import java.util.Optional;

public enum LeadImportField {

    FULL_NAME("full_name", false),
    PHONE("phone", true),
    EMAIL("email", true),
    ZALO_ID("zalo_id", true),
    SOURCE("source", false),
    CAMPAIGN("campaign", false),
    LANGUAGE_CODE("language_code", false),
    PROGRAM_CODE("program_code", false),
    BRANCH_CODE("branch_code", false);

    private final String code;
    private final boolean contactIdentifier;

    LeadImportField(String code, boolean contactIdentifier) {
        this.code = code;
        this.contactIdentifier = contactIdentifier;
    }

    public String code() {
        return code;
    }

    public boolean contactIdentifier() {
        return contactIdentifier;
    }

    public static Optional<LeadImportField> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(field -> field.code.equals(code.trim()))
                .findFirst();
    }
}
