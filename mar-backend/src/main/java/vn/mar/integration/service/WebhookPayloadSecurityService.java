package vn.mar.integration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.exception.BusinessException;
import vn.mar.integration.config.WebhookProperties;

@Service
public class WebhookPayloadSecurityService {

    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final String HMAC_SHA_256 = "HmacSHA256";
    private static final String SHA_256 = "SHA-256";

    private final ObjectMapper objectMapper;
    private final WebhookProperties webhookProperties;

    public WebhookPayloadSecurityService(ObjectMapper objectMapper, WebhookProperties webhookProperties) {
        this.objectMapper = objectMapper;
        this.webhookProperties = webhookProperties;
    }

    public String payloadHash(JsonNode payload) {
        return hex(sha256(canonicalBytes(payload)));
    }

    public String signature(JsonNode payload) {
        byte[] signature = hmacSha256(canonicalBytes(payload), requireSecret());
        return SIGNATURE_PREFIX + hex(signature);
    }

    public boolean verifySignature(JsonNode payload, String signatureHeader) {
        if (!StringUtils.hasText(signatureHeader)) {
            return false;
        }
        String supplied = normalizeSignature(signatureHeader);
        String expected = normalizeSignature(signature(payload));
        return MessageDigest.isEqual(
                supplied.getBytes(StandardCharsets.US_ASCII),
                expected.getBytes(StandardCharsets.US_ASCII)
        );
    }

    private byte[] canonicalBytes(JsonNode payload) {
        try {
            return objectMapper.writeValueAsBytes(canonicalize(payload));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Webhook payload cannot be serialized");
        }
    }

    private JsonNode canonicalize(JsonNode node) {
        if (node == null || node.isNull() || node.isValueNode()) {
            return node;
        }
        if (node.isArray()) {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            node.forEach(item -> arrayNode.add(canonicalize(item)));
            return arrayNode;
        }
        ObjectNode objectNode = objectMapper.createObjectNode();
        Map<String, JsonNode> sortedFields = new TreeMap<>();
        for (Map.Entry<String, JsonNode> field : node.properties()) {
            sortedFields.put(field.getKey(), canonicalize(field.getValue()));
        }
        sortedFields.forEach(objectNode::set);
        return objectNode;
    }

    private byte[] sha256(byte[] payload) {
        try {
            return MessageDigest.getInstance(SHA_256).digest(payload);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }

    private byte[] hmacSha256(byte[] payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));
            return mac.doFinal(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 algorithm is not available", exception);
        }
    }

    private String normalizeSignature(String value) {
        String signature = value.trim();
        if (signature.startsWith(SIGNATURE_PREFIX)) {
            signature = signature.substring(SIGNATURE_PREFIX.length());
        }
        return signature.toLowerCase();
    }

    private String requireSecret() {
        if (!StringUtils.hasText(webhookProperties.signatureSecret())) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Webhook signature secret is not configured");
        }
        return webhookProperties.signatureSecret();
    }

    private String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
