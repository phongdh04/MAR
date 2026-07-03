package vn.mar.permission.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import vn.mar.authz.model.PermissionAccessLevel;
import vn.mar.authz.model.PermissionCodes;
import vn.mar.authz.model.PermissionProfile;
import vn.mar.authz.model.PermissionScope;
import vn.mar.role.model.RoleCode;

final class PermissionMatrixDefaultPolicy {

    private static final Map<String, DefaultPermission> DEFAULTS = buildDefaults();

    private PermissionMatrixDefaultPolicy() {
    }

    static List<PermissionProfile> createDefaultProfiles(
            UUID tenantId,
            UUID actorId,
            Instant now,
            Collection<String> activeFunctionCodes) {
        List<String> orderedFunctionCodes = activeFunctionCodes.stream()
                .sorted()
                .toList();
        List<PermissionProfile> profiles = new ArrayList<>();
        for (RoleCode roleCode : RoleCode.values()) {
            for (String functionCode : orderedFunctionCodes) {
                DefaultPermission permission = DEFAULTS.getOrDefault(
                        key(roleCode.name(), functionCode),
                        new DefaultPermission(PermissionAccessLevel.NONE, PermissionScope.NONE)
                );
                profiles.add(PermissionProfile.create(
                        UUID.randomUUID(),
                        tenantId,
                        roleCode.name(),
                        functionCode,
                        permission.accessLevel(),
                        permission.scope(),
                        actorId,
                        now
                ));
            }
        }
        return profiles;
    }

