package vn.mar.catalog.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ProgramDetailResponse(
        UUID programId,
        UUID tenantId,
        UUID languageId,
        String programCode,
        String programName,
        String examTrack,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
