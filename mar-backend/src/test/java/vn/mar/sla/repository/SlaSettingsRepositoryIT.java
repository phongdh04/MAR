package vn.mar.sla.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.mar.sla.entity.SlaPolicy;
import vn.mar.sla.entity.WorkingHoursConfig;
import vn.mar.sla.model.SlaPolicyStatus;
import vn.mar.sla.model.SlaPolicyType;
import vn.mar.sla.model.WorkingHoursConfigStatus;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class SlaSettingsRepositoryIT {

    private static final UUID TENANT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_TENANT_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID WORKING_HOURS_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_WORKING_HOURS_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID SLA_POLICY_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_SLA_POLICY_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final Instant NOW = Instant.parse("2026-07-06T01:00:00Z");
    private static final TimeZone ORIGINAL_TIME_ZONE = TimeZone.getDefault();

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WorkingHoursConfigRepository workingHoursConfigRepository;

    @Autowired
    private SlaPolicyRepository slaPolicyRepository;

    @AfterAll
    static void restoreTimeZone() {
        TimeZone.setDefault(ORIGINAL_TIME_ZONE);
    }

    @Test
    void activeQueries_shouldStayTenantScoped() {
        insertTenant(TENANT_ID, "TENANT_A");
        insertTenant(OTHER_TENANT_ID, "TENANT_B");
        workingHoursConfigRepository.save(WorkingHoursConfig.create(
                WORKING_HOURS_ID,
                TENANT_ID,
                null,
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                "Asia/Ho_Chi_Minh",
                true,
                null,
                NOW
        ));
        workingHoursConfigRepository.save(WorkingHoursConfig.create(
                OTHER_WORKING_HOURS_ID,
                OTHER_TENANT_ID,
                null,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                "Asia/Ho_Chi_Minh",
                true,
                null,
                NOW
        ));
        slaPolicyRepository.save(SlaPolicy.create(
                SLA_POLICY_ID,
                TENANT_ID,
                null,
                SlaPolicyType.HOT,
                15,
                "Asia/Ho_Chi_Minh",
                null,
                NOW
        ));
        slaPolicyRepository.save(SlaPolicy.create(
                OTHER_SLA_POLICY_ID,
                OTHER_TENANT_ID,
                null,
                SlaPolicyType.HOT,
                30,
                "Asia/Ho_Chi_Minh",
                null,
                NOW
        ));

        List<WorkingHoursConfig> workingHours = workingHoursConfigRepository
                .findByTenantIdAndBranchIdIsNullAndStatus(TENANT_ID, WorkingHoursConfigStatus.ACTIVE);
        List<SlaPolicy> policies = slaPolicyRepository
                .findByTenantIdAndBranchIdIsNullAndStatus(TENANT_ID, SlaPolicyStatus.ACTIVE);

        assertThat(workingHours)
                .extracting(WorkingHoursConfig::id)
                .contains(WORKING_HOURS_ID)
                .doesNotContain(OTHER_WORKING_HOURS_ID);
        assertThat(policies)
                .extracting(SlaPolicy::id)
                .contains(SLA_POLICY_ID)
                .doesNotContain(OTHER_SLA_POLICY_ID);
    }

    private void insertTenant(UUID tenantId, String tenantCode) {
        jdbcTemplate.update("""
                insert into tenants (
                    tenant_id,
                    tenant_code,
                    tenant_name,
                    timezone,
                    default_currency,
                    status,
                    created_at,
                    updated_at
                ) values (?, ?, ?, 'Asia/Ho_Chi_Minh', 'VND', 'ACTIVE', ?, ?)
                """, tenantId, tenantCode, "Tenant " + tenantCode, Timestamp.from(NOW), Timestamp.from(NOW));
    }
}
