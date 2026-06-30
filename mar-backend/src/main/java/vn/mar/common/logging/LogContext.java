package vn.mar.common.logging;

public final class LogContext {

    private LogContext() {
    }

    public static String requestId() {
        return RequestContext.currentRequestId().orElse("missing-request-id");
    }
}
