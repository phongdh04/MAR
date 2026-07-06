package vn.mar.sla.repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.mar.sla.entity.WorkingHoursConfig;
import vn.mar.sla.model.WorkingHoursConfigStatus;

public interface WorkingHoursConfigRepository extends JpaRepository<WorkingHoursConfig, UUID> {

    List<WorkingHoursConfig> findByTenantIdAndBranchIdAndStatus(
            UUID tenantId,
            UUID branchId,
            WorkingHoursConfigStatus status);

    List<WorkingHoursConfig> findByTenantIdAndBranchIdIsNullAndStatus(
            UUID tenantId,
            WorkingHoursConfigStatus status);

    Optional<WorkingHoursConfig> findByTenantIdAndBranchIdAndWeekdayAndStatus(
            UUID tenantId,
            UUID branchId,
            DayOfWeek weekday,
            WorkingHoursConfigStatus status);

    Optional<WorkingHoursConfig> findByTenantIdAndBranchIdIsNullAndWeekdayAndStatus(
            UUID tenantId,
            DayOfWeek weekday,
            WorkingHoursConfigStatus status);
}
