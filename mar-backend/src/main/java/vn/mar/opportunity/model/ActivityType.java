package vn.mar.opportunity.model;

public enum ActivityType {

    CALL(true),
    ZALO(true),
    SMS(true),
    EMAIL(true),
    NOTE(false),
    MEETING(true),
    SYSTEM(false);

    private final boolean outbound;

    ActivityType(boolean outbound) {
        this.outbound = outbound;
    }

    public boolean isOutbound() {
        return outbound;
    }

    public boolean requiresResult() {
        return outbound;
    }
}
