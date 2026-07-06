package vn.mar.opportunity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
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
import vn.mar.common.time.TimeProvider;
import vn.mar.customer.entity.CustomerProfile;
import vn.mar.customer.repository.CustomerProfileRepository;
import vn.mar.lead.entity.Lead;
import vn.mar.lead.model.LeadSourceType;
import vn.mar.lead.model.LeadTemperature;
import vn.mar.lead.repository.LeadRepository;
import vn.mar.opportunity.api.AdmissionOpportunitySearchCommand;
import vn.mar.opportunity.api.AdmissionOpportunitySnapshot;
import vn.mar.opportunity.api.CreateAdmissionOpportunityCommand;
import vn.mar.opportunity.entity.AdmissionOpportunity;
import vn.mar.opportunity.entity.Touchpoint;
import vn.mar.opportunity.mapper.AdmissionOpportunityMapper;
import vn.mar.opportunity.model.OpportunityStage;
import vn.mar.opportunity.model.TouchpointType;
import vn.mar.opportunity.repository.AdmissionOpportunityRepository;
import vn.mar.opportunity.repository.TouchpointRepository;
import vn.mar.security.context.CurrentUser;
import vn.mar.security.context.CurrentUserContext;
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

    private AdmissionOpportunityService admissionOpportunityService;

    @BeforeEach
    void setUp() {
        TimeProvider timeProvider = () -> NOW;
        admissionOpportunityService = new AdmissionOpportunityService(
                admissionOpportunityRepository,
                touchpointRepository,
                leadRepository,
                customerProfileRepository,
                languageRepository,
                programRepository,
                courseRepository,
                branchRepository,
                userRepository,
                new AdmissionOpportunityMapper(),
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
