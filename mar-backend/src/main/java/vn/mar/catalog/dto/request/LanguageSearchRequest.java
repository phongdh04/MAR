package vn.mar.catalog.dto.request;

public record LanguageSearchRequest(
        String keyword,
        String status,
        Integer page,
        Integer size
) {
}
