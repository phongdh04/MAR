package vn.mar.common.cache;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mar.cache.permission-profile")
public record PermissionProfileCacheProperties(
        Duration ttl,
        long maximumSize
) {

    public PermissionProfileCacheProperties {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            ttl = Duration.ofMinutes(10);
        }
        if (maximumSize <= 0) {
            maximumSize = 10_000L;
        }
    }
}
