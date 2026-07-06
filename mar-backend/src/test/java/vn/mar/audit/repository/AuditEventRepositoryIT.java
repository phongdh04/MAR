package vn.mar.audit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.mar.audit.entity.AuditEvent;
import vn.mar.audit.model.AuditActions;
import vn.mar.audit.model.AuditResourceTypes;
import vn.mar.audit.service.AuditRecordCommand;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class AuditEventRepositoryIT {

    private static final UUID TENANT_A_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TENANT_B_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID ACTOR_A_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID ACTOR_B_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID RESOURCE_A_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID RESOURCE_B_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
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
    private AuditEventRepository auditEventRepository;

    @AfterAll
    static void restoreTimeZone() {
        TimeZone.setDefault(ORIGINAL_TIME_ZONE);
    }

    @Test
    void search_shouldFilterByCurrentTenantAndAuditFields() {
        insertTenant(TENANT_A_ID, "TENANT_A");
        insertTenant(TENANT_B_ID, "TENANT_B");
        AuditEvent tenantAFirst = auditEvent(
                TENANT_A_ID,
                ACTOR_A_ID,
                AuditActions.USER_STATUS_CHANGED,
                AuditResourceTypes.USER,
                RESOURCE_A_ID,
                "user-a@example.test",
                "req_audit_repo_001",
                NOW
        );
        AuditEvent tenantASecond = auditEvent(
                TENANT_A_ID,
                ACTOR_A_ID,
                AuditActions.USER_UPDATED,
                AuditResourceTypes.USER,
                RESOURCE_A_ID,
                "user-a@example.test",
                "req_audit_repo_002",
                NOW.plusSeconds(60)
        );
        AuditEvent tenantBEvent = auditEvent(
                TENANT_B_ID,
                ACTOR_B_ID,
                AuditActions.USER_STATUS_CHANGED,
                AuditResourceTypes.USER,
                RESOURCE_B_ID,
                "user-b@example.test",
                "req_audit_repo_003",
                NOW.plusSeconds(120)
        );
        auditEventRepository.saveAll(List.of(tenantAFirst, tenantASecond, tenantBEvent));

        Page<AuditEvent> filteredPage = auditEventRepository.search(
                TENANT_A_ID,
                AuditResourceTypes.USER,
                RESOURCE_A_ID,
                "user-a@example.test",
                ACTOR_A_ID,
                "USER",
                AuditActions.USER_STATUS_CHANGED,
                "req_audit_repo_001",
                NOW.minusSeconds(60),
                NOW.plusSeconds(90),
                PageRequest.of(0, 10, createdAtDescSort())
        );
        Page<AuditEvent> tenantPage = auditEventRepository.search(
                TENANT_A_ID,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10, createdAtDescSort())
        );

        assertThat(filteredPage.getContent())
                .extracting(AuditEvent::id)
                .containsExactly(tenantAFirst.id());
        assertThat(tenantPage.getContent())
                .extracting(AuditEvent::id)
                .containsExactly(tenantASecond.id(), tenantAFirst.id());
    }

    @Test
    void findByIdAndTenantId_shouldHideCrossTenantEvents() {
        insertTenant(TENANT_A_ID, "TENANT_A");
        insertTenant(TENANT_B_ID, "TENANT_B");
        AuditEvent tenantBEvent = auditEvent(
                TENANT_B_ID,
                ACTOR_B_ID,
                AuditActions.USER_STATUS_CHANGED,
                AuditResourceTypes.USER,
                RESOURCE_B_ID,
                "user-b@example.test",
                "req_audit_repo_004",
                NOW
        );
        auditEventRepository.save(tenantBEvent);

        assertThat(auditEventRepository.findByIdAndTenantId(tenantBEvent.id(), TENANT_B_ID)).isPresent();
        assertThat(auditEventRepository.findByIdAndTenantId(tenantBEvent.id(), TENANT_A_ID)).isEmpty();
    }

    private AuditEvent auditEvent(
            UUID tenantId,
            UUID actorId,
            String action,
            String resourceType,
            UUID resourceId,
            String resourceKey,
            String requestId,
            Instant createdAt) {
        return AuditEvent.create(new AuditRecordCommand(
                tenantId,
                actorId,
                "USER",
                "ADMIN",
                action,
                resourceType,
                resourceId,
                resourceKey,
                Map.of("status", "ACTIVE"),
                Map.of("status", "INACTIVE"),
                Map.of("source", "repository-it"),
                "Repository test",
                requestId
        ), createdAt);
    }

    private Sort createdAtDescSort() {
        return Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
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