    private static Map<String, DefaultPermission> buildDefaults() {
        Map<String, DefaultPermission> defaults = new HashMap<>();
        grant(defaults, RoleCode.ADMIN, PermissionCodes.TENANT_VIEW, PermissionAccessLevel.VIEW, PermissionScope.TENANT);
        grant(defaults, RoleCode.ADMIN, PermissionCodes.TENANT_MANAGE, PermissionAccessLevel.MANAGE, PermissionScope.TENANT);
        grant(defaults, RoleCode.ADMIN, PermissionCodes.BRANCH_VIEW, PermissionAccessLevel.VIEW, PermissionScope.TENANT);
        grant(defaults, RoleCode.ADMIN, PermissionCodes.BRANCH_MANAGE, PermissionAccessLevel.MANAGE, PermissionScope.TENANT);
        grant(defaults, RoleCode.ADMIN, PermissionCodes.USER_VIEW, PermissionAccessLevel.VIEW, PermissionScope.TENANT);
        grant(defaults, RoleCode.ADMIN, PermissionCodes.USER_MANAGE, PermissionAccessLevel.MANAGE, PermissionScope.TENANT);
        grant(defaults, RoleCode.ADMIN, PermissionCodes.PERMISSION_VIEW, PermissionAccessLevel.VIEW, PermissionScope.TENANT);
        grant(defaults, RoleCode.ADMIN, PermissionCodes.PERMISSION_MANAGE, PermissionAccessLevel.MANAGE, PermissionScope.TENANT);
        grant(defaults, RoleCode.ADMIN, PermissionCodes.CATALOG_VIEW, PermissionAccessLevel.VIEW, PermissionScope.TENANT);
        grant(defaults, RoleCode.ADMIN, PermissionCodes.CATALOG_MANAGE, PermissionAccessLevel.MANAGE, PermissionScope.TENANT);
        grant(defaults, RoleCode.ADMIN, PermissionCodes.IMPORT_VIEW, PermissionAccessLevel.VIEW, PermissionScope.TENANT);
        grant(defaults, RoleCode.ADMIN, PermissionCodes.IMPORT_MANAGE, PermissionAccessLevel.MANAGE, PermissionScope.TENANT);
        grant(defaults, RoleCode.ADMIN, PermissionCodes.LEAD_VIEW, PermissionAccessLevel.VIEW, PermissionScope.TENANT);
        grant(defaults, RoleCode.ADMIN, PermissionCodes.DUPLICATE_MANAGE, PermissionAccessLevel.MANAGE, PermissionScope.TENANT);
        grant(defaults, RoleCode.ADMIN, PermissionCodes.CUSTOMER_MERGE, PermissionAccessLevel.MANAGE, PermissionScope.TENANT);
        grant(defaults, RoleCode.ADMIN, PermissionCodes.AUDIT_VIEW, PermissionAccessLevel.VIEW, PermissionScope.TENANT);

        grant(defaults, RoleCode.CEO, PermissionCodes.TENANT_VIEW, PermissionAccessLevel.VIEW, PermissionScope.TENANT);
        grant(defaults, RoleCode.CEO, PermissionCodes.BRANCH_VIEW, PermissionAccessLevel.VIEW, PermissionScope.TENANT);
        grant(defaults, RoleCode.CEO, PermissionCodes.USER_VIEW, PermissionAccessLevel.VIEW, PermissionScope.TENANT);
        grant(defaults, RoleCode.CEO, PermissionCodes.PERMISSION_VIEW, PermissionAccessLevel.VIEW, PermissionScope.TENANT);
        grant(defaults, RoleCode.CEO, PermissionCodes.CATALOG_VIEW, PermissionAccessLevel.VIEW, PermissionScope.TENANT);
        grant(defaults, RoleCode.CEO, PermissionCodes.LEAD_VIEW, PermissionAccessLevel.VIEW, PermissionScope.TENANT);
        grant(defaults, RoleCode.CEO, PermissionCodes.AUDIT_VIEW, PermissionAccessLevel.VIEW, PermissionScope.TENANT);

        grant(defaults, RoleCode.MARKETING, PermissionCodes.CATALOG_VIEW, PermissionAccessLevel.VIEW, PermissionScope.TENANT);
        grant(defaults, RoleCode.MARKETING, PermissionCodes.IMPORT_VIEW, PermissionAccessLevel.VIEW, PermissionScope.TENANT);
        grant(defaults, RoleCode.MARKETING, PermissionCodes.LEAD_VIEW, PermissionAccessLevel.VIEW, PermissionScope.TENANT);

        grant(defaults, RoleCode.SALES_LEAD, PermissionCodes.BRANCH_VIEW, PermissionAccessLevel.VIEW, PermissionScope.BRANCH);
        grant(defaults, RoleCode.SALES_LEAD, PermissionCodes.USER_VIEW, PermissionAccessLevel.VIEW, PermissionScope.BRANCH);
        grant(defaults, RoleCode.SALES_LEAD, PermissionCodes.IMPORT_VIEW, PermissionAccessLevel.VIEW, PermissionScope.BRANCH);
        grant(defaults, RoleCode.SALES_LEAD, PermissionCodes.LEAD_VIEW, PermissionAccessLevel.VIEW, PermissionScope.BRANCH);
        grant(defaults, RoleCode.SALES_LEAD, PermissionCodes.DUPLICATE_MANAGE, PermissionAccessLevel.MANAGE, PermissionScope.BRANCH);

        grant(defaults, RoleCode.ADVISOR, PermissionCodes.LEAD_VIEW, PermissionAccessLevel.VIEW, PermissionScope.OWN);
        grant(defaults, RoleCode.FINANCE, PermissionCodes.CATALOG_VIEW, PermissionAccessLevel.VIEW, PermissionScope.TENANT);
        grant(defaults, RoleCode.CSKH, PermissionCodes.CATALOG_VIEW, PermissionAccessLevel.VIEW, PermissionScope.TENANT);
        return defaults;
    }

    private static void grant(
            Map<String, DefaultPermission> defaults,
            RoleCode roleCode,
            String functionCode,
            PermissionAccessLevel accessLevel,
            PermissionScope scope) {
        defaults.put(key(roleCode.name(), functionCode), new DefaultPermission(accessLevel, scope));
    }

    private static String key(String roleCode, String functionCode) {
        return roleCode + "::" + functionCode;
    }

    private record DefaultPermission(PermissionAccessLevel accessLevel, PermissionScope scope) {
    }
}
