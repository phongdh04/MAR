package vn.mar.branch.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBranchRequest(
        @Size(max = 50)
        String branchCode,

        @NotBlank
        @Size(max = 255)
        String branchName,

        @Size(max = 100)
        String city,

        @Size(max = 50)
        String phoneNumber,

        @Size(max = 1000)
        String address,

        @Size(max = 50)
        String status
) {
}
