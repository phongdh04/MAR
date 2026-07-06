package vn.mar.opportunity.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.mar.opportunity.entity.AdmissionOpportunity;
import vn.mar.opportunity.model.OpportunityStage;

public interface AdmissionOpportunityRepository extends JpaRepository<AdmissionOpportunity, UUID> {

    Optional<AdmissionOpportunity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<AdmissionOpportunity> findFirstByTenantIdAndCustomerIdAndProgramIdAndCurrentStageInOrderByCreatedAtDesc(
            UUID tenantId,
            UUID customerId,
            UUID programId,
            Collection<OpportunityStage> stages);

    @Query("""
            select opportunity
            from AdmissionOpportunity opportunity
            where opportunity.tenantId = :tenantId
              and (:ownerId is null or opportunity.ownerId = :ownerId)
              and (:stage is null or opportunity.currentStage = :stage)
              and (:languageId is null or opportunity.languageId = :languageId)
              and (:programId is null or opportunity.programId = :programId)
            """)
    Page<AdmissionOpportunity> search(
            @Param("tenantId") UUID tenantId,
            @Param("ownerId") UUID ownerId,
            @Param("stage") OpportunityStage stage,
            @Param("languageId") UUID languageId,
            @Param("programId") UUID programId,
            Pageable pageable);
}
