package vn.mar.catalog.service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.mar.audit.model.AuditActions;
import vn.mar.audit.model.AuditResourceTypes;
import vn.mar.audit.service.AuditRecordCommand;
import vn.mar.audit.service.AuditService;
import vn.mar.catalog.dto.request.CourseSearchRequest;
import vn.mar.catalog.dto.request.CreateCourseRequest;
import vn.mar.catalog.dto.request.CreateLanguageRequest;
import vn.mar.catalog.dto.request.CreateProgramRequest;
import vn.mar.catalog.dto.request.LanguageSearchRequest;
import vn.mar.catalog.dto.request.ProgramSearchRequest;
import vn.mar.catalog.dto.request.UpdateCourseRequest;
import vn.mar.catalog.dto.request.UpdateLanguageRequest;
import vn.mar.catalog.dto.request.UpdateProgramRequest;
import vn.mar.catalog.dto.response.CourseDetailResponse;
import vn.mar.catalog.dto.response.LanguageDetailResponse;
import vn.mar.catalog.dto.response.ProgramDetailResponse;
import vn.mar.catalog.entity.Course;
import vn.mar.catalog.entity.Language;
import vn.mar.catalog.entity.Program;
import vn.mar.catalog.mapper.CatalogMapper;
import vn.mar.catalog.model.CatalogStatus;
import vn.mar.catalog.repository.CourseRepository;
import vn.mar.catalog.repository.LanguageRepository;
import vn.mar.catalog.repository.ProgramRepository;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.error.ErrorDetail;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ResourceNotFoundException;
import vn.mar.common.exception.ValidationException;
import vn.mar.common.logging.LogContext;
import vn.mar.common.pagination.PageResponse;
import vn.mar.common.time.TimeProvider;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;

@Service
public class CatalogService {

    private static final int CODE_MAX_LENGTH = 50;
    private static final int CODE_SUFFIX_LENGTH = 8;
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final String DEFAULT_CURRENCY = "VND";

    private final LanguageRepository languageRepository;
    private final ProgramRepository programRepository;
    private final CourseRepository courseRepository;
    private final CatalogMapper catalogMapper;
    private final TimeProvider timeProvider;
    private final CurrentUserContext currentUserContext;
    private final AuditService auditService;

    public CatalogService(
            LanguageRepository languageRepository,
            ProgramRepository programRepository,
            CourseRepository courseRepository,
            CatalogMapper catalogMapper,
            TimeProvider timeProvider,
            CurrentUserContext currentUserContext,
            AuditService auditService) {
        this.languageRepository = languageRepository;
        this.programRepository = programRepository;
        this.courseRepository = courseRepository;
        this.catalogMapper = catalogMapper;
        this.timeProvider = timeProvider;
        this.currentUserContext = currentUserContext;
        this.auditService = auditService;
    }

    @Transactional
    public LanguageDetailResponse createLanguage(CreateLanguageRequest request) {
        CurrentUser actor = currentUserContext.currentUser();
        UUID tenantId = requireTenantContext(actor);
        Instant now = timeProvider.now();
        UUID languageId = UUID.randomUUID();
        String name = requireText(request.name(), "name", "Language name is required");
        String code = resolveCode(request.code(), name, languageId, "code", "name");
        CatalogStatus status = resolveStatus(request.status(), CatalogStatus.ACTIVE);

        assertLanguageAvailableForCreate(tenantId, code, name, status);

        Language language = Language.create(languageId, tenantId, code, name, status, actor.actorId(), now);
        Language savedLanguage = languageRepository.save(language);
        auditCatalogChange(
                AuditActions.LANGUAGE_CREATED,
                AuditResourceTypes.LANGUAGE,
                savedLanguage.tenantId(),
                savedLanguage.id(),
                savedLanguage.languageCode(),
                actor,
                null,
                catalogMapper.toAuditData(savedLanguage),
                null
        );
        return catalogMapper.toDetailResponse(savedLanguage);
    }

    @Transactional(readOnly = true)
    public LanguageDetailResponse getLanguage(UUID languageId) {
        Language language = findLanguageInCurrentTenant(languageId);
        return catalogMapper.toDetailResponse(language);
    }

