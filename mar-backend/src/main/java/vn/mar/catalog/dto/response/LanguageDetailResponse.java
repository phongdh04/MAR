package vn.mar.catalog.dto.response;

import java.time.Instant;
import java.util.UUID;

public record LanguageDetailResponse(
        UUID languageId,
        UUID tenantId,
        String code,
        String name,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
