package vn.mar.catalog.dto.request;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateProgramRequest(
        UUID languageId,

        @Size(max = 50)
        String programCode,

        @Size(max = 255)
        String programName,

        @Size(max = 128)
        String examTrack,

        @Size(max = 50)
        String status,

        @Size(max = 500)
        String reason
) {
}
