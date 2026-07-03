package vn.mar.lead.model;

public enum LeadContactField {

    PHONE("phone"),
    EMAIL("email"),
    ZALO_ID("zalo_id"),
    CONTACT_IDENTIFIER("contact_identifier");

    private final String code;

    LeadContactField(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
