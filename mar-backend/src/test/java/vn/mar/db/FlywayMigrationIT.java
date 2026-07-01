package vn.mar.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

class FlywayMigrationIT {

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
            "import_batches",
            "import_rows"
    );

    @Test
    void migrate_whenFreshPostgres_shouldCreateSprintOneFoundationSchema() throws Exception {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker is not available");

        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")) {
            postgres.start();

            Flyway flyway = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .validateOnMigrate(true)
                    .baselineOnMigrate(false)
                    .load();

            flyway.migrate();
            flyway.validate();

            try (Connection connection = postgres.createConnection("")) {
                for (String table : FOUNDATION_TABLES) {
                    assertThat(tableExists(connection, table))
                            .as("table %s should exist", table)
                            .isTrue();
                }

                assertThat(columnExists(connection, "branches", "city")).isTrue();
                assertThat(databaseObjectExists(connection, "ux_branches__tenant_code_active")).isTrue();
                assertThat(databaseObjectExists(connection, "ux_branches__tenant_name_active")).isTrue();
                assertThat(databaseObjectExists(connection, "ux_users__tenant_email")).isTrue();
                assertThat(databaseObjectExists(connection, "ux_permission_profiles__tenant_role_function_scope")).isTrue();
                assertThat(databaseObjectExists(connection, "ck_courses__tuition_non_negative")).isTrue();
                assertThat(databaseObjectExists(connection, "idx_import_rows__tenant_batch_status")).isTrue();
            }
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
}
