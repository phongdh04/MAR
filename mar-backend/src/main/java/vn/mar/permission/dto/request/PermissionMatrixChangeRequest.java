package vn.mar.permission.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PermissionMatrixChangeRequest(
        @NotBlank
        @Size(max = 50)
        String role,

        @NotBlank
        @Size(max = 100)
        String functionCode,

        @NotBlank
        @Size(max = 50)
        String accessLevel,

        @Size(max = 50)
        String scope
) {
}
