package vn.mar.leadimport.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.mar.leadimport.entity.ImportBatch;
import vn.mar.leadimport.model.ImportBatchStatus;
import vn.mar.leadimport.model.ImportSourceType;
import vn.mar.leadimport.model.ImportType;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, UUID> {

    Optional<ImportBatch> findByIdAndTenantIdAndImportType(UUID id, UUID tenantId, ImportType importType);

    Optional<ImportBatch> findFirstByTenantIdAndImportTypeAndOriginalFileNameOrderByCreatedAtDesc(
            UUID tenantId,
            ImportType importType,
            String originalFileName);

    @Query("""
            select batch
            from ImportBatch batch
            where batch.tenantId = :tenantId
              and batch.importType = :importType
              and (:status is null or batch.status = :status)
              and (:sourceType is null or batch.sourceType = :sourceType)
            order by coalesce(batch.importedAt, batch.createdAt) desc, batch.id desc
            """)
    Page<ImportBatch> search(
            @Param("tenantId") UUID tenantId,
            @Param("importType") ImportType importType,
            @Param("status") ImportBatchStatus status,
            @Param("sourceType") ImportSourceType sourceType,
            Pageable pageable);
}
