package vn.mar.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
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
import vn.mar.customer.entity.CustomerProfile;
import vn.mar.customer.mapper.CustomerProfileMapper;
import vn.mar.customer.model.CustomerAutoLinkAction;
import vn.mar.customer.repository.CustomerProfileRepository;
import vn.mar.lead.service.DefaultLeadNormalizationService;

@ExtendWith(MockitoExtension.class)
class CustomerProfileServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID OTHER_TENANT_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final UUID CUSTOMER_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID SECOND_CUSTOMER_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final Instant NOW = Instant.parse("2026-07-03T01:00:00Z");

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    private CustomerProfileService customerProfileService;

    @BeforeEach
    void setUp() {
        TimeProvider timeProvider = () -> NOW;
        customerProfileService = new CustomerProfileService(
                customerProfileRepository,
                new CustomerProfileMapper(),
                new DefaultLeadNormalizationService(),
                timeProvider
        );
    }

    @Test
    void autoLinkOrCreate_whenPhoneExactInTenant_shouldLinkExistingCustomer() {
        CustomerProfile existingCustomer = customer(
                CUSTOMER_ID,
                TENANT_ID,
                "Existing Customer",
                "0901234567",
                "existing@example.com",
                null
        );
        when(customerProfileRepository.findByTenantIdAndPrimaryPhone(TENANT_ID, "0901234567"))
                .thenReturn(List.of(existingCustomer));

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
        assertThat(result.customer().tenantId()).isEqualTo(TENANT_ID);
        verify(customerProfileRepository, never()).save(any(CustomerProfile.class));
    }

    @Test
    void autoLinkOrCreate_whenSamePhoneExistsInAnotherTenant_shouldCreateCustomerForCurrentTenant() {
        when(customerProfileRepository.findByTenantIdAndPrimaryPhone(TENANT_ID, "0901234567"))
                .thenReturn(List.of());
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
        verify(customerProfileRepository).findByTenantIdAndPrimaryPhone(TENANT_ID, "0901234567");
        verify(customerProfileRepository, never()).findByTenantIdAndPrimaryPhone(OTHER_TENANT_ID, "0901234567");
    }

    @Test
    void autoLinkOrCreate_whenVerifiedZaloExact_shouldLinkExistingCustomer() {
        CustomerProfile existingCustomer = customer(
                CUSTOMER_ID,
                TENANT_ID,
                "Zalo Customer",
                null,
                null,
                "zalo-001"
        );
        when(customerProfileRepository.findByTenantIdAndZaloId(TENANT_ID, "zalo-001"))
                .thenReturn(List.of(existingCustomer));

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
        verify(customerProfileRepository, never()).save(any(CustomerProfile.class));
    }

    @Test
    void autoLinkOrCreate_whenPhoneAndVerifiedZaloBothMatch_shouldPreferPhoneLink() {
        CustomerProfile phoneCustomer = customer(
                CUSTOMER_ID,
                TENANT_ID,
                "Phone Customer",
                "0901234567",
                null,
                null
        );
        when(customerProfileRepository.findByTenantIdAndPrimaryPhone(TENANT_ID, "0901234567"))
                .thenReturn(List.of(phoneCustomer));

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
        verify(customerProfileRepository, never()).findByTenantIdAndZaloId(TENANT_ID, "zalo-001");
        verify(customerProfileRepository, never()).save(any(CustomerProfile.class));
    }

    @Test
    void autoLinkOrCreate_whenZaloNotVerified_shouldNotAutoLinkByZalo() {
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
        verify(customerProfileRepository, never()).findByTenantIdAndZaloId(TENANT_ID, "zalo-001");
    }

    @Test
    void autoLinkOrCreate_whenVerifiedZaloExistsInAnotherTenant_shouldCreateCustomerForCurrentTenant() {
        when(customerProfileRepository.findByTenantIdAndZaloId(TENANT_ID, "zalo-001"))
                .thenReturn(List.of());
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
        verify(customerProfileRepository).findByTenantIdAndZaloId(TENANT_ID, "zalo-001");
        verify(customerProfileRepository, never()).findByTenantIdAndZaloId(OTHER_TENANT_ID, "zalo-001");
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
        verify(customerProfileRepository, never()).findByTenantIdAndPrimaryPhone(any(), any());
        verify(customerProfileRepository, never()).findByTenantIdAndZaloId(any(), any());
    }

    @Test
    void autoLinkOrCreate_whenOnlyNameLooksSame_shouldCreateAndNotMergeByName() {
        when(customerProfileRepository.findByTenantIdAndPrimaryPhone(TENANT_ID, "0900000001"))
                .thenReturn(List.of());
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
    }

    @Test
    void autoLinkOrCreate_whenMultiplePhoneMatches_shouldRejectAmbiguousAutoLink() {
        when(customerProfileRepository.findByTenantIdAndPrimaryPhone(TENANT_ID, "0901234567"))
                .thenReturn(List.of(
                        customer(CUSTOMER_ID, TENANT_ID, "One", "0901234567", null, null),
                        customer(SECOND_CUSTOMER_ID, TENANT_ID, "Two", "0901234567", null, null)
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
    void autoLinkOrCreate_whenMultipleVerifiedZaloMatches_shouldRejectAmbiguousAutoLink() {
        when(customerProfileRepository.findByTenantIdAndZaloId(TENANT_ID, "zalo-001"))
                .thenReturn(List.of(
                        customer(CUSTOMER_ID, TENANT_ID, "One", null, null, "zalo-001"),
                        customer(SECOND_CUSTOMER_ID, TENANT_ID, "Two", null, null, "zalo-001")
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
    void autoLinkOrCreate_whenNewCustomer_shouldPersistNormalizedSnapshotAndTimestamp() {
        when(customerProfileRepository.findByTenantIdAndPrimaryPhone(TENANT_ID, "0901234567"))
                .thenReturn(List.of());
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
}
