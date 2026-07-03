package vn.mar.customer.api;

import java.util.List;
import java.util.UUID;
import vn.mar.customer.model.DuplicateCaseStatus;

public interface DuplicateCaseManagementService {

    DuplicateCaseSnapshot createEmailExactPhoneDifferentCase(DuplicateCaseCreateCommand command);

    DuplicateCaseSnapshot createNearMatchCase(DuplicateCaseCreateCommand command);

    DuplicateCaseSnapshot resolveCase(DuplicateCaseResolveCommand command);

    DuplicateCaseSnapshot findCase(UUID tenantId, UUID duplicateCaseId);

    List<DuplicateCaseSnapshot> listCases(UUID tenantId, DuplicateCaseStatus status);
}
