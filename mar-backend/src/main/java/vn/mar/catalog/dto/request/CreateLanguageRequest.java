package vn.mar.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLanguageRequest(
        @Size(max = 50)
        String code,

        @NotBlank
        @Size(max = 255)
        String name,

        @Size(max = 50)
        String status
) {
}
