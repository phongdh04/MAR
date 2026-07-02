package vn.mar.permission.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdatePermissionMatrixRequest(
        @NotEmpty
        List<@Valid PermissionMatrixChangeRequest> changes,

        @NotBlank
        @Size(max = 500)
        String reason
) {
}
