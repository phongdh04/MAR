package vn.mar.customer.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.mar.customer.entity.DuplicateCase;
import vn.mar.customer.model.DuplicateCaseStatus;
import vn.mar.customer.model.DuplicateMatchType;

public interface DuplicateCaseRepository extends JpaRepository<DuplicateCase, UUID> {

    Optional<DuplicateCase> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("""
            select duplicateCase
            from DuplicateCase duplicateCase
            where duplicateCase.tenantId = :tenantId
              and (:status is null or duplicateCase.status = :status)
              and (:matchType is null or duplicateCase.matchType = :matchType)
            """)
    Page<DuplicateCase> search(
            @Param("tenantId") UUID tenantId,
            @Param("status") DuplicateCaseStatus status,
            @Param("matchType") DuplicateMatchType matchType,
            Pageable pageable);

    @Query("""
            select duplicateCase
            from DuplicateCase duplicateCase
            where duplicateCase.tenantId = :tenantId
              and duplicateCase.matchType = :matchType
              and duplicateCase.status = :status
              and (
                    (
                        duplicateCase.sourceCustomerId = :sourceCustomerId
                        and duplicateCase.matchedCustomerId = :matchedCustomerId
                    )
                    or (
                        duplicateCase.sourceCustomerId = :matchedCustomerId
                        and duplicateCase.matchedCustomerId = :sourceCustomerId
                    )
              )
            """)
    Optional<DuplicateCase> findOpenCaseForPair(
            @Param("tenantId") UUID tenantId,
            @Param("sourceCustomerId") UUID sourceCustomerId,
            @Param("matchedCustomerId") UUID matchedCustomerId,
            @Param("matchType") DuplicateMatchType matchType,
            @Param("status") DuplicateCaseStatus status
    );
}
