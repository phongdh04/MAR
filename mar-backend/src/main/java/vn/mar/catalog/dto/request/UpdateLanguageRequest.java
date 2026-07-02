package vn.mar.catalog.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateLanguageRequest(
        @Size(max = 50)
        String code,

        @Size(max = 255)
        String name,

        @Size(max = 50)
        String status,

        @Size(max = 500)
        String reason
) {
}
