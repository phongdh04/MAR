package vn.mar.common.error;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ConstraintErrorMapper {

    private static final Map<String, ErrorCode> KNOWN_CONSTRAINTS = Map.of(
            "ux_tenants__code", ErrorCode.DUPLICATE_TENANT_CODE,
            "ux_branches__tenant_code_active", ErrorCode.DUPLICATE_ACTIVE_BRANCH,
            "ux_users__tenant_email", ErrorCode.DUPLICATE_USER_EMAIL,
            "ck_courses__tuition_non_negative", ErrorCode.NEGATIVE_TUITION,
            "ux_permission_profiles__tenant_role_function_scope", ErrorCode.INVALID_PERMISSION_GUARDRAIL
    );

    public Optional<ErrorCode> map(String constraintName) {
        if (constraintName == null || constraintName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(KNOWN_CONSTRAINTS.get(constraintName.toLowerCase(Locale.ROOT)));
    }
}
