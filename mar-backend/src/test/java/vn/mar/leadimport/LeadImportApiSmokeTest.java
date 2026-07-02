package vn.mar.leadimport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.mar.MarApplication;
import vn.mar.audit.repository.AuditEventRepository;
import vn.mar.authz.repository.PermissionProfileRepository;
import vn.mar.branch.repository.BranchRepository;
import vn.mar.catalog.repository.CourseRepository;
import vn.mar.catalog.repository.LanguageRepository;
import vn.mar.catalog.repository.ProgramRepository;
import vn.mar.common.cache.CacheEvictionService;
import vn.mar.common.error.ErrorDetail;
import vn.mar.common.logging.RequestIdFilter;
import vn.mar.leadimport.entity.ImportBatch;
import vn.mar.leadimport.entity.ImportRow;
import vn.mar.leadimport.model.ImportBatchStatus;
import vn.mar.leadimport.model.ImportRowStatus;
import vn.mar.leadimport.model.ImportSourceType;
import vn.mar.leadimport.model.ImportType;
import vn.mar.leadimport.repository.ImportBatchRepository;
import vn.mar.leadimport.repository.ImportRowRepository;
import vn.mar.role.repository.RoleRepository;
import vn.mar.security.jwt.JwtTokenProvider;
import vn.mar.tenant.repository.TenantRepository;
import vn.mar.user.repository.UserRepository;
import vn.mar.userbranch.repository.UserBranchRepository;

