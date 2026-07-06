package vn.mar.sla.api;

import java.util.Optional;
import vn.mar.common.pagination.PageResponse;

public interface SlaTaskManagementService {

    SlaTaskSnapshot openFirstResponseTask(OpenFirstResponseSlaTaskCommand command);

    Optional<SlaTaskSnapshot> completeFirstResponseTask(CompleteFirstResponseSlaTaskCommand command);

    PageResponse<SlaTaskSnapshot> searchTasks(SlaTaskSearchCommand command);

    SlaOverdueScanSnapshot scanOverdueTasks();
}
