package vn.mar.authz.service;

import java.util.Collection;
import org.springframework.stereotype.Component;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.error.ErrorDetail;
import vn.mar.common.exception.BusinessException;
import vn.mar.security.context.CurrentUser;

@Component
public class PermissionGuard {

    public void requirePermission(CurrentUser actor, String permissionCode, String detailCode, String message) {
        if (actor == null || !actor.hasPermission(permissionCode)) {
            throw BusinessException.forbidden("permission", detailCode, message);
        }
    }

    public void requirePermission(CurrentUser actor, String permissionCode, String detailCode, String detailMessage, String errorMessage) {
        if (actor == null || !actor.hasPermission(permissionCode)) {
            throw new BusinessException(
                    ErrorCode.PERMISSION_DENIED,
                    errorMessage,
                    java.util.List.of(ErrorDetail.of("permission", detailCode, detailMessage))
            );
        }
    }

    public void requireAnyPermission(CurrentUser actor, Collection<String> permissionCodes, String detailCode, String message) {
        if (actor == null || permissionCodes == null || permissionCodes.stream().noneMatch(actor::hasPermission)) {
            throw BusinessException.forbidden("permission", detailCode, message);
        }
    }
}
