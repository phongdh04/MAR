package vn.mar.authz.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.exception.BusinessException;
import vn.mar.security.context.CurrentUser;

class PermissionGuardTest {

    private final PermissionGuard permissionGuard = new PermissionGuard();

    @Test
    void requirePermission_whenActorHasPermission_shouldAllow() {
        permissionGuard.requirePermission(actor("lead.view"), "lead.view", "LEAD_VIEW_DENIED", "Permission is required");
    }

    @Test
    void requireAnyPermission_whenActorHasOnePermission_shouldAllow() {
        permissionGuard.requireAnyPermission(
                actor("lead.view"),
                List.of("lead.view", "opportunity.update"),
                "OPPORTUNITY_VIEW_DENIED",
                "Permission is required"
        );
    }

    @Test
    void requireAnyPermission_whenActorMissingPermission_shouldReject() {
        assertThatThrownBy(() -> permissionGuard.requireAnyPermission(
                actor("activity.view"),
                List.of("lead.view", "opportunity.update"),
                "OPPORTUNITY_VIEW_DENIED",
                "Permission is required"
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    void requirePermission_whenActorMissing_shouldReject() {
        assertThatThrownBy(() -> permissionGuard.requirePermission(null, "lead.view", "LEAD_VIEW_DENIED", "Permission is required"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PERMISSION_DENIED);
    }

    private CurrentUser actor(String... permissions) {
        return new CurrentUser(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                "ADMIN",
                Set.of(permissions),
                "req_permission_guard_unit"
        );
    }
}
