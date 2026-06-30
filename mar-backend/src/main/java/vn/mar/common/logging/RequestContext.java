package vn.mar.common.logging;

import java.util.Optional;

public final class RequestContext {

    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void setRequestId(String requestId) {
        REQUEST_ID.set(requestId);
    }

    public static Optional<String> currentRequestId() {
        return Optional.ofNullable(REQUEST_ID.get());
    }

    public static void clear() {
        REQUEST_ID.remove();
    }
}
