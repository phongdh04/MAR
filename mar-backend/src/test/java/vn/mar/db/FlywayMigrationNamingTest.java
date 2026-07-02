package vn.mar.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class FlywayMigrationNamingTest {

    private static final Pattern FLYWAY_NAME_PATTERN =
            Pattern.compile("^V\\d{8}_\\d{2}__[a-z0-9_]+\\.sql$");

    @Test
    void migrationFiles_whenPresent_shouldFollowMarFlywayNamingConvention() throws IOException {
        Path migrationDir = Path.of("src", "main", "resources", "db", "migration");

        List<String> migrationFiles;
        try (var paths = Files.list(migrationDir)) {
            migrationFiles = paths
                    .map(path -> path.getFileName().toString())
                    .filter(fileName -> fileName.endsWith(".sql"))
                    .sorted()
                    .toList();
        }

        assertThat(migrationFiles)
                .containsExactly(
                        "V20260630_01__create_sprint_1_foundation.sql",
                        "V20260701_01__add_branch_city_and_name_uniqueness.sql",
                        "V20260702_01__seed_roles_and_permissions.sql",
                        "V20260702_02__expand_permission_matrix_baseline.sql"
                )
                .allMatch(fileName -> FLYWAY_NAME_PATTERN.matcher(fileName).matches());
    }
}
