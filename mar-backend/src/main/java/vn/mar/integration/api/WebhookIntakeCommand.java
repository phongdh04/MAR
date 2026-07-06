package vn.mar.integration.api;

import com.fasterxml.jackson.databind.JsonNode;
import vn.mar.lead.model.LeadSourceType;

public record WebhookIntakeCommand(
        LeadSourceType sourceType,
        String tenantKey,
        String signature,
        String idempotencyKey,
        JsonNode payload
) {
}
