package vn.mar.catalog.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.mar.catalog.entity.Language;
import vn.mar.catalog.model.CatalogStatus;

public interface LanguageRepository extends JpaRepository<Language, UUID> {

    Optional<Language> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndLanguageCodeIgnoreCaseAndStatus(
            UUID tenantId,
            String languageCode,
            CatalogStatus status);

    boolean existsByTenantIdAndLanguageNameIgnoreCaseAndStatus(
            UUID tenantId,
            String languageName,
            CatalogStatus status);

    boolean existsByTenantIdAndLanguageCodeIgnoreCaseAndStatusAndIdNot(
            UUID tenantId,
            String languageCode,
            CatalogStatus status,
            UUID id);

    boolean existsByTenantIdAndLanguageNameIgnoreCaseAndStatusAndIdNot(
            UUID tenantId,
            String languageName,
            CatalogStatus status,
            UUID id);

    @Query("""
            select language
            from Language language
            where language.tenantId = :tenantId
              and (:status is null or language.status = :status)
              and (
                    :keyword is null
                    or lower(language.languageCode) like concat('%', :keyword, '%')
                    or lower(language.languageName) like concat('%', :keyword, '%')
              )
            """)
    Page<Language> search(
            @Param("tenantId") UUID tenantId,
            @Param("status") CatalogStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);
}
