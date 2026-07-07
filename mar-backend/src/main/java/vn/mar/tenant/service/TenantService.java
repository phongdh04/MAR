package vn.mar.tenant.service;

import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.mar.audit.model.AuditActions;
import vn.mar.audit.model.AuditResourceTypes;
import vn.mar.audit.service.AuditRecordCommand;
import vn.mar.audit.service.AuditService;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.error.ErrorDetail;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ResourceNotFoundException;
import vn.mar.common.exception.ValidationException;
import vn.mar.common.logging.LogContext;
import vn.mar.common.search.SearchText;
import vn.mar.common.time.TimeProvider;
import vn.mar.common.validation.EnumParser;
import vn.mar.permission.service.PermissionMatrixInitializationService;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;
import vn.mar.tenant.dto.request.CreateTenantRequest;
import vn.mar.tenant.dto.request.UpdateTenantRequest;
import vn.mar.tenant.dto.response.TenantDetailResponse;
import vn.mar.tenant.entity.Tenant;
import vn.mar.tenant.mapper.TenantMapper;
import vn.mar.tenant.model.TenantStatus;
import vn.mar.tenant.repository.TenantRepository;

@Service
public class TenantService {

    private static final String DEFAULT_TIMEZONE = "Asia/Ho_Chi_Minh";
    private static final String DEFAULT_CURRENCY = "VND";
    private static final int TENANT_CODE_MAX_LENGTH = 50;
    private static final int TENANT_CODE_SUFFIX_LENGTH = 8;

    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;
    private final TimeProvider timeProvider;
    private final CurrentUserContext currentUserContext;
    private final AuditService auditService;
    private final PermissionMatrixInitializationService permissionMatrixInitializationService;

    public TenantService(
            TenantRepository tenantRepository,
            TenantMapper tenantMapper,
            TimeProvider timeProvider,
            CurrentUserContext currentUserContext,
            AuditService auditService,
            PermissionMatrixInitializationService permissionMatrixInitializationService) {
        this.tenantRepository = tenantRepository;
        this.tenantMapper = tenantMapper;
        this.timeProvider = timeProvider;
        this.currentUserContext = currentUserContext;
        this.auditService = auditService;
        this.permissionMatrixInitializationService = permissionMatrixInitializationService;
    }

    @Transactional
    public TenantDetailResponse createTenant(CreateTenantRequest request) {
        CurrentUser actor = currentUserContext.currentUser();
        Instant now = timeProvider.now();
        UUID tenantId = UUID.randomUUID();
        String tenantName = requireTenantName(request.tenantName());
        String tenantCode = resolveTenantCode(request.tenantCode(), tenantName, tenantId);
        assertTenantCodeAvailable(tenantCode);

        Tenant tenant = Tenant.create(
                tenantId,
                tenantCode,
                tenantName,
                resolveTimezone(request.timezone(), DEFAULT_TIMEZONE),
                resolveCurrency(request.defaultCurrency(), DEFAULT_CURRENCY),
                resolveStatus(request.status(), TenantStatus.ACTIVE),
                actor.actorId(),
                now
        );
        Tenant savedTenant = tenantRepository.save(tenant);
        permissionMatrixInitializationService.initializeDefaults(savedTenant.id(), actor.actorId(), now);
        auditTenantChange(AuditActions.TENANT_CREATED, actor, savedTenant, null, tenantMapper.toAuditData(savedTenant), null);
        return tenantMapper.toDetailResponse(savedTenant);
    }

    @Transactional(readOnly = true)
    public TenantDetailResponse getTenant(UUID tenantId) {
        Tenant tenant = findTenant(tenantId);
        return tenantMapper.toDetailResponse(tenant);
    }

    @Transactional
    public TenantDetailResponse updateTenant(UUID tenantId, UpdateTenantRequest request) {
        CurrentUser actor = currentUserContext.currentUser();
        Tenant tenant = findTenant(tenantId);
        Map<String, Object> beforeData = tenantMapper.toAuditData(tenant);
        TenantStatus previousStatus = tenant.status();

        tenant.update(
                resolveTenantNameForUpdate(request.tenantName(), tenant.tenantName()),
                resolveTimezone(request.timezone(), tenant.timezone()),
                resolveCurrency(request.defaultCurrency(), tenant.defaultCurrency()),
                resolveStatus(request.status(), tenant.status()),
                actor.actorId(),
                timeProvider.now()
        );

        Tenant savedTenant = tenantRepository.save(tenant);
        String action = previousStatus == savedTenant.status()
                ? AuditActions.TENANT_UPDATED
                : AuditActions.TENANT_STATUS_CHANGED;
        auditTenantChange(
                action,
                actor,
                savedTenant,
                beforeData,
                tenantMapper.toAuditData(savedTenant),
                normalizeReason(request.reason())
        );
        return tenantMapper.toDetailResponse(savedTenant);
    }

