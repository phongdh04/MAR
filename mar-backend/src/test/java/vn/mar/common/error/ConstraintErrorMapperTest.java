package vn.mar.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConstraintErrorMapperTest {

    private final ConstraintErrorMapper constraintErrorMapper = new ConstraintErrorMapper();

    @Test
    void map_whenKnownConstraint_shouldReturnMappedErrorCode() {
        assertThat(constraintErrorMapper.map("ux_tenants__code"))
                .contains(ErrorCode.DUPLICATE_TENANT_CODE);
        assertThat(constraintErrorMapper.map("ux_branches__tenant_code_active"))
                .contains(ErrorCode.DUPLICATE_ACTIVE_BRANCH);
        assertThat(constraintErrorMapper.map("ux_branches__tenant_name_active"))
                .contains(ErrorCode.DUPLICATE_ACTIVE_BRANCH);
        assertThat(constraintErrorMapper.map("ux_users__tenant_email"))
                .contains(ErrorCode.DUPLICATE_USER_EMAIL);
        assertThat(constraintErrorMapper.map("fk_users__roles"))
                .contains(ErrorCode.INVALID_PARENT_STATUS);
        assertThat(constraintErrorMapper.map("ux_user_branches__tenant_user_branch_active"))
                .contains(ErrorCode.DUPLICATE_RESOURCE);
        assertThat(constraintErrorMapper.map("ck_courses__tuition_non_negative"))
                .contains(ErrorCode.NEGATIVE_TUITION);
        assertThat(constraintErrorMapper.map("ux_permission_profiles__tenant_role_function_scope"))
                .contains(ErrorCode.INVALID_PERMISSION_GUARDRAIL);
        assertThat(constraintErrorMapper.map("fk_permission_profiles__roles"))
                .contains(ErrorCode.INVALID_PARENT_STATUS);
        assertThat(constraintErrorMapper.map("fk_permission_profiles__permissions"))
                .contains(ErrorCode.INVALID_PERMISSION_GUARDRAIL);
        assertThat(constraintErrorMapper.map("ck_permission_profiles__access_level"))
                .contains(ErrorCode.INVALID_PERMISSION_GUARDRAIL);
    }

    @Test
    void map_whenUnknownConstraint_shouldReturnEmpty() {
        assertThat(constraintErrorMapper.map("ux_unknown")).isEmpty();
    }
}
