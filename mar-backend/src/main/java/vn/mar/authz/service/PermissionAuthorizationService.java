package vn.mar.authz.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import vn.mar.security.context.CurrentUserPrincipal;

@Service("authz")
public class PermissionAuthorizationService {

    public boolean hasPermission(Authentication authentication, String permissionCode) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !StringUtils.hasText(permissionCode)
                || !(authentication.getPrincipal() instanceof CurrentUserPrincipal principal)) {
            return false;
        }
        return principal.currentUser().hasPermission(permissionCode);
    }

    public void requirePermission(Authentication authentication, String permissionCode) {
        if (!hasPermission(authentication, permissionCode)) {
            throw new AccessDeniedException("Permission is required: " + permissionCode);
        }
    }
}
