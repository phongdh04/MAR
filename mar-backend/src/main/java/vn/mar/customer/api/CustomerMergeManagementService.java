package vn.mar.customer.api;

public interface CustomerMergeManagementService {

    MergeHistorySnapshot unmerge(UnmergeCustomerCommand command);
}
