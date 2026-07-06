package vn.mar.customer.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.mar.common.error.ErrorCode;
import vn.mar.common.error.ErrorDetail;
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
import vn.mar.lead.api.LeadNormalizationIssue;
import vn.mar.lead.api.LeadNormalizationRequest;
import vn.mar.lead.api.LeadNormalizationResult;
import vn.mar.lead.api.LeadNormalizationService;

@Service
public class CustomerIdentityService {

    private final CustomerIdentityRepository customerIdentityRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final CustomerIdentityMapper customerIdentityMapper;
    private final LeadNormalizationService leadNormalizationService;
    private final TimeProvider timeProvider;

    public CustomerIdentityService(
            CustomerIdentityRepository customerIdentityRepository,
            CustomerProfileRepository customerProfileRepository,
            CustomerIdentityMapper customerIdentityMapper,
            LeadNormalizationService leadNormalizationService,
            TimeProvider timeProvider) {
        this.customerIdentityRepository = customerIdentityRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.customerIdentityMapper = customerIdentityMapper;
        this.leadNormalizationService = leadNormalizationService;
        this.timeProvider = timeProvider;
    }

    @Transactional
    public CustomerIdentitySnapshot registerIdentity(CustomerIdentityRegisterCommand command) {
        validateRegisterCommand(command);
        ensureCustomerExists(command.tenantId(), command.customerId());
        NormalizedIdentity normalizedIdentity = normalizeIdentity(command.identityType(), command.rawValue());

        if (customerIdentityRepository.existsByTenantIdAndCustomerIdAndIdentityTypeAndNormalizedValue(
                command.tenantId(),
                command.customerId(),
                command.identityType(),
                normalizedIdentity.normalizedValue()
        )) {
            throw duplicateIdentity(command.identityType());
        }
        if (command.primaryIdentity()
                && customerIdentityRepository.existsByTenantIdAndCustomerIdAndIdentityTypeAndPrimaryIdentityTrue(
                        command.tenantId(),
                        command.customerId(),
                        command.identityType()
                )) {
            throw primaryIdentityAlreadyExists(command.identityType());
        }

        CustomerIdentity identity = CustomerIdentity.create(
                UUID.randomUUID(),
                command.tenantId(),
                command.customerId(),
                command.identityType(),
                normalizedIdentity.rawValue(),
                normalizedIdentity.normalizedValue(),
                command.primaryIdentity(),
                timeProvider.now()
        );
        return customerIdentityMapper.toSnapshot(customerIdentityRepository.save(identity));
    }

    @Transactional(readOnly = true)
    public List<CustomerIdentity> findExactIdentities(
            UUID tenantId,
            CustomerIdentityType identityType,
            String normalizedValue) {
        if (tenantId == null || identityType == null || !StringUtils.hasText(normalizedValue)) {
            return List.of();
        }
        return customerIdentityRepository.findByTenantIdAndIdentityTypeAndNormalizedValue(
                tenantId,
                identityType,
                normalizedValue
        );
    }

    List<CustomerIdentity> createInitialIdentities(
            CustomerProfile customerProfile,
            CustomerAutoLinkCommand command,
            LeadNormalizationResult normalizedContact,
            Instant now) {
        List<CustomerIdentity> identities = new ArrayList<>();
        addIdentityIfPresent(
                identities,
                customerProfile,
                CustomerIdentityType.PHONE,
                command.phone(),
                normalizedContact.phoneNormalized(),
                now
        );
        addIdentityIfPresent(
                identities,
                customerProfile,
                CustomerIdentityType.EMAIL,
                command.email(),
                normalizedContact.email(),
                now
        );
        addIdentityIfPresent(
                identities,
                customerProfile,
                CustomerIdentityType.ZALO_ID,
                command.zaloId(),
                normalizedContact.zaloId(),
                now
        );
        return identities.isEmpty() ? List.of() : customerIdentityRepository.saveAll(identities);
    }