    @Transactional(readOnly = true)
    public PageResponse<LanguageDetailResponse> searchLanguages(LanguageSearchRequest request) {
        UUID tenantId = requireTenantContext(currentUserContext.currentUser());
        PageRequest pageable = PageRequest.of(resolvePage(request.page()), resolveSize(request.size()), newestFirstSort());
        Page<LanguageDetailResponse> responsePage = languageRepository.search(
                        tenantId,
                        resolveStatus(request.status(), null),
                        normalizeKeyword(request.keyword()),
                        pageable
                )
                .map(catalogMapper::toDetailResponse);
        return PageResponse.from(responsePage);
    }

    @Transactional
    public LanguageDetailResponse updateLanguage(UUID languageId, UpdateLanguageRequest request) {
        CurrentUser actor = currentUserContext.currentUser();
        UUID tenantId = requireTenantContext(actor);
        Language language = findLanguage(tenantId, languageId);
        Map<String, Object> beforeData = catalogMapper.toAuditData(language);
        CatalogStatus previousStatus = language.status();

        String code = resolveCodeForUpdate(request.code(), language.languageCode(), "code");
        String name = resolveRequiredTextForUpdate(request.name(), language.languageName(), "name", "Language name is required");
        CatalogStatus status = resolveStatus(request.status(), language.status());

        assertLanguageAvailableForUpdate(tenantId, language.id(), code, name, status);

        language.update(code, name, status, actor.actorId(), timeProvider.now());
        Language savedLanguage = languageRepository.save(language);
        String action = previousStatus == savedLanguage.status()
                ? AuditActions.LANGUAGE_UPDATED
                : AuditActions.LANGUAGE_STATUS_CHANGED;
        auditCatalogChange(
                action,
                AuditResourceTypes.LANGUAGE,
                savedLanguage.tenantId(),
                savedLanguage.id(),
                savedLanguage.languageCode(),
                actor,
                beforeData,
                catalogMapper.toAuditData(savedLanguage),
                normalizeOptional(request.reason())
        );
        return catalogMapper.toDetailResponse(savedLanguage);
    }

    @Transactional
    public ProgramDetailResponse createProgram(CreateProgramRequest request) {
        CurrentUser actor = currentUserContext.currentUser();
        UUID tenantId = requireTenantContext(actor);
        Instant now = timeProvider.now();
        UUID programId = UUID.randomUUID();
        UUID languageId = requireId(request.languageId(), "language_id", "Language is required");
        assertLanguageActive(tenantId, languageId);
        String name = requireText(request.programName(), "program_name", "Program name is required");
        String code = resolveCode(request.programCode(), name, programId, "program_code", "program_name");
        String examTrack = normalizeOptional(request.examTrack());
        CatalogStatus status = resolveStatus(request.status(), CatalogStatus.ACTIVE);

        assertProgramAvailableForCreate(tenantId, languageId, code, name, status);

        Program program = Program.create(
                programId,
                tenantId,
                languageId,
                code,
                name,
                examTrack,
                status,
                actor.actorId(),
                now
        );
        Program savedProgram = programRepository.save(program);
        auditCatalogChange(
                AuditActions.PROGRAM_CREATED,
                AuditResourceTypes.PROGRAM,
                savedProgram.tenantId(),
                savedProgram.id(),
                savedProgram.programCode(),
                actor,
                null,
                catalogMapper.toAuditData(savedProgram),
                null
        );
        return catalogMapper.toDetailResponse(savedProgram);
    }

