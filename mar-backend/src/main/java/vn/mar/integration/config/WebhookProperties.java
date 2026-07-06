package vn.mar.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mar.webhook")
public record WebhookProperties(
        String signatureSecret
) {
}
