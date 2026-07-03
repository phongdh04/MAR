package vn.mar.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.mar.audit.service.AuditRecordCommand;
import vn.mar.audit.service.AuditService;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ValidationException;
import vn.mar.customer.api.DuplicateCaseCreateCommand;
import vn.mar.customer.api.DuplicateCaseResolveCommand;
import vn.mar.customer.api.DuplicateCaseSnapshot;
import vn.mar.customer.entity.CustomerIdentity;
import vn.mar.customer.entity.CustomerProfile;
import vn.mar.customer.entity.DuplicateCase;
import vn.mar.customer.mapper.DuplicateCaseMapper;
import vn.mar.customer.model.CustomerIdentityType;
import vn.mar.customer.model.DuplicateCaseStatus;
import vn.mar.customer.model.DuplicateConfidence;
import vn.mar.customer.model.DuplicateMatchType;
import vn.mar.customer.model.DuplicateResolutionAction;
import vn.mar.customer.repository.CustomerIdentityRepository;
import vn.mar.customer.repository.CustomerProfileRepository;
import vn.mar.customer.repository.DuplicateCaseRepository;

@ExtendWith(MockitoExtension.class)
class DuplicateCaseServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID SOURCE_CUSTOMER_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID MATCHED_CUSTOMER_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID DUPLICATE_CASE_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final Instant NOW = Instant.parse("2026-07-03T01:00:00Z");

    @Mock
    private DuplicateCaseRepository duplicateCaseRepository;

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private CustomerIdentityRepository customerIdentityRepository;

    @Mock
    private AuditService auditService;

    private DuplicateCaseService duplicateCaseService;

    @BeforeEach
    void setUp() {
        duplicateCaseService = new DuplicateCaseService(
                duplicateCaseRepository,
                customerProfileRepository,
                customerIdentityRepository,
                new DuplicateCaseMapper(),
                () -> NOW,
                auditService
        );
    }

    @Test
    void createEmailExactPhoneDifferentCase_whenEmailMatchesAndPhonesDiffer_shouldCreateNeedsReviewCase() {
        mockCustomerPair();
        when(customerIdentityRepository.findByTenantIdAndCustomerId(TENANT_ID, SOURCE_CUSTOMER_ID))
                .thenReturn(List.of(
                        identity(SOURCE_CUSTOMER_ID, CustomerIdentityType.EMAIL, "student@example.com"),
                        identity(SOURCE_CUSTOMER_ID, CustomerIdentityType.PHONE, "0900000001")
                ));
        when(customerIdentityRepository.findByTenantIdAndCustomerId(TENANT_ID, MATCHED_CUSTOMER_ID))
                .thenReturn(List.of(
                        identity(MATCHED_CUSTOMER_ID, CustomerIdentityType.EMAIL, "student@example.com"),
                        identity(MATCHED_CUSTOMER_ID, CustomerIdentityType.PHONE, "0900000002")
                ));
        when(duplicateCaseRepository.findOpenCaseForPair(
                TENANT_ID,
                SOURCE_CUSTOMER_ID,
                MATCHED_CUSTOMER_ID,
                DuplicateMatchType.EMAIL_EXACT_PHONE_DIFFERENT,
                DuplicateCaseStatus.NEEDS_REVIEW
        )).thenReturn(Optional.empty());
        when(duplicateCaseRepository.save(any(DuplicateCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DuplicateCaseSnapshot result = duplicateCaseService.createEmailExactPhoneDifferentCase(createCommand(null, " "));

        assertThat(result.status()).isEqualTo(DuplicateCaseStatus.NEEDS_REVIEW);
        assertThat(result.matchType()).isEqualTo(DuplicateMatchType.EMAIL_EXACT_PHONE_DIFFERENT);
        assertThat(result.confidence()).isEqualTo(DuplicateConfidence.MEDIUM);
        assertThat(result.resolutionAction()).isNull();

        ArgumentCaptor<DuplicateCase> duplicateCaseCaptor = ArgumentCaptor.forClass(DuplicateCase.class);
        verify(duplicateCaseRepository).save(duplicateCaseCaptor.capture());
        assertThat(duplicateCaseCaptor.getValue().reviewReason())
                .isEqualTo("Email exact match with different phone identifier");
        verify(auditService).record(any(AuditRecordCommand.class));
    }

    @Test
    void createEmailExactPhoneDifferentCase_whenPhoneAlsoMatches_shouldRejectAndNotCreate() {
        mockCustomerPair();
        when(customerIdentityRepository.findByTenantIdAndCustomerId(TENANT_ID, SOURCE_CUSTOMER_ID))
                .thenReturn(List.of(
                        identity(SOURCE_CUSTOMER_ID, CustomerIdentityType.EMAIL, "student@example.com"),
                        identity(SOURCE_CUSTOMER_ID, CustomerIdentityType.PHONE, "0900000001")
                ));
        when(customerIdentityRepository.findByTenantIdAndCustomerId(TENANT_ID, MATCHED_CUSTOMER_ID))
                .thenReturn(List.of(
                        identity(MATCHED_CUSTOMER_ID, CustomerIdentityType.EMAIL, "student@example.com"),
                        identity(MATCHED_CUSTOMER_ID, CustomerIdentityType.PHONE, "0900000001")
                ));

        assertThatThrownBy(() -> duplicateCaseService.createEmailExactPhoneDifferentCase(createCommand(null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);

        verify(duplicateCaseRepository, never()).save(any(DuplicateCase.class));
        verify(auditService, never()).record(any(AuditRecordCommand.class));
    }

    @Test
    void createNearMatchCase_whenValid_shouldCreateNeedsReviewCaseWithReviewReason() {
        mockCustomerPair();
        when(duplicateCaseRepository.findOpenCaseForPair(
                TENANT_ID,
                SOURCE_CUSTOMER_ID,
                MATCHED_CUSTOMER_ID,
                DuplicateMatchType.NEAR_MATCH,
                DuplicateCaseStatus.NEEDS_REVIEW
        )).thenReturn(Optional.empty());
        when(duplicateCaseRepository.save(any(DuplicateCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DuplicateCaseSnapshot result = duplicateCaseService.createNearMatchCase(createCommand(
                DuplicateConfidence.LOW,
                "Similar learner name and close phone pattern"
        ));

        assertThat(result.status()).isEqualTo(DuplicateCaseStatus.NEEDS_REVIEW);
        assertThat(result.matchType()).isEqualTo(DuplicateMatchType.NEAR_MATCH);
        assertThat(result.confidence()).isEqualTo(DuplicateConfidence.LOW);
        assertThat(result.reviewReason()).isEqualTo("Similar learner name and close phone pattern");
        verify(auditService).record(any(AuditRecordCommand.class));
    }

    @Test
    void createNearMatchCase_whenOpenCaseAlreadyExists_shouldReturnExistingAndNotSave() {
        mockCustomerPair();
        DuplicateCase existingCase = duplicateCase(DuplicateCaseStatus.NEEDS_REVIEW);
        when(duplicateCaseRepository.findOpenCaseForPair(
                TENANT_ID,
                SOURCE_CUSTOMER_ID,
                MATCHED_CUSTOMER_ID,
                DuplicateMatchType.NEAR_MATCH,
                DuplicateCaseStatus.NEEDS_REVIEW
        )).thenReturn(Optional.of(existingCase));

        DuplicateCaseSnapshot result = duplicateCaseService.createNearMatchCase(createCommand(
                DuplicateConfidence.LOW,
                "Similar name"
        ));

        assertThat(result.duplicateCaseId()).isEqualTo(DUPLICATE_CASE_ID);
        verify(duplicateCaseRepository, never()).save(any(DuplicateCase.class));
        verify(auditService, never()).record(any(AuditRecordCommand.class));
    }

    @Test
    void createNearMatchCase_whenCustomerFromOtherTenant_shouldRejectAndNotSave() {
        when(customerProfileRepository.findByIdAndTenantId(SOURCE_CUSTOMER_ID, TENANT_ID))
                .thenReturn(Optional.of(sourceCustomer()));
        when(customerProfileRepository.findByIdAndTenantId(MATCHED_CUSTOMER_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> duplicateCaseService.createNearMatchCase(createCommand(
                DuplicateConfidence.LOW,
                "Similar name"
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

        verify(duplicateCaseRepository, never()).save(any(DuplicateCase.class));
        verify(auditService, never()).record(any(AuditRecordCommand.class));
    }

    @Test
    void resolveCase_whenMergeWithoutReason_shouldReject() {
        assertThatThrownBy(() -> duplicateCaseService.resolveCase(new DuplicateCaseResolveCommand(
                TENANT_ID,
                DUPLICATE_CASE_ID,
                DuplicateResolutionAction.MERGE,
                " ",
                ACTOR_ID,
                "ADMIN"
        )))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void resolveCase_whenAdvisorAttemptsMerge_shouldRejectBeforeSaving() {
        assertThatThrownBy(() -> duplicateCaseService.resolveCase(new DuplicateCaseResolveCommand(
                TENANT_ID,
                DUPLICATE_CASE_ID,
                DuplicateResolutionAction.MERGE,
                "Confirmed same learner",
                ACTOR_ID,
                "ADVISOR"
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PERMISSION_DENIED);

        verify(duplicateCaseRepository, never()).save(any(DuplicateCase.class));
        verify(auditService, never()).record(any(AuditRecordCommand.class));
    }

    @Test
    void resolveCase_whenAdminMergeValid_shouldMarkMergedWithAuditReason() {
        DuplicateCase duplicateCase = duplicateCase(DuplicateCaseStatus.NEEDS_REVIEW);
        when(duplicateCaseRepository.findByIdAndTenantId(DUPLICATE_CASE_ID, TENANT_ID))
                .thenReturn(Optional.of(duplicateCase));
        mockCustomerPair();
        when(duplicateCaseRepository.save(any(DuplicateCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DuplicateCaseSnapshot result = duplicateCaseService.resolveCase(new DuplicateCaseResolveCommand(
                TENANT_ID,
                DUPLICATE_CASE_ID,
                DuplicateResolutionAction.MERGE,
                "Confirmed same learner by parent",
                ACTOR_ID,
                "ADMIN"
        ));

        assertThat(result.status()).isEqualTo(DuplicateCaseStatus.MERGED);
        assertThat(result.resolutionAction()).isEqualTo(DuplicateResolutionAction.MERGE);
        assertThat(result.resolutionReason()).isEqualTo("Confirmed same learner by parent");
        assertThat(result.resolvedBy()).isEqualTo(ACTOR_ID);

        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().reason()).isEqualTo("Confirmed same learner by parent");
    }

    @Test
    void resolveCase_whenSalesLeadIgnoreValid_shouldMarkIgnored() {
        DuplicateCase duplicateCase = duplicateCase(DuplicateCaseStatus.NEEDS_REVIEW);
        when(duplicateCaseRepository.findByIdAndTenantId(DUPLICATE_CASE_ID, TENANT_ID))
                .thenReturn(Optional.of(duplicateCase));
        mockCustomerPair();
        when(duplicateCaseRepository.save(any(DuplicateCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DuplicateCaseSnapshot result = duplicateCaseService.resolveCase(new DuplicateCaseResolveCommand(
                TENANT_ID,
                DUPLICATE_CASE_ID,
                DuplicateResolutionAction.IGNORE,
                "Different learners sharing guardian email",
                ACTOR_ID,
                "SALES_LEAD"
        ));

        assertThat(result.status()).isEqualTo(DuplicateCaseStatus.IGNORED);
        assertThat(result.resolutionAction()).isEqualTo(DuplicateResolutionAction.IGNORE);
    }

    @Test
    void resolveCase_whenAlreadyResolved_shouldRejectConflict() {
        when(duplicateCaseRepository.findByIdAndTenantId(DUPLICATE_CASE_ID, TENANT_ID))
                .thenReturn(Optional.of(duplicateCase(DuplicateCaseStatus.MERGED)));

        assertThatThrownBy(() -> duplicateCaseService.resolveCase(new DuplicateCaseResolveCommand(
                TENANT_ID,
                DUPLICATE_CASE_ID,
                DuplicateResolutionAction.IGNORE,
                "Wrong case",
                ACTOR_ID,
                "ADMIN"
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONFLICT);

        verify(duplicateCaseRepository, never()).save(any(DuplicateCase.class));
    }

    private void mockCustomerPair() {
        when(customerProfileRepository.findByIdAndTenantId(SOURCE_CUSTOMER_ID, TENANT_ID))
                .thenReturn(Optional.of(sourceCustomer()));
        when(customerProfileRepository.findByIdAndTenantId(MATCHED_CUSTOMER_ID, TENANT_ID))
                .thenReturn(Optional.of(matchedCustomer()));
    }

    private DuplicateCaseCreateCommand createCommand(DuplicateConfidence confidence, String reviewReason) {
        return new DuplicateCaseCreateCommand(
                TENANT_ID,
                SOURCE_CUSTOMER_ID,
                MATCHED_CUSTOMER_ID,
                confidence,
                reviewReason,
                ACTOR_ID,
                "ADMIN"
        );
    }

    private DuplicateCase duplicateCase(DuplicateCaseStatus status) {
        return DuplicateCase.restore(
                DUPLICATE_CASE_ID,
                TENANT_ID,
                SOURCE_CUSTOMER_ID,
                MATCHED_CUSTOMER_ID,
                DuplicateMatchType.NEAR_MATCH,
                DuplicateConfidence.LOW,
                status,
                "Similar name",
                status == DuplicateCaseStatus.NEEDS_REVIEW ? null : DuplicateResolutionAction.MERGE,
                status == DuplicateCaseStatus.NEEDS_REVIEW ? null : ACTOR_ID,
                status == DuplicateCaseStatus.NEEDS_REVIEW ? null : NOW,
                status == DuplicateCaseStatus.NEEDS_REVIEW ? null : "Confirmed",
                NOW,
                NOW
        );
    }

    private CustomerProfile sourceCustomer() {
        return customer(SOURCE_CUSTOMER_ID, "0900000001", "student@example.com");
    }

    private CustomerProfile matchedCustomer() {
        return customer(MATCHED_CUSTOMER_ID, "0900000002", "student@example.com");
    }

    private CustomerProfile customer(UUID customerId, String phone, String email) {
        return CustomerProfile.restore(
                customerId,
                TENANT_ID,
                "Student",
                phone,
                email,
                null,
                null,
                null,
                null,
                NOW,
                NOW
        );
    }

    private CustomerIdentity identity(UUID customerId, CustomerIdentityType identityType, String value) {
        return CustomerIdentity.restore(
                UUID.randomUUID(),
                TENANT_ID,
                customerId,
                identityType,
                value,
                value,
                true,
                NOW,
                NOW
        );
    }
}
