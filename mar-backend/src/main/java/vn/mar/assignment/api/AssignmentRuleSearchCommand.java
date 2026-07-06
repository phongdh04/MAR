package vn.mar.assignment.api;

import java.util.UUID;

public record AssignmentRuleSearchCommand(
        String status,
        UUID branchId,
        Integer page,
        Integer size
) {
}
