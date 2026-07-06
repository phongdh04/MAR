package vn.mar.audit.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AuditPayloadSanitizer {

    private static final String REDACTED = "[REDACTED]";

    public Map<String, Object> sanitize(Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        payload.forEach((key, value) -> sanitized.put(key, sanitizeEntry(key, value)));
        return sanitized;
    }

    private Object sanitizeEntry(String key, Object value) {
        if (isSensitiveKey(key)) {
            return REDACTED;
        }
        return sanitizeValue(value);
    }

    private Object sanitizeValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            map.forEach((nestedKey, nestedValue) -> {
                String key = String.valueOf(nestedKey);
                sanitized.put(key, sanitizeEntry(key, nestedValue));
            });
            return sanitized;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(this::sanitizeValue)
                    .toList();
        }
        if (value instanceof Object[] array) {
            return Arrays.stream(array)
                    .map(this::sanitizeValue)
                    .toList();
        }
        return value;
    }

    private boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String normalizedKey = key.toLowerCase(Locale.ROOT);
        return normalizedKey.contains("password")
                || normalizedKey.contains("token")
                || normalizedKey.contains("secret")
                || normalizedKey.contains("otp")
                || normalizedKey.contains("authorization")
                || normalizedKey.equals("raw_row")
                || normalizedKey.equals("raw_import_row")
                || normalizedKey.equals("full_import_row")
                || normalizedKey.contains("full_import_row");
    }
}
