package vn.mar.common.health;

public record HealthResponse(
        String status,
        String application,
        String profile
) {
}