    private Tenant findTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    }

    private String requireTenantName(String tenantName) {
        if (!StringUtils.hasText(tenantName)) {
            throw ValidationException.of("tenant_name", "REQUIRED", "Tenant name is required");
        }
        return tenantName.trim();
    }

    private String resolveTenantNameForUpdate(String requestedTenantName, String currentTenantName) {
        if (requestedTenantName == null) {
            return currentTenantName;
        }
        return requireTenantName(requestedTenantName);
    }

    private String resolveTimezone(String requestedTimezone, String fallbackTimezone) {
        if (requestedTimezone == null) {
            return fallbackTimezone;
        }
        if (!StringUtils.hasText(requestedTimezone)) {
            throw ValidationException.of("timezone", "INVALID_TIMEZONE", "Timezone is invalid");
        }
        String timezone = requestedTimezone.trim();
        try {
            ZoneId.of(timezone);
            return timezone;
        } catch (DateTimeException exception) {
            throw ValidationException.of("timezone", "INVALID_TIMEZONE", "Timezone is invalid");
        }
    }

    private String resolveCurrency(String requestedCurrency, String fallbackCurrency) {
        if (requestedCurrency == null) {
            return fallbackCurrency;
        }
        if (!StringUtils.hasText(requestedCurrency)) {
            throw ValidationException.of("default_currency", "INVALID_CURRENCY", "Default currency is invalid");
        }
        String currency = requestedCurrency.trim().toUpperCase(Locale.ROOT);
        try {
            Currency.getInstance(currency);
            return currency;
        } catch (IllegalArgumentException exception) {
            throw ValidationException.of("default_currency", "INVALID_CURRENCY", "Default currency is invalid");
        }
    }

    private TenantStatus resolveStatus(String requestedStatus, TenantStatus fallbackStatus) {
        if (requestedStatus == null) {
            return fallbackStatus;
        }
        return EnumParser.requiredEnum(TenantStatus.class, requestedStatus, "status", "INVALID_STATUS", "Tenant status is invalid");
    }

    private String resolveTenantCode(String requestedTenantCode, String tenantName, UUID tenantId) {
        if (StringUtils.hasText(requestedTenantCode)) {
            return normalizeTenantCode(requestedTenantCode, "tenant_code");
        }

        String suffix = tenantId.toString().substring(0, TENANT_CODE_SUFFIX_LENGTH).toUpperCase(Locale.ROOT);
        String prefix = normalizeTenantCode(tenantName, "tenant_name");
        int maxPrefixLength = TENANT_CODE_MAX_LENGTH - suffix.length() - 1;
        if (prefix.length() > maxPrefixLength) {
            prefix = prefix.substring(0, maxPrefixLength);
        }
        return "%s-%s".formatted(prefix, suffix);
    }

    private String normalizeTenantCode(String source, String fieldName) {
        String ascii = Normalizer.normalize(source, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String code = ascii.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (!StringUtils.hasText(code)) {
            throw ValidationException.of(fieldName, "INVALID_FORMAT", "Tenant code is invalid");
        }
        if (code.length() > TENANT_CODE_MAX_LENGTH) {
            return code.substring(0, TENANT_CODE_MAX_LENGTH);
        }
        return code;
    }

    private void assertTenantCodeAvailable(String tenantCode) {
        if (tenantRepository.existsByTenantCodeIgnoreCase(tenantCode)) {
            throw new BusinessException(ErrorCode.DUPLICATE_TENANT_CODE, ErrorCode.DUPLICATE_TENANT_CODE.defaultMessage());
        }
    }

    private void auditTenantChange(
            String action,
            CurrentUser actor,
            Tenant tenant,
            Map<String, Object> beforeData,
            Map<String, Object> afterData,
            String reason) {
        auditService.record(new AuditRecordCommand(
                tenant.id(),
                actor.actorId(),
                "USER",
                actor.roleCode(),
                action,
                AuditResourceTypes.TENANT,
                tenant.id(),
                tenant.tenantCode(),
                beforeData,
                afterData,
                auditMetadata(actor),
                reason,
                LogContext.requestId()
        ));
    }

    private Map<String, Object> auditMetadata(CurrentUser actor) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (actor.tenantId() != null) {
            metadata.put("actor_tenant_id", actor.tenantId().toString());
        }
        return metadata;
    }

    private String normalizeReason(String reason) {
        return SearchText.textOrNull(reason);
    }

}
