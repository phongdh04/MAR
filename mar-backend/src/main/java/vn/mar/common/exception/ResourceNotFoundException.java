package vn.mar.common.exception;

import vn.mar.common.error.ErrorCode;

public class ResourceNotFoundException extends MarException {

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }
}
