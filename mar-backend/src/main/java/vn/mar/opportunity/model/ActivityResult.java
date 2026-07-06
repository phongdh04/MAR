package vn.mar.opportunity.model;

public enum ActivityResult {

    ATTEMPTED,
    CONNECTED,
    REPLIED,
    NO_ANSWER,
    FAILED,
    SENT;

    public boolean isContactSuccess() {
        return this == CONNECTED || this == REPLIED;
    }

    public boolean isFirstResponseCandidate() {
        return this == ATTEMPTED
                || this == CONNECTED
                || this == REPLIED
                || this == NO_ANSWER
                || this == SENT;
    }
}
