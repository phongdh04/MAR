package vn.mar.common.exception;

import java.util.List;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.error.ErrorDetail;

public class ValidationException extends MarException {

    public ValidationException(String message, List<ErrorDetail> details) {
        super(ErrorCode.VALIDATION_ERROR, message, details);
    }
}
