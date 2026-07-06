package vn.mar.sla;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.mar.MarApplication;
import vn.mar.audit.entity.AuditEvent;
import vn.mar.audit.repository.AuditEventRepository;
import vn.mar.authz.model.PermissionCodes;
import vn.mar.authz.repository.PermissionProfileRepository;
import vn.mar.branch.repository.BranchRepository;
import vn.mar.catalog.repository.CourseRepository;
import vn.mar.catalog.repository.LanguageRepository;
import vn.mar.catalog.repository.ProgramRepository;
import vn.mar.common.cache.CacheEvictionService;
import vn.mar.common.logging.RequestIdFilter;
import vn.mar.common.time.TimeProvider;
import vn.mar.customer.repository.CustomerIdentityRepository;
import vn.mar.customer.repository.CustomerProfileRepository;
import vn.mar.customer.repository.DuplicateCaseRepository;
import vn.mar.customer.repository.MergeHistoryRepository;
import vn.mar.lead.model.LeadTemperature;
import vn.mar.leadimport.repository.ImportBatchRepository;
import vn.mar.leadimport.repository.ImportRowRepository;
import vn.mar.lead.repository.LeadRepository;
import vn.mar.opportunity.repository.ActivityRepository;
import vn.mar.opportunity.repository.AdmissionOpportunityRepository;
import vn.mar.opportunity.repository.StageHistoryRepository;
import vn.mar.opportunity.repository.TouchpointRepository;
import vn.mar.role.repository.RoleRepository;
import vn.mar.security.jwt.JwtTokenProvider;
import vn.mar.sla.entity.SlaTask;
import vn.mar.sla.model.SlaOverdueLevel;
import vn.mar.sla.model.SlaTaskStatus;
import vn.mar.sla.model.SlaTaskType;
import vn.mar.sla.repository.SlaPolicyRepository;
import vn.mar.sla.repository.SlaTaskRepository;
import vn.mar.sla.repository.WorkingHoursConfigRepository;
import vn.mar.tenant.repository.TenantRepository;
import vn.mar.user.repository.UserRepository;
import vn.mar.userbranch.repository.UserBranchRepository;

@SpringBootTest(classes = MarApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SlaTaskApiSmokeTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID OPPORTUNITY_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID LEAD_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID SLA_TASK_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final UUID SLA_POLICY_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
    private static final Instant NOW = Instant.parse("2026-07-06T01:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CacheEvictionService cacheEvictionService;

    @Autowired
    private SlaTaskRepository slaTaskRepository;

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

    @MockitoBean
    private TimeProvider timeProvider;

    @BeforeEach
    void setUp() {
        cacheEvictionService.clearPermissionProfiles();
        when(timeProvider.now()).thenReturn(NOW);
        when(slaTaskRepository.save(any(SlaTask.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void searchTasks_whenAdvisorHasSlaTaskView_shouldReturnOwnTaskEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADVISOR"))
                .thenReturn(Set.of(PermissionCodes.SLA_TASK_VIEW));
        when(slaTaskRepository.search(
                eq(TENANT_ID),
                eq(ACTOR_ID),
                eq(null),
                eq(SlaTaskStatus.OPEN),
                eq(null),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(openTask(NOW.plusSeconds(900))), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/sla-tasks")
                        .queryParam("status", "OPEN")
                        .header(RequestIdFilter.HEADER_NAME, "req_sla_task_001")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("ADVISOR")))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "req_sla_task_001"))
                .andExpect(jsonPath("$.data.items[0].sla_task_id").value(SLA_TASK_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].opportunity_id").value(OPPORTUNITY_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].owner_id").value(ACTOR_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].status").value("OPEN"))
                .andExpect(jsonPath("$.data.items[0].overdue_level").value("NONE"))
                .andExpect(jsonPath("$.data.total_elements").value(1))
                .andExpect(jsonPath("$.meta.request_id").value("req_sla_task_001"));
    }

    @Test
    void scanOverdueTasks_whenAdminHasManagePermission_shouldMarkOpenDueTask() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of(PermissionCodes.SLA_TASK_MANAGE));
        when(slaTaskRepository.findAdvisorOverdueReadyForSalesLead(
                TENANT_ID,
                SlaTaskStatus.OVERDUE,
                SlaOverdueLevel.ADVISOR,
                NOW.minusSeconds(900)
        )).thenReturn(List.of());
        when(slaTaskRepository.findByTenantIdAndStatusAndDueAtLessThanEqualOrderByDueAtAscIdAsc(
                TENANT_ID,
                SlaTaskStatus.OPEN,
                NOW
        )).thenReturn(List.of(openTask(NOW.minusSeconds(60))));

        mockMvc.perform(post("/api/v1/sla-tasks/overdue-scan")
                        .header(RequestIdFilter.HEADER_NAME, "req_sla_task_002")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.marked_overdue_count").value(1))
                .andExpect(jsonPath("$.data.escalated_sales_lead_count").value(0))
                .andExpect(jsonPath("$.meta.request_id").value("req_sla_task_002"));
    }

    @Test
    void searchTasks_whenPermissionMissing_shouldReturnForbiddenEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADVISOR"))
                .thenReturn(Set.of(PermissionCodes.ACTIVITY_VIEW));

        mockMvc.perform(get("/api/v1/sla-tasks")
                        .header(RequestIdFilter.HEADER_NAME, "req_sla_task_003")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("ADVISOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"))
                .andExpect(jsonPath("$.meta.request_id").value("req_sla_task_003"));
    }

    private String bearerToken(String roleCode) {
        return "Bearer " + jwtTokenProvider.createAccessToken(ACTOR_ID, TENANT_ID, roleCode).token();
    }

    private SlaTask openTask(Instant dueAt) {
        return SlaTask.openFirstResponse(
                SLA_TASK_ID,
                TENANT_ID,
                OPPORTUNITY_ID,
                LEAD_ID,
                ACTOR_ID,
                null,
                SLA_POLICY_ID,
                LeadTemperature.HOT,
                dueAt,
                ACTOR_ID,
                NOW
        );
    }
}
