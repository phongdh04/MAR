package vn.mar.integration.api;

import java.util.UUID;
import vn.mar.common.pagination.PageResponse;

public interface IntegrationEventQueryService {

    PageResponse<IntegrationEventSnapshot> searchEvents(IntegrationEventSearchCommand command);

    IntegrationEventSnapshot getEvent(UUID eventId);
}
