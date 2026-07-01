package vn.mar.security.jwt;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mar.security.jwt")
public record JwtProperties(
        String issuer,
        String secret,
        Duration accessTokenExpiration
) {

    private static final int MINIMUM_HMAC_SECRET_LENGTH = 32;

    public JwtProperties {
        if (issuer == null || issuer.isBlank()) {
            issuer = "mar-api";
        }
        if (secret == null || secret.length() < MINIMUM_HMAC_SECRET_LENGTH) {
            throw new IllegalStateException("mar.security.jwt.secret must contain at least 32 characters");
        }
        if (accessTokenExpiration == null || accessTokenExpiration.isZero() || accessTokenExpiration.isNegative()) {
            accessTokenExpiration = Duration.ofHours(1);
        }
    }
}
