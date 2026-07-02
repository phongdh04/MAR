package vn.mar.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record UpdateUserRequest(
        @Size(max = 255)
        String fullName,

        @Email
        @Size(max = 255)
        String email,

        @Size(max = 50)
        String phone,

        @Size(max = 50)
        String role,

        Set<UUID> branchIds,

        @Size(max = 50)
        String status,

        @Size(max = 500)
        String reason
) {
}
