package vn.mar.role.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.mar.role.entity.Role;
import vn.mar.role.model.RoleStatus;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    boolean existsByRoleCodeAndStatus(String roleCode, RoleStatus status);
}
