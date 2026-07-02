package vn.mar.user.dto.request;

public record UserSearchRequest(
        String keyword,
        String role,
        String status,
        Integer page,
        Integer size
) {
}