    private void validateRegisterCommand(CustomerIdentityRegisterCommand command) {
        if (command == null) {
            throw ValidationException.of("command", "REQUIRED", "Customer identity command is required");
        }
        if (command.tenantId() == null) {
            throw ValidationException.of("tenant_id", "REQUIRED", "Tenant id is required");
        }
        if (command.customerId() == null) {
            throw ValidationException.of("customer_id", "REQUIRED", "Customer id is required");
        }
        if (command.identityType() == null) {
            throw ValidationException.of("identity_type", "REQUIRED", "Identity type is required");
        }
        if (!StringUtils.hasText(command.rawValue())) {
            throw ValidationException.of("raw_value", "REQUIRED", "Identity raw value is required");
        }
    }

    private void ensureCustomerExists(UUID tenantId, UUID customerId) {
        if (customerProfileRepository.findByIdAndTenantId(customerId, tenantId).isEmpty()) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Customer profile not found",
                    List.of(ErrorDetail.of("customer_id", "NOT_FOUND", "Customer profile not found"))
            );
        }
    }

    private NormalizedIdentity normalizeIdentity(CustomerIdentityType identityType, String rawValue) {
        LeadNormalizationResult result = switch (identityType) {
            case PHONE -> leadNormalizationService.normalize(new LeadNormalizationRequest(rawValue, null, null));
            case EMAIL -> leadNormalizationService.normalize(new LeadNormalizationRequest(null, rawValue, null));
            case ZALO_ID -> leadNormalizationService.normalize(new LeadNormalizationRequest(null, null, rawValue));
        };
        if (!result.issues().isEmpty()) {
            throw new ValidationException(
                    ErrorCode.VALIDATION_ERROR.defaultMessage(),
                    result.issues().stream()
                            .map(this::toErrorDetail)
                            .toList()
            );
        }
        return new NormalizedIdentity(normalizeRawValue(rawValue), normalizedValue(identityType, result));
    }

    private String normalizedValue(CustomerIdentityType identityType, LeadNormalizationResult result) {
        return switch (identityType) {
            case PHONE -> result.phoneNormalized();
            case EMAIL -> result.email();
            case ZALO_ID -> result.zaloId();
        };
    }

    private void addIdentityIfPresent(
            List<CustomerIdentity> identities,
            CustomerProfile customerProfile,
            CustomerIdentityType identityType,
            String rawValue,
            String normalizedValue,
            Instant now) {
        if (!StringUtils.hasText(normalizedValue)) {
            return;
        }
        identities.add(CustomerIdentity.create(
                UUID.randomUUID(),
                customerProfile.tenantId(),
                customerProfile.id(),
                identityType,
                normalizeRawValue(rawValue),
                normalizedValue,
                true,
                now
        ));
    }

    private BusinessException duplicateIdentity(CustomerIdentityType identityType) {
        return new BusinessException(
                ErrorCode.DUPLICATE_RESOURCE,
                "Customer identity already exists",
                List.of(ErrorDetail.of(
                        identityTypeField(identityType),
                        "DUPLICATE_CUSTOMER_IDENTITY",
                        "Customer identity already exists"
                ))
        );
    }

    private BusinessException primaryIdentityAlreadyExists(CustomerIdentityType identityType) {
        return new BusinessException(
                ErrorCode.BUSINESS_RULE_VIOLATION,
                "Primary customer identity already exists",
                List.of(ErrorDetail.of(
                        identityTypeField(identityType),
                        "PRIMARY_IDENTITY_ALREADY_EXISTS",
                        "Primary customer identity already exists"
                ))
        );
    }


    private ErrorDetail toErrorDetail(LeadNormalizationIssue issue) {
        return ErrorDetail.of(issue.field().code(), issue.code().name(), issue.message());
    }

    private String identityTypeField(CustomerIdentityType identityType) {
        return switch (identityType) {
            case PHONE -> "phone";
            case EMAIL -> "email";
            case ZALO_ID -> "zalo_id";
        };
    }

    private String normalizeRawValue(String rawValue) {
        return rawValue == null ? null : rawValue.trim();
    }

    private record NormalizedIdentity(
            String rawValue,
            String normalizedValue
    ) {
    }
}
