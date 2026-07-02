package vn.mar.branch.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.mar.branch.entity.Branch;
import vn.mar.branch.model.BranchStatus;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

    Optional<Branch> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Branch> findByTenantIdAndIdIn(UUID tenantId, Collection<UUID> ids);

    boolean existsByTenantIdAndBranchCodeIgnoreCaseAndStatus(UUID tenantId, String branchCode, BranchStatus status);

    boolean existsByTenantIdAndBranchNameIgnoreCaseAndStatus(UUID tenantId, String branchName, BranchStatus status);

    boolean existsByTenantIdAndBranchCodeIgnoreCaseAndStatusAndIdNot(
            UUID tenantId,
            String branchCode,
            BranchStatus status,
            UUID id);

    boolean existsByTenantIdAndBranchNameIgnoreCaseAndStatusAndIdNot(
            UUID tenantId,
            String branchName,
            BranchStatus status,
            UUID id);

    @Query("""
            select branch
            from Branch branch
            where branch.tenantId = :tenantId
              and (:status is null or branch.status = :status)
              and (
                    :keyword is null
                    or lower(branch.branchCode) like concat('%', :keyword, '%')
                    or lower(branch.branchName) like concat('%', :keyword, '%')
                    or lower(coalesce(branch.city, '')) like concat('%', :keyword, '%')
              )
            """)
    Page<Branch> search(
            @Param("tenantId") UUID tenantId,
            @Param("status") BranchStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);
}
