package vn.mar.security.context;

import org.springframework.security.core.AuthenticatedPrincipal;

public record CurrentUserPrincipal(CurrentUser currentUser) implements AuthenticatedPrincipal {

    @Override
    public String getName() {
        return currentUser.actorId().toString();
    }
}
