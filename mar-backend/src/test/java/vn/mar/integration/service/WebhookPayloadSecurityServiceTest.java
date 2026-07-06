package vn.mar.integration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.mar.integration.config.WebhookProperties;

class WebhookPayloadSecurityServiceTest {

    private ObjectMapper objectMapper;
    private WebhookPayloadSecurityService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new WebhookPayloadSecurityService(objectMapper, new WebhookProperties("test-secret"));
    }

    @Test
    void payloadHash_whenJsonFieldOrderDiffers_shouldStayStable() throws Exception {
        JsonNode first = objectMapper.readTree("""
                {
                  "external_id": "webform_001",
                  "full_name": "Nguyen Minh A",
                  "utm": {
                    "source": "google",
                    "campaign": "ielts"
                  }
                }
                """);
        JsonNode second = objectMapper.readTree("""
                {
                  "utm": {
                    "campaign": "ielts",
                    "source": "google"
                  },
                  "full_name": "Nguyen Minh A",
                  "external_id": "webform_001"
                }
                """);

        assertThat(service.payloadHash(first)).isEqualTo(service.payloadHash(second));
        assertThat(service.signature(first)).isEqualTo(service.signature(second));
    }
}
