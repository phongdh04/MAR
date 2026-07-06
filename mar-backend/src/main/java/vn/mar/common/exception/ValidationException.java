package vn.mar.common.exception;

import java.util.List;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.error.ErrorDetail;

public class ValidationException extends MarException {

    public ValidationException(String message, List<ErrorDetail> details) {
        super(ErrorCode.VALIDATION_ERROR, message, details);
    }

    public static ValidationException of(String field, String code, String message) {
        return new ValidationException(
                ErrorCode.VALIDATION_ERROR.defaultMessage(),
                List.of(ErrorDetail.of(field, code, message))
        );
    }
}
