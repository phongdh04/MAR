package vn.mar.sla;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.mar.MarApplication;
import vn.mar.audit.entity.AuditEvent;
import vn.mar.audit.repository.AuditEventRepository;
import vn.mar.authz.repository.PermissionProfileRepository;
import vn.mar.branch.repository.BranchRepository;
import vn.mar.catalog.repository.CourseRepository;
import vn.mar.catalog.repository.LanguageRepository;
import vn.mar.catalog.repository.ProgramRepository;
import vn.mar.common.cache.CacheEvictionService;
import vn.mar.common.logging.RequestIdFilter;
import vn.mar.customer.repository.CustomerIdentityRepository;
import vn.mar.customer.repository.CustomerProfileRepository;
import vn.mar.customer.repository.DuplicateCaseRepository;
import vn.mar.customer.repository.MergeHistoryRepository;
import vn.mar.lead.repository.LeadRepository;
import vn.mar.leadimport.repository.ImportBatchRepository;
import vn.mar.leadimport.repository.ImportRowRepository;
import vn.mar.opportunity.repository.ActivityRepository;
import vn.mar.opportunity.repository.AdmissionOpportunityRepository;
import vn.mar.opportunity.repository.StageHistoryRepository;
import vn.mar.opportunity.repository.TouchpointRepository;
import vn.mar.role.repository.RoleRepository;
import vn.mar.security.jwt.JwtTokenProvider;
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
import vn.mar.user.repository.UserRepository;
import vn.mar.userbranch.repository.UserBranchRepository;

@SpringBootTest(classes = MarApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SlaSettingsApiSmokeTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Instant NOW = Instant.parse("2026-07-06T01:00:00Z");
    private static final String TIMEZONE = "Asia/Ho_Chi_Minh";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CacheEvictionService cacheEvictionService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserBranchRepository userBranchRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private PermissionProfileRepository permissionProfileRepository;

    @MockitoBean
    private TenantRepository tenantRepository;

    @MockitoBean
    private BranchRepository branchRepository;

    @MockitoBean
    private LanguageRepository languageRepository;

    @MockitoBean
    private ProgramRepository programRepository;

    @MockitoBean
    private CourseRepository courseRepository;

    @MockitoBean
    private CustomerProfileRepository customerProfileRepository;

    @MockitoBean
    private CustomerIdentityRepository customerIdentityRepository;

    @MockitoBean
    private DuplicateCaseRepository duplicateCaseRepository;

    @MockitoBean
    private MergeHistoryRepository mergeHistoryRepository;

    @MockitoBean
    private LeadRepository leadRepository;

    @MockitoBean
    private AdmissionOpportunityRepository admissionOpportunityRepository;

    @MockitoBean
    private StageHistoryRepository stageHistoryRepository;

    @MockitoBean
    private ActivityRepository activityRepository;

    @MockitoBean
    private TouchpointRepository touchpointRepository;

    @MockitoBean
    private ImportBatchRepository importBatchRepository;

    @MockitoBean
    private ImportRowRepository importRowRepository;

    @MockitoBean
    private WorkingHoursConfigRepository workingHoursConfigRepository;

    @MockitoBean
    private SlaPolicyRepository slaPolicyRepository;

    @MockitoBean
    private AuditEventRepository auditEventRepository;

    @BeforeEach
    void setUp() {
        cacheEvictionService.clearPermissionProfiles();
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant()));
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void getWorkingHours_whenSlaViewPermission_shouldReturnWeeklyConfigEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("sla.view"));
        when(workingHoursConfigRepository.findByTenantIdAndBranchIdIsNullAndStatus(
                TENANT_ID,
                WorkingHoursConfigStatus.ACTIVE
        )).thenReturn(workingHoursConfigs());

        mockMvc.perform(get("/api/v1/working-hours")
                        .header(RequestIdFilter.HEADER_NAME, "req_sla_001")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "req_sla_001"))
                .andExpect(jsonPath("$.data.tenant_id").value(TENANT_ID.toString()))
                .andExpect(jsonPath("$.data.timezone").value(TIMEZONE))
                .andExpect(jsonPath("$.data.source").value("TENANT"))
                .andExpect(jsonPath("$.data.days[0].weekday").value("MONDAY"))
                .andExpect(jsonPath("$.data.days[0].start_time").value("08:00:00"))
                .andExpect(jsonPath("$.data.days[6].working_day").value(false))
                .andExpect(jsonPath("$.meta.request_id").value("req_sla_001"));
    }

    @Test
    void getSlaPolicies_whenSlaViewPermission_shouldReturnPolicyEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("sla.view"));
        when(slaPolicyRepository.findByTenantIdAndBranchIdIsNullAndStatus(TENANT_ID, SlaPolicyStatus.ACTIVE))
                .thenReturn(slaPolicies());

        mockMvc.perform(get("/api/v1/sla-policies")
                        .header(RequestIdFilter.HEADER_NAME, "req_sla_002")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.policies[0].policy_type").value("HOT"))
                .andExpect(jsonPath("$.data.policies[0].response_due_minutes").value(15))
                .andExpect(jsonPath("$.data.policies[2].policy_type").value("AFTER_HOURS"))
                .andExpect(jsonPath("$.meta.request_id").value("req_sla_002"));
    }

    @Test
    void updateSlaPolicies_whenSlaManagePermission_shouldPersistAndReturnUpdatedEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("sla.manage"));
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

        mockMvc.perform(patch("/api/v1/sla-policies")
                        .header(RequestIdFilter.HEADER_NAME, "req_sla_003")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "timezone": "Asia/Ho_Chi_Minh",
                                  "policies": [
                                    { "policy_type": "HOT", "response_due_minutes": 10 },
                                    { "policy_type": "NORMAL", "response_due_minutes": 45 },
                                    { "policy_type": "AFTER_HOURS", "response_due_minutes": 0 }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.policies[0].response_due_minutes").value(10))
                .andExpect(jsonPath("$.data.policies[1].response_due_minutes").value(45))
                .andExpect(jsonPath("$.meta.request_id").value("req_sla_003"));
    }

    @Test
    void getWorkingHours_whenPermissionMissing_shouldReturnForbiddenEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("branch.view"));

        mockMvc.perform(get("/api/v1/working-hours")
                        .header(RequestIdFilter.HEADER_NAME, "req_sla_004")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"))
                .andExpect(jsonPath("$.meta.request_id").value("req_sla_004"));
    }

    private String bearerToken() {
        return "Bearer " + jwtTokenProvider.createAccessToken(ACTOR_ID, TENANT_ID, "ADMIN").token();
    }

    private Tenant tenant() {
        return Tenant.restore(
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
        );
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
}
