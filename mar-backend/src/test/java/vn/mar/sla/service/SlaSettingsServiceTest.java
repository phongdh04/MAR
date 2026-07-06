package vn.mar.sla.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import vn.mar.audit.service.AuditRecordCommand;
import vn.mar.audit.service.AuditService;
import vn.mar.authz.model.PermissionCodes;
import vn.mar.branch.repository.BranchRepository;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.time.TimeProvider;
import vn.mar.lead.model.LeadTemperature;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;
import vn.mar.sla.api.DueTimeCalculationCommand;
import vn.mar.sla.api.DueTimeCalculationResult;
import vn.mar.sla.dto.request.SlaPolicyItemRequest;
import vn.mar.sla.dto.request.UpdateSlaPoliciesRequest;
import vn.mar.sla.dto.request.UpdateWorkingHoursRequest;
import vn.mar.sla.entity.SlaPolicy;
import vn.mar.sla.entity.WorkingHoursConfig;
import vn.mar.sla.model.SlaPolicyStatus;
import vn.mar.sla.model.SlaPolicyType;
import vn.mar.sla.model.WorkingHoursConfigStatus;
import vn.mar.sla.repository.SlaPolicyRepository;
import vn.mar.sla.repository.WorkingHoursConfigRepository;
import vn.mar.tenant.entity.Tenant;
import vn.mar.tenant.model.TenantStatus;
import vn.mar.tenant.repository.TenantRepository;
import vn.mar.userbranch.repository.UserBranchRepository;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class SlaSettingsServiceTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID BRANCH_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final Instant NOW = Instant.parse("2026-07-06T01:00:00Z");
    private static final String TIMEZONE = "Asia/Ho_Chi_Minh";

    @Mock
    private WorkingHoursConfigRepository workingHoursConfigRepository;

    @Mock
    private SlaPolicyRepository slaPolicyRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private UserBranchRepository userBranchRepository;

    @Mock
    private CurrentUserContext currentUserContext;

    @Mock
    private AuditService auditService;

    private SlaSettingsService slaSettingsService;

    @BeforeEach
    void setUp() {
        TimeProvider timeProvider = () -> NOW;
        slaSettingsService = new SlaSettingsService(
                workingHoursConfigRepository,
                slaPolicyRepository,
                tenantRepository,
                branchRepository,
                userBranchRepository,
                currentUserContext,
                auditService,
                timeProvider
        );
    }

    @Test
    void calculateFirstResponseDueAt_whenHotInsideWorkingHours_shouldUseHotPolicy() {
        stubTenant();
        stubDefaultWorkingHoursAndPolicies();

        DueTimeCalculationResult result = slaSettingsService.calculateFirstResponseDueAt(
                new DueTimeCalculationCommand(
                        TENANT_ID,
                        null,
                        LeadTemperature.HOT,
                        Instant.parse("2026-07-06T02:00:00Z")
                )
        );

        assertThat(result.dueAt()).isEqualTo(Instant.parse("2026-07-06T02:15:00Z"));
        assertThat(result.policy().policyType()).isEqualTo("HOT");
        assertThat(result.afterHoursApplied()).isFalse();
    }

    @Test
    void calculateFirstResponseDueAt_whenNormalCrossesNonWorkingDay_shouldCarryWithinWorkingHours() {
        stubTenant();
        stubDefaultWorkingHoursAndPolicies();

        DueTimeCalculationResult result = slaSettingsService.calculateFirstResponseDueAt(
                new DueTimeCalculationCommand(
                        TENANT_ID,
                        null,
                        LeadTemperature.NORMAL,
                        Instant.parse("2026-07-04T10:30:00Z")
                )
        );

        assertThat(result.dueAt()).isEqualTo(Instant.parse("2026-07-06T01:30:00Z"));
        assertThat(result.policy().policyType()).isEqualTo("NORMAL");
        assertThat(result.afterHoursApplied()).isFalse();
    }

    @Test
    void calculateFirstResponseDueAt_whenConfigMissing_shouldUseDefaultPilotAndEmitWarning(CapturedOutput output) {
        stubTenant();
        when(workingHoursConfigRepository.findByTenantIdAndBranchIdIsNullAndStatus(
                TENANT_ID,
                WorkingHoursConfigStatus.ACTIVE
        )).thenReturn(List.of());
        when(slaPolicyRepository.findByTenantIdAndBranchIdIsNullAndStatus(
                TENANT_ID,
                SlaPolicyStatus.ACTIVE
        )).thenReturn(List.of());

        DueTimeCalculationResult result = slaSettingsService.calculateFirstResponseDueAt(
                new DueTimeCalculationCommand(
                        TENANT_ID,
                        null,
                        LeadTemperature.AFTER_HOURS,
                        Instant.parse("2026-07-05T15:00:00Z")
                )
        );

        assertThat(result.dueAt()).isEqualTo(Instant.parse("2026-07-06T01:00:00Z"));
        assertThat(result.policy().policyType()).isEqualTo("AFTER_HOURS");
        assertThat(result.afterHoursApplied()).isTrue();
        assertThat(output).contains("working hours config missing");
        assertThat(output).contains("sla policy missing");
    }

    @Test
    void updateWorkingHours_whenSalesLeadTargetsTenantDefault_shouldRejectByBranchScope() {
        when(currentUserContext.currentUser()).thenReturn(salesLead(Set.of(PermissionCodes.SLA_MANAGE)));

        assertThatThrownBy(() -> slaSettingsService.updateWorkingHours(
                new UpdateWorkingHoursRequest(null, TIMEZONE, List.of())
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    void updateSlaPolicies_whenAdminManagesTenantDefault_shouldPersistPoliciesAndAudit() {
        stubTenant();
        when(currentUserContext.currentUser()).thenReturn(admin(Set.of(PermissionCodes.SLA_MANAGE)));
        List<SlaPolicy> savedPolicies = new ArrayList<>();
        when(slaPolicyRepository.findByTenantIdAndBranchIdIsNullAndPolicyTypeAndStatus(
                eq(TENANT_ID),
                any(SlaPolicyType.class),
                eq(SlaPolicyStatus.ACTIVE)
        )).thenReturn(Optional.empty());
        when(slaPolicyRepository.saveAll(any())).thenAnswer(invocation -> {
            Iterable<SlaPolicy> policies = invocation.getArgument(0);
            policies.forEach(savedPolicies::add);
            return savedPolicies;
        });
        when(slaPolicyRepository.findByTenantIdAndBranchIdIsNullAndStatus(TENANT_ID, SlaPolicyStatus.ACTIVE))
                .thenAnswer(invocation -> List.copyOf(savedPolicies));

        var response = slaSettingsService.updateSlaPolicies(new UpdateSlaPoliciesRequest(
                null,
                TIMEZONE,
                List.of(
                        new SlaPolicyItemRequest("HOT", 10),
                        new SlaPolicyItemRequest("NORMAL", 45),
                        new SlaPolicyItemRequest("AFTER_HOURS", 0)
                )
        ));

        assertThat(response.policies())
                .extracting(policy -> policy.policyType() + ":" + policy.responseDueMinutes())
                .containsExactly("HOT:10", "NORMAL:45", "AFTER_HOURS:0");
        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo("SLA_POLICY_UPDATED");
        assertThat(savedPolicies).hasSize(3);
    }

    private void stubTenant() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.restore(
                TENANT_ID,
                "MAR",
                "MAR Academy",
                TIMEZONE,
                "VND",
                TenantStatus.ACTIVE,
                NOW,
                ACTOR_ID,
                NOW,
                ACTOR_ID
        )));
    }

    private void stubDefaultWorkingHoursAndPolicies() {
        when(workingHoursConfigRepository.findByTenantIdAndBranchIdIsNullAndStatus(
                TENANT_ID,
                WorkingHoursConfigStatus.ACTIVE
        )).thenReturn(workingHoursConfigs());
        when(slaPolicyRepository.findByTenantIdAndBranchIdIsNullAndStatus(
                TENANT_ID,
                SlaPolicyStatus.ACTIVE
        )).thenReturn(slaPolicies());
    }

    private List<WorkingHoursConfig> workingHoursConfigs() {
        List<WorkingHoursConfig> configs = new ArrayList<>();
        for (DayOfWeek weekday : DayOfWeek.values()) {
            boolean workingDay = weekday != DayOfWeek.SUNDAY;
            configs.add(WorkingHoursConfig.create(
                    UUID.randomUUID(),
                    TENANT_ID,
                    null,
                    weekday,
                    workingDay ? LocalTime.of(8, 0) : null,
                    workingDay ? LocalTime.of(18, 0) : null,
                    TIMEZONE,
                    workingDay,
                    ACTOR_ID,
                    NOW
            ));
        }
        return configs;
    }

    private List<SlaPolicy> slaPolicies() {
        return List.of(
                SlaPolicy.create(UUID.randomUUID(), TENANT_ID, null, SlaPolicyType.HOT, 15, TIMEZONE, ACTOR_ID, NOW),
                SlaPolicy.create(UUID.randomUUID(), TENANT_ID, null, SlaPolicyType.NORMAL, 60, TIMEZONE, ACTOR_ID, NOW),
                SlaPolicy.create(UUID.randomUUID(), TENANT_ID, null, SlaPolicyType.AFTER_HOURS, 0, TIMEZONE, ACTOR_ID, NOW)
        );
    }

    private CurrentUser admin(Set<String> permissions) {
        return new CurrentUser(ACTOR_ID, TENANT_ID, "ADMIN", permissions, "req_sla_test");
    }

    private CurrentUser salesLead(Set<String> permissions) {
        return new CurrentUser(ACTOR_ID, TENANT_ID, "SALES_LEAD", permissions, "req_sla_test");
    }
}
