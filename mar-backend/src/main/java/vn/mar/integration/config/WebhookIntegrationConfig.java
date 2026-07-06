package vn.mar.integration.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WebhookProperties.class)
public class WebhookIntegrationConfig {
}
