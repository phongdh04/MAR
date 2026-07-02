package vn.mar.catalog.mapper;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import vn.mar.catalog.dto.response.CourseDetailResponse;
import vn.mar.catalog.dto.response.LanguageDetailResponse;
import vn.mar.catalog.dto.response.ProgramDetailResponse;
import vn.mar.catalog.entity.Course;
import vn.mar.catalog.entity.Language;
import vn.mar.catalog.entity.Program;

@Component
public class CatalogMapper {

    public LanguageDetailResponse toDetailResponse(Language language) {
        return new LanguageDetailResponse(
                language.id(),
                language.tenantId(),
                language.languageCode(),
                language.languageName(),
                language.status().name(),
                language.createdAt(),
                language.updatedAt()
        );
    }

    public ProgramDetailResponse toDetailResponse(Program program) {
        return new ProgramDetailResponse(
                program.id(),
                program.tenantId(),
                program.languageId(),
                program.programCode(),
                program.programName(),
                program.examTrack(),
                program.status().name(),
                program.createdAt(),
                program.updatedAt()
        );
    }

    public CourseDetailResponse toDetailResponse(Course course) {
        return new CourseDetailResponse(
                course.id(),
                course.tenantId(),
                course.programId(),
                course.courseCode(),
                course.courseName(),
                course.level(),
                course.tuitionAmount(),
                course.currency(),
                course.status().name(),
                course.createdAt(),
                course.updatedAt()
        );
    }

    public Map<String, Object> toAuditData(Language language) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("language_id", language.id().toString());
        data.put("tenant_id", language.tenantId().toString());
        data.put("code", language.languageCode());
        data.put("name", language.languageName());
        data.put("status", language.status().name());
        return data;
    }

    public Map<String, Object> toAuditData(Program program) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("program_id", program.id().toString());
        data.put("tenant_id", program.tenantId().toString());
        data.put("language_id", program.languageId().toString());
        data.put("program_code", program.programCode());
        data.put("program_name", program.programName());
        data.put("exam_track", program.examTrack());
        data.put("status", program.status().name());
        return data;
    }

    public Map<String, Object> toAuditData(Course course) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("course_id", course.id().toString());
        data.put("tenant_id", course.tenantId().toString());
        data.put("program_id", course.programId().toString());
        data.put("course_code", course.courseCode());
        data.put("course_name", course.courseName());
        data.put("level", course.level());
        data.put("tuition_gross", course.tuitionAmount());
        data.put("currency", course.currency());
        data.put("status", course.status().name());
        return data;
    }
}
