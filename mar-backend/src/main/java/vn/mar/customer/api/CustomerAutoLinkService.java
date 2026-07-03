package vn.mar.customer.api;

public interface CustomerAutoLinkService {

    CustomerAutoLinkResult autoLinkOrCreate(CustomerAutoLinkCommand command);
}
