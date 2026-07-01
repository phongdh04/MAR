package vn.mar.common.cache;

import java.util.Locale;
import java.util.UUID;

public final class CacheKeyBuilder {

    private CacheKeyBuilder() {
    }

    public static String permissionProfile(UUID tenantId, String roleCode) {
        return "tenant:%s:permission:%s".formatted(tenantId, normalizeRoleCode(roleCode));
    }

    public static String normalizeRoleCode(String roleCode) {
        if (roleCode == null) {
            return "missing";
        }
        return roleCode.trim().toUpperCase(Locale.ROOT);
    }
}
