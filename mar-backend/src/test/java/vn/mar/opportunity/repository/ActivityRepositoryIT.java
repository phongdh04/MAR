package vn.mar.opportunity.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.TimeZone;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.mar.opportunity.entity.Activity;
import vn.mar.opportunity.model.ActivityResult;
import vn.mar.opportunity.model.ActivityType;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ActivityRepositoryIT {

    private static final UUID TENANT_A_ID = UUID.fromString("11000000-0000-0000-0000-000000000001");
    private static final UUID TENANT_B_ID = UUID.fromString("11000000-0000-0000-0000-000000000002");
    private static final UUID USER_A_ID = UUID.fromString("21000000-0000-0000-0000-000000000001");
    private static final UUID USER_B_ID = UUID.fromString("21000000-0000-0000-0000-000000000002");
    private static final UUID CUSTOMER_A_ID = UUID.fromString("31000000-0000-0000-0000-000000000001");
    private static final UUID CUSTOMER_B_ID = UUID.fromString("31000000-0000-0000-0000-000000000002");
    private static final UUID LEAD_A_ID = UUID.fromString("41000000-0000-0000-0000-000000000001");
    private static final UUID LEAD_B_ID = UUID.fromString("41000000-0000-0000-0000-000000000002");
    private static final UUID OPPORTUNITY_A_ID = UUID.fromString("51000000-0000-0000-0000-000000000001");
    private static final UUID OPPORTUNITY_B_ID = UUID.fromString("51000000-0000-0000-0000-000000000002");
    private static final UUID ACTIVITY_A1_ID = UUID.fromString("61000000-0000-0000-0000-000000000001");
    private static final UUID ACTIVITY_A2_ID = UUID.fromString("61000000-0000-0000-0000-000000000002");
    private static final UUID ACTIVITY_A3_ID = UUID.fromString("61000000-0000-0000-0000-000000000003");
    private static final UUID ACTIVITY_B1_ID = UUID.fromString("61000000-0000-0000-0000-000000000004");
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
    private ActivityRepository activityRepository;

    @AfterAll
    static void restoreTimeZone() {
        TimeZone.setDefault(ORIGINAL_TIME_ZONE);
    }

    @Test
    void findByTenantIdAndOpportunityIdOrderByOccurredAtDescIdDesc_shouldReturnTenantTimelineInOrder() {
        insertOpportunityFixture(
                TENANT_A_ID,
                USER_A_ID,
                CUSTOMER_A_ID,
                LEAD_A_ID,
                OPPORTUNITY_A_ID,
                "ACTIVITY_A",
                "activity-a@example.test",
                "0910000001"
        );
        insertOpportunityFixture(
                TENANT_B_ID,
                USER_B_ID,
                CUSTOMER_B_ID,
                LEAD_B_ID,
                OPPORTUNITY_B_ID,
                "ACTIVITY_B",
                "activity-b@example.test",
                "0910000002"
        );
        insertActivity(ACTIVITY_A1_ID, TENANT_A_ID, CUSTOMER_A_ID, OPPORTUNITY_A_ID, USER_A_ID, ActivityType.CALL, ActivityResult.NO_ANSWER, NOW);
        insertActivity(ACTIVITY_A2_ID, TENANT_A_ID, CUSTOMER_A_ID, OPPORTUNITY_A_ID, USER_A_ID, ActivityType.ZALO, ActivityResult.REPLIED, NOW.plusSeconds(60));
        insertActivity(ACTIVITY_A3_ID, TENANT_A_ID, CUSTOMER_A_ID, OPPORTUNITY_A_ID, USER_A_ID, ActivityType.SMS, ActivityResult.SENT, NOW.plusSeconds(60));
        insertActivity(ACTIVITY_B1_ID, TENANT_B_ID, CUSTOMER_B_ID, OPPORTUNITY_B_ID, USER_B_ID, ActivityType.EMAIL, ActivityResult.SENT, NOW.plusSeconds(120));

        var timeline = activityRepository
                .findByTenantIdAndOpportunityIdOrderByOccurredAtDescIdDesc(
                        TENANT_A_ID,
                        OPPORTUNITY_A_ID,
                        PageRequest.of(0, 10)
                );

        assertThat(timeline.getContent())
                .extracting(Activity::id)
                .containsExactly(ACTIVITY_A3_ID, ACTIVITY_A2_ID, ACTIVITY_A1_ID);
        assertThat(timeline.getContent())
                .extracting(Activity::activityType)
                .containsExactly(ActivityType.SMS, ActivityType.ZALO, ActivityType.CALL);
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

    private void insertActivity(
            UUID activityId,
            UUID tenantId,
            UUID customerId,
            UUID opportunityId,
            UUID actorId,
            ActivityType activityType,
            ActivityResult activityResult,
            Instant occurredAt) {
        jdbcTemplate.update("""
                insert into activities (
                    activity_id,
                    tenant_id,
                    customer_id,
                    opportunity_id,
                    actor_id,
                    actor_type,
                    activity_type,
                    activity_result,
                    occurred_at,
                    note,
                    source,
                    created_at
                ) values (?, ?, ?, ?, ?, 'USER', ?, ?, ?, 'Repository timeline test', 'MANUAL', ?)
                """,
                activityId,
                tenantId,
                customerId,
                opportunityId,
                actorId,
                activityType.name(),
                activityResult.name(),
                timestamp(occurredAt),
                timestamp(NOW)
        );
    }

    private Timestamp timestamp(Instant instant) {
        return Timestamp.from(instant);
    }
}
