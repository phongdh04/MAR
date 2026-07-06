package vn.mar.assignment.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.mar.assignment.entity.AssignmentPoolState;

public interface AssignmentPoolStateRepository extends JpaRepository<AssignmentPoolState, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select state
            from AssignmentPoolState state
            where state.tenantId = :tenantId
              and (
                    (:assignmentRuleId is null and state.assignmentRuleId is null)
                    or state.assignmentRuleId = :assignmentRuleId
              )
            """)
    Optional<AssignmentPoolState> findForUpdate(
            @Param("tenantId") UUID tenantId,
            @Param("assignmentRuleId") UUID assignmentRuleId);
}
