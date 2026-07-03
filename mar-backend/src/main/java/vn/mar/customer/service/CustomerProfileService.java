package vn.mar.customer.service;

import java.time.Instant;
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
import vn.mar.customer.api.CustomerAutoLinkResult;
import vn.mar.customer.api.CustomerAutoLinkService;
import vn.mar.customer.entity.CustomerIdentity;
import vn.mar.customer.entity.CustomerProfile;
import vn.mar.customer.mapper.CustomerProfileMapper;
import vn.mar.customer.model.CustomerAutoLinkAction;
import vn.mar.customer.model.CustomerIdentityType;
import vn.mar.customer.repository.CustomerProfileRepository;
import vn.mar.lead.api.LeadNormalizationIssue;
import vn.mar.lead.api.LeadNormalizationRequest;
import vn.mar.lead.api.LeadNormalizationResult;
import vn.mar.lead.api.LeadNormalizationService;

@Service
public class CustomerProfileService implements CustomerAutoLinkService {

    private final CustomerProfileRepository customerProfileRepository;
    private final CustomerIdentityService customerIdentityService;
    private final CustomerProfileMapper customerProfileMapper;
    private final LeadNormalizationService leadNormalizationService;
    private final TimeProvider timeProvider;

    public CustomerProfileService(
            CustomerProfileRepository customerProfileRepository,
            CustomerIdentityService customerIdentityService,
            CustomerProfileMapper customerProfileMapper,
            LeadNormalizationService leadNormalizationService,
            TimeProvider timeProvider) {
        this.customerProfileRepository = customerProfileRepository;
        this.customerIdentityService = customerIdentityService;
        this.customerProfileMapper = customerProfileMapper;
        this.leadNormalizationService = leadNormalizationService;
        this.timeProvider = timeProvider;
    }

    @Override
    @Transactional
    public CustomerAutoLinkResult autoLinkOrCreate(CustomerAutoLinkCommand command) {
        validateCommand(command);
        LeadNormalizationResult normalizedContact = normalizeContact(command);

        if (StringUtils.hasText(normalizedContact.phoneNormalized())) {
            CustomerProfile phoneMatch = findSingleMatch(
                    command.tenantId(),
                    customerIdentityService.findExactIdentities(
                            command.tenantId(),
                            CustomerIdentityType.PHONE,
                            normalizedContact.phoneNormalized()
                    ),
                    "phone",
                    "PHONE_EXACT"
            );
            if (phoneMatch != null) {
                return result(phoneMatch, CustomerAutoLinkAction.LINKED_BY_PHONE);
            }
        }

        if (command.zaloVerified() && StringUtils.hasText(normalizedContact.zaloId())) {
            CustomerProfile zaloMatch = findSingleMatch(
                    command.tenantId(),
                    customerIdentityService.findExactIdentities(
                            command.tenantId(),
                            CustomerIdentityType.ZALO_ID,
                            normalizedContact.zaloId()
                    ),
                    "zalo_id",
                    "ZALO_EXACT"
            );
            if (zaloMatch != null) {
                return result(zaloMatch, CustomerAutoLinkAction.LINKED_BY_VERIFIED_ZALO);
            }
        }

        Instant now = timeProvider.now();
        CustomerProfile createdCustomer = CustomerProfile.create(
                UUID.randomUUID(),
                command.tenantId(),
                normalizeOptional(command.fullName()),
                normalizedContact.phoneNormalized(),
                normalizedContact.email(),
                normalizedContact.zaloId(),
                now
        );
        CustomerProfile savedCustomer = customerProfileRepository.save(createdCustomer);
        customerIdentityService.createInitialIdentities(savedCustomer, command, normalizedContact, now);
        return result(savedCustomer, CustomerAutoLinkAction.CREATED);
    }

    @Transactional(readOnly = true)
    public CustomerProfile findCustomer(UUID tenantId, UUID customerId) {
        if (tenantId == null || customerId == null) {
            throw new ValidationException(
                    ErrorCode.VALIDATION_ERROR.defaultMessage(),
                    List.of(ErrorDetail.of("customer_id", "REQUIRED", "Customer id is required"))
            );
        }
        return customerProfileRepository.findByIdAndTenantId(customerId, tenantId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Customer profile not found",
                        List.of(ErrorDetail.of("customer_id", "NOT_FOUND", "Customer profile not found"))
                ));
    }

    private void validateCommand(CustomerAutoLinkCommand command) {
        if (command == null) {
            throw new ValidationException(
                    ErrorCode.VALIDATION_ERROR.defaultMessage(),
                    List.of(ErrorDetail.of("command", "REQUIRED", "Customer auto-link command is required"))
            );
        }
        if (command.tenantId() == null) {
            throw new ValidationException(
                    ErrorCode.VALIDATION_ERROR.defaultMessage(),
                    List.of(ErrorDetail.of("tenant_id", "REQUIRED", "Tenant id is required"))
            );
        }
    }

    private LeadNormalizationResult normalizeContact(CustomerAutoLinkCommand command) {
        LeadNormalizationResult result = leadNormalizationService.normalize(new LeadNormalizationRequest(
                command.phone(),
                command.email(),
                command.zaloId()
        ));
        if (!result.issues().isEmpty()) {
            throw new ValidationException(
                    ErrorCode.VALIDATION_ERROR.defaultMessage(),
                    result.issues().stream()
                            .map(this::toErrorDetail)
                            .toList()
            );
        }
        return result;
    }

    private ErrorDetail toErrorDetail(LeadNormalizationIssue issue) {
        return ErrorDetail.of(issue.field().code(), issue.code().name(), issue.message());
    }

    private CustomerProfile findSingleMatch(
            UUID tenantId,
            List<CustomerIdentity> identityMatches,
            String field,
            String matchType) {
        if (identityMatches.isEmpty()) {
            return null;
        }
        List<UUID> matchedCustomerIds = identityMatches.stream()
                .map(CustomerIdentity::customerId)
                .distinct()
                .toList();
        if (matchedCustomerIds.size() > 1) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Multiple customer profiles match the same contact identifier",
                    List.of(ErrorDetail.of(field, "MULTIPLE_CUSTOMER_MATCHES", "Multiple customer profiles match " + matchType))
            );
        }
        return customerProfileRepository.findByIdAndTenantId(matchedCustomerIds.getFirst(), tenantId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Customer profile not found",
                        List.of(ErrorDetail.of("customer_id", "NOT_FOUND", "Customer profile not found"))
                ));
    }

    private CustomerAutoLinkResult result(CustomerProfile customerProfile, CustomerAutoLinkAction action) {
        return new CustomerAutoLinkResult(customerProfileMapper.toSnapshot(customerProfile), action);
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