@SpringBootTest(classes = MarApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LeadImportApiSmokeTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID BATCH_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID ROW_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
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
    private ImportBatchRepository importBatchRepository;

    @MockitoBean
    private ImportRowRepository importRowRepository;

    @MockitoBean
    private AuditEventRepository auditEventRepository;

    @BeforeEach
    void setUp() {
        cacheEvictionService.clearPermissionProfiles();
    }

    @Test
    void searchLeadImports_whenImportViewPermission_shouldReturnPaginatedHistory() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("import.view"));
        when(importBatchRepository.search(
                eq(TENANT_ID),
                eq(ImportType.LEAD),
                eq(ImportBatchStatus.DRAFT),
                eq(ImportSourceType.CSV),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(batch()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/imports/leads")
                        .queryParam("status", "DRAFT")
                        .queryParam("source_type", "CSV")
                        .header(RequestIdFilter.HEADER_NAME, "req_imp_001")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "req_imp_001"))
                .andExpect(jsonPath("$.data.items[0].batch_id").value(BATCH_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].tenant_id").value(TENANT_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].total_rows").value(3))
                .andExpect(jsonPath("$.data.items[0].valid_count").value(1))
                .andExpect(jsonPath("$.data.items[0].error_count").value(1))
                .andExpect(jsonPath("$.data.items[0].duplicate_count").value(1))
                .andExpect(jsonPath("$.data.total_elements").value(1))
                .andExpect(jsonPath("$.meta.request_id").value("req_imp_001"));
    }

    @Test
    void searchLeadImports_whenEmpty_shouldReturnEmptyPage() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("import.view"));
        when(importBatchRepository.search(
                eq(TENANT_ID),
                eq(ImportType.LEAD),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/imports/leads")
                        .header(RequestIdFilter.HEADER_NAME, "req_imp_002")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.total_elements").value(0))
                .andExpect(jsonPath("$.meta.request_id").value("req_imp_002"));
    }

    @Test
    void getLeadImportBatch_whenFound_shouldReturnDetailWithMappingConfig() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("import.view"));
        when(importBatchRepository.findByIdAndTenantIdAndImportType(BATCH_ID, TENANT_ID, ImportType.LEAD))
                .thenReturn(Optional.of(batch()));

        mockMvc.perform(get("/api/v1/imports/leads/{batchId}", BATCH_ID)
                        .header(RequestIdFilter.HEADER_NAME, "req_imp_003")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batch_id").value(BATCH_ID.toString()))
                .andExpect(jsonPath("$.data.mapping_config.columns.phone").value("Phone"))
                .andExpect(jsonPath("$.data.error_count").value(1))
                .andExpect(jsonPath("$.meta.request_id").value("req_imp_003"));
    }

    @Test
    void getLeadImportErrors_whenFound_shouldReturnErrorDetailsWithoutRawRow() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("import.view"));
        when(importBatchRepository.findByIdAndTenantIdAndImportType(BATCH_ID, TENANT_ID, ImportType.LEAD))
                .thenReturn(Optional.of(batch()));
        when(importRowRepository.findByTenantIdAndImportBatchIdAndRowStatusOrderByRowNumberAsc(
                eq(TENANT_ID),
                eq(BATCH_ID),
                eq(ImportRowStatus.ERROR),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(errorRow()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/imports/leads/{batchId}/errors", BATCH_ID)
                        .header(RequestIdFilter.HEADER_NAME, "req_imp_004")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].row_number").value(2))
                .andExpect(jsonPath("$.data.items[0].field").value("phone"))
                .andExpect(jsonPath("$.data.items[0].code").value("CONTACT_IDENTIFIER_REQUIRED"))
                .andExpect(jsonPath("$.data.items[0].message").value("Lead must include contact"))
                .andExpect(jsonPath("$.data.items[0].raw_row").doesNotExist())
                .andExpect(jsonPath("$.meta.request_id").value("req_imp_004"));
    }

    @Test
    void getLeadImportBatch_whenCrossTenant_shouldReturnImportBatchNotFound() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("import.view"));
        when(importBatchRepository.findByIdAndTenantIdAndImportType(BATCH_ID, TENANT_ID, ImportType.LEAD))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/imports/leads/{batchId}", BATCH_ID)
                        .header(RequestIdFilter.HEADER_NAME, "req_imp_005")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("IMPORT_BATCH_NOT_FOUND"))
                .andExpect(jsonPath("$.meta.request_id").value("req_imp_005"));
    }

    @Test
    void searchLeadImports_whenPermissionMissing_shouldReturnForbiddenEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("catalog.view"));

        mockMvc.perform(get("/api/v1/imports/leads")
                        .header(RequestIdFilter.HEADER_NAME, "req_imp_006")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"))
                .andExpect(jsonPath("$.meta.request_id").value("req_imp_006"));
    }

    @Test
    void searchLeadImports_whenStatusInvalid_shouldReturnValidationEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of("import.view"));

        mockMvc.perform(get("/api/v1/imports/leads")
                        .queryParam("status", "DONE")
                        .header(RequestIdFilter.HEADER_NAME, "req_imp_007")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("status"))
                .andExpect(jsonPath("$.meta.request_id").value("req_imp_007"));
    }

    private String bearerToken() {
        return "Bearer " + jwtTokenProvider.createAccessToken(ACTOR_ID, TENANT_ID, "ADMIN").token();
    }

    private ImportBatch batch() {
        return ImportBatch.restore(
                BATCH_ID,
                TENANT_ID,
                ImportType.LEAD,
                ImportSourceType.CSV,
                ImportBatchStatus.DRAFT,
                Map.of("columns", Map.of("phone", "Phone")),
                null,
                "qa-lead-import-fixture.csv",
                3,
                1,
                1,
                1,
                null,
                NOW,
                ACTOR_ID,
                NOW,
                ACTOR_ID
        );
    }

    private ImportRow errorRow() {
        return ImportRow.restore(
                ROW_ID,
                TENANT_ID,
                BATCH_ID,
                2,
                ImportRowStatus.ERROR,
                Map.of("Phone", ""),
                Map.of("full_name", "Fixture Missing Contact"),
                List.of(ErrorDetail.of("phone", "CONTACT_IDENTIFIER_REQUIRED", "Lead must include contact")),
                NOW
        );
    }
}
