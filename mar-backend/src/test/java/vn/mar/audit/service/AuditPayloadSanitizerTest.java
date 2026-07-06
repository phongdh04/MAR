package vn.mar.audit.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuditPayloadSanitizerTest {

    private final AuditPayloadSanitizer sanitizer = new AuditPayloadSanitizer();

    @Test
    void sanitize_whenPayloadContainsSensitiveKeys_shouldRedactRecursively() {
        Map<String, Object> sanitized = sanitizer.sanitize(Map.of(
                "status", "ACTIVE",
                "password_hash", "hash-value",
                "nested", Map.of(
                        "access_token", "token-value",
                        "phone_number", "0900000000"
                ),
                "rows", List.of(Map.of(
                        "full_import_row", Map.of("name", "raw customer"),
                        "result", "VALID"
                ))
        ));

        assertThat(sanitized).containsEntry("status", "ACTIVE");
        assertThat(sanitized).containsEntry("password_hash", "[REDACTED]");
        Map<?, ?> nested = (Map<?, ?>) sanitized.get("nested");
        assertThat(nested.get("access_token")).isEqualTo("[REDACTED]");
        assertThat(nested.get("phone_number")).isEqualTo("0900000000");
        Map<?, ?> firstRow = (Map<?, ?>) ((List<?>) sanitized.get("rows")).getFirst();
        assertThat(firstRow.get("full_import_row")).isEqualTo("[REDACTED]");
        assertThat(firstRow.get("result")).isEqualTo("VALID");
    }
}
