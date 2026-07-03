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
        assertThat(constraintErrorMapper.map("ux_languages__tenant_code_active"))
                .contains(ErrorCode.DUPLICATE_RESOURCE);
        assertThat(constraintErrorMapper.map("ux_languages__tenant_name_active"))
                .contains(ErrorCode.DUPLICATE_RESOURCE);
        assertThat(constraintErrorMapper.map("ux_programs__tenant_code_active"))
                .contains(ErrorCode.DUPLICATE_RESOURCE);
        assertThat(constraintErrorMapper.map("ux_programs__tenant_language_name_active"))
                .contains(ErrorCode.DUPLICATE_RESOURCE);
        assertThat(constraintErrorMapper.map("fk_programs__languages"))
                .contains(ErrorCode.INVALID_PARENT_STATUS);
        assertThat(constraintErrorMapper.map("ux_courses__tenant_code_active"))
                .contains(ErrorCode.DUPLICATE_RESOURCE);
        assertThat(constraintErrorMapper.map("ux_courses__tenant_program_name_active"))
                .contains(ErrorCode.DUPLICATE_RESOURCE);
        assertThat(constraintErrorMapper.map("fk_courses__programs"))
                .contains(ErrorCode.INVALID_PARENT_STATUS);
        assertThat(constraintErrorMapper.map("ck_courses__tuition_non_negative"))
                .contains(ErrorCode.NEGATIVE_TUITION);
        assertThat(constraintErrorMapper.map("fk_import_rows__import_batches"))
                .contains(ErrorCode.IMPORT_BATCH_NOT_FOUND);
        assertThat(constraintErrorMapper.map("ck_import_batches__import_type"))
                .contains(ErrorCode.VALIDATION_ERROR);
        assertThat(constraintErrorMapper.map("ck_import_batches__source_type"))
                .contains(ErrorCode.VALIDATION_ERROR);
        assertThat(constraintErrorMapper.map("ck_import_batches__status"))
                .contains(ErrorCode.VALIDATION_ERROR);
        assertThat(constraintErrorMapper.map("ck_import_batches__counts_non_negative"))
                .contains(ErrorCode.VALIDATION_ERROR);
        assertThat(constraintErrorMapper.map("ck_import_batches__mapping_object"))
                .contains(ErrorCode.VALIDATION_ERROR);
        assertThat(constraintErrorMapper.map("ck_import_rows__status"))
                .contains(ErrorCode.IMPORT_ROW_VALIDATION_ERROR);
        assertThat(constraintErrorMapper.map("ck_import_rows__row_number_positive"))
                .contains(ErrorCode.IMPORT_ROW_VALIDATION_ERROR);
        assertThat(constraintErrorMapper.map("ck_import_rows__raw_row_object"))
                .contains(ErrorCode.IMPORT_ROW_VALIDATION_ERROR);
        assertThat(constraintErrorMapper.map("ck_import_rows__normalized_row_object"))
                .contains(ErrorCode.IMPORT_ROW_VALIDATION_ERROR);
        assertThat(constraintErrorMapper.map("ck_import_rows__error_details_array"))
                .contains(ErrorCode.IMPORT_ROW_VALIDATION_ERROR);
        assertThat(constraintErrorMapper.map("ux_import_rows__batch_row_number"))
                .contains(ErrorCode.DUPLICATE_RESOURCE);
        assertThat(constraintErrorMapper.map("ux_permission_profiles__tenant_role_function_scope"))
                .contains(ErrorCode.INVALID_PERMISSION_GUARDRAIL);
        assertThat(constraintErrorMapper.map("fk_permission_profiles__roles"))
                .contains(ErrorCode.INVALID_PARENT_STATUS);
        assertThat(constraintErrorMapper.map("fk_permission_profiles__permissions"))
                .contains(ErrorCode.INVALID_PERMISSION_GUARDRAIL);
        assertThat(constraintErrorMapper.map("ck_permission_profiles__access_level"))
                .contains(ErrorCode.INVALID_PERMISSION_GUARDRAIL);
        assertThat(constraintErrorMapper.map("ux_customer_identities__tenant_customer_type_value"))
                .contains(ErrorCode.DUPLICATE_RESOURCE);
        assertThat(constraintErrorMapper.map("ux_customer_identities__tenant_customer_type_primary"))
                .contains(ErrorCode.DUPLICATE_RESOURCE);
        assertThat(constraintErrorMapper.map("ux_duplicate_cases__tenant_pair_type_open"))
                .contains(ErrorCode.DUPLICATE_RESOURCE);
        assertThat(constraintErrorMapper.map("ck_duplicate_cases__customers_different"))
                .contains(ErrorCode.BUSINESS_RULE_VIOLATION);
        assertThat(constraintErrorMapper.map("ck_duplicate_cases__resolution_reason_required"))
                .contains(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void map_whenUnknownConstraint_shouldReturnEmpty() {
        assertThat(constraintErrorMapper.map("ux_unknown")).isEmpty();
    }
}
