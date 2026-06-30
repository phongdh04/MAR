package vn.mar.common.dto;

import vn.mar.common.logging.RequestContext;

public record ApiMeta(
        String requestId
) {

    private static final String MISSING_REQUEST_ID = "missing-request-id";

    public static ApiMeta current() {
        return new ApiMeta(RequestContext.currentRequestId().orElse(MISSING_REQUEST_ID));
    }
}
