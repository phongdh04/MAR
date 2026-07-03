package vn.mar.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.TimeZone;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

class FlywayMigrationIT {

    private static final String PRE_DUPLICATE_PERMISSION_TARGET = "20260703.03";
    private static final String DUPLICATE_PERMISSION_CODE = "duplicate.manage";
    private static final String EXISTING_TENANT_ID = "11111111-1111-1111-1111-111111111111";

    private static final List<String> FOUNDATION_TABLES = List.of(
            "tenants",
            "branches",
            "users",
            "roles",
            "permissions",
            "permission_profiles",
            "audit_events",
            "user_branches",
            "languages",
            "programs",
            "courses",
            "customer_profiles",
            "customer_identities",
            "duplicate_cases",
            "import_batches",
            "import_rows"
    );

    @Test
    void migrate_whenFreshPostgres_shouldCreateSprintOneFoundationSchema() throws Exception {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker is not available");

        TimeZone originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")) {
            postgres.start();

            Flyway preDuplicatePermissionFlyway = flyway(postgres)
                    .target(PRE_DUPLICATE_PERMISSION_TARGET)
                    .load();
            preDuplicatePermissionFlyway.migrate();

            try (Connection connection = postgres.createConnection("")) {
                seedExistingTenant(connection);
            }

            Flyway flyway = flyway(postgres).load();
            flyway.migrate();
            flyway.validate();

            try (Connection connection = postgres.createConnection("")) {
                for (String table : FOUNDATION_TABLES) {
                    assertThat(tableExists(connection, table))
                            .as("table %s should exist", table)
                            .isTrue();
                }

                assertThat(columnExists(connection, "branches", "city")).isTrue();
                assertThat(columnExists(connection, "programs", "exam_track")).isTrue();
                assertThat(columnExists(connection, "courses", "level")).isTrue();
                assertThat(databaseObjectExists(connection, "ux_branches__tenant_code_active")).isTrue();
                assertThat(databaseObjectExists(connection, "ux_branches__tenant_name_active")).isTrue();
                assertThat(databaseObjectExists(connection, "ux_languages__tenant_name_active")).isTrue();
                assertThat(databaseObjectExists(connection, "ux_programs__tenant_language_name_active")).isTrue();
                assertThat(databaseObjectExists(connection, "ux_courses__tenant_program_name_active")).isTrue();
                assertThat(databaseObjectExists(connection, "ux_users__tenant_email")).isTrue();
                assertThat(databaseObjectExists(connection, "ux_permission_profiles__tenant_role_function_scope")).isTrue();
                assertThat(databaseObjectExists(connection, "ux_duplicate_cases__tenant_pair_type_open")).isTrue();
                assertThat(constraintExists(connection, "courses", "ck_courses__tuition_non_negative")).isTrue();
                assertThat(databaseObjectExists(connection, "idx_import_rows__tenant_batch_status")).isTrue();
                assertThat(rowExists(connection, "roles", "role_code", "ADVISOR")).isTrue();
                assertThat(rowExists(connection, "permissions", "function_code", "user.manage")).isTrue();
                assertThat(rowExists(connection, "permissions", "function_code", "import.manage")).isTrue();
                assertThat(rowExists(connection, "permissions", "function_code", "data.export")).isTrue();
                assertThat(rowExists(connection, "permissions", "function_code", "payment.write")).isTrue();
                assertThat(rowExists(connection, "permissions", "function_code", "duplicate.manage")).isTrue();
                assertThat(permissionProfileExists(connection, EXISTING_TENANT_ID, "ADMIN", DUPLICATE_PERMISSION_CODE, "MANAGE", "TENANT")).isTrue();
                assertThat(permissionProfileExists(connection, EXISTING_TENANT_ID, "SALES_LEAD", DUPLICATE_PERMISSION_CODE, "MANAGE", "BRANCH")).isTrue();
            }
        } finally {
            TimeZone.setDefault(originalTimeZone);
        }
    }

    private org.flywaydb.core.api.configuration.FluentConfiguration flyway(PostgreSQLContainer<?> postgres) {
        return Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .validateOnMigrate(true)
                .baselineOnMigrate(false);
    }

    private void seedExistingTenant(Connection connection) throws Exception {
        try (var statement = connection.prepareStatement("""
                insert into tenants (
                    tenant_id,
                    tenant_code,
                    tenant_name,
                    timezone,
                    default_currency,
                    status,
                    created_at,
                    updated_at
                ) values (
                    ?::uuid,
                    'EXISTING',
                    'Existing Tenant',
                    'Asia/Ho_Chi_Minh',
                    'VND',
                    'ACTIVE',
                    now(),
                    now()
                )
                """)) {
            statement.setString(1, EXISTING_TENANT_ID);
            statement.executeUpdate();
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws Exception {
        try (var statement = connection.prepareStatement("""
                select count(*)
                from information_schema.tables
                where table_schema = 'public'
                  and table_name = ?
                """)) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws Exception {
        try (var statement = connection.prepareStatement("""
                select count(*)
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = ?
                  and column_name = ?
                """)) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }

    private boolean databaseObjectExists(Connection connection, String objectName) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select to_regclass('public." + objectName + "') is not null")) {
            resultSet.next();
            return resultSet.getBoolean(1);
        }
    }

    private boolean constraintExists(Connection connection, String tableName, String constraintName) throws Exception {
        try (var statement = connection.prepareStatement("""
                select count(*)
                from information_schema.table_constraints
                where table_schema = 'public'
                  and table_name = ?
                  and constraint_name = ?
                """)) {
            statement.setString(1, tableName);
            statement.setString(2, constraintName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }

    private boolean rowExists(Connection connection, String tableName, String columnName, String value) throws Exception {
        try (var statement = connection.prepareStatement("select count(*) from " + tableName + " where " + columnName + " = ?")) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }

    private boolean permissionProfileExists(
            Connection connection,
            String tenantId,
            String roleCode,
            String functionCode,
            String accessLevel,
            String scope) throws Exception {
        try (var statement = connection.prepareStatement("""
                select count(*)
                from permission_profiles
                where tenant_id = ?::uuid
                  and role_code = ?
                  and function_code = ?
                  and access_level = ?
                  and scope = ?
                  and status = 'ACTIVE'
                """)) {
            statement.setString(1, tenantId);
            statement.setString(2, roleCode);
            statement.setString(3, functionCode);
            statement.setString(4, accessLevel);
            statement.setString(5, scope);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }
}
