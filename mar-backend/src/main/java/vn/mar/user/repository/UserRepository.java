package vn.mar.user.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.mar.user.entity.User;
import vn.mar.user.model.UserStatus;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<User> findByTenantIdAndEmailIgnoreCase(UUID tenantId, String email);

    List<User> findByTenantIdAndIdInAndStatusAndRoleCode(
            UUID tenantId,
            Collection<UUID> ids,
            UserStatus status,
            String roleCode);

    List<User> findByTenantIdAndStatusAndRoleCode(UUID tenantId, UserStatus status, String roleCode);

    boolean existsByTenantIdAndEmailIgnoreCase(UUID tenantId, String email);

    boolean existsByTenantIdAndEmailIgnoreCaseAndIdNot(UUID tenantId, String email, UUID id);

    @Query("""
            select u
            from MarUser u
            where u.tenantId = :tenantId
              and (:status is null or u.status = :status)
              and (:roleCode is null or u.roleCode = :roleCode)
              and (
                    :keyword is null
                    or lower(u.email) like concat('%', :keyword, '%')
                    or lower(u.fullName) like concat('%', :keyword, '%')
                    or lower(coalesce(u.phoneNumber, '')) like concat('%', :keyword, '%')
              )
            """)
    Page<User> search(
            @Param("tenantId") UUID tenantId,
            @Param("status") UserStatus status,
            @Param("roleCode") String roleCode,
            @Param("keyword") String keyword,
            Pageable pageable);
}
