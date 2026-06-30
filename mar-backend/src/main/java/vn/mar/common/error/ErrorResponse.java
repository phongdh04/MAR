package vn.mar.common.error;

import java.util.List;
import vn.mar.common.dto.ApiMeta;

public record ErrorResponse(
        ErrorBody error,
        ApiMeta meta
) {

    public static ErrorResponse of(ErrorCode errorCode, String message, List<ErrorDetail> details) {
        return new ErrorResponse(
                new ErrorBody(errorCode.code(), message, details),
                ApiMeta.current()
        );
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, errorCode.defaultMessage(), List.of());
    }
}
