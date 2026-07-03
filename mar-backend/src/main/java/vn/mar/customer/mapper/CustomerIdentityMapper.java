package vn.mar.customer.mapper;

import org.springframework.stereotype.Component;
import vn.mar.customer.api.CustomerIdentitySnapshot;
import vn.mar.customer.entity.CustomerIdentity;

@Component
public class CustomerIdentityMapper {

    public CustomerIdentitySnapshot toSnapshot(CustomerIdentity customerIdentity) {
        return new CustomerIdentitySnapshot(
                customerIdentity.id(),
                customerIdentity.tenantId(),
                customerIdentity.customerId(),
                customerIdentity.identityType(),
                customerIdentity.rawValue(),
                customerIdentity.normalizedValue(),
                customerIdentity.primaryIdentity()
        );
    }
}
