package vn.mar.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String TEST_SECRET = "12345678901234567890123456789012";

    @Test
    void createAndParse_whenValidClaims_shouldRoundTripIdentity() {
        JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(
                "mar-api",
                TEST_SECRET,
                Duration.ofMinutes(30)
        ));
        UUID actorId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID tenantId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

        JwtToken token = provider.createAccessToken(actorId, tenantId, "ADMIN");
        JwtClaims claims = provider.parse(token.token());

        assertThat(claims.actorId()).isEqualTo(actorId);
        assertThat(claims.tenantId()).isEqualTo(tenantId);
        assertThat(claims.roleCode()).isEqualTo("ADMIN");
        assertThat(claims.expiresAt()).isAfter(claims.issuedAt());
    }

    @Test
    void parse_whenTokenInvalid_shouldReject() {
        JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(
                "mar-api",
                TEST_SECRET,
                Duration.ofMinutes(30)
        ));

        assertThatThrownBy(() -> provider.parse("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parse_whenRequiredClaimMissing_shouldReject() {
        JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(
                "mar-api",
                TEST_SECRET,
                Duration.ofMinutes(30)
        ));
        Instant now = Instant.now();
        String token = Jwts.builder()
                .issuer("mar-api")
                .subject("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(30))))
                .claim("role_code", "ADMIN")
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> provider.parse(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("tenant_id");
    }
}
