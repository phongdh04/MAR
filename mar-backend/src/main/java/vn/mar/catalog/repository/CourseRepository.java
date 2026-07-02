package vn.mar.catalog.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.mar.catalog.entity.Course;
import vn.mar.catalog.model.CatalogStatus;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    Optional<Course> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndCourseCodeIgnoreCaseAndStatus(
            UUID tenantId,
            String courseCode,
            CatalogStatus status);

    boolean existsByTenantIdAndProgramIdAndCourseNameIgnoreCaseAndStatus(
            UUID tenantId,
            UUID programId,
            String courseName,
            CatalogStatus status);

    boolean existsByTenantIdAndCourseCodeIgnoreCaseAndStatusAndIdNot(
            UUID tenantId,
            String courseCode,
            CatalogStatus status,
            UUID id);

    boolean existsByTenantIdAndProgramIdAndCourseNameIgnoreCaseAndStatusAndIdNot(
            UUID tenantId,
            UUID programId,
            String courseName,
            CatalogStatus status,
            UUID id);

    @Query("""
            select course
            from Course course
            where course.tenantId = :tenantId
              and (:programId is null or course.programId = :programId)
              and (:status is null or course.status = :status)
              and (
                    :keyword is null
                    or lower(course.courseCode) like concat('%', :keyword, '%')
                    or lower(course.courseName) like concat('%', :keyword, '%')
                    or lower(coalesce(course.level, '')) like concat('%', :keyword, '%')
              )
            """)
    Page<Course> search(
            @Param("tenantId") UUID tenantId,
            @Param("programId") UUID programId,
            @Param("status") CatalogStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);
}
