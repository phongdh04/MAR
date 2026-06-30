package vn.mar.common.exception;

import java.util.List;
import java.util.Objects;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.error.ErrorDetail;

public abstract class MarException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<ErrorDetail> details;

    protected MarException(ErrorCode errorCode, String message) {
        this(errorCode, message, List.of());
    }

    protected MarException(ErrorCode errorCode, String message, List<ErrorDetail> details) {
        super(Objects.requireNonNull(message, "message must not be null"));
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public List<ErrorDetail> getDetails() {
        return details;
    }
}
