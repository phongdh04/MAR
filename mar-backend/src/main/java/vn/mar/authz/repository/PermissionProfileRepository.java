package vn.mar.authz.repository;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PermissionProfileRepository {

    private static final String ACTIVE_PERMISSION_CODES_SQL = """
            select function_code
            from permission_profiles
            where tenant_id = :tenantId
              and role_code = :roleCode
              and status = 'ACTIVE'
              and access_level <> 'NONE'
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PermissionProfileRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Set<String> findActivePermissionCodes(UUID tenantId, String roleCode) {
        Map<String, Object> params = Map.of(
                "tenantId", tenantId,
                "roleCode", roleCode
        );
        return Set.copyOf(jdbcTemplate.queryForList(ACTIVE_PERMISSION_CODES_SQL, params, String.class));
    }
}
