package vn.mar.opportunity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
import vn.mar.opportunity.entity.Activity;
import vn.mar.opportunity.entity.AdmissionOpportunity;
import vn.mar.opportunity.entity.StageHistory;
import vn.mar.opportunity.model.ActivityActorType;
import vn.mar.opportunity.model.ActivityResult;
import vn.mar.opportunity.model.ActivitySource;
import vn.mar.opportunity.model.ActivityType;
import vn.mar.opportunity.model.OpportunityStage;
import vn.mar.opportunity.repository.ActivityRepository;
import vn.mar.opportunity.repository.AdmissionOpportunityRepository;
import vn.mar.opportunity.repository.StageHistoryRepository;
import vn.mar.opportunity.repository.TouchpointRepository;
import vn.mar.role.repository.RoleRepository;
import vn.mar.sla.repository.SlaPolicyRepository;
import vn.mar.sla.repository.WorkingHoursConfigRepository;
import vn.mar.security.jwt.JwtTokenProvider;
import vn.mar.tenant.repository.TenantRepository;
import vn.mar.user.repository.UserRepository;
import vn.mar.userbranch.repository.UserBranchRepository;

@SpringBootTest(classes = MarApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpportunityApiSmokeTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID CUSTOMER_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID LEAD_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID OPPORTUNITY_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final UUID LANGUAGE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PROGRAM_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant NOW = Instant.parse("2026-07-06T01:00:00Z");

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
        when(admissionOpportunityRepository.save(any(AdmissionOpportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(stageHistoryRepository.save(any(StageHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void searchOpportunities_whenLeadViewPermission_shouldReturnPaginatedEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("lead.view"));
        when(admissionOpportunityRepository.search(
                eq(TENANT_ID),
                eq(null),
                eq(OpportunityStage.NEW),
                eq(LANGUAGE_ID),
                eq(PROGRAM_ID),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(opportunity()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/opportunities")
                        .queryParam("stage", "NEW")
                        .queryParam("language_id", LANGUAGE_ID.toString())
                        .queryParam("program_id", PROGRAM_ID.toString())
                        .header(RequestIdFilter.HEADER_NAME, "req_opp_001")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "req_opp_001"))
                .andExpect(jsonPath("$.data.items[0].opportunity_id").value(OPPORTUNITY_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].customer_id").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].current_stage").value("NEW"))
                .andExpect(jsonPath("$.data.total_elements").value(1))
                .andExpect(jsonPath("$.meta.request_id").value("req_opp_001"));
    }

    @Test
    void updateOpportunity_whenOpportunityUpdatePermission_shouldReturnUpdatedEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("opportunity.update"));
        when(admissionOpportunityRepository.findByIdAndTenantId(OPPORTUNITY_ID, TENANT_ID))
                .thenReturn(Optional.of(opportunity()));

        mockMvc.perform(patch("/api/v1/opportunities/{opportunityId}", OPPORTUNITY_ID)
                        .header(RequestIdFilter.HEADER_NAME, "req_opp_002")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "qualification_status": "QUALIFIED",
                                  "note": "Qualified after advisor review"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.opportunity_id").value(OPPORTUNITY_ID.toString()))
                .andExpect(jsonPath("$.data.qualification_status").value("QUALIFIED"))
                .andExpect(jsonPath("$.meta.request_id").value("req_opp_002"));
    }

    @Test
    void updateOpportunity_whenPermissionMissing_shouldReturnForbiddenEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("lead.view"));

        mockMvc.perform(patch("/api/v1/opportunities/{opportunityId}", OPPORTUNITY_ID)
                        .header(RequestIdFilter.HEADER_NAME, "req_opp_003")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "qualification_status": "QUALIFIED"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"))
                .andExpect(jsonPath("$.meta.request_id").value("req_opp_003"));
    }

    @Test
    void changeStage_whenValidTransition_shouldReturnStageChangeEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("opportunity.update"));
        when(admissionOpportunityRepository.findByIdAndTenantId(OPPORTUNITY_ID, TENANT_ID))
                .thenReturn(Optional.of(opportunity()));
        when(stageHistoryRepository.findFirstByTenantIdAndOpportunityIdOrderByChangedAtDesc(TENANT_ID, OPPORTUNITY_ID))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/opportunities/{opportunityId}/stage", OPPORTUNITY_ID)
                        .header(RequestIdFilter.HEADER_NAME, "req_opp_004")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "to_stage": "CONTACTING",
                                  "reason": "Advisor started handling"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.opportunity_id").value(OPPORTUNITY_ID.toString()))
                .andExpect(jsonPath("$.data.from_stage").value("NEW"))
                .andExpect(jsonPath("$.data.to_stage").value("CONTACTING"))
                .andExpect(jsonPath("$.data.stage_history_id").exists())
                .andExpect(jsonPath("$.meta.request_id").value("req_opp_004"));
    }

    @Test
    void changeStage_whenInvalidTransition_shouldReturnConflictEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("opportunity.update"));
        when(admissionOpportunityRepository.findByIdAndTenantId(OPPORTUNITY_ID, TENANT_ID))
                .thenReturn(Optional.of(opportunity()));

        mockMvc.perform(post("/api/v1/opportunities/{opportunityId}/stage", OPPORTUNITY_ID)
                        .header(RequestIdFilter.HEADER_NAME, "req_opp_005")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "to_stage": "ENROLLED"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STAGE_TRANSITION"))
                .andExpect(jsonPath("$.meta.request_id").value("req_opp_005"));
    }

    @Test
    void getStageHistory_whenLeadViewPermission_shouldReturnTimelineEnvelope() throws Exception {
        UUID historyId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("lead.view"));
        when(admissionOpportunityRepository.findByIdAndTenantId(OPPORTUNITY_ID, TENANT_ID))
                .thenReturn(Optional.of(opportunity()));
        when(stageHistoryRepository.findByTenantIdAndOpportunityIdOrderByChangedAtAscIdAsc(TENANT_ID, OPPORTUNITY_ID))
                .thenReturn(List.of(StageHistory.create(
                        historyId,
                        TENANT_ID,
                        OPPORTUNITY_ID,
                        OpportunityStage.NEW,
                        OpportunityStage.CONTACTING,
                        ACTOR_ID,
                        "USER",
                        NOW,
                        "Advisor started handling",
                        0L
                )));

        mockMvc.perform(get("/api/v1/opportunities/{opportunityId}/stage-history", OPPORTUNITY_ID)
                        .header(RequestIdFilter.HEADER_NAME, "req_opp_006")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "req_opp_006"))
                .andExpect(jsonPath("$.data[0].stage_history_id").value(historyId.toString()))
                .andExpect(jsonPath("$.data[0].from_stage").value("NEW"))
                .andExpect(jsonPath("$.data[0].to_stage").value("CONTACTING"))
                .andExpect(jsonPath("$.data[0].changed_by").value(ACTOR_ID.toString()))
                .andExpect(jsonPath("$.data[0].changed_by_type").value("USER"))
                .andExpect(jsonPath("$.data[0].duration_in_previous_stage_seconds").value(0))
                .andExpect(jsonPath("$.meta.request_id").value("req_opp_006"));
    }

    @Test
    void createActivity_whenActivityCreatePermission_shouldReturnCreatedEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("activity.create"));
        when(admissionOpportunityRepository.findByIdAndTenantId(OPPORTUNITY_ID, TENANT_ID))
                .thenReturn(Optional.of(opportunity()));

        mockMvc.perform(post("/api/v1/opportunities/{opportunityId}/activities", OPPORTUNITY_ID)
                        .header(RequestIdFilter.HEADER_NAME, "req_opp_007")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activity_type": "CALL",
                                  "activity_result": "CONNECTED",
                                  "occurred_at": "2026-07-06T08:10:00+07:00",
                                  "note": "Customer answered first call",
                                  "next_action_at": "2026-07-06T09:10:00+07:00",
                                  "source": "MANUAL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "req_opp_007"))
                .andExpect(jsonPath("$.data.activity_id").exists())
                .andExpect(jsonPath("$.data.opportunity_id").value(OPPORTUNITY_ID.toString()))
                .andExpect(jsonPath("$.data.customer_id").value(CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.data.actor_id").value(ACTOR_ID.toString()))
                .andExpect(jsonPath("$.data.actor_type").value("USER"))
                .andExpect(jsonPath("$.data.activity_type").value("CALL"))
                .andExpect(jsonPath("$.data.activity_result").value("CONNECTED"))
                .andExpect(jsonPath("$.data.source").value("MANUAL"))
                .andExpect(jsonPath("$.data.first_response_candidate").value(true))
                .andExpect(jsonPath("$.data.contact_success").value(true))
                .andExpect(jsonPath("$.meta.request_id").value("req_opp_007"));
    }

    @Test
    void searchActivities_whenActivityViewPermission_shouldReturnPaginatedEnvelope() throws Exception {
        UUID activityId = UUID.fromString("abababab-abab-abab-abab-abababababab");
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("activity.view"));
        when(admissionOpportunityRepository.findByIdAndTenantId(OPPORTUNITY_ID, TENANT_ID))
                .thenReturn(Optional.of(opportunity()));
        when(activityRepository.findByTenantIdAndOpportunityIdOrderByOccurredAtDescIdDesc(
                eq(TENANT_ID),
                eq(OPPORTUNITY_ID),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(activity(
                activityId,
                ActivityType.ZALO,
                ActivityResult.REPLIED,
                NOW.plusSeconds(90)
        )), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/opportunities/{opportunityId}/activities", OPPORTUNITY_ID)
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .header(RequestIdFilter.HEADER_NAME, "req_opp_008")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].activity_id").value(activityId.toString()))
                .andExpect(jsonPath("$.data.items[0].activity_type").value("ZALO"))
                .andExpect(jsonPath("$.data.items[0].activity_result").value("REPLIED"))
                .andExpect(jsonPath("$.data.items[0].contact_success").value(true))
                .andExpect(jsonPath("$.data.total_elements").value(1))
                .andExpect(jsonPath("$.meta.request_id").value("req_opp_008"));
    }

    @Test
    void createActivity_whenPermissionMissing_shouldReturnForbiddenEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("activity.view"));

        mockMvc.perform(post("/api/v1/opportunities/{opportunityId}/activities", OPPORTUNITY_ID)
                        .header(RequestIdFilter.HEADER_NAME, "req_opp_009")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activity_type": "CALL",
                                  "activity_result": "NO_ANSWER",
                                  "occurred_at": "2026-07-06T08:10:00+07:00",
                                  "source": "MANUAL"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"))
                .andExpect(jsonPath("$.meta.request_id").value("req_opp_009"));
    }

    @Test
    void getStageHistory_whenPermissionMissing_shouldReturnForbiddenEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("branch.view"));

        mockMvc.perform(get("/api/v1/opportunities/{opportunityId}/stage-history", OPPORTUNITY_ID)
                        .header(RequestIdFilter.HEADER_NAME, "req_opp_010")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"))
                .andExpect(jsonPath("$.meta.request_id").value("req_opp_010"));
    }

    @Test
    void searchOpportunities_whenUnauthenticated_shouldReturnUnauthorizedEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/opportunities")
                        .header(RequestIdFilter.HEADER_NAME, "req_opp_011"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "req_opp_011"))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.meta.request_id").value("req_opp_011"));
    }

    private String bearerToken() {
        return "Bearer " + jwtTokenProvider.createAccessToken(ACTOR_ID, TENANT_ID, "ADMIN").token();
    }

    private Activity activity(
            UUID activityId,
            ActivityType activityType,
            ActivityResult activityResult,
            Instant occurredAt) {
        return Activity.create(
                activityId,
                TENANT_ID,
                CUSTOMER_ID,
                OPPORTUNITY_ID,
                ACTOR_ID,
                ActivityActorType.USER,
                activityType,
                activityResult,
                occurredAt,
                "Activity fixture",
                null,
                ActivitySource.MANUAL,
                NOW
        );
    }

    private AdmissionOpportunity opportunity() {
        return AdmissionOpportunity.create(
                OPPORTUNITY_ID,
                TENANT_ID,
                CUSTOMER_ID,
                LEAD_ID,
                LANGUAGE_ID,
                PROGRAM_ID,
                null,
                null,
                ACTOR_ID,
                null,
                NOW
        );
    }
}
