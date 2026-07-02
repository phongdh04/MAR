package vn.mar.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateCourseRequest(
        @NotNull
        UUID programId,

        @Size(max = 50)
        String courseCode,

        @NotBlank
        @Size(max = 255)
        String courseName,

        @Size(max = 128)
        String level,

        BigDecimal tuitionGross,

        @Size(max = 10)
        String currency,

        @Size(max = 50)
        String status
) {
}
