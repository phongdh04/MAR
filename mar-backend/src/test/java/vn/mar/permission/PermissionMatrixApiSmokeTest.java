package vn.mar.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import org.mockito.ArgumentCaptor;
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
import vn.mar.audit.model.AuditActions;
import vn.mar.audit.repository.AuditEventRepository;
import vn.mar.authz.model.PermissionAccessLevel;
import vn.mar.authz.model.PermissionCodes;
import vn.mar.authz.model.PermissionProfile;
import vn.mar.authz.model.PermissionScope;
import vn.mar.authz.repository.PermissionProfileRepository;
import vn.mar.branch.repository.BranchRepository;
import vn.mar.common.cache.CacheEvictionService;
import vn.mar.common.logging.RequestIdFilter;
import vn.mar.role.model.RoleStatus;
import vn.mar.role.repository.RoleRepository;
import vn.mar.security.jwt.JwtTokenProvider;
import vn.mar.tenant.repository.TenantRepository;
import vn.mar.user.repository.UserRepository;
import vn.mar.userbranch.repository.UserBranchRepository;

@SpringBootTest(classes = MarApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PermissionMatrixApiSmokeTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID PROFILE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
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
    private AuditEventRepository auditEventRepository;

    @BeforeEach
    void setUp() {
        cacheEvictionService.clearPermissionProfiles();
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void getMatrix_whenAdminHasPermissionManage_shouldReturnMatrix() throws Exception {
        allowAdminPermissionManage();
        when(permissionProfileRepository.findByTenantId(TENANT_ID))
                .thenReturn(List.of(adminPermissionManage()));
        when(permissionProfileRepository.findActiveFunctionCodes())
                .thenReturn(List.of(PermissionCodes.IMPORT_MANAGE, PermissionCodes.PERMISSION_MANAGE));

        mockMvc.perform(get("/api/v1/permissions/matrix")
                        .header(RequestIdFilter.HEADER_NAME, "req_perm_001")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "req_perm_001"))
                .andExpect(jsonPath("$.data.tenant_id").value(TENANT_ID.toString()))
                .andExpect(jsonPath("$.data.permission_codes[0]").value(PermissionCodes.IMPORT_MANAGE))
                .andExpect(jsonPath("$.data.permission_codes[1]").value(PermissionCodes.PERMISSION_MANAGE))
                .andExpect(jsonPath("$.data.roles[1].role").value("ADMIN"))
                .andExpect(jsonPath("$.data.roles[1].permissions[1].access_level").value("MANAGE"))
                .andExpect(jsonPath("$.meta.request_id").value("req_perm_001"));
    }

    @Test
    void updateMatrix_whenValidSalesLeadImport_shouldReturnUpdatedAndAudit() throws Exception {
        PermissionProfile beforeProfile = salesLeadImport(PermissionAccessLevel.VIEW);
        PermissionProfile afterProfile = beforeProfile.update(PermissionAccessLevel.CREATE, PermissionScope.BRANCH, ACTOR_ID, NOW);
        allowAdminPermissionManage();
        when(roleRepository.existsByRoleCodeAndStatus("SALES_LEAD", RoleStatus.ACTIVE)).thenReturn(true);
        when(permissionProfileRepository.existsActiveFunctionCode(PermissionCodes.IMPORT_MANAGE)).thenReturn(true);
        when(permissionProfileRepository.findByTenantId(TENANT_ID))
                .thenReturn(List.of(beforeProfile))
                .thenReturn(List.of(afterProfile));
        when(permissionProfileRepository.findByTenantIdAndRoleCodeAndFunctionCode(
                TENANT_ID,
                "SALES_LEAD",
                PermissionCodes.IMPORT_MANAGE
        )).thenReturn(Optional.of(beforeProfile));
        when(permissionProfileRepository.findActiveFunctionCodes()).thenReturn(List.of(PermissionCodes.IMPORT_MANAGE));

        mockMvc.perform(patch("/api/v1/permissions/matrix")
                        .header(RequestIdFilter.HEADER_NAME, "req_perm_002")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "changes": [
                                    {
                                      "role": "SALES_LEAD",
                                      "function_code": "import.manage",
                                      "access_level": "CREATE",
                                      "scope": "BRANCH"
                                    }
                                  ],
                                  "reason": "Pilot import for branch"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[3].role").value("SALES_LEAD"))
                .andExpect(jsonPath("$.data.roles[3].permissions[0].access_level").value("CREATE"))
                .andExpect(jsonPath("$.data.roles[3].permissions[0].scope").value("BRANCH"))
                .andExpect(jsonPath("$.meta.request_id").value("req_perm_002"));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditActions.PERMISSION_MATRIX_UPDATED);
        assertThat(auditCaptor.getValue().reason()).isEqualTo("Pilot import for branch");
    }

    @Test
    void updateMatrix_whenPermissionMissing_shouldReturnForbiddenEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "MARKETING"))
                .thenReturn(Set.of(PermissionCodes.CATALOG_VIEW));

        mockMvc.perform(patch("/api/v1/permissions/matrix")
                        .header(RequestIdFilter.HEADER_NAME, "req_perm_003")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("MARKETING"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "changes": [
                                    {
                                      "role": "SALES_LEAD",
                                      "function_code": "import.manage",
                                      "access_level": "CREATE",
                                      "scope": "BRANCH"
                                    }
                                  ],
                                  "reason": "Try non-admin update"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"))
                .andExpect(jsonPath("$.meta.request_id").value("req_perm_003"));
    }

    @Test
    void updateMatrix_whenAdvisorExportEnabled_shouldReturnGuardrailEnvelope() throws Exception {
        allowAdminPermissionManage();
        when(roleRepository.existsByRoleCodeAndStatus("ADVISOR", RoleStatus.ACTIVE)).thenReturn(true);
        when(permissionProfileRepository.existsActiveFunctionCode(PermissionCodes.DATA_EXPORT)).thenReturn(true);
        when(permissionProfileRepository.findByTenantId(TENANT_ID)).thenReturn(List.of());

        mockMvc.perform(patch("/api/v1/permissions/matrix")
                        .header(RequestIdFilter.HEADER_NAME, "req_perm_004")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "changes": [
                                    {
                                      "role": "ADVISOR",
                                      "function_code": "data.export",
                                      "access_level": "MANAGE",
                                      "scope": "TENANT"
                                    }
                                  ],
                                  "reason": "Try export"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("INVALID_PERMISSION_GUARDRAIL"))
                .andExpect(jsonPath("$.meta.request_id").value("req_perm_004"));
    }

    @Test
    void updateMatrix_whenMarketingPaymentWriteEnabled_shouldReturnGuardrailEnvelope() throws Exception {
        allowAdminPermissionManage();
        when(roleRepository.existsByRoleCodeAndStatus("MARKETING", RoleStatus.ACTIVE)).thenReturn(true);
        when(permissionProfileRepository.existsActiveFunctionCode(PermissionCodes.PAYMENT_WRITE)).thenReturn(true);
        when(permissionProfileRepository.findByTenantId(TENANT_ID)).thenReturn(List.of());

        mockMvc.perform(patch("/api/v1/permissions/matrix")
                        .header(RequestIdFilter.HEADER_NAME, "req_perm_005")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "changes": [
                                    {
                                      "role": "MARKETING",
                                      "function_code": "payment.write",
                                      "access_level": "UPDATE",
                                      "scope": "TENANT"
                                    }
                                  ],
                                  "reason": "Try payment"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("INVALID_PERMISSION_GUARDRAIL"))
                .andExpect(jsonPath("$.meta.request_id").value("req_perm_005"));
    }

    @Test
    void createTenant_whenAdvisorCallsDirectApi_shouldReturnForbiddenEnvelope() throws Exception {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADVISOR"))
                .thenReturn(Set.of(PermissionCodes.LEAD_VIEW));

        mockMvc.perform(post("/api/v1/tenants")
                        .header(RequestIdFilter.HEADER_NAME, "req_perm_006")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("ADVISOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenant_name": "Blocked Tenant"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("PERMISSION_DENIED"))
                .andExpect(jsonPath("$.meta.request_id").value("req_perm_006"));
    }

    private void allowAdminPermissionManage() {
        when(permissionProfileRepository.findActivePermissionCodes(TENANT_ID, "ADMIN"))
                .thenReturn(Set.of(PermissionCodes.PERMISSION_MANAGE));
    }

    private String bearerToken(String roleCode) {
        return "Bearer " + jwtTokenProvider.createAccessToken(ACTOR_ID, TENANT_ID, roleCode).token();
    }

    private PermissionProfile adminPermissionManage() {
        return PermissionProfile.create(
                PROFILE_ID,
                TENANT_ID,
                "ADMIN",
                PermissionCodes.PERMISSION_MANAGE,
                PermissionAccessLevel.MANAGE,
                PermissionScope.TENANT,
                ACTOR_ID,
                NOW
        );
    }

    private PermissionProfile salesLeadImport(PermissionAccessLevel accessLevel) {
        return PermissionProfile.create(
                PROFILE_ID,
                TENANT_ID,
                "SALES_LEAD",
                PermissionCodes.IMPORT_MANAGE,
                accessLevel,
                PermissionScope.BRANCH,
                ACTOR_ID,
                NOW
        );
    }
}
