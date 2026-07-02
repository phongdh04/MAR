package vn.mar.catalog.dto.request;

import java.util.UUID;

public record ProgramSearchRequest(
        UUID languageId,
        String keyword,
        String status,
        Integer page,
        Integer size
) {
}
