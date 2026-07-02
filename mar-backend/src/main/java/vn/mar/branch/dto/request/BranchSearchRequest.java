package vn.mar.branch.dto.request;

public record BranchSearchRequest(
        String keyword,
        String status,
        Integer page,
        Integer size
) {
}
