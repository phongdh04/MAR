package vn.mar.catalog.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.mar.catalog.entity.Program;
import vn.mar.catalog.model.CatalogStatus;

public interface ProgramRepository extends JpaRepository<Program, UUID> {

    Optional<Program> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndProgramCodeIgnoreCaseAndStatus(
            UUID tenantId,
            String programCode,
            CatalogStatus status);

    boolean existsByTenantIdAndLanguageIdAndProgramNameIgnoreCaseAndStatus(
            UUID tenantId,
            UUID languageId,
            String programName,
            CatalogStatus status);

    boolean existsByTenantIdAndProgramCodeIgnoreCaseAndStatusAndIdNot(
            UUID tenantId,
            String programCode,
            CatalogStatus status,
            UUID id);

    boolean existsByTenantIdAndLanguageIdAndProgramNameIgnoreCaseAndStatusAndIdNot(
            UUID tenantId,
            UUID languageId,
            String programName,
            CatalogStatus status,
            UUID id);

    @Query("""
            select program
            from Program program
            where program.tenantId = :tenantId
              and (:languageId is null or program.languageId = :languageId)
              and (:status is null or program.status = :status)
              and (
                    :keyword is null
                    or lower(program.programCode) like concat('%', :keyword, '%')
                    or lower(program.programName) like concat('%', :keyword, '%')
                    or lower(coalesce(program.examTrack, '')) like concat('%', :keyword, '%')
              )
            """)
    Page<Program> search(
            @Param("tenantId") UUID tenantId,
            @Param("languageId") UUID languageId,
            @Param("status") CatalogStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);
}
