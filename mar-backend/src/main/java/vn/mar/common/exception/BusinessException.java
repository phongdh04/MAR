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

    public static BusinessException notFound(String field, String message) {
        return detailed(ErrorCode.RESOURCE_NOT_FOUND, message, field, "NOT_FOUND");
    }

    public static BusinessException forbidden(String field, String code, String message) {
        return detailed(ErrorCode.PERMISSION_DENIED, message, field, code);
    }

    public static BusinessException detailed(ErrorCode errorCode, String message, String field, String code) {
        return new BusinessException(errorCode, message, List.of(ErrorDetail.of(field, code, message)));
    }
}
