package vn.mar.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import vn.mar.audit.model.AuditActions;
import vn.mar.audit.repository.AuditEventRepository;
import vn.mar.authz.repository.PermissionProfileRepository;
import vn.mar.branch.repository.BranchRepository;
import vn.mar.catalog.entity.Course;
import vn.mar.catalog.entity.Language;
import vn.mar.catalog.entity.Program;
import vn.mar.catalog.model.CatalogStatus;
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
import vn.mar.opportunity.repository.AdmissionOpportunityRepository;
import vn.mar.opportunity.repository.TouchpointRepository;
import vn.mar.role.repository.RoleRepository;
import vn.mar.security.jwt.JwtTokenProvider;
import vn.mar.tenant.repository.TenantRepository;
import vn.mar.user.repository.UserRepository;
import vn.mar.userbranch.repository.UserBranchRepository;

@SpringBootTest(classes = MarApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogApiSmokeTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID LANGUAGE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID PROGRAM_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID COURSE_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final Instant NOW = Instant.parse("2026-07-01T08:00:00Z");

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
    private TouchpointRepository touchpointRepository;

    @MockitoBean
    private ImportBatchRepository importBatchRepository;

    @MockitoBean
    private ImportRowRepository importRowRepository;

    @MockitoBean
    private AuditEventRepository auditEventRepository;

    @BeforeEach
    void setUp() {
        cacheEvictionService.clearPermissionProfiles();
        when(languageRepository.save(any(Language.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(programRepository.save(any(Program.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createLanguage_whenValid_shouldReturnCreatedAndAuditCreated() throws Exception {
        allowCatalogManage();

        mockMvc.perform(post("/api/v1/languages")
                        .header(RequestIdFilter.HEADER_NAME, "req_cat_001")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "JA",
                                  "name": "Japanese"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "req_cat_001"))
                .andExpect(jsonPath("$.data.language_id").isString())
                .andExpect(jsonPath("$.data.tenant_id").value(TENANT_ID.toString()))
                .andExpect(jsonPath("$.data.code").value("JA"))
                .andExpect(jsonPath("$.data.name").value("Japanese"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.meta.request_id").value("req_cat_001"));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditActions.LANGUAGE_CREATED);
    }

    @Test
    void createLanguage_whenActiveNameDuplicated_shouldReturnConflictEnvelope() throws Exception {
        allowCatalogManage();
        when(languageRepository.existsByTenantIdAndLanguageNameIgnoreCaseAndStatus(
                TENANT_ID,
                "Japanese",
                CatalogStatus.ACTIVE
        )).thenReturn(true);

        mockMvc.perform(post("/api/v1/languages")
                        .header(RequestIdFilter.HEADER_NAME, "req_cat_002")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Japanese"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_RESOURCE"))
                .andExpect(jsonPath("$.meta.request_id").value("req_cat_002"));
    }

    @Test
    void createProgram_whenLanguageActive_shouldReturnCreated() throws Exception {
        allowCatalogManage();
        when(languageRepository.findByIdAndTenantId(LANGUAGE_ID, TENANT_ID)).thenReturn(Optional.of(activeLanguage()));

        mockMvc.perform(post("/api/v1/programs")
                        .header(RequestIdFilter.HEADER_NAME, "req_cat_003")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "language_id": "cccccccc-cccc-cccc-cccc-cccccccccccc",
                                  "program_name": "JLPT N5",
                                  "exam_track": "JLPT"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.program_id").isString())
                .andExpect(jsonPath("$.data.language_id").value(LANGUAGE_ID.toString()))
                .andExpect(jsonPath("$.data.program_code").isString())
                .andExpect(jsonPath("$.data.program_name").value("JLPT N5"))
                .andExpect(jsonPath("$.data.exam_track").value("JLPT"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.meta.request_id").value("req_cat_003"));
    }

    @Test
    void createProgram_whenLanguageInactive_shouldReturnInvalidParentStatus() throws Exception {
        allowCatalogManage();
        when(languageRepository.findByIdAndTenantId(LANGUAGE_ID, TENANT_ID)).thenReturn(Optional.of(inactiveLanguage()));

        mockMvc.perform(post("/api/v1/programs")
                        .header(RequestIdFilter.HEADER_NAME, "req_cat_004")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "language_id": "cccccccc-cccc-cccc-cccc-cccccccccccc",
                                  "program_name": "JLPT N5"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("INVALID_PARENT_STATUS"))
                .andExpect(jsonPath("$.meta.request_id").value("req_cat_004"));
    }

    @Test
    void createCourse_whenProgramActive_shouldReturnCreated() throws Exception {
        allowCatalogManage();
        when(programRepository.findByIdAndTenantId(PROGRAM_ID, TENANT_ID)).thenReturn(Optional.of(activeProgram()));

        mockMvc.perform(post("/api/v1/courses")
                        .header(RequestIdFilter.HEADER_NAME, "req_cat_005")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "program_id": "dddddddd-dddd-dddd-dddd-dddddddddddd",
                                  "course_name": "JLPT N5 Foundation",
                                  "level": "N5",
                                  "tuition_gross": 4500000,
                                  "currency": "VND"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.course_id").isString())
                .andExpect(jsonPath("$.data.program_id").value(PROGRAM_ID.toString()))
                .andExpect(jsonPath("$.data.course_code").isString())
                .andExpect(jsonPath("$.data.course_name").value("JLPT N5 Foundation"))
                .andExpect(jsonPath("$.data.level").value("N5"))
                .andExpect(jsonPath("$.data.tuition_gross").value(4500000))
                .andExpect(jsonPath("$.data.currency").value("VND"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.meta.request_id").value("req_cat_005"));
    }

    @Test
    void createCourse_whenTuitionNegative_shouldReturnNegativeTuitionEnvelope() throws Exception {
        allowCatalogManage();
        when(programRepository.findByIdAndTenantId(PROGRAM_ID, TENANT_ID)).thenReturn(Optional.of(activeProgram()));

        mockMvc.perform(post("/api/v1/courses")
                        .header(RequestIdFilter.HEADER_NAME, "req_cat_006")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "program_id": "dddddddd-dddd-dddd-dddd-dddddddddddd",
                                  "course_name": "JLPT N5 Foundation",
                                  "tuition_gross": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("NEGATIVE_TUITION"))
                .andExpect(jsonPath("$.meta.request_id").value("req_cat_006"));
    }

    @Test
    void searchLanguages_whenLeadViewPermission_shouldReturnTenantScopedPage() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("lead.view"));
        when(languageRepository.search(
                eq(TENANT_ID),
                eq(CatalogStatus.ACTIVE),
                eq("ja"),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(activeLanguage()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/languages")
                        .queryParam("keyword", "JA")
                        .queryParam("status", "ACTIVE")
                        .header(RequestIdFilter.HEADER_NAME, "req_cat_007")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].language_id").value(LANGUAGE_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].code").value("JA"))
                .andExpect(jsonPath("$.data.items[0].name").value("Japanese"))
                .andExpect(jsonPath("$.data.total_elements").value(1))
                .andExpect(jsonPath("$.meta.request_id").value("req_cat_007"));
    }

    @Test
    void getCourse_whenCrossTenant_shouldReturnNotFoundEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("catalog.view"));
        when(courseRepository.findByIdAndTenantId(COURSE_ID, TENANT_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/courses/{courseId}", COURSE_ID)
                        .header(RequestIdFilter.HEADER_NAME, "req_cat_008")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.meta.request_id").value("req_cat_008"));
    }

    @Test
    void updateProgram_whenStatusInactive_shouldReturnUpdatedAndAuditStatusChanged() throws Exception {
        allowCatalogManage();
        when(programRepository.findByIdAndTenantId(PROGRAM_ID, TENANT_ID)).thenReturn(Optional.of(activeProgram()));

        mockMvc.perform(patch("/api/v1/programs/{programId}", PROGRAM_ID)
                        .header(RequestIdFilter.HEADER_NAME, "req_cat_009")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "INACTIVE",
                                  "reason": "Stop pilot"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.program_id").value(PROGRAM_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"))
                .andExpect(jsonPath("$.meta.request_id").value("req_cat_009"));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditActions.PROGRAM_STATUS_CHANGED);
    }

    @Test
    void createLanguage_whenPermissionMissing_shouldReturnForbiddenEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("catalog.view"));

        mockMvc.perform(post("/api/v1/languages")
                        .header(RequestIdFilter.HEADER_NAME, "req_cat_010")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Japanese"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"))
                .andExpect(jsonPath("$.meta.request_id").value("req_cat_010"));
    }

    private void allowCatalogManage() {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("catalog.manage"));
    }

    private String bearerToken() {
        return "Bearer " + jwtTokenProvider.createAccessToken(ACTOR_ID, TENANT_ID, "ADMIN").token();
    }

    private Language activeLanguage() {
        return Language.restore(
                LANGUAGE_ID,
                TENANT_ID,
                "JA",
                "Japanese",
                CatalogStatus.ACTIVE,
                NOW,
                ACTOR_ID,
                NOW,
                ACTOR_ID
        );
    }

    private Language inactiveLanguage() {
        return Language.restore(
                LANGUAGE_ID,
                TENANT_ID,
                "JA",
                "Japanese",
                CatalogStatus.INACTIVE,
                NOW,
                ACTOR_ID,
                NOW,
                ACTOR_ID
        );
    }

    private Program activeProgram() {
        return Program.restore(
                PROGRAM_ID,
                TENANT_ID,
                LANGUAGE_ID,
                "JLPT_N5",
                "JLPT N5",
                "JLPT",
                CatalogStatus.ACTIVE,
                NOW,
                ACTOR_ID,
                NOW,
                ACTOR_ID
        );
    }
}
