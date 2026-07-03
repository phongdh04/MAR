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
import vn.mar.common.error.ErrorCode;
import vn.mar.common.exception.BusinessException;
import vn.mar.common.exception.ValidationException;
import vn.mar.common.time.TimeProvider;
import vn.mar.customer.api.CustomerAutoLinkCommand;
import vn.mar.customer.api.CustomerIdentityRegisterCommand;
import vn.mar.customer.api.CustomerIdentitySnapshot;
import vn.mar.customer.entity.CustomerIdentity;
import vn.mar.customer.entity.CustomerProfile;
import vn.mar.customer.mapper.CustomerIdentityMapper;
import vn.mar.customer.model.CustomerIdentityType;
import vn.mar.customer.repository.CustomerIdentityRepository;
import vn.mar.customer.repository.CustomerProfileRepository;
import vn.mar.lead.api.LeadNormalizationRequest;
import vn.mar.lead.api.LeadNormalizationResult;
import vn.mar.lead.service.DefaultLeadNormalizationService;

@ExtendWith(MockitoExtension.class)
class CustomerIdentityServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID OTHER_TENANT_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final UUID CUSTOMER_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID IDENTITY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant NOW = Instant.parse("2026-07-03T01:00:00Z");

    @Mock
    private CustomerIdentityRepository customerIdentityRepository;

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    private CustomerIdentityService customerIdentityService;
    private DefaultLeadNormalizationService leadNormalizationService;

    @BeforeEach
    void setUp() {
        TimeProvider timeProvider = () -> NOW;
        leadNormalizationService = new DefaultLeadNormalizationService();
        customerIdentityService = new CustomerIdentityService(
                customerIdentityRepository,
                customerProfileRepository,
                new CustomerIdentityMapper(),
                leadNormalizationService,
                timeProvider
        );
    }

    @Test
    void registerIdentity_whenPhonePrimary_shouldNormalizeAndPersistIdentity() {
        when(customerProfileRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
                .thenReturn(Optional.of(customer()));
        when(customerIdentityRepository.save(any(CustomerIdentity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerIdentitySnapshot result = customerIdentityService.registerIdentity(new CustomerIdentityRegisterCommand(
                TENANT_ID,
                CUSTOMER_ID,
                CustomerIdentityType.PHONE,
                "+84 90 123 4567",
                true
        ));

        assertThat(result.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(result.identityType()).isEqualTo(CustomerIdentityType.PHONE);
        assertThat(result.rawValue()).isEqualTo("+84 90 123 4567");
        assertThat(result.normalizedValue()).isEqualTo("0901234567");
        assertThat(result.primaryIdentity()).isTrue();

        ArgumentCaptor<CustomerIdentity> identityCaptor = ArgumentCaptor.forClass(CustomerIdentity.class);
        verify(customerIdentityRepository).save(identityCaptor.capture());
        assertThat(identityCaptor.getValue().createdAt()).isEqualTo(NOW);
    }

    @Test
    void registerIdentity_whenSecondPhoneNonPrimary_shouldAllowMultiplePhoneIdentitiesForOneCustomer() {
        when(customerProfileRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
                .thenReturn(Optional.of(customer()));
        when(customerIdentityRepository.save(any(CustomerIdentity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerIdentitySnapshot result = customerIdentityService.registerIdentity(new CustomerIdentityRegisterCommand(
                TENANT_ID,
                CUSTOMER_ID,
                CustomerIdentityType.PHONE,
                "0912345678",
                false
        ));

        assertThat(result.normalizedValue()).isEqualTo("0912345678");
        assertThat(result.primaryIdentity()).isFalse();
        verify(customerIdentityRepository, never())
                .existsByTenantIdAndCustomerIdAndIdentityTypeAndPrimaryIdentityTrue(any(), any(), any());
    }

    @Test
    void registerIdentity_whenEmailMixedCase_shouldNormalizeEmail() {
        when(customerProfileRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
                .thenReturn(Optional.of(customer()));
        when(customerIdentityRepository.save(any(CustomerIdentity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerIdentitySnapshot result = customerIdentityService.registerIdentity(new CustomerIdentityRegisterCommand(
                TENANT_ID,
                CUSTOMER_ID,
                CustomerIdentityType.EMAIL,
                " Student@Example.COM ",
                true
        ));

        assertThat(result.rawValue()).isEqualTo("Student@Example.COM");
        assertThat(result.normalizedValue()).isEqualTo("student@example.com");
    }

    @Test
    void registerIdentity_whenCustomerBelongsToAnotherTenant_shouldReturnNotFoundAndNotSave() {
        when(customerProfileRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerIdentityService.registerIdentity(new CustomerIdentityRegisterCommand(
                TENANT_ID,
                CUSTOMER_ID,
                CustomerIdentityType.PHONE,
                "0901234567",
                true
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

        verify(customerProfileRepository).findByIdAndTenantId(CUSTOMER_ID, TENANT_ID);
        verify(customerProfileRepository, never()).findByIdAndTenantId(CUSTOMER_ID, OTHER_TENANT_ID);
        verify(customerIdentityRepository, never()).save(any(CustomerIdentity.class));
    }

    @Test
    void registerIdentity_whenDuplicateSameCustomerTypeValue_shouldReject() {
        when(customerProfileRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
                .thenReturn(Optional.of(customer()));
        when(customerIdentityRepository.existsByTenantIdAndCustomerIdAndIdentityTypeAndNormalizedValue(
                TENANT_ID,
                CUSTOMER_ID,
                CustomerIdentityType.PHONE,
                "0901234567"
        )).thenReturn(true);

        assertThatThrownBy(() -> customerIdentityService.registerIdentity(new CustomerIdentityRegisterCommand(
                TENANT_ID,
                CUSTOMER_ID,
                CustomerIdentityType.PHONE,
                "+84 90 123 4567",
                false
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_RESOURCE);

        verify(customerIdentityRepository, never()).save(any(CustomerIdentity.class));
    }

    @Test
    void registerIdentity_whenPrimaryAlreadyExistsForType_shouldReject() {
        when(customerProfileRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
                .thenReturn(Optional.of(customer()));
        when(customerIdentityRepository.existsByTenantIdAndCustomerIdAndIdentityTypeAndPrimaryIdentityTrue(
                TENANT_ID,
                CUSTOMER_ID,
                CustomerIdentityType.EMAIL
        )).thenReturn(true);

        assertThatThrownBy(() -> customerIdentityService.registerIdentity(new CustomerIdentityRegisterCommand(
                TENANT_ID,
                CUSTOMER_ID,
                CustomerIdentityType.EMAIL,
                "student@example.com",
                true
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);

        verify(customerIdentityRepository, never()).save(any(CustomerIdentity.class));
    }

    @Test
    void registerIdentity_whenRawValueBlank_shouldReject() {
        assertThatThrownBy(() -> customerIdentityService.registerIdentity(new CustomerIdentityRegisterCommand(
                TENANT_ID,
                CUSTOMER_ID,
                CustomerIdentityType.EMAIL,
                " ",
                true
        )))
                .isInstanceOf(ValidationException.class);

        verify(customerProfileRepository, never()).findByIdAndTenantId(any(), any());
        verify(customerIdentityRepository, never()).save(any(CustomerIdentity.class));
    }

    @Test
    void findExactIdentities_whenValidInput_shouldQueryTenantScopedIdentity() {
        CustomerIdentity identity = identity(CustomerIdentityType.PHONE, "0901234567");
        when(customerIdentityRepository.findByTenantIdAndIdentityTypeAndNormalizedValue(
                TENANT_ID,
                CustomerIdentityType.PHONE,
                "0901234567"
        )).thenReturn(List.of(identity));

        List<CustomerIdentity> result = customerIdentityService.findExactIdentities(
                TENANT_ID,
                CustomerIdentityType.PHONE,
                "0901234567"
        );

        assertThat(result).containsExactly(identity);
        verify(customerIdentityRepository).findByTenantIdAndIdentityTypeAndNormalizedValue(
                TENANT_ID,
                CustomerIdentityType.PHONE,
                "0901234567"
        );
    }

    @Test
    void createInitialIdentities_whenAllContactsPresent_shouldPersistAllAsPrimaryIdentities() {
        CustomerProfile customer = customer();
        CustomerAutoLinkCommand command = new CustomerAutoLinkCommand(
                TENANT_ID,
                "New Customer",
                "+84 90 123 4567",
                " Student@Example.COM ",
                " zalo-001 ",
                false
        );
        LeadNormalizationResult normalizedContact = leadNormalizationService.normalize(new LeadNormalizationRequest(
                command.phone(),
                command.email(),
                command.zaloId()
        ));

        customerIdentityService.createInitialIdentities(customer, command, normalizedContact, NOW);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CustomerIdentity>> identitiesCaptor = ArgumentCaptor.forClass(List.class);
        verify(customerIdentityRepository).saveAll(identitiesCaptor.capture());
        assertThat(identitiesCaptor.getValue())
                .extracting(CustomerIdentity::identityType)
                .containsExactly(
                        CustomerIdentityType.PHONE,
                        CustomerIdentityType.EMAIL,
                        CustomerIdentityType.ZALO_ID
                );
        assertThat(identitiesCaptor.getValue())
                .extracting(CustomerIdentity::normalizedValue)
                .containsExactly("0901234567", "student@example.com", "zalo-001");
        assertThat(identitiesCaptor.getValue())
                .allMatch(CustomerIdentity::primaryIdentity);
    }

    private CustomerProfile customer() {
        return CustomerProfile.restore(
                CUSTOMER_ID,
                TENANT_ID,
                "Customer",
                "0901234567",
                "student@example.com",
                "zalo-001",
                null,
                null,
                null,
                NOW,
                NOW
        );
    }

    private CustomerIdentity identity(CustomerIdentityType identityType, String normalizedValue) {
        return CustomerIdentity.restore(
                IDENTITY_ID,
                TENANT_ID,
                CUSTOMER_ID,
                identityType,
                normalizedValue,
                normalizedValue,
                true,
                NOW,
                NOW
        );
    }
}
