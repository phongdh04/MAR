package vn.mar.common.error;

public record ErrorDetail(
        String field,
        String code,
        String message
) {

    public static ErrorDetail of(String field, String code, String message) {
        return new ErrorDetail(field, code, message);
    }
}
