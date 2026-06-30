package vn.mar.common.exception;

import java.util.List;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.error.ErrorDetail;

public class BusinessException extends MarException {

    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public BusinessException(ErrorCode errorCode, String message, List<ErrorDetail> details) {
        super(errorCode, message, details);
    }
}
