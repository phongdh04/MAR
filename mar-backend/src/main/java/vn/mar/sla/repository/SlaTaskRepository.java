package vn.mar.sla.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.mar.sla.entity.SlaTask;
import vn.mar.sla.model.SlaOverdueLevel;
import vn.mar.sla.model.SlaTaskStatus;
import vn.mar.sla.model.SlaTaskType;

public interface SlaTaskRepository extends JpaRepository<SlaTask, UUID> {

    Optional<SlaTask> findFirstByTenantIdAndOpportunityIdAndTaskTypeAndStatusInOrderByCreatedAtDescIdDesc(
            UUID tenantId,
            UUID opportunityId,
            SlaTaskType taskType,
            Collection<SlaTaskStatus> statuses);

    Optional<SlaTask> findFirstByTenantIdAndOpportunityIdAndTaskTypeOrderByCreatedAtDescIdDesc(
            UUID tenantId,
            UUID opportunityId,
            SlaTaskType taskType);

    List<SlaTask> findByTenantIdAndStatusAndDueAtLessThanEqualOrderByDueAtAscIdAsc(
            UUID tenantId,
            SlaTaskStatus status,
            Instant dueAt);

    List<SlaTask> findByTenantIdAndBranchIdInAndStatusAndDueAtLessThanEqualOrderByDueAtAscIdAsc(
            UUID tenantId,
            Collection<UUID> branchIds,
            SlaTaskStatus status,
            Instant dueAt);

    List<SlaTask> findByTenantIdAndStatusAndOverdueLevelAndDueAtLessThanEqualOrderByDueAtAscIdAsc(
            UUID tenantId,
            SlaTaskStatus status,
            SlaOverdueLevel overdueLevel,
            Instant dueAt);

    List<SlaTask> findByTenantIdAndBranchIdInAndStatusAndOverdueLevelAndDueAtLessThanEqualOrderByDueAtAscIdAsc(
            UUID tenantId,
            Collection<UUID> branchIds,
            SlaTaskStatus status,
            SlaOverdueLevel overdueLevel,
            Instant dueAt);

    @Query("""
            select task
            from SlaTask task
            where task.tenantId = :tenantId
              and task.status = :status
              and task.overdueLevel = :overdueLevel
              and task.overdueMarkedAt <= :cutoff
            order by task.dueAt asc, task.id asc
            """)
    List<SlaTask> findAdvisorOverdueReadyForSalesLead(
            @Param("tenantId") UUID tenantId,
            @Param("status") SlaTaskStatus status,
            @Param("overdueLevel") SlaOverdueLevel overdueLevel,
            @Param("cutoff") Instant cutoff);

    @Query("""
            select task
            from SlaTask task
            where task.tenantId = :tenantId
              and task.branchId in :branchIds
              and task.status = :status
              and task.overdueLevel = :overdueLevel
              and task.overdueMarkedAt <= :cutoff
            order by task.dueAt asc, task.id asc
            """)
    List<SlaTask> findBranchScopedAdvisorOverdueReadyForSalesLead(
            @Param("tenantId") UUID tenantId,
            @Param("branchIds") Collection<UUID> branchIds,
            @Param("status") SlaTaskStatus status,
            @Param("overdueLevel") SlaOverdueLevel overdueLevel,
            @Param("cutoff") Instant cutoff);

    @Query("""
            select task
            from SlaTask task
            where task.tenantId = :tenantId
              and (:ownerId is null or task.ownerId = :ownerId)
              and (:branchId is null or task.branchId = :branchId)
              and (:status is null or task.status = :status)
              and (:taskType is null or task.taskType = :taskType)
            """)
    Page<SlaTask> search(
            @Param("tenantId") UUID tenantId,
            @Param("ownerId") UUID ownerId,
            @Param("branchId") UUID branchId,
            @Param("status") SlaTaskStatus status,
            @Param("taskType") SlaTaskType taskType,
            Pageable pageable);

    @Query("""
            select task
            from SlaTask task
            where task.tenantId = :tenantId
              and task.branchId in :branchIds
              and (:ownerId is null or task.ownerId = :ownerId)
              and (:status is null or task.status = :status)
              and (:taskType is null or task.taskType = :taskType)
            """)
    Page<SlaTask> searchBranchScoped(
            @Param("tenantId") UUID tenantId,
            @Param("branchIds") Collection<UUID> branchIds,
            @Param("ownerId") UUID ownerId,
            @Param("status") SlaTaskStatus status,
            @Param("taskType") SlaTaskType taskType,
            Pageable pageable);
}
