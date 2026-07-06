package vn.mar.common.tenant;

import java.util.UUID;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.exception.BusinessException;
import vn.mar.security.context.CurrentUser;

public final class TenantContext {

    private TenantContext() {
    }

    public static UUID requireTenantId(CurrentUser actor) {
        if (actor == null || actor.tenantId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Tenant context is required");
        }
        return actor.tenantId();
    }
}
