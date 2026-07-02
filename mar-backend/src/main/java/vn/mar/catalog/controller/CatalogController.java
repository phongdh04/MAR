package vn.mar.catalog.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.mar.catalog.dto.request.CourseSearchRequest;
import vn.mar.catalog.dto.request.CreateCourseRequest;
import vn.mar.catalog.dto.request.CreateLanguageRequest;
import vn.mar.catalog.dto.request.CreateProgramRequest;
import vn.mar.catalog.dto.request.LanguageSearchRequest;
import vn.mar.catalog.dto.request.ProgramSearchRequest;
import vn.mar.catalog.dto.request.UpdateCourseRequest;
import vn.mar.catalog.dto.request.UpdateLanguageRequest;
import vn.mar.catalog.dto.request.UpdateProgramRequest;
import vn.mar.catalog.dto.response.CourseDetailResponse;
import vn.mar.catalog.dto.response.LanguageDetailResponse;
import vn.mar.catalog.dto.response.ProgramDetailResponse;
import vn.mar.catalog.service.CatalogService;
import vn.mar.common.dto.ApiResponse;
import vn.mar.common.pagination.PageResponse;

@RestController
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping("/api/v1/languages")
    @PreAuthorize("@authz.hasPermission(authentication, 'catalog.manage')")
    public ResponseEntity<ApiResponse<LanguageDetailResponse>> createLanguage(
            @Valid @RequestBody CreateLanguageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(catalogService.createLanguage(request)));
    }

    @GetMapping("/api/v1/languages")
    @PreAuthorize("@authz.hasAnyPermission(authentication, 'catalog.manage', 'catalog.view', 'lead.view')")
    public ResponseEntity<ApiResponse<PageResponse<LanguageDetailResponse>>> searchLanguages(
            LanguageSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(catalogService.searchLanguages(request)));
    }

    @GetMapping("/api/v1/languages/{languageId}")
    @PreAuthorize("@authz.hasAnyPermission(authentication, 'catalog.manage', 'catalog.view', 'lead.view')")
    public ResponseEntity<ApiResponse<LanguageDetailResponse>> getLanguage(@PathVariable UUID languageId) {
        return ResponseEntity.ok(ApiResponse.success(catalogService.getLanguage(languageId)));
    }

    @PatchMapping("/api/v1/languages/{languageId}")
    @PreAuthorize("@authz.hasPermission(authentication, 'catalog.manage')")
    public ResponseEntity<ApiResponse<LanguageDetailResponse>> updateLanguage(
            @PathVariable UUID languageId,
            @Valid @RequestBody UpdateLanguageRequest request) {
        return ResponseEntity.ok(ApiResponse.success(catalogService.updateLanguage(languageId, request)));
    }

    @PostMapping("/api/v1/programs")
    @PreAuthorize("@authz.hasPermission(authentication, 'catalog.manage')")
    public ResponseEntity<ApiResponse<ProgramDetailResponse>> createProgram(
            @Valid @RequestBody CreateProgramRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(catalogService.createProgram(request)));
    }

    @GetMapping("/api/v1/programs")
    @PreAuthorize("@authz.hasAnyPermission(authentication, 'catalog.manage', 'catalog.view', 'lead.view')")
    public ResponseEntity<ApiResponse<PageResponse<ProgramDetailResponse>>> searchPrograms(
            ProgramSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(catalogService.searchPrograms(request)));
    }

    @GetMapping("/api/v1/programs/{programId}")
    @PreAuthorize("@authz.hasAnyPermission(authentication, 'catalog.manage', 'catalog.view', 'lead.view')")
    public ResponseEntity<ApiResponse<ProgramDetailResponse>> getProgram(@PathVariable UUID programId) {
        return ResponseEntity.ok(ApiResponse.success(catalogService.getProgram(programId)));
    }

    @PatchMapping("/api/v1/programs/{programId}")
    @PreAuthorize("@authz.hasPermission(authentication, 'catalog.manage')")
    public ResponseEntity<ApiResponse<ProgramDetailResponse>> updateProgram(
            @PathVariable UUID programId,
            @Valid @RequestBody UpdateProgramRequest request) {
        return ResponseEntity.ok(ApiResponse.success(catalogService.updateProgram(programId, request)));
    }

    @PostMapping("/api/v1/courses")
    @PreAuthorize("@authz.hasPermission(authentication, 'catalog.manage')")
    public ResponseEntity<ApiResponse<CourseDetailResponse>> createCourse(
            @Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(catalogService.createCourse(request)));
    }

    @GetMapping("/api/v1/courses")
    @PreAuthorize("@authz.hasAnyPermission(authentication, 'catalog.manage', 'catalog.view', 'lead.view')")
    public ResponseEntity<ApiResponse<PageResponse<CourseDetailResponse>>> searchCourses(
            CourseSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(catalogService.searchCourses(request)));
    }

    @GetMapping("/api/v1/courses/{courseId}")
    @PreAuthorize("@authz.hasAnyPermission(authentication, 'catalog.manage', 'catalog.view', 'lead.view')")
    public ResponseEntity<ApiResponse<CourseDetailResponse>> getCourse(@PathVariable UUID courseId) {
        return ResponseEntity.ok(ApiResponse.success(catalogService.getCourse(courseId)));
    }

    @PatchMapping("/api/v1/courses/{courseId}")
    @PreAuthorize("@authz.hasPermission(authentication, 'catalog.manage')")
    public ResponseEntity<ApiResponse<CourseDetailResponse>> updateCourse(
            @PathVariable UUID courseId,
            @Valid @RequestBody UpdateCourseRequest request) {
        return ResponseEntity.ok(ApiResponse.success(catalogService.updateCourse(courseId, request)));
    }
}
