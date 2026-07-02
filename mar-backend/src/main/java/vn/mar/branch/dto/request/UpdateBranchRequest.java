package vn.mar.branch.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateBranchRequest(
        @Size(max = 255)
        String branchName,

        @Size(max = 100)
        String city,

        @Size(max = 50)
        String phoneNumber,

        @Size(max = 1000)
        String address,

        @Size(max = 50)
        String status,

        @Size(max = 500)
        String reason
) {
}
