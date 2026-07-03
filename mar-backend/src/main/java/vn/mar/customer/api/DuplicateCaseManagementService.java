package vn.mar.customer.api;

import java.util.UUID;
import vn.mar.common.pagination.PageResponse;

public interface DuplicateCaseManagementService {

    DuplicateCaseSnapshot createEmailExactPhoneDifferentCase(DuplicateCaseCreateCommand command);

    DuplicateCaseSnapshot createNearMatchCase(DuplicateCaseCreateCommand command);

    DuplicateCaseSnapshot resolveCase(DuplicateCaseResolveCommand command);

    DuplicateCaseSnapshot findCase(UUID duplicateCaseId);

    DuplicateCaseSnapshot findCase(UUID tenantId, UUID duplicateCaseId);

    PageResponse<DuplicateCaseSnapshot> searchCases(DuplicateCaseSearchCommand command);
}
