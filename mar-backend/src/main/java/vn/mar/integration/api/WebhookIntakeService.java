package vn.mar.integration.api;

public interface WebhookIntakeService {

    WebhookIntakeSnapshot receiveWebsiteLead(WebhookIntakeCommand command);
}
