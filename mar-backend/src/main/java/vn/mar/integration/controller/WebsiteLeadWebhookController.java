package vn.mar.integration.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.mar.common.dto.ApiResponse;
import vn.mar.integration.api.WebhookIntakeCommand;
import vn.mar.integration.api.WebhookIntakeService;
import vn.mar.integration.api.WebhookIntakeSnapshot;
import vn.mar.integration.dto.response.WebhookIntakeResponse;
import vn.mar.integration.mapper.IntegrationEventMapper;
import vn.mar.integration.model.IntegrationEventStatus;
import vn.mar.lead.model.LeadSourceType;

@RestController
@RequestMapping("/api/v1/webhooks/leads")
public class WebsiteLeadWebhookController {

    public static final String TENANT_KEY_HEADER = "X-Mar-Tenant-Key";
    public static final String SIGNATURE_HEADER = "X-Mar-Signature";
    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final WebhookIntakeService webhookIntakeService;
    private final IntegrationEventMapper integrationEventMapper;

    public WebsiteLeadWebhookController(
            WebhookIntakeService webhookIntakeService,
            IntegrationEventMapper integrationEventMapper) {
        this.webhookIntakeService = webhookIntakeService;
        this.integrationEventMapper = integrationEventMapper;
    }

    @PostMapping("/website")
    public ResponseEntity<ApiResponse<WebhookIntakeResponse>> receiveWebsiteLead(
            @RequestHeader(value = TENANT_KEY_HEADER, required = false) String tenantKey,
            @RequestHeader(value = SIGNATURE_HEADER, required = false) String signature,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @RequestBody(required = false) JsonNode payload) {
        WebhookIntakeSnapshot snapshot = webhookIntakeService.receiveWebsiteLead(new WebhookIntakeCommand(
                LeadSourceType.WEBSITE_FORM,
                tenantKey,
                signature,
                idempotencyKey,
                payload
        ));
        HttpStatus status = snapshot.status() == IntegrationEventStatus.DUPLICATE
                ? HttpStatus.OK
                : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status)
                .body(ApiResponse.success(integrationEventMapper.toResponse(snapshot)));
    }
}
