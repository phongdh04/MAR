package vn.mar.security.context;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SpringSecurityCurrentUserContext implements CurrentUserContext {

    @Override
    public CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUserPrincipal principal)) {
            throw new AuthenticationCredentialsNotFoundException("Authenticated user context is required");
        }
        return principal.currentUser();
    }
}
