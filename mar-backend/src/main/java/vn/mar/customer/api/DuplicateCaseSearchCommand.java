package vn.mar.customer.api;

public record DuplicateCaseSearchCommand(
        String status,
        String matchType,
        Integer page,
        Integer size
) {
}
