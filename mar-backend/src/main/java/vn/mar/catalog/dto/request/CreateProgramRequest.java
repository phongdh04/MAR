package vn.mar.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateProgramRequest(
        @NotNull
        UUID languageId,

        @Size(max = 50)
        String programCode,

        @NotBlank
        @Size(max = 255)
        String programName,

        @Size(max = 128)
        String examTrack,

        @Size(max = 50)
        String status
) {
}
