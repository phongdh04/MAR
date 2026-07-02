package vn.mar.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record CreateUserRequest(
        @NotBlank
        @Size(max = 255)
        String fullName,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @Size(max = 50)
        String phone,

        @NotBlank
        @Size(max = 50)
        String role,

        Set<UUID> branchIds,

        @Size(max = 50)
        String status
) {
}
