package vn.mar.user.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.mar.user.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByTenantIdAndEmailIgnoreCase(UUID tenantId, String email);
}