    @Transactional(readOnly = true)
    public ProgramDetailResponse getProgram(UUID programId) {
        Program program = findProgramInCurrentTenant(programId);
        return catalogMapper.toDetailResponse(program);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProgramDetailResponse> searchPrograms(ProgramSearchRequest request) {
        UUID tenantId = requireTenantContext(currentUserContext.currentUser());
        PageRequest pageable = PageRequest.of(resolvePage(request.page()), resolveSize(request.size()), newestFirstSort());
        Page<ProgramDetailResponse> responsePage = programRepository.search(
                        tenantId,
                        request.languageId(),
                        resolveStatus(request.status(), null),
                        normalizeKeyword(request.keyword()),
                        pageable
                )
                .map(catalogMapper::toDetailResponse);
        return PageResponse.from(responsePage);
    }

    @Transactional
    public ProgramDetailResponse updateProgram(UUID programId, UpdateProgramRequest request) {
        CurrentUser actor = currentUserContext.currentUser();
        UUID tenantId = requireTenantContext(actor);
        Program program = findProgram(tenantId, programId);
        Map<String, Object> beforeData = catalogMapper.toAuditData(program);
        CatalogStatus previousStatus = program.status();

        UUID languageId = request.languageId() == null ? program.languageId() : request.languageId();
        CatalogStatus status = resolveStatus(request.status(), program.status());
        if (request.languageId() != null || status == CatalogStatus.ACTIVE) {
            assertLanguageActive(tenantId, languageId);
        }
        String code = resolveCodeForUpdate(request.programCode(), program.programCode(), "program_code");
        String name = resolveRequiredTextForUpdate(request.programName(), program.programName(), "program_name", "Program name is required");
        String examTrack = resolveOptionalForUpdate(request.examTrack(), program.examTrack());

        assertProgramAvailableForUpdate(tenantId, program.id(), languageId, code, name, status);

        program.update(languageId, code, name, examTrack, status, actor.actorId(), timeProvider.now());
        Program savedProgram = programRepository.save(program);
        String action = previousStatus == savedProgram.status()
                ? AuditActions.PROGRAM_UPDATED
                : AuditActions.PROGRAM_STATUS_CHANGED;
        auditCatalogChange(
                action,
                AuditResourceTypes.PROGRAM,
                savedProgram.tenantId(),
                savedProgram.id(),
                savedProgram.programCode(),
                actor,
                beforeData,
                catalogMapper.toAuditData(savedProgram),
                normalizeOptional(request.reason())
        );
        return catalogMapper.toDetailResponse(savedProgram);
    }

    @Transactional
    public CourseDetailResponse createCourse(CreateCourseRequest request) {
        CurrentUser actor = currentUserContext.currentUser();
        UUID tenantId = requireTenantContext(actor);
        Instant now = timeProvider.now();
        UUID courseId = UUID.randomUUID();
        UUID programId = requireId(request.programId(), "program_id", "Program is required");
        assertProgramActive(tenantId, programId);
        String name = requireText(request.courseName(), "course_name", "Course name is required");
        String code = resolveCode(request.courseCode(), name, courseId, "course_code", "course_name");
        String level = normalizeOptional(request.level());
        BigDecimal tuitionGross = resolveTuition(request.tuitionGross(), BigDecimal.ZERO);
        String currency = resolveCurrency(request.currency(), DEFAULT_CURRENCY);
        CatalogStatus status = resolveStatus(request.status(), CatalogStatus.ACTIVE);

        assertCourseAvailableForCreate(tenantId, programId, code, name, status);

        Course course = Course.create(
                courseId,
                tenantId,
                programId,
                code,
                name,
                level,
                tuitionGross,
                currency,
                status,
                actor.actorId(),
                now
        );
        Course savedCourse = courseRepository.save(course);
        auditCatalogChange(
                AuditActions.COURSE_CREATED,
                AuditResourceTypes.COURSE,
                savedCourse.tenantId(),
                savedCourse.id(),
                savedCourse.courseCode(),
                actor,
                null,
                catalogMapper.toAuditData(savedCourse),
                null
        );
        return catalogMapper.toDetailResponse(savedCourse);
    }

    @Transactional(readOnly = true)
    public CourseDetailResponse getCourse(UUID courseId) {
        Course course = findCourseInCurrentTenant(courseId);
        return catalogMapper.toDetailResponse(course);
    }

    @Transactional(readOnly = true)
    public PageResponse<CourseDetailResponse> searchCourses(CourseSearchRequest request) {
        UUID tenantId = requireTenantContext(currentUserContext.currentUser());
        PageRequest pageable = PageRequest.of(resolvePage(request.page()), resolveSize(request.size()), newestFirstSort());
        Page<CourseDetailResponse> responsePage = courseRepository.search(
                        tenantId,
                        request.programId(),
                        resolveStatus(request.status(), null),
                        normalizeKeyword(request.keyword()),
                        pageable
                )
                .map(catalogMapper::toDetailResponse);
        return PageResponse.from(responsePage);
    }

    @Transactional
    public CourseDetailResponse updateCourse(UUID courseId, UpdateCourseRequest request) {
        CurrentUser actor = currentUserContext.currentUser();
        UUID tenantId = requireTenantContext(actor);
        Course course = findCourse(tenantId, courseId);
        Map<String, Object> beforeData = catalogMapper.toAuditData(course);
        CatalogStatus previousStatus = course.status();

        UUID programId = request.programId() == null ? course.programId() : request.programId();
        CatalogStatus status = resolveStatus(request.status(), course.status());
        if (request.programId() != null || status == CatalogStatus.ACTIVE) {
            assertProgramActive(tenantId, programId);
        }
        String code = resolveCodeForUpdate(request.courseCode(), course.courseCode(), "course_code");
        String name = resolveRequiredTextForUpdate(request.courseName(), course.courseName(), "course_name", "Course name is required");
        String level = resolveOptionalForUpdate(request.level(), course.level());
        BigDecimal tuitionGross = resolveTuition(request.tuitionGross(), course.tuitionAmount());
        String currency = resolveCurrencyForUpdate(request.currency(), course.currency());

        assertCourseAvailableForUpdate(tenantId, course.id(), programId, code, name, status);

        course.update(programId, code, name, level, tuitionGross, currency, status, actor.actorId(), timeProvider.now());
        Course savedCourse = courseRepository.save(course);
        String action = previousStatus == savedCourse.status()
                ? AuditActions.COURSE_UPDATED
                : AuditActions.COURSE_STATUS_CHANGED;
        auditCatalogChange(
                action,
                AuditResourceTypes.COURSE,
                savedCourse.tenantId(),
                savedCourse.id(),
                savedCourse.courseCode(),
                actor,
                beforeData,
                catalogMapper.toAuditData(savedCourse),
                normalizeOptional(request.reason())
        );
        return catalogMapper.toDetailResponse(savedCourse);
    }

    private Language findLanguageInCurrentTenant(UUID languageId) {
        return findLanguage(requireTenantContext(currentUserContext.currentUser()), languageId);
    }

    private Program findProgramInCurrentTenant(UUID programId) {
        return findProgram(requireTenantContext(currentUserContext.currentUser()), programId);
    }

    private Course findCourseInCurrentTenant(UUID courseId) {
        return findCourse(requireTenantContext(currentUserContext.currentUser()), courseId);
    }

    private Language findLanguage(UUID tenantId, UUID languageId) {
        return languageRepository.findByIdAndTenantId(languageId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Language not found"));
    }

    private Program findProgram(UUID tenantId, UUID programId) {
        return programRepository.findByIdAndTenantId(programId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found"));
    }

    private Course findCourse(UUID tenantId, UUID courseId) {
        return courseRepository.findByIdAndTenantId(courseId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
    }

    private void assertLanguageActive(UUID tenantId, UUID languageId) {
        Language language = findLanguage(tenantId, languageId);
        if (language.status() != CatalogStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_PARENT_STATUS, "Language is inactive");
        }
    }

    private void assertProgramActive(UUID tenantId, UUID programId) {
        Program program = findProgram(tenantId, programId);
        if (program.status() != CatalogStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_PARENT_STATUS, "Program is inactive");
        }
    }

    private void assertLanguageAvailableForCreate(
            UUID tenantId,
            String code,
            String name,
            CatalogStatus status) {
        if (status != CatalogStatus.ACTIVE) {
            return;
        }
        if (languageRepository.existsByTenantIdAndLanguageCodeIgnoreCaseAndStatus(tenantId, code, CatalogStatus.ACTIVE)
                || languageRepository.existsByTenantIdAndLanguageNameIgnoreCaseAndStatus(tenantId, name, CatalogStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Active language already exists");
        }
    }

    private void assertLanguageAvailableForUpdate(
            UUID tenantId,
            UUID languageId,
            String code,
            String name,
            CatalogStatus status) {
        if (status != CatalogStatus.ACTIVE) {
            return;
        }
        if (languageRepository.existsByTenantIdAndLanguageCodeIgnoreCaseAndStatusAndIdNot(tenantId, code, CatalogStatus.ACTIVE, languageId)
                || languageRepository.existsByTenantIdAndLanguageNameIgnoreCaseAndStatusAndIdNot(tenantId, name, CatalogStatus.ACTIVE, languageId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Active language already exists");
        }
    }

    private void assertProgramAvailableForCreate(
            UUID tenantId,
            UUID languageId,
            String code,
            String name,
            CatalogStatus status) {
        if (status != CatalogStatus.ACTIVE) {
            return;
        }
        if (programRepository.existsByTenantIdAndProgramCodeIgnoreCaseAndStatus(tenantId, code, CatalogStatus.ACTIVE)
                || programRepository.existsByTenantIdAndLanguageIdAndProgramNameIgnoreCaseAndStatus(tenantId, languageId, name, CatalogStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Active program already exists");
        }
    }

    private void assertProgramAvailableForUpdate(
            UUID tenantId,
            UUID programId,
            UUID languageId,
            String code,
            String name,
            CatalogStatus status) {
        if (status != CatalogStatus.ACTIVE) {
            return;
        }
        if (programRepository.existsByTenantIdAndProgramCodeIgnoreCaseAndStatusAndIdNot(tenantId, code, CatalogStatus.ACTIVE, programId)
                || programRepository.existsByTenantIdAndLanguageIdAndProgramNameIgnoreCaseAndStatusAndIdNot(tenantId, languageId, name, CatalogStatus.ACTIVE, programId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Active program already exists");
        }
    }

    private void assertCourseAvailableForCreate(
            UUID tenantId,
            UUID programId,
            String code,
            String name,
            CatalogStatus status) {
        if (status != CatalogStatus.ACTIVE) {
            return;
        }
        if (courseRepository.existsByTenantIdAndCourseCodeIgnoreCaseAndStatus(tenantId, code, CatalogStatus.ACTIVE)
                || courseRepository.existsByTenantIdAndProgramIdAndCourseNameIgnoreCaseAndStatus(tenantId, programId, name, CatalogStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Active course already exists");
        }
    }

    private void assertCourseAvailableForUpdate(
            UUID tenantId,
            UUID courseId,
            UUID programId,
            String code,
            String name,
            CatalogStatus status) {
        if (status != CatalogStatus.ACTIVE) {
            return;
        }
        if (courseRepository.existsByTenantIdAndCourseCodeIgnoreCaseAndStatusAndIdNot(tenantId, code, CatalogStatus.ACTIVE, courseId)
                || courseRepository.existsByTenantIdAndProgramIdAndCourseNameIgnoreCaseAndStatusAndIdNot(tenantId, programId, name, CatalogStatus.ACTIVE, courseId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Active course already exists");
        }
    }

    private UUID requireId(UUID id, String field, String message) {
        if (id == null) {
            throw validation(field, "REQUIRED", message);
        }
        return id;
    }

    private String requireText(String value, String field, String message) {
        if (!StringUtils.hasText(value)) {
            throw validation(field, "REQUIRED", message);
        }
        return value.trim();
    }

    private String resolveRequiredTextForUpdate(String requestedValue, String currentValue, String field, String message) {
        if (requestedValue == null) {
            return currentValue;
        }
        return requireText(requestedValue, field, message);
    }

    private String resolveOptionalForUpdate(String requestedValue, String currentValue) {
        if (requestedValue == null) {
            return currentValue;
        }
        return normalizeOptional(requestedValue);
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private CatalogStatus resolveStatus(String requestedStatus, CatalogStatus fallbackStatus) {
        if (requestedStatus == null) {
            return fallbackStatus;
        }
        if (!StringUtils.hasText(requestedStatus)) {
            throw validation("status", "INVALID_STATUS", "Catalog status is invalid");
        }
        try {
            return CatalogStatus.valueOf(requestedStatus.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw validation("status", "INVALID_STATUS", "Catalog status is invalid");
        }
    }

    private String resolveCode(String requestedCode, String name, UUID id, String codeField, String nameField) {
        if (StringUtils.hasText(requestedCode)) {
            return normalizeCode(requestedCode, codeField);
        }

        String suffix = id.toString().substring(0, CODE_SUFFIX_LENGTH).toUpperCase(Locale.ROOT);
        String prefix = normalizeCode(name, nameField);
        int maxPrefixLength = CODE_MAX_LENGTH - suffix.length() - 1;
        if (prefix.length() > maxPrefixLength) {
            prefix = prefix.substring(0, maxPrefixLength);
        }
        return "%s-%s".formatted(prefix, suffix);
    }

    private String resolveCodeForUpdate(String requestedCode, String currentCode, String codeField) {
        if (requestedCode == null) {
            return currentCode;
        }
        return normalizeCode(requestedCode, codeField);
    }

    private String normalizeCode(String source, String field) {
        String ascii = Normalizer.normalize(source, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String code = ascii.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (!StringUtils.hasText(code)) {
            throw validation(field, "INVALID_FORMAT", "Catalog code is invalid");
        }
        if (code.length() > CODE_MAX_LENGTH) {
            return code.substring(0, CODE_MAX_LENGTH);
        }
        return code;
    }

    private BigDecimal resolveTuition(BigDecimal requestedTuition, BigDecimal fallbackTuition) {
        BigDecimal tuition = requestedTuition == null ? fallbackTuition : requestedTuition;
        if (tuition.signum() < 0) {
            throw new BusinessException(ErrorCode.NEGATIVE_TUITION, ErrorCode.NEGATIVE_TUITION.defaultMessage());
        }
        return tuition;
    }

    private String resolveCurrency(String requestedCurrency, String fallbackCurrency) {
        if (requestedCurrency == null) {
            return fallbackCurrency;
        }
        return normalizeCurrency(requestedCurrency);
    }

    private String resolveCurrencyForUpdate(String requestedCurrency, String currentCurrency) {
        if (requestedCurrency == null) {
            return currentCurrency;
        }
        return normalizeCurrency(requestedCurrency);
    }

    private String normalizeCurrency(String requestedCurrency) {
        if (!StringUtils.hasText(requestedCurrency)) {
            throw validation("currency", "INVALID_CURRENCY", "Currency is invalid");
        }
        String currency = requestedCurrency.trim().toUpperCase(Locale.ROOT);
        try {
            Currency.getInstance(currency);
            return currency;
        } catch (IllegalArgumentException exception) {
            throw validation("currency", "INVALID_CURRENCY", "Currency is invalid");
        }
    }

    private int resolvePage(Integer requestedPage) {
        if (requestedPage == null) {
            return DEFAULT_PAGE;
        }
        if (requestedPage < 0) {
            throw validation("page", "MIN_VALUE", "Page must be greater than or equal to 0");
        }
        return requestedPage;
    }

    private int resolveSize(Integer requestedSize) {
        if (requestedSize == null) {
            return DEFAULT_SIZE;
        }
        if (requestedSize < 1 || requestedSize > MAX_SIZE) {
            throw validation("size", "INVALID_SIZE", "Size must be between 1 and 100");
        }
        return requestedSize;
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private Sort newestFirstSort() {
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    private UUID requireTenantContext(CurrentUser actor) {
        if (actor.tenantId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Tenant context is required");
        }
        return actor.tenantId();
    }

    private void auditCatalogChange(
            String action,
            String resourceType,
            UUID tenantId,
            UUID resourceId,
            String resourceKey,
            CurrentUser actor,
            Map<String, Object> beforeData,
            Map<String, Object> afterData,
            String reason) {
        auditService.record(new AuditRecordCommand(
                tenantId,
                actor.actorId(),
                "USER",
                actor.roleCode(),
                action,
                resourceType,
                resourceId,
                resourceKey,
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

    private ValidationException validation(String field, String code, String message) {
        return new ValidationException(
                ErrorCode.VALIDATION_ERROR.defaultMessage(),
                List.of(ErrorDetail.of(field, code, message))
        );
    }
}
