package vn.mar.customer.api;

import java.util.UUID;

public record UnmergeCustomerCommand(
        UUID customerId,
        UUID mergeId,
        String reason
) {
}
