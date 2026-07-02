package vn.mar.authz.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.stereotype.Repository;
import vn.mar.authz.model.PermissionAccessLevel;
import vn.mar.authz.model.PermissionProfile;
import vn.mar.authz.model.PermissionProfileStatus;
import vn.mar.authz.model.PermissionScope;

@Repository
public class PermissionProfileRepository {

    private static final String ACTIVE_PERMISSION_CODES_SQL = """
            select pp.function_code
            from permission_profiles pp
            join permissions p
              on p.function_code = pp.function_code
             and p.status = 'ACTIVE'
            where pp.tenant_id = :tenantId
              and pp.role_code = :roleCode
              and pp.status = 'ACTIVE'
              and pp.access_level <> 'NONE'
            """;
    private static final String FIND_BY_TENANT_SQL = """
            select permission_profile_id,
                   tenant_id,
                   role_code,
                   function_code,
                   access_level,
                   scope,
                   status,
                   created_at,
                   created_by,
                   updated_at,
                   updated_by
            from permission_profiles
            where tenant_id = :tenantId
            order by role_code, function_code, coalesce(scope, 'NONE')
            """;
    private static final String FIND_BY_TENANT_ROLE_FUNCTION_SQL = """
            select permission_profile_id,
                   tenant_id,
                   role_code,
                   function_code,
                   access_level,
                   scope,
                   status,
                   created_at,
                   created_by,
                   updated_at,
                   updated_by
            from permission_profiles
            where tenant_id = :tenantId
              and role_code = :roleCode
              and function_code = :functionCode
            order by case when status = 'ACTIVE' then 0 else 1 end,
                     updated_at desc
            """;
    private static final String ACTIVE_FUNCTION_CODES_SQL = """
            select function_code
            from permissions
            where status = 'ACTIVE'
            order by function_code
            """;
    private static final String EXISTS_TENANT_PROFILE_SQL = """
            select count(*)
            from permission_profiles
            where tenant_id = :tenantId
            """;
    private static final String EXISTS_ACTIVE_FUNCTION_CODE_SQL = """
            select count(*)
            from permissions
            where function_code = :functionCode
              and status = 'ACTIVE'
            """;
    private static final String INSERT_PROFILE_SQL = """
            insert into permission_profiles (
                permission_profile_id,
                tenant_id,
                role_code,
                function_code,
                access_level,
                scope,
                status,
                created_at,
                created_by,
                updated_at,
                updated_by
            ) values (
                :id,
                :tenantId,
                :roleCode,
                :functionCode,
                :accessLevel,
                :scope,
                :status,
                :createdAt,
                :createdBy,
                :updatedAt,
                :updatedBy
            )
            """;
    private static final String UPDATE_PROFILE_SQL = """
            update permission_profiles
            set access_level = :accessLevel,
                scope = :scope,
                status = :status,
                updated_at = :updatedAt,
                updated_by = :updatedBy
            where permission_profile_id = :id
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

    public List<PermissionProfile> findByTenantId(UUID tenantId) {
        return jdbcTemplate.query(FIND_BY_TENANT_SQL, Map.of("tenantId", tenantId), this::mapProfile);
    }

    public Optional<PermissionProfile> findByTenantIdAndRoleCodeAndFunctionCode(
            UUID tenantId,
            String roleCode,
            String functionCode) {
        Map<String, Object> params = Map.of(
                "tenantId", tenantId,
                "roleCode", roleCode,
                "functionCode", functionCode
        );
        return jdbcTemplate.query(FIND_BY_TENANT_ROLE_FUNCTION_SQL, params, this::mapProfile)
                .stream()
                .findFirst();
    }

    public List<String> findActiveFunctionCodes() {
        return jdbcTemplate.queryForList(ACTIVE_FUNCTION_CODES_SQL, Map.of(), String.class);
    }

    public boolean existsByTenantId(UUID tenantId) {
        Integer count = jdbcTemplate.queryForObject(EXISTS_TENANT_PROFILE_SQL, Map.of("tenantId", tenantId), Integer.class);
        return count != null && count > 0;
    }

    public boolean existsActiveFunctionCode(String functionCode) {
        Integer count = jdbcTemplate.queryForObject(
                EXISTS_ACTIVE_FUNCTION_CODE_SQL,
                Map.of("functionCode", functionCode),
                Integer.class
        );
        return count != null && count > 0;
    }

    public void insert(PermissionProfile profile) {
        jdbcTemplate.update(INSERT_PROFILE_SQL, toParams(profile));
    }

    public void update(PermissionProfile profile) {
        jdbcTemplate.update(UPDATE_PROFILE_SQL, toParams(profile));
    }

    public void insertAll(Collection<PermissionProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(
                INSERT_PROFILE_SQL,
                SqlParameterSourceUtils.createBatch(profiles.stream().map(this::toParams).toArray())
        );
    }

    private Map<String, Object> toParams(PermissionProfile profile) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("id", profile.id());
        params.put("tenantId", profile.tenantId());
        params.put("roleCode", profile.roleCode());
        params.put("functionCode", profile.functionCode());
        params.put("accessLevel", profile.accessLevel().name());
        params.put("scope", profile.scope() == null ? PermissionScope.NONE.name() : profile.scope().name());
        params.put("status", profile.status().name());
        params.put("createdAt", profile.createdAt());
        params.put("createdBy", profile.createdBy());
        params.put("updatedAt", profile.updatedAt());
        params.put("updatedBy", profile.updatedBy());
        return params;
    }

    private PermissionProfile mapProfile(ResultSet resultSet, int rowNumber) throws SQLException {
        return new PermissionProfile(
                resultSet.getObject("permission_profile_id", UUID.class),
                resultSet.getObject("tenant_id", UUID.class),
                resultSet.getString("role_code"),
                resultSet.getString("function_code"),
                PermissionAccessLevel.valueOf(resultSet.getString("access_level")),
                resolveScope(resultSet.getString("scope")),
                PermissionProfileStatus.valueOf(resultSet.getString("status")),
                readInstant(resultSet, "created_at"),
                resultSet.getObject("created_by", UUID.class),
                readInstant(resultSet, "updated_at"),
                resultSet.getObject("updated_by", UUID.class)
        );
    }

    private PermissionScope resolveScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return PermissionScope.NONE;
        }
        return PermissionScope.valueOf(scope);
    }

    private Instant readInstant(ResultSet resultSet, String columnName) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
