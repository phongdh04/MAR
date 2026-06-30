package vn.mar.common.dto;

public record ApiResponse<T>(
        T data,
        ApiMeta meta
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, ApiMeta.current());
    }
}
