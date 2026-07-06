package vn.mar.opportunity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.mar.audit.model.AuditActions;
import vn.mar.audit.service.AuditRecordCommand;
import vn.mar.audit.service.AuditService;
import vn.mar.branch.entity.Branch;
import vn.mar.branch.model.BranchStatus;
import vn.mar.branch.repository.BranchRepository;
import vn.mar.catalog.entity.Language;
import vn.mar.catalog.entity.Program;
import vn.mar.catalog.model.CatalogStatus;
import vn.mar.catalog.repository.CourseRepository;
import vn.mar.catalog.repository.LanguageRepository;
import vn.mar.catalog.repository.ProgramRepository;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ValidationException;
import vn.mar.common.time.TimeProvider;
import vn.mar.customer.entity.CustomerProfile;
import vn.mar.customer.repository.CustomerProfileRepository;
import vn.mar.lead.entity.Lead;
import vn.mar.lead.model.LeadSourceType;
import vn.mar.lead.model.LeadTemperature;
import vn.mar.lead.repository.LeadRepository;
import vn.mar.opportunity.api.AdmissionOpportunitySearchCommand;
import vn.mar.opportunity.api.AdmissionOpportunitySnapshot;
import vn.mar.opportunity.api.ChangeOpportunityStageCommand;
import vn.mar.opportunity.api.StageChangeSnapshot;
import vn.mar.opportunity.api.CreateAdmissionOpportunityCommand;
import vn.mar.opportunity.api.CreateOpportunityActivityCommand;
import vn.mar.opportunity.api.OpportunityActivitySearchCommand;
import vn.mar.opportunity.api.OpportunityActivitySnapshot;
import vn.mar.opportunity.api.StageHistorySnapshot;
import vn.mar.opportunity.entity.Activity;
import vn.mar.opportunity.entity.AdmissionOpportunity;
import vn.mar.opportunity.entity.StageHistory;
import vn.mar.opportunity.entity.Touchpoint;
import vn.mar.opportunity.mapper.AdmissionOpportunityMapper;
import vn.mar.opportunity.model.ActivityActorType;
import vn.mar.opportunity.model.ActivityResult;
import vn.mar.opportunity.model.ActivitySource;
import vn.mar.opportunity.model.ActivityType;
import vn.mar.opportunity.model.LostReason;
import vn.mar.opportunity.model.OpportunityStage;
import vn.mar.opportunity.model.TouchpointType;
import vn.mar.opportunity.repository.ActivityRepository;
import vn.mar.opportunity.repository.AdmissionOpportunityRepository;
import vn.mar.opportunity.repository.StageHistoryRepository;
import vn.mar.opportunity.repository.TouchpointRepository;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;
import vn.mar.sla.api.CompleteFirstResponseSlaTaskCommand;
import vn.mar.sla.api.SlaTaskManagementService;
import vn.mar.user.entity.User;
import vn.mar.user.model.UserStatus;
import vn.mar.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdmissionOpportunityServiceTest {

    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID CUSTOMER_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID LEAD_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID OLD_LEAD_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OPPORTUNITY_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final UUID LANGUAGE_ID = UUID.fromString("12121212-1212-1212-1212-121212121212");
    private static final UUID PROGRAM_ID = UUID.fromString("23232323-2323-2323-2323-232323232323");
    private static final UUID BRANCH_ID = UUID.fromString("34343434-3434-3434-3434-343434343434");
    private static final UUID OWNER_ID = UUID.fromString("45454545-4545-4545-4545-454545454545");
    private static final UUID OTHER_OWNER_ID = UUID.fromString("56565656-5656-5656-5656-565656565656");
    private static final Instant NOW = Instant.parse("2026-07-06T01:00:00Z");

    @Mock
    private AdmissionOpportunityRepository admissionOpportunityRepository;

    @Mock
    private TouchpointRepository touchpointRepository;

    @Mock
    private StageHistoryRepository stageHistoryRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private LanguageRepository languageRepository;

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserContext currentUserContext;

    @Mock
    private AuditService auditService;

    @Mock
    private SlaTaskManagementService slaTaskManagementService;

    private AdmissionOpportunityService admissionOpportunityService;

    @BeforeEach
    void setUp() {
        TimeProvider timeProvider = () -> NOW;
        admissionOpportunityService = new AdmissionOpportunityService(
                admissionOpportunityRepository,
                touchpointRepository,
                stageHistoryRepository,
                activityRepository,
                leadRepository,
                customerProfileRepository,
                languageRepository,
                programRepository,
                courseRepository,
                branchRepository,
                userRepository,
                new AdmissionOpportunityMapper(),
                slaTaskManagementService,
                timeProvider,
                currentUserContext,
                auditService
        );
    }

    @Test
    void createOrLinkFromLead_whenSameActiveProgramExists_shouldReuseOpportunityAndCreateTouchpoint() {
        allowAdmin("opportunity.update");
        when(leadRepository.findByIdAndTenantId(LEAD_ID, TENANT_ID)).thenReturn(Optional.of(lead(LEAD_ID, CUSTOMER_ID)));
        when(customerProfileRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID)).thenReturn(Optional.of(customer()));
        when(programRepository.findByIdAndTenantId(PROGRAM_ID, TENANT_ID)).thenReturn(Optional.of(program()));
        when(languageRepository.findByIdAndTenantId(LANGUAGE_ID, TENANT_ID)).thenReturn(Optional.of(language()));
        when(branchRepository.findByIdAndTenantId(BRANCH_ID, TENANT_ID)).thenReturn(Optional.of(branch()));
        when(userRepository.findByIdAndTenantId(OWNER_ID, TENANT_ID)).thenReturn(Optional.of(owner(OWNER_ID)));
        when(admissionOpportunityRepository.findFirstByTenantIdAndCustomerIdAndProgramIdAndCurrentStageInOrderByCreatedAtDesc(
                eq(TENANT_ID),
                eq(CUSTOMER_ID),
                eq(PROGRAM_ID),
                anyCollection()
        )).thenReturn(Optional.of(opportunity(OPPORTUNITY_ID, OLD_LEAD_ID, OWNER_ID)));
        when(touchpointRepository.save(any(Touchpoint.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(admissionOpportunityRepository.save(any(AdmissionOpportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(leadRepository.save(any(Lead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdmissionOpportunitySnapshot snapshot = admissionOpportunityService.createOrLinkFromLead(new CreateAdmissionOpportunityCommand(
                TENANT_ID,
                CUSTOMER_ID,
                LEAD_ID,
                null,
                PROGRAM_ID,
                null,
                BRANCH_ID,
                OWNER_ID,
                LeadTemperature.HOT,
                TouchpointType.IMPORT,
                NOW
        ));

        assertThat(snapshot.opportunityId()).isEqualTo(OPPORTUNITY_ID);
        assertThat(snapshot.programId()).isEqualTo(PROGRAM_ID);
        assertThat(snapshot.lastTouchId()).isNotNull();
        ArgumentCaptor<AdmissionOpportunity> opportunityCaptor = ArgumentCaptor.forClass(AdmissionOpportunity.class);
        verify(admissionOpportunityRepository).save(opportunityCaptor.capture());
        assertThat(opportunityCaptor.getValue().id()).isEqualTo(OPPORTUNITY_ID);
        verify(leadRepository).save(any(Lead.class));
    }

    @Test
    void createOrLinkFromLead_whenProgramDifferent_shouldCreateNewOpportunity() {
        allowAdmin("opportunity.update");
        when(leadRepository.findByIdAndTenantId(LEAD_ID, TENANT_ID)).thenReturn(Optional.of(lead(LEAD_ID, CUSTOMER_ID)));
        when(customerProfileRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID)).thenReturn(Optional.of(customer()));
        when(programRepository.findByIdAndTenantId(PROGRAM_ID, TENANT_ID)).thenReturn(Optional.of(program()));
        when(languageRepository.findByIdAndTenantId(LANGUAGE_ID, TENANT_ID)).thenReturn(Optional.of(language()));
        when(branchRepository.findByIdAndTenantId(BRANCH_ID, TENANT_ID)).thenReturn(Optional.of(branch()));
        when(admissionOpportunityRepository.findFirstByTenantIdAndCustomerIdAndProgramIdAndCurrentStageInOrderByCreatedAtDesc(
                eq(TENANT_ID),
                eq(CUSTOMER_ID),
                eq(PROGRAM_ID),
                anyCollection()
        )).thenReturn(Optional.empty());
        when(admissionOpportunityRepository.save(any(AdmissionOpportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(touchpointRepository.save(any(Touchpoint.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(leadRepository.save(any(Lead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdmissionOpportunitySnapshot snapshot = admissionOpportunityService.createOrLinkFromLead(new CreateAdmissionOpportunityCommand(
                TENANT_ID,
                CUSTOMER_ID,
                LEAD_ID,
                null,
                PROGRAM_ID,
                null,
                null,
                null,
                null,
                null,
                null
        ));

        assertThat(snapshot.opportunityId()).isNotNull();
        assertThat(snapshot.opportunityId()).isNotEqualTo(OPPORTUNITY_ID);
        assertThat(snapshot.currentStage()).isEqualTo(OpportunityStage.NEW.name());
        assertThat(snapshot.firstTouchId()).isNotNull();
        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService, org.mockito.Mockito.atLeastOnce()).record(auditCaptor.capture());
        assertThat(auditCaptor.getAllValues())
                .extracting(AuditRecordCommand::action)
                .contains(AuditActions.OPPORTUNITY_CREATED, AuditActions.TOUCHPOINT_CREATED);
    }

    @Test
    void searchOpportunities_whenAdvisorRequestsOtherOwner_shouldReject() {
        when(currentUserContext.currentUser()).thenReturn(new CurrentUser(
                OWNER_ID,
                TENANT_ID,
                "ADVISOR",
                Set.of("opportunity.update"),
                "req_opp_unit_001"
        ));

        assertThatThrownBy(() -> admissionOpportunityService.searchOpportunities(new AdmissionOpportunitySearchCommand(
                OTHER_OWNER_ID,
                null,
                null,
                null,
                0,
                20
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    void getStageHistory_whenLeadViewPermission_shouldReturnTimeline() {
        allowAdmin("lead.view");
        when(admissionOpportunityRepository.findByIdAndTenantId(OPPORTUNITY_ID, TENANT_ID))
                .thenReturn(Optional.of(opportunity(OPPORTUNITY_ID, LEAD_ID, ACTOR_ID)));
        StageHistory firstHistory = StageHistory.create(
                UUID.fromString("77777777-7777-7777-7777-777777777777"),
                TENANT_ID,
                OPPORTUNITY_ID,
                OpportunityStage.NEW,
                OpportunityStage.CONTACTING,
                ACTOR_ID,
                "USER",
                NOW,
                "Started handling",
                0L
        );
        StageHistory secondHistory = StageHistory.create(
                UUID.fromString("88888888-8888-8888-8888-888888888888"),
                TENANT_ID,
                OPPORTUNITY_ID,
                OpportunityStage.CONTACTING,
                OpportunityStage.CONTACTED,
                ACTOR_ID,
                "USER",
                NOW.plusSeconds(3600),
                "Customer answered",
                3600L
        );
        when(stageHistoryRepository.findByTenantIdAndOpportunityIdOrderByChangedAtAscIdAsc(TENANT_ID, OPPORTUNITY_ID))
                .thenReturn(List.of(firstHistory, secondHistory));

        List<StageHistorySnapshot> timeline = admissionOpportunityService.getStageHistory(OPPORTUNITY_ID);

        assertThat(timeline).hasSize(2);
        assertThat(timeline)
                .extracting(StageHistorySnapshot::fromStage)
                .containsExactly("NEW", "CONTACTING");
        assertThat(timeline)
                .extracting(StageHistorySnapshot::toStage)
                .containsExactly("CONTACTING", "CONTACTED");
        assertThat(timeline.get(1).durationInPreviousStageSeconds()).isEqualTo(3600L);
        verify(stageHistoryRepository).findByTenantIdAndOpportunityIdOrderByChangedAtAscIdAsc(TENANT_ID, OPPORTUNITY_ID);
    }

    @Test
    void getStageHistory_whenAdvisorReadsOtherOwnerOpportunity_shouldRejectBeforeTimelineQuery() {
        when(currentUserContext.currentUser()).thenReturn(new CurrentUser(
                OWNER_ID,
                TENANT_ID,
                "ADVISOR",
                Set.of("lead.view"),
                "req_opp_unit_001"
        ));
        when(admissionOpportunityRepository.findByIdAndTenantId(OPPORTUNITY_ID, TENANT_ID))
                .thenReturn(Optional.of(opportunity(OPPORTUNITY_ID, LEAD_ID, OTHER_OWNER_ID)));

        assertThatThrownBy(() -> admissionOpportunityService.getStageHistory(OPPORTUNITY_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PERMISSION_DENIED);
        verify(stageHistoryRepository, never()).findByTenantIdAndOpportunityIdOrderByChangedAtAscIdAsc(TENANT_ID, OPPORTUNITY_ID);
    }

    @Test
    void createActivity_whenConnectedCall_shouldPersistActivityReturnSlaFlagsAndAuditWithoutNoteContent() {
        allowAdmin("activity.create");
        when(admissionOpportunityRepository.findByIdAndTenantId(OPPORTUNITY_ID, TENANT_ID))
                .thenReturn(Optional.of(opportunity(OPPORTUNITY_ID, LEAD_ID, ACTOR_ID)));
        when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OpportunityActivitySnapshot snapshot = admissionOpportunityService.createActivity(new CreateOpportunityActivityCommand(
                OPPORTUNITY_ID,
                "CALL",
                "CONNECTED",
                NOW.plusSeconds(30),
                "Customer shared private budget context",
                NOW.plusSeconds(3600),
                "MANUAL"
        ));

        assertThat(snapshot.activityId()).isNotNull();
        assertThat(snapshot.activityType()).isEqualTo("CALL");
        assertThat(snapshot.activityResult()).isEqualTo("CONNECTED");
        assertThat(snapshot.firstResponseCandidate()).isTrue();
        assertThat(snapshot.contactSuccess()).isTrue();
        ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(activityCaptor.capture());
        assertThat(activityCaptor.getValue().customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(activityCaptor.getValue().actorType()).isEqualTo(ActivityActorType.USER);
        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditActions.ACTIVITY_CREATED);
        assertThat(auditCaptor.getValue().afterData())
                .containsEntry("note_present", true)
                .doesNotContainValue("Customer shared private budget context");
        ArgumentCaptor<CompleteFirstResponseSlaTaskCommand> slaCommandCaptor =
                ArgumentCaptor.forClass(CompleteFirstResponseSlaTaskCommand.class);
        verify(slaTaskManagementService).completeFirstResponseTask(slaCommandCaptor.capture());
        assertThat(slaCommandCaptor.getValue().opportunityId()).isEqualTo(OPPORTUNITY_ID);
        assertThat(slaCommandCaptor.getValue().activityId()).isEqualTo(snapshot.activityId());
        assertThat(slaCommandCaptor.getValue().occurredAt()).isEqualTo(NOW.plusSeconds(30));
    }

    @Test
    void createActivity_whenOutboundFailed_shouldPersistActivityButNotCompleteFirstResponseSla() {
        allowAdmin("activity.create");
        when(admissionOpportunityRepository.findByIdAndTenantId(OPPORTUNITY_ID, TENANT_ID))
                .thenReturn(Optional.of(opportunity(OPPORTUNITY_ID, LEAD_ID, ACTOR_ID)));
        when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OpportunityActivitySnapshot snapshot = admissionOpportunityService.createActivity(new CreateOpportunityActivityCommand(
                OPPORTUNITY_ID,
                "CALL",
                "FAILED",
                NOW.plusSeconds(45),
                "Line unavailable",
                null,
                "MANUAL"
        ));

        assertThat(snapshot.activityType()).isEqualTo("CALL");
        assertThat(snapshot.activityResult()).isEqualTo("FAILED");
        assertThat(snapshot.firstResponseCandidate()).isFalse();
        assertThat(snapshot.contactSuccess()).isFalse();
        verify(activityRepository).save(any(Activity.class));
        verify(auditService).record(any(AuditRecordCommand.class));
        verify(slaTaskManagementService, never()).completeFirstResponseTask(any(CompleteFirstResponseSlaTaskCommand.class));
    }

    @Test
    void searchActivities_whenActivityViewPermission_shouldReturnPaginatedTimeline() {
        allowAdmin("activity.view");
        when(admissionOpportunityRepository.findByIdAndTenantId(OPPORTUNITY_ID, TENANT_ID))
                .thenReturn(Optional.of(opportunity(OPPORTUNITY_ID, LEAD_ID, ACTOR_ID)));
        Activity activity = activity(
                UUID.fromString("99999999-9999-9999-9999-999999999999"),
                ActivityType.ZALO,
                ActivityResult.REPLIED,
                NOW.plusSeconds(120)
        );
        when(activityRepository.findByTenantIdAndOpportunityIdOrderByOccurredAtDescIdDesc(
                eq(TENANT_ID),
                eq(OPPORTUNITY_ID),
                any(PageRequest.class)
        )).thenReturn(new PageImpl<>(List.of(activity), PageRequest.of(0, 20), 1));

        var response = admissionOpportunityService.searchActivities(new OpportunityActivitySearchCommand(
                OPPORTUNITY_ID,
                0,
                20
        ));

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.items().getFirst().activityType()).isEqualTo("ZALO");
        assertThat(response.items().getFirst().contactSuccess()).isTrue();
    }

    @Test
    void createActivity_whenAdvisorCreatesForOtherOwnerOpportunity_shouldRejectBeforeSave() {
        when(currentUserContext.currentUser()).thenReturn(new CurrentUser(
                OWNER_ID,
                TENANT_ID,
                "ADVISOR",
                Set.of("activity.create"),
                "req_opp_unit_001"
        ));
        when(admissionOpportunityRepository.findByIdAndTenantId(OPPORTUNITY_ID, TENANT_ID))
                .thenReturn(Optional.of(opportunity(OPPORTUNITY_ID, LEAD_ID, OTHER_OWNER_ID)));

        assertThatThrownBy(() -> admissionOpportunityService.createActivity(new CreateOpportunityActivityCommand(
                OPPORTUNITY_ID,
                "CALL",
                "NO_ANSWER",
                NOW,
                null,
                null,
                "MANUAL"
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PERMISSION_DENIED);
        verify(activityRepository, never()).save(any(Activity.class));
    }

    @Test
    void createActivity_whenOutboundResultMissing_shouldReject() {
        allowAdmin("activity.create");
        when(admissionOpportunityRepository.findByIdAndTenantId(OPPORTUNITY_ID, TENANT_ID))
                .thenReturn(Optional.of(opportunity(OPPORTUNITY_ID, LEAD_ID, ACTOR_ID)));

        assertThatThrownBy(() -> admissionOpportunityService.createActivity(new CreateOpportunityActivityCommand(
                OPPORTUNITY_ID,
                "SMS",
                null,
                NOW,
                null,
                null,
                "MANUAL"
        )))
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(activityRepository, never()).save(any(Activity.class));
    }

    @Test
    void changeStage_whenNewToContacting_shouldUpdateOpportunityAppendHistoryAndAudit() {
        allowAdmin("opportunity.update");
        when(admissionOpportunityRepository.findByIdAndTenantId(OPPORTUNITY_ID, TENANT_ID))
                .thenReturn(Optional.of(opportunity(OPPORTUNITY_ID, LEAD_ID, ACTOR_ID)));
        when(stageHistoryRepository.findFirstByTenantIdAndOpportunityIdOrderByChangedAtDesc(TENANT_ID, OPPORTUNITY_ID))
                .thenReturn(Optional.empty());
        when(stageHistoryRepository.save(any(StageHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(admissionOpportunityRepository.save(any(AdmissionOpportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StageChangeSnapshot snapshot = admissionOpportunityService.changeStage(new ChangeOpportunityStageCommand(
                OPPORTUNITY_ID,
                "CONTACTING",
                null,
                null,
                "Advisor started first handling"
        ));

        assertThat(snapshot.opportunityId()).isEqualTo(OPPORTUNITY_ID);
        assertThat(snapshot.fromStage()).isEqualTo("NEW");
        assertThat(snapshot.toStage()).isEqualTo("CONTACTING");
        assertThat(snapshot.stageHistoryId()).isNotNull();
        ArgumentCaptor<StageHistory> historyCaptor = ArgumentCaptor.forClass(StageHistory.class);
        verify(stageHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().fromStage()).isEqualTo(OpportunityStage.NEW);
        assertThat(historyCaptor.getValue().toStage()).isEqualTo(OpportunityStage.CONTACTING);
        ArgumentCaptor<AdmissionOpportunity> opportunityCaptor = ArgumentCaptor.forClass(AdmissionOpportunity.class);
        verify(admissionOpportunityRepository).save(opportunityCaptor.capture());
        assertThat(opportunityCaptor.getValue().currentStage()).isEqualTo(OpportunityStage.CONTACTING);
        verify(slaTaskManagementService, never()).completeFirstResponseTask(any(CompleteFirstResponseSlaTaskCommand.class));
        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo(AuditActions.OPPORTUNITY_STAGE_CHANGED);
    }

    @Test
    void changeStage_whenTransitionIsNotAllowed_shouldReject() {
        allowAdmin("opportunity.update");
        when(admissionOpportunityRepository.findByIdAndTenantId(OPPORTUNITY_ID, TENANT_ID))
                .thenReturn(Optional.of(opportunity(OPPORTUNITY_ID, LEAD_ID, ACTOR_ID)));

        assertThatThrownBy(() -> admissionOpportunityService.changeStage(new ChangeOpportunityStageCommand(
                OPPORTUNITY_ID,
                "ENROLLED",
                null,
                null,
                null
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STAGE_TRANSITION);
        verify(stageHistoryRepository, never()).save(any(StageHistory.class));
        verify(admissionOpportunityRepository, never()).save(any(AdmissionOpportunity.class));
        verify(auditService, never()).record(any(AuditRecordCommand.class));
    }

    @Test
    void changeStage_whenLostReasonMissing_shouldReject() {
        allowAdmin("opportunity.update");
        AdmissionOpportunity opportunity = opportunity(OPPORTUNITY_ID, LEAD_ID, ACTOR_ID);
        opportunity.changeStage(OpportunityStage.CONTACTED, null, null, NOW);
        when(admissionOpportunityRepository.findByIdAndTenantId(OPPORTUNITY_ID, TENANT_ID))
                .thenReturn(Optional.of(opportunity));

        assertThatThrownBy(() -> admissionOpportunityService.changeStage(new ChangeOpportunityStageCommand(
                OPPORTUNITY_ID,
                "LOST",
                null,
                null,
                null
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LOST_REASON_REQUIRED);
        verify(stageHistoryRepository, never()).save(any(StageHistory.class));
        verify(admissionOpportunityRepository, never()).save(any(AdmissionOpportunity.class));
        verify(auditService, never()).record(any(AuditRecordCommand.class));
    }

    @Test
    void changeStage_whenLostReasonOtherWithoutNote_shouldReject() {
        allowAdmin("opportunity.update");
        AdmissionOpportunity opportunity = opportunity(OPPORTUNITY_ID, LEAD_ID, ACTOR_ID);
        opportunity.changeStage(OpportunityStage.CONTACTED, null, null, NOW);
        when(admissionOpportunityRepository.findByIdAndTenantId(OPPORTUNITY_ID, TENANT_ID))
                .thenReturn(Optional.of(opportunity));

        assertThatThrownBy(() -> admissionOpportunityService.changeStage(new ChangeOpportunityStageCommand(
                OPPORTUNITY_ID,
                "LOST",
                "OTHER",
                null,
                null
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LOST_REASON_REQUIRED);
        verify(stageHistoryRepository, never()).save(any(StageHistory.class));
        verify(admissionOpportunityRepository, never()).save(any(AdmissionOpportunity.class));
        verify(auditService, never()).record(any(AuditRecordCommand.class));
    }

    @Test
    void changeStage_whenContactedToLostWithOtherReasonAndNote_shouldPersistLostFields() {
        allowAdmin("opportunity.update");
        AdmissionOpportunity opportunity = opportunity(OPPORTUNITY_ID, LEAD_ID, ACTOR_ID);
        opportunity.changeStage(OpportunityStage.CONTACTED, null, null, NOW);
        when(admissionOpportunityRepository.findByIdAndTenantId(OPPORTUNITY_ID, TENANT_ID))
                .thenReturn(Optional.of(opportunity));
        when(stageHistoryRepository.findFirstByTenantIdAndOpportunityIdOrderByChangedAtDesc(TENANT_ID, OPPORTUNITY_ID))
                .thenReturn(Optional.empty());
        when(stageHistoryRepository.save(any(StageHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(admissionOpportunityRepository.save(any(AdmissionOpportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StageChangeSnapshot snapshot = admissionOpportunityService.changeStage(new ChangeOpportunityStageCommand(
                OPPORTUNITY_ID,
                "LOST",
                "OTHER",
                "Customer has a non-standard budget objection",
                "Customer cannot commit now"
        ));

        assertThat(snapshot.fromStage()).isEqualTo("CONTACTED");
        assertThat(snapshot.toStage()).isEqualTo("LOST");
        ArgumentCaptor<AdmissionOpportunity> opportunityCaptor = ArgumentCaptor.forClass(AdmissionOpportunity.class);
        verify(admissionOpportunityRepository).save(opportunityCaptor.capture());
        assertThat(opportunityCaptor.getValue().currentStage()).isEqualTo(OpportunityStage.LOST);
        assertThat(opportunityCaptor.getValue().lostReason()).isEqualTo(LostReason.OTHER);
        assertThat(opportunityCaptor.getValue().lostNote()).isEqualTo("Customer has a non-standard budget objection");
        ArgumentCaptor<StageHistory> historyCaptor = ArgumentCaptor.forClass(StageHistory.class);
        verify(stageHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().fromStage()).isEqualTo(OpportunityStage.CONTACTED);
        assertThat(historyCaptor.getValue().toStage()).isEqualTo(OpportunityStage.LOST);
        assertThat(historyCaptor.getValue().reason()).isEqualTo("Customer cannot commit now");
    }

    @Test
    void changeStage_whenSalesLeadReopensLostWithReason_shouldClearLostFieldsAndAppendHistory() {
        when(currentUserContext.currentUser()).thenReturn(new CurrentUser(
                ACTOR_ID,
                TENANT_ID,
                "SALES_LEAD",
                Set.of("opportunity.update"),
                "req_opp_unit_001"
        ));
        AdmissionOpportunity opportunity = opportunity(OPPORTUNITY_ID, LEAD_ID, ACTOR_ID);
        opportunity.changeStage(OpportunityStage.LOST, LostReason.TUITION_TOO_HIGH, null, NOW);
        when(admissionOpportunityRepository.findByIdAndTenantId(OPPORTUNITY_ID, TENANT_ID))
                .thenReturn(Optional.of(opportunity));
        when(stageHistoryRepository.findFirstByTenantIdAndOpportunityIdOrderByChangedAtDesc(TENANT_ID, OPPORTUNITY_ID))
                .thenReturn(Optional.empty());
        when(stageHistoryRepository.save(any(StageHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(admissionOpportunityRepository.save(any(AdmissionOpportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StageChangeSnapshot snapshot = admissionOpportunityService.changeStage(new ChangeOpportunityStageCommand(
                OPPORTUNITY_ID,
                "NURTURING",
                null,
                null,
                "Customer asked to revisit next month"
        ));

        assertThat(snapshot.fromStage()).isEqualTo("LOST");
        assertThat(snapshot.toStage()).isEqualTo("NURTURING");
        ArgumentCaptor<AdmissionOpportunity> opportunityCaptor = ArgumentCaptor.forClass(AdmissionOpportunity.class);
        verify(admissionOpportunityRepository).save(opportunityCaptor.capture());
        assertThat(opportunityCaptor.getValue().currentStage()).isEqualTo(OpportunityStage.NURTURING);
        assertThat(opportunityCaptor.getValue().lostReason()).isNull();
        assertThat(opportunityCaptor.getValue().lostNote()).isNull();
    }

    private void allowAdmin(String... permissions) {
        when(currentUserContext.currentUser()).thenReturn(new CurrentUser(
                ACTOR_ID,
                TENANT_ID,
                "ADMIN",
                Set.of(permissions),
                "req_opp_unit_001"
        ));
    }

    private Lead lead(UUID leadId, UUID customerId) {
        return Lead.create(
                leadId,
                TENANT_ID,
                "Incoming Learner",
                "0901234567",
                null,
                null,
                LeadSourceType.CSV,
                LANGUAGE_ID,
                PROGRAM_ID,
                BRANCH_ID,
                LeadTemperature.NORMAL,
                customerId,
                NOW
        );
    }

    private CustomerProfile customer() {
        return CustomerProfile.create(
                CUSTOMER_ID,
                TENANT_ID,
                "Existing Customer",
                "0901234567",
                null,
                null,
                NOW
        );
    }

    private Language language() {
        return Language.restore(
                LANGUAGE_ID,
                TENANT_ID,
                "EN",
                "English",
                CatalogStatus.ACTIVE,
                NOW,
                ACTOR_ID,
                NOW,
                ACTOR_ID
        );
    }

    private Program program() {
        return Program.restore(
                PROGRAM_ID,
                TENANT_ID,
                LANGUAGE_ID,
                "IELTS",
                "IELTS",
                "IELTS",
                CatalogStatus.ACTIVE,
                NOW,
                ACTOR_ID,
                NOW,
                ACTOR_ID
        );
    }

    private Branch branch() {
        return Branch.restore(
                BRANCH_ID,
                TENANT_ID,
                "HN_CG",
                "Ha Noi Cau Giay",
                "Ha Noi",
                null,
                null,
                BranchStatus.ACTIVE,
                NOW,
                ACTOR_ID,
                NOW,
                ACTOR_ID
        );
    }

    private User owner(UUID ownerId) {
        return User.restore(
                ownerId,
                TENANT_ID,
                "advisor@example.com",
                "Advisor",
                null,
                null,
                "ADVISOR",
                UserStatus.ACTIVE,
                null,
                NOW,
                ACTOR_ID,
                NOW,
                ACTOR_ID
        );
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

    private AdmissionOpportunity opportunity(UUID opportunityId, UUID sourceLeadId, UUID ownerId) {
        return AdmissionOpportunity.create(
                opportunityId,
                TENANT_ID,
                CUSTOMER_ID,
                sourceLeadId,
                LANGUAGE_ID,
                PROGRAM_ID,
                null,
                BRANCH_ID,
                ownerId,
                LeadTemperature.NORMAL,
                NOW
        );
    }
}
