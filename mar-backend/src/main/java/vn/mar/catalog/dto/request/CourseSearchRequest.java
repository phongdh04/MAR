package vn.mar.catalog.dto.request;

import java.util.UUID;

public record CourseSearchRequest(
        UUID programId,
        String keyword,
        String status,
        Integer page,
        Integer size
) {
}
