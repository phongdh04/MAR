package vn.mar.catalog.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CourseDetailResponse(
        UUID courseId,
        UUID tenantId,
        UUID programId,
        String courseCode,
        String courseName,
        String level,
        BigDecimal tuitionGross,
        String currency,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
