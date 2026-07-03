package vn.mar.customer.api;

import vn.mar.customer.model.CustomerAutoLinkAction;

public record CustomerAutoLinkResult(
        CustomerProfileSnapshot customer,
        CustomerAutoLinkAction action
) {
}
