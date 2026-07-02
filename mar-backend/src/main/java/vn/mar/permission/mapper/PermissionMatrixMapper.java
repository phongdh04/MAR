package vn.mar.permission.mapper;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import vn.mar.authz.model.PermissionAccessLevel;
import vn.mar.authz.model.PermissionProfile;
import vn.mar.authz.model.PermissionProfileStatus;
import vn.mar.authz.model.PermissionScope;
import vn.mar.permission.dto.response.PermissionMatrixEntryResponse;
import vn.mar.permission.dto.response.PermissionMatrixResponse;
import vn.mar.permission.dto.response.PermissionRoleMatrixResponse;
import vn.mar.role.model.RoleCode;

@Component
public class PermissionMatrixMapper {

    public PermissionMatrixResponse toMatrixResponse(
            UUID tenantId,
            Collection<String> activeFunctionCodes,
            Collection<PermissionProfile> profiles) {
        List<String> orderedFunctionCodes = activeFunctionCodes.stream()
                .sorted()
                .toList();
        Map<String, PermissionProfile> profileByRoleAndFunction = profiles.stream()
                .sorted(Comparator.comparing(PermissionProfile::updatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toMap(
                        profile -> key(profile.roleCode(), profile.functionCode()),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        List<PermissionRoleMatrixResponse> roleResponses = List.of(RoleCode.values())
                .stream()
                .map(roleCode -> new PermissionRoleMatrixResponse(
                        roleCode.name(),
                        orderedFunctionCodes.stream()
                                .map(functionCode -> toEntry(profileByRoleAndFunction.get(key(roleCode.name(), functionCode)), functionCode))
                                .toList()
                ))
                .toList();
        return new PermissionMatrixResponse(tenantId, orderedFunctionCodes, roleResponses);
    }

    public Map<String, Object> toAuditData(Collection<PermissionProfile> profiles) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("profiles", profiles.stream()
                .sorted(Comparator.comparing(PermissionProfile::roleCode)
                        .thenComparing(PermissionProfile::functionCode))
                .map(this::toProfileAuditData)
                .toList());
        return data;
    }

    private PermissionMatrixEntryResponse toEntry(PermissionProfile profile, String functionCode) {
        if (profile == null) {
            return new PermissionMatrixEntryResponse(
                    null,
                    functionCode,
                    PermissionAccessLevel.NONE.name(),
                    PermissionScope.NONE.name(),
                    PermissionProfileStatus.INACTIVE.name(),
                    null
            );
        }
        return new PermissionMatrixEntryResponse(
                profile.id(),
                profile.functionCode(),
                profile.accessLevel().name(),
                profile.scope().name(),
                profile.status().name(),
                profile.updatedAt()
        );
    }

    private Map<String, Object> toProfileAuditData(PermissionProfile profile) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("permission_profile_id", profile.id().toString());
        data.put("role", profile.roleCode());
        data.put("function_code", profile.functionCode());
        data.put("access_level", profile.accessLevel().name());
        data.put("scope", profile.scope().name());
        data.put("status", profile.status().name());
        return data;
    }

    private String key(String roleCode, String functionCode) {
        return roleCode + "::" + functionCode;
    }
}
