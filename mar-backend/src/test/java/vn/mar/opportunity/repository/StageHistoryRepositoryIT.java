package vn.mar.opportunity.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
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
import vn.mar.opportunity.entity.StageHistory;
import vn.mar.opportunity.model.OpportunityStage;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class StageHistoryRepositoryIT {

    private static final UUID TENANT_A_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TENANT_B_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID USER_A_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID USER_B_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID CUSTOMER_A_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID CUSTOMER_B_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID LEAD_A_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID LEAD_B_ID = UUID.fromString("40000000-0000-0000-0000-000000000002");
    private static final UUID OPPORTUNITY_A_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID OPPORTUNITY_B_ID = UUID.fromString("50000000-0000-0000-0000-000000000002");
    private static final UUID HISTORY_A1_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");
    private static final UUID HISTORY_A2_ID = UUID.fromString("60000000-0000-0000-0000-000000000002");
    private static final UUID HISTORY_B1_ID = UUID.fromString("60000000-0000-0000-0000-000000000003");
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
    private StageHistoryRepository stageHistoryRepository;

    @AfterAll
    static void restoreTimeZone() {
        TimeZone.setDefault(ORIGINAL_TIME_ZONE);
    }

    @Test
    void findByTenantIdAndOpportunityIdOrderByChangedAtAscIdAsc_shouldReturnTenantTimelineInOrder() {
        insertOpportunityFixture(
                TENANT_A_ID,
                USER_A_ID,
                CUSTOMER_A_ID,
                LEAD_A_ID,
                OPPORTUNITY_A_ID,
                "TENANT_A",
                "tenant-a@example.test",
                "0900000001"
        );
        insertOpportunityFixture(
                TENANT_B_ID,
                USER_B_ID,
                CUSTOMER_B_ID,
                LEAD_B_ID,
                OPPORTUNITY_B_ID,
                "TENANT_B",
                "tenant-b@example.test",
                "0900000002"
        );
        insertStageHistory(HISTORY_A2_ID, TENANT_A_ID, OPPORTUNITY_A_ID, OpportunityStage.CONTACTING, OpportunityStage.CONTACTED, USER_A_ID, NOW.plusSeconds(60), 60L);
        insertStageHistory(HISTORY_B1_ID, TENANT_B_ID, OPPORTUNITY_B_ID, OpportunityStage.NEW, OpportunityStage.CONTACTING, USER_B_ID, NOW, 0L);
        insertStageHistory(HISTORY_A1_ID, TENANT_A_ID, OPPORTUNITY_A_ID, OpportunityStage.NEW, OpportunityStage.CONTACTING, USER_A_ID, NOW, 0L);

        List<StageHistory> timeline = stageHistoryRepository
                .findByTenantIdAndOpportunityIdOrderByChangedAtAscIdAsc(TENANT_A_ID, OPPORTUNITY_A_ID);

        assertThat(timeline)
                .extracting(StageHistory::id)
                .containsExactly(HISTORY_A1_ID, HISTORY_A2_ID);
        assertThat(timeline)
                .extracting(StageHistory::toStage)
                .containsExactly(OpportunityStage.CONTACTING, OpportunityStage.CONTACTED);
    }

    private void insertOpportunityFixture(
            UUID tenantId,
            UUID userId,
            UUID customerId,
            UUID leadId,
            UUID opportunityId,
            String tenantCode,
            String email,
            String phone) {
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
                """, tenantId, tenantCode, "Tenant " + tenantCode, timestamp(NOW), timestamp(NOW));
        jdbcTemplate.update("""
                insert into users (
                    user_id,
                    tenant_id,
                    email,
                    full_name,
                    role_code,
                    status,
                    created_at,
                    updated_at
                ) values (?, ?, ?, 'Advisor', 'ADMIN', 'ACTIVE', ?, ?)
                """, userId, tenantId, email, timestamp(NOW), timestamp(NOW));
        jdbcTemplate.update("""
                insert into customer_profiles (
                    customer_id,
                    tenant_id,
                    full_name,
                    primary_phone,
                    created_at,
                    updated_at
                ) values (?, ?, 'Customer', ?, ?, ?)
                """, customerId, tenantId, phone, timestamp(NOW), timestamp(NOW));
        jdbcTemplate.update("""
                insert into leads (
                    lead_id,
                    tenant_id,
                    full_name,
                    phone_normalized,
                    source_type,
                    contactability,
                    lead_status,
                    customer_id,
                    created_at,
                    updated_at
                ) values (?, ?, 'Lead', ?, 'MANUAL', 'HIGH', 'VALID', ?, ?, ?)
                """, leadId, tenantId, phone, customerId, timestamp(NOW), timestamp(NOW));
        jdbcTemplate.update("""
                insert into admission_opportunities (
                    opportunity_id,
                    tenant_id,
                    customer_id,
                    source_lead_id,
                    owner_id,
                    current_stage,
                    qualification_status,
                    created_at,
                    updated_at
                ) values (?, ?, ?, ?, ?, 'NEW', 'UNKNOWN', ?, ?)
                """, opportunityId, tenantId, customerId, leadId, userId, timestamp(NOW), timestamp(NOW));
    }

    private void insertStageHistory(
            UUID historyId,
            UUID tenantId,
            UUID opportunityId,
            OpportunityStage fromStage,
            OpportunityStage toStage,
            UUID changedBy,
            Instant changedAt,
            Long durationInPreviousStageSeconds) {
        jdbcTemplate.update("""
                insert into stage_history (
                    stage_history_id,
                    tenant_id,
                    opportunity_id,
                    from_stage,
                    to_stage,
                    changed_by,
                    changed_by_type,
                    changed_at,
                    reason,
                    duration_in_previous_stage_seconds
                ) values (?, ?, ?, ?, ?, ?, 'USER', ?, 'Repository timeline test', ?)
                """,
                historyId,
                tenantId,
                opportunityId,
                fromStage.name(),
                toStage.name(),
                changedBy,
                timestamp(changedAt),
                durationInPreviousStageSeconds
        );
    }

    private Timestamp timestamp(Instant instant) {
        return Timestamp.from(instant);
    }
}
