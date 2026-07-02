package vn.mar.user.mapper;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import vn.mar.user.dto.response.UserDetailResponse;
import vn.mar.user.entity.User;

@Component
public class UserMapper {

    public UserDetailResponse toDetailResponse(User user, Collection<UUID> branchIds) {
        List<UUID> orderedBranchIds = branchIds == null ? List.of() : List.copyOf(branchIds);
        return new UserDetailResponse(
                user.id(),
                user.tenantId(),
                user.fullName(),
                user.email(),
                user.phoneNumber(),
                user.roleCode(),
                user.status().name(),
                orderedBranchIds,
                user.createdAt(),
                user.updatedAt()
        );
    }

    public Map<String, Object> toAuditData(User user, Collection<UUID> branchIds) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user_id", user.id().toString());
        data.put("tenant_id", user.tenantId().toString());
        data.put("email", user.email());
        data.put("full_name", user.fullName());
        data.put("phone", user.phoneNumber());
        data.put("role", user.roleCode());
        data.put("status", user.status().name());
        data.put("branch_ids", branchIds == null ? List.of() : List.copyOf(branchIds));
        return data;
    }
}
