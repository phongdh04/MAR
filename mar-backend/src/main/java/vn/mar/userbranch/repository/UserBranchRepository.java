package vn.mar.userbranch.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.mar.userbranch.entity.UserBranch;
import vn.mar.userbranch.model.UserBranchStatus;

public interface UserBranchRepository extends JpaRepository<UserBranch, UUID> {

    List<UserBranch> findByTenantIdAndUserIdAndStatus(UUID tenantId, UUID userId, UserBranchStatus status);

    List<UserBranch> findByTenantIdAndUserIdInAndStatus(
            UUID tenantId,
            Collection<UUID> userIds,
            UserBranchStatus status);
}
