package vn.mar.catalog.dto.request;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record UpdateCourseRequest(
        UUID programId,

        @Size(max = 50)
        String courseCode,

        @Size(max = 255)
        String courseName,

        @Size(max = 128)
        String level,

        BigDecimal tuitionGross,

        @Size(max = 10)
        String currency,

        @Size(max = 50)
        String status,

        @Size(max = 500)
        String reason
) {
}
