package vn.mar.common.error;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ErrorResponseFactory {

    public ErrorResponse create(ErrorCode errorCode) {
        return ErrorResponse.of(errorCode);
    }

    public ErrorResponse create(ErrorCode errorCode, String message) {
        return ErrorResponse.of(errorCode, message, List.of());
    }

    public ErrorResponse create(ErrorCode errorCode, String message, List<ErrorDetail> details) {
        return ErrorResponse.of(errorCode, message, details);
    }
}
