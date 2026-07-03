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
import vn.mar.customer.api.CustomerAutoLinkResult;
import vn.mar.customer.entity.CustomerIdentity;
import vn.mar.customer.entity.CustomerProfile;
import vn.mar.customer.mapper.CustomerIdentityMapper;
import vn.mar.customer.mapper.CustomerProfileMapper;
import vn.mar.customer.model.CustomerAutoLinkAction;
import vn.mar.customer.model.CustomerIdentityType;
import vn.mar.customer.repository.CustomerIdentityRepository;
import vn.mar.customer.repository.CustomerProfileRepository;
import vn.mar.lead.service.DefaultLeadNormalizationService;

@ExtendWith(MockitoExtension.class)
class CustomerProfileServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID OTHER_TENANT_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final UUID CUSTOMER_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID SECOND_CUSTOMER_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID IDENTITY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SECOND_IDENTITY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant NOW = Instant.parse("2026-07-03T01:00:00Z");

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private CustomerIdentityRepository customerIdentityRepository;

    private CustomerProfileService customerProfileService;

    @BeforeEach
    void setUp() {
        TimeProvider timeProvider = () -> NOW;
        CustomerIdentityService customerIdentityService = new CustomerIdentityService(
                customerIdentityRepository,
                customerProfileRepository,
                new CustomerIdentityMapper(),
                new DefaultLeadNormalizationService(),
                timeProvider
        );
        customerProfileService = new CustomerProfileService(
                customerProfileRepository,
                customerIdentityService,
                new CustomerProfileMapper(),
                new DefaultLeadNormalizationService(),
                timeProvider
        );
    }

    @Test
    void autoLinkOrCreate_whenPhoneExactIdentityInTenant_shouldLinkExistingCustomer() {
        CustomerProfile existingCustomer = customer(
                CUSTOMER_ID,
                TENANT_ID,
                "Existing Customer",
                null,
                "existing@example.com",
                null
        );
        when(customerIdentityRepository.findByTenantIdAndIdentityTypeAndNormalizedValue(
                TENANT_ID,
                CustomerIdentityType.PHONE,
                "0901234567"
        )).thenReturn(List.of(identity(
                IDENTITY_ID,
                CUSTOMER_ID,
                CustomerIdentityType.PHONE,
                "0901234567",
                "0901234567"
        )));
        when(customerProfileRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
                .thenReturn(Optional.of(existingCustomer));

        CustomerAutoLinkResult result = customerProfileService.autoLinkOrCreate(new CustomerAutoLinkCommand(
                TENANT_ID,
                "Incoming Lead",
                "+84 90 123 4567",
                "incoming@example.com",
                null,
                false
        ));

        assertThat(result.action()).isEqualTo(CustomerAutoLinkAction.LINKED_BY_PHONE);
        assertThat(result.customer().customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(result.customer().primaryPhone()).isNull();
        verify(customerProfileRepository, never()).findByTenantIdAndPrimaryPhone(any(), any());
        verify(customerProfileRepository, never()).save(any(CustomerProfile.class));
    }

    @Test
    void autoLinkOrCreate_whenSamePhoneIdentityExistsInAnotherTenant_shouldCreateCustomerForCurrentTenant() {
        when(customerIdentityRepository.findByTenantIdAndIdentityTypeAndNormalizedValue(
                TENANT_ID,
                CustomerIdentityType.PHONE,
                "0901234567"
        )).thenReturn(List.of());
        when(customerProfileRepository.save(any(CustomerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerAutoLinkResult result = customerProfileService.autoLinkOrCreate(new CustomerAutoLinkCommand(
                TENANT_ID,
                "Tenant B Customer",
                "0901234567",
                null,
                null,
                false
        ));

        assertThat(result.action()).isEqualTo(CustomerAutoLinkAction.CREATED);
        assertThat(result.customer().tenantId()).isEqualTo(TENANT_ID);
        assertThat(result.customer().primaryPhone()).isEqualTo("0901234567");
        verify(customerIdentityRepository).findByTenantIdAndIdentityTypeAndNormalizedValue(
                TENANT_ID,
                CustomerIdentityType.PHONE,
                "0901234567"
        );
        verify(customerIdentityRepository, never()).findByTenantIdAndIdentityTypeAndNormalizedValue(
                OTHER_TENANT_ID,
                CustomerIdentityType.PHONE,
                "0901234567"
        );
    }

    @Test
    void autoLinkOrCreate_whenVerifiedZaloExactIdentity_shouldLinkExistingCustomer() {
        CustomerProfile existingCustomer = customer(
                CUSTOMER_ID,
                TENANT_ID,
                "Zalo Customer",
                null,
                null,
                null
        );
        when(customerIdentityRepository.findByTenantIdAndIdentityTypeAndNormalizedValue(
                TENANT_ID,
                CustomerIdentityType.ZALO_ID,
                "zalo-001"
        )).thenReturn(List.of(identity(
                IDENTITY_ID,
                CUSTOMER_ID,
                CustomerIdentityType.ZALO_ID,
                "zalo-001",
                "zalo-001"
        )));
        when(customerProfileRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
                .thenReturn(Optional.of(existingCustomer));

        CustomerAutoLinkResult result = customerProfileService.autoLinkOrCreate(new CustomerAutoLinkCommand(
                TENANT_ID,
                "Incoming Lead",
                null,
                null,
                " zalo-001 ",
                true
        ));

        assertThat(result.action()).isEqualTo(CustomerAutoLinkAction.LINKED_BY_VERIFIED_ZALO);
        assertThat(result.customer().customerId()).isEqualTo(CUSTOMER_ID);
        verify(customerProfileRepository, never()).findByTenantIdAndZaloId(any(), any());
        verify(customerProfileRepository, never()).save(any(CustomerProfile.class));
    }

    @Test
    void autoLinkOrCreate_whenPhoneAndVerifiedZaloBothMatch_shouldPreferPhoneLink() {
        CustomerProfile phoneCustomer = customer(
                CUSTOMER_ID,
                TENANT_ID,
                "Phone Customer",
                null,
                null,
                null
        );
        when(customerIdentityRepository.findByTenantIdAndIdentityTypeAndNormalizedValue(
                TENANT_ID,
                CustomerIdentityType.PHONE,
                "0901234567"
        )).thenReturn(List.of(identity(
                IDENTITY_ID,
                CUSTOMER_ID,
                CustomerIdentityType.PHONE,
                "0901234567",
                "0901234567"
        )));
        when(customerProfileRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
                .thenReturn(Optional.of(phoneCustomer));

        CustomerAutoLinkResult result = customerProfileService.autoLinkOrCreate(new CustomerAutoLinkCommand(
                TENANT_ID,
                "Incoming Lead",
                "0901234567",
                null,
                "zalo-001",
                true
        ));

        assertThat(result.action()).isEqualTo(CustomerAutoLinkAction.LINKED_BY_PHONE);
        assertThat(result.customer().customerId()).isEqualTo(CUSTOMER_ID);
        verify(customerIdentityRepository, never()).findByTenantIdAndIdentityTypeAndNormalizedValue(
                TENANT_ID,
                CustomerIdentityType.ZALO_ID,
                "zalo-001"
        );
        verify(customerProfileRepository, never()).save(any(CustomerProfile.class));
    }

    @Test
    void autoLinkOrCreate_whenZaloNotVerified_shouldNotAutoLinkByZaloButShouldCreateIdentity() {
        when(customerProfileRepository.save(any(CustomerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerAutoLinkResult result = customerProfileService.autoLinkOrCreate(new CustomerAutoLinkCommand(
                TENANT_ID,
                "Unverified Zalo",
                null,
                null,
                "zalo-001",
                false
        ));

        assertThat(result.action()).isEqualTo(CustomerAutoLinkAction.CREATED);
        assertThat(result.customer().zaloId()).isEqualTo("zalo-001");
        verify(customerIdentityRepository, never()).findByTenantIdAndIdentityTypeAndNormalizedValue(
                TENANT_ID,
                CustomerIdentityType.ZALO_ID,
                "zalo-001"
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CustomerIdentity>> identitiesCaptor = ArgumentCaptor.forClass(List.class);
        verify(customerIdentityRepository).saveAll(identitiesCaptor.capture());
        assertThat(identitiesCaptor.getValue())
                .extracting(CustomerIdentity::identityType)
                .containsExactly(CustomerIdentityType.ZALO_ID);
    }

    @Test
    void autoLinkOrCreate_whenVerifiedZaloIdentityExistsInAnotherTenant_shouldCreateCustomerForCurrentTenant() {
        when(customerIdentityRepository.findByTenantIdAndIdentityTypeAndNormalizedValue(
                TENANT_ID,
                CustomerIdentityType.ZALO_ID,
                "zalo-001"
        )).thenReturn(List.of());
        when(customerProfileRepository.save(any(CustomerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerAutoLinkResult result = customerProfileService.autoLinkOrCreate(new CustomerAutoLinkCommand(
                TENANT_ID,
                "Tenant B Zalo",
                null,
                null,
                "zalo-001",
                true
        ));

        assertThat(result.action()).isEqualTo(CustomerAutoLinkAction.CREATED);
        assertThat(result.customer().tenantId()).isEqualTo(TENANT_ID);
        assertThat(result.customer().zaloId()).isEqualTo("zalo-001");
        verify(customerIdentityRepository).findByTenantIdAndIdentityTypeAndNormalizedValue(
                TENANT_ID,
                CustomerIdentityType.ZALO_ID,
                "zalo-001"
        );
        verify(customerIdentityRepository, never()).findByTenantIdAndIdentityTypeAndNormalizedValue(
                OTHER_TENANT_ID,
                CustomerIdentityType.ZALO_ID,
                "zalo-001"
        );
    }

    @Test
    void autoLinkOrCreate_whenOnlyEmailMatchesPossibleExistingCustomer_shouldCreateAndNotAutoLink() {
        when(customerProfileRepository.save(any(CustomerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerAutoLinkResult result = customerProfileService.autoLinkOrCreate(new CustomerAutoLinkCommand(
                TENANT_ID,
                "Email Only",
                null,
                " Existing@Example.COM ",
                null,
                false
        ));

        assertThat(result.action()).isEqualTo(CustomerAutoLinkAction.CREATED);
        assertThat(result.customer().primaryEmail()).isEqualTo("existing@example.com");
        verify(customerIdentityRepository, never()).findByTenantIdAndIdentityTypeAndNormalizedValue(any(), any(), any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CustomerIdentity>> identitiesCaptor = ArgumentCaptor.forClass(List.class);
        verify(customerIdentityRepository).saveAll(identitiesCaptor.capture());
        assertThat(identitiesCaptor.getValue())
                .extracting(CustomerIdentity::identityType)
                .containsExactly(CustomerIdentityType.EMAIL);
    }

    @Test
    void autoLinkOrCreate_whenOnlyNameLooksSame_shouldCreateAndNotMergeByName() {
        when(customerIdentityRepository.findByTenantIdAndIdentityTypeAndNormalizedValue(
                TENANT_ID,
                CustomerIdentityType.PHONE,
                "0900000001"
        )).thenReturn(List.of());
        when(customerProfileRepository.save(any(CustomerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerAutoLinkResult result = customerProfileService.autoLinkOrCreate(new CustomerAutoLinkCommand(
                TENANT_ID,
                "Existing Customer",
                "0900000001",
                null,
                null,
                false
        ));

        assertThat(result.action()).isEqualTo(CustomerAutoLinkAction.CREATED);
        assertThat(result.customer().fullName()).isEqualTo("Existing Customer");
        assertThat(result.customer().primaryPhone()).isEqualTo("0900000001");
    }

    @Test
    void autoLinkOrCreate_whenNoContactIdentifier_shouldReject() {
        assertThatThrownBy(() -> customerProfileService.autoLinkOrCreate(new CustomerAutoLinkCommand(
                TENANT_ID,
                "No Contact",
                null,
                null,
                null,
                false
        )))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void autoLinkOrCreate_whenPhoneInvalid_shouldRejectAndNotCreate() {
        assertThatThrownBy(() -> customerProfileService.autoLinkOrCreate(new CustomerAutoLinkCommand(
                TENANT_ID,
                "Invalid Phone",
                "123",
                "valid@example.com",
                null,
                false
        )))
                .isInstanceOf(ValidationException.class);

        verify(customerProfileRepository, never()).save(any(CustomerProfile.class));
        verify(customerIdentityRepository, never()).saveAll(any());
    }

    @Test
    void autoLinkOrCreate_whenMultiplePhoneIdentitiesForDifferentCustomers_shouldRejectAmbiguousAutoLink() {
        when(customerIdentityRepository.findByTenantIdAndIdentityTypeAndNormalizedValue(
                TENANT_ID,
                CustomerIdentityType.PHONE,
                "0901234567"
        )).thenReturn(List.of(
                identity(IDENTITY_ID, CUSTOMER_ID, CustomerIdentityType.PHONE, "0901234567", "0901234567"),
                identity(SECOND_IDENTITY_ID, SECOND_CUSTOMER_ID, CustomerIdentityType.PHONE, "0901234567", "0901234567")
        ));

        assertThatThrownBy(() -> customerProfileService.autoLinkOrCreate(new CustomerAutoLinkCommand(
                TENANT_ID,
                "Ambiguous",
                "0901234567",
                null,
                null,
                false
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);

        verify(customerProfileRepository, never()).save(any(CustomerProfile.class));
    }

    @Test
    void autoLinkOrCreate_whenMultipleVerifiedZaloIdentitiesForDifferentCustomers_shouldRejectAmbiguousAutoLink() {
        when(customerIdentityRepository.findByTenantIdAndIdentityTypeAndNormalizedValue(
                TENANT_ID,
                CustomerIdentityType.ZALO_ID,
                "zalo-001"
        )).thenReturn(List.of(
                identity(IDENTITY_ID, CUSTOMER_ID, CustomerIdentityType.ZALO_ID, "zalo-001", "zalo-001"),
                identity(SECOND_IDENTITY_ID, SECOND_CUSTOMER_ID, CustomerIdentityType.ZALO_ID, "zalo-001", "zalo-001")
        ));

        assertThatThrownBy(() -> customerProfileService.autoLinkOrCreate(new CustomerAutoLinkCommand(
                TENANT_ID,
                "Ambiguous",
                null,
                null,
                "zalo-001",
                true
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);

        verify(customerProfileRepository, never()).save(any(CustomerProfile.class));
    }

    @Test
    void autoLinkOrCreate_whenNewCustomer_shouldPersistNormalizedSnapshotTimestampAndPrimaryIdentities() {
        when(customerIdentityRepository.findByTenantIdAndIdentityTypeAndNormalizedValue(
                TENANT_ID,
                CustomerIdentityType.PHONE,
                "0901234567"
        )).thenReturn(List.of());
        when(customerProfileRepository.save(any(CustomerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        customerProfileService.autoLinkOrCreate(new CustomerAutoLinkCommand(
                TENANT_ID,
                " New Customer ",
                "+84 90 123 4567",
                " New@Example.COM ",
                " zalo-new ",
                false
        ));

        ArgumentCaptor<CustomerProfile> customerCaptor = ArgumentCaptor.forClass(CustomerProfile.class);
        verify(customerProfileRepository).save(customerCaptor.capture());
        CustomerProfile savedCustomer = customerCaptor.getValue();
        assertThat(savedCustomer.tenantId()).isEqualTo(TENANT_ID);
        assertThat(savedCustomer.fullName()).isEqualTo("New Customer");
        assertThat(savedCustomer.primaryPhone()).isEqualTo("0901234567");
        assertThat(savedCustomer.primaryEmail()).isEqualTo("new@example.com");
        assertThat(savedCustomer.zaloId()).isEqualTo("zalo-new");
        assertThat(savedCustomer.createdAt()).isEqualTo(NOW);
        assertThat(savedCustomer.updatedAt()).isEqualTo(NOW);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CustomerIdentity>> identitiesCaptor = ArgumentCaptor.forClass(List.class);
        verify(customerIdentityRepository).saveAll(identitiesCaptor.capture());
        List<CustomerIdentity> identities = identitiesCaptor.getValue();
        assertThat(identities)
                .extracting(CustomerIdentity::identityType)
                .containsExactly(
                        CustomerIdentityType.PHONE,
                        CustomerIdentityType.EMAIL,
                        CustomerIdentityType.ZALO_ID
                );
        assertThat(identities)
                .extracting(CustomerIdentity::normalizedValue)
                .containsExactly("0901234567", "new@example.com", "zalo-new");
        assertThat(identities)
                .allMatch(CustomerIdentity::primaryIdentity)
                .allMatch(identity -> identity.customerId().equals(savedCustomer.id()))
                .allMatch(identity -> identity.createdAt().equals(NOW));
    }

    private CustomerProfile customer(
            UUID customerId,
            UUID tenantId,
            String fullName,
            String primaryPhone,
            String primaryEmail,
            String zaloId) {
        return CustomerProfile.restore(
                customerId,
                tenantId,
                fullName,
                primaryPhone,
                primaryEmail,
                zaloId,
                null,
                null,
                null,
                NOW,
                NOW
        );
    }

    private CustomerIdentity identity(
            UUID identityId,
            UUID customerId,
            CustomerIdentityType identityType,
            String rawValue,
            String normalizedValue) {
        return CustomerIdentity.restore(
                identityId,
                TENANT_ID,
                customerId,
                identityType,
                rawValue,
                normalizedValue,
                true,
                NOW,
                NOW
        );
    }
}
