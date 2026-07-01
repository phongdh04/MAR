package vn.mar.security.context;

import java.util.UUID;

public interface CurrentUserContext {

    CurrentUser currentUser();

    default UUID currentTenantId() {
        return currentUser().tenantId();
    }

    default UUID currentActorId() {
        return currentUser().actorId();
    }
}
