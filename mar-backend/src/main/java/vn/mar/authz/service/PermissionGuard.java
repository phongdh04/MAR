package vn.mar.authz.service;

import java.util.Collection;
import org.springframework.stereotype.Component;
import vn.mar.common.exception.BusinessException;
import vn.mar.security.context.CurrentUser;

@Component
public class PermissionGuard {

    public void requirePermission(CurrentUser actor, String permissionCode, String detailCode, String message) {
        if (actor == null || !actor.hasPermission(permissionCode)) {
            throw BusinessException.forbidden("permission", detailCode, message);
        }
    }

    public void requireAnyPermission(CurrentUser actor, Collection<String> permissionCodes, String detailCode, String message) {
        if (actor == null || permissionCodes == null || permissionCodes.stream().noneMatch(actor::hasPermission)) {
            throw BusinessException.forbidden("permission", detailCode, message);
        }
    }
}
