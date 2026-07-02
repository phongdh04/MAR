package vn.mar.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.mar.audit.model.AuditActions;
import vn.mar.audit.service.AuditRecordCommand;
import vn.mar.audit.service.AuditService;
import vn.mar.catalog.dto.request.CreateCourseRequest;
import vn.mar.catalog.dto.request.CreateLanguageRequest;
import vn.mar.catalog.dto.request.CreateProgramRequest;
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
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ResourceNotFoundException;
import vn.mar.common.time.TimeProvider;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID LANGUAGE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID PROGRAM_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID COURSE_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final Instant NOW = Instant.parse("2026-07-01T08:00:00Z");

    @Mock
    private LanguageRepository languageRepository;

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CurrentUserContext currentUserContext;

    @Mock
    private AuditService auditService;

    private CatalogService catalogService;

    @BeforeEach
    void setUp() {
        TimeProvider timeProvider = () -> NOW;
        catalogService = new CatalogService(
                languageRepository,
                programRepository,
                courseRepository,
                new CatalogMapper(),
                timeProvider,
                currentUserContext,
                auditService
        );
        when(currentUserContext.currentUser()).thenReturn(new CurrentUser(
                ACTOR_ID,
                TENANT_ID,
                "ADMIN",
                Set.of("catalog.manage"),
                "req_catalog_unit_001"
        ));
    }

    @Test
    void createLanguage_whenValid_shouldDefaultActiveAndAuditCreated() {
        when(languageRepository.save(any(Language.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LanguageDetailResponse response = catalogService.createLanguage(new CreateLanguageRequest(
                "JA",
                "Japanese",
                null
        ));

        assertThat(response.code()).isEqualTo("JA");
        assertThat(response.name()).isEqualTo("Japanese");
        assertThat(response.status()).isEqualTo("ACTIVE");

        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditActions.LANGUAGE_CREATED);
        assertThat(auditCaptor.getValue().tenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void createLanguage_whenActiveNameDuplicated_shouldRejectWithConflict() {
        when(languageRepository.existsByTenantIdAndLanguageNameIgnoreCaseAndStatus(
                TENANT_ID,
                "Japanese",
                CatalogStatus.ACTIVE
        )).thenReturn(true);

        assertThatThrownBy(() -> catalogService.createLanguage(new CreateLanguageRequest(
                null,
                "Japanese",
                "ACTIVE"
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_RESOURCE);
    }

    @Test
    void createProgram_whenLanguageInactive_shouldRejectWithInvalidParentStatus() {
        when(languageRepository.findByIdAndTenantId(LANGUAGE_ID, TENANT_ID)).thenReturn(Optional.of(inactiveLanguage()));

        assertThatThrownBy(() -> catalogService.createProgram(new CreateProgramRequest(
                LANGUAGE_ID,
                null,
                "JLPT N5",
                "JLPT",
                "ACTIVE"
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARENT_STATUS);
    }

    @Test
    void createProgram_whenValid_shouldCreateUnderActiveLanguage() {
        when(languageRepository.findByIdAndTenantId(LANGUAGE_ID, TENANT_ID)).thenReturn(Optional.of(activeLanguage()));
        when(programRepository.save(any(Program.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProgramDetailResponse response = catalogService.createProgram(new CreateProgramRequest(
                LANGUAGE_ID,
                null,
                "JLPT N5",
                "JLPT",
                null
        ));

        assertThat(response.languageId()).isEqualTo(LANGUAGE_ID);
        assertThat(response.programCode()).startsWith("JLPT_N5-");
        assertThat(response.examTrack()).isEqualTo("JLPT");
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void createCourse_whenTuitionNegative_shouldRejectWithNegativeTuition() {
        when(programRepository.findByIdAndTenantId(PROGRAM_ID, TENANT_ID)).thenReturn(Optional.of(activeProgram()));

        assertThatThrownBy(() -> catalogService.createCourse(new CreateCourseRequest(
                PROGRAM_ID,
                null,
                "JLPT N5 Foundation",
                "N5",
                BigDecimal.valueOf(-1),
                "VND",
                "ACTIVE"
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NEGATIVE_TUITION);
    }

    @Test
    void createCourse_whenProgramInactive_shouldRejectWithInvalidParentStatus() {
        when(programRepository.findByIdAndTenantId(PROGRAM_ID, TENANT_ID)).thenReturn(Optional.of(inactiveProgram()));

        assertThatThrownBy(() -> catalogService.createCourse(new CreateCourseRequest(
                PROGRAM_ID,
                null,
                "JLPT N5 Foundation",
                "N5",
                BigDecimal.valueOf(4500000),
                "VND",
                "ACTIVE"
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PARENT_STATUS);
    }

    @Test
    void createCourse_whenValid_shouldDefaultCurrencyAndAuditCreated() {
        when(programRepository.findByIdAndTenantId(PROGRAM_ID, TENANT_ID)).thenReturn(Optional.of(activeProgram()));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourseDetailResponse response = catalogService.createCourse(new CreateCourseRequest(
                PROGRAM_ID,
                "N5_FOUNDATION",
                "JLPT N5 Foundation",
                "N5",
                BigDecimal.valueOf(4500000),
                null,
                null
        ));

        assertThat(response.courseCode()).isEqualTo("N5_FOUNDATION");
        assertThat(response.tuitionGross()).isEqualByComparingTo("4500000");
        assertThat(response.currency()).isEqualTo("VND");

        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditActions.COURSE_CREATED);
    }

    @Test
    void getCourse_whenBelongsToAnotherTenant_shouldThrowNotFoundAndNeverFindByIdOnly() {
        when(courseRepository.findByIdAndTenantId(COURSE_ID, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.getCourse(COURSE_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(courseRepository).findByIdAndTenantId(COURSE_ID, TENANT_ID);
        verify(courseRepository, never()).findById(COURSE_ID);
    }

    @Test
    void updateProgram_whenStatusChangedToInactive_shouldAuditStatusChanged() {
        when(programRepository.findByIdAndTenantId(PROGRAM_ID, TENANT_ID)).thenReturn(Optional.of(activeProgram()));
        when(programRepository.save(any(Program.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProgramDetailResponse response = catalogService.updateProgram(PROGRAM_ID, new UpdateProgramRequest(
                null,
                null,
                null,
                null,
                "INACTIVE",
                "Stop pilot"
        ));

        assertThat(response.status()).isEqualTo("INACTIVE");
        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditActions.PROGRAM_STATUS_CHANGED);
        assertThat(auditCaptor.getValue().reason()).isEqualTo("Stop pilot");
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

    private Program inactiveProgram() {
        return Program.restore(
                PROGRAM_ID,
                TENANT_ID,
                LANGUAGE_ID,
                "JLPT_N5",
                "JLPT N5",
                "JLPT",
                CatalogStatus.INACTIVE,
                NOW,
                ACTOR_ID,
                NOW,
                ACTOR_ID
        );
    }
}
