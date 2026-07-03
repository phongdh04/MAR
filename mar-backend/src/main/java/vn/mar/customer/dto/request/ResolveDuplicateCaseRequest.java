package vn.mar.customer.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record ResolveDuplicateCaseRequest(
        @NotBlank
        String action,
        UUID targetCustomerId,
        @NotBlank
        String reason
) {
}
