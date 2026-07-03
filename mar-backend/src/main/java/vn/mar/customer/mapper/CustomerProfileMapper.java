package vn.mar.customer.mapper;

import org.springframework.stereotype.Component;
import vn.mar.customer.api.CustomerProfileSnapshot;
import vn.mar.customer.entity.CustomerProfile;

@Component
public class CustomerProfileMapper {

    public CustomerProfileSnapshot toSnapshot(CustomerProfile customerProfile) {
        return new CustomerProfileSnapshot(
                customerProfile.id(),
                customerProfile.tenantId(),
                customerProfile.fullName(),
                customerProfile.primaryPhone(),
                customerProfile.primaryEmail(),
                customerProfile.zaloId()
        );
    }
}
