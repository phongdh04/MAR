package vn.mar.leadimport.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.mar.leadimport.entity.ImportRow;
import vn.mar.leadimport.model.ImportRowStatus;

public interface ImportRowRepository extends JpaRepository<ImportRow, UUID> {

    Page<ImportRow> findByTenantIdAndImportBatchIdAndRowStatusOrderByRowNumberAsc(
            UUID tenantId,
            UUID importBatchId,
            ImportRowStatus rowStatus,
            Pageable pageable);

    List<ImportRow> findByTenantIdAndImportBatchIdAndRowNumberIn(
            UUID tenantId,
            UUID importBatchId,
            Collection<Integer> rowNumbers);
}
