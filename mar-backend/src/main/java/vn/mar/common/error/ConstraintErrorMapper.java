package vn.mar.common.error;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ConstraintErrorMapper {

    private static final Map<String, ErrorCode> KNOWN_CONSTRAINTS = Map.ofEntries(
            Map.entry("ux_tenants__code", ErrorCode.DUPLICATE_TENANT_CODE),
            Map.entry("ux_branches__tenant_code_active", ErrorCode.DUPLICATE_ACTIVE_BRANCH),
            Map.entry("ux_branches__tenant_name_active", ErrorCode.DUPLICATE_ACTIVE_BRANCH),
            Map.entry("ux_users__tenant_email", ErrorCode.DUPLICATE_USER_EMAIL),
            Map.entry("fk_users__roles", ErrorCode.INVALID_PARENT_STATUS),
            Map.entry("ux_user_branches__tenant_user_branch_active", ErrorCode.DUPLICATE_RESOURCE),
            Map.entry("ux_languages__tenant_code_active", ErrorCode.DUPLICATE_RESOURCE),
            Map.entry("ux_languages__tenant_name_active", ErrorCode.DUPLICATE_RESOURCE),
            Map.entry("ux_programs__tenant_code_active", ErrorCode.DUPLICATE_RESOURCE),
            Map.entry("ux_programs__tenant_language_name_active", ErrorCode.DUPLICATE_RESOURCE),
            Map.entry("fk_programs__languages", ErrorCode.INVALID_PARENT_STATUS),
            Map.entry("ux_courses__tenant_code_active", ErrorCode.DUPLICATE_RESOURCE),
            Map.entry("ux_courses__tenant_program_name_active", ErrorCode.DUPLICATE_RESOURCE),
            Map.entry("fk_courses__programs", ErrorCode.INVALID_PARENT_STATUS),
            Map.entry("ck_courses__tuition_non_negative", ErrorCode.NEGATIVE_TUITION),
            Map.entry("fk_import_rows__import_batches", ErrorCode.IMPORT_BATCH_NOT_FOUND),
            Map.entry("ck_import_batches__import_type", ErrorCode.VALIDATION_ERROR),
            Map.entry("ck_import_batches__source_type", ErrorCode.VALIDATION_ERROR),
            Map.entry("ck_import_batches__status", ErrorCode.VALIDATION_ERROR),
            Map.entry("ck_import_batches__counts_non_negative", ErrorCode.VALIDATION_ERROR),
            Map.entry("ck_import_batches__mapping_object", ErrorCode.VALIDATION_ERROR),
            Map.entry("ck_import_rows__status", ErrorCode.IMPORT_ROW_VALIDATION_ERROR),
            Map.entry("ck_import_rows__row_number_positive", ErrorCode.IMPORT_ROW_VALIDATION_ERROR),
            Map.entry("ck_import_rows__raw_row_object", ErrorCode.IMPORT_ROW_VALIDATION_ERROR),
            Map.entry("ck_import_rows__normalized_row_object", ErrorCode.IMPORT_ROW_VALIDATION_ERROR),
            Map.entry("ck_import_rows__error_details_array", ErrorCode.IMPORT_ROW_VALIDATION_ERROR),
            Map.entry("ux_import_rows__batch_row_number", ErrorCode.DUPLICATE_RESOURCE),
            Map.entry("ux_permission_profiles__tenant_role_function_scope", ErrorCode.INVALID_PERMISSION_GUARDRAIL),
            Map.entry("fk_permission_profiles__roles", ErrorCode.INVALID_PARENT_STATUS),
            Map.entry("fk_permission_profiles__permissions", ErrorCode.INVALID_PERMISSION_GUARDRAIL),
            Map.entry("ck_permission_profiles__access_level", ErrorCode.INVALID_PERMISSION_GUARDRAIL),
            Map.entry("ux_customer_identities__tenant_customer_type_value", ErrorCode.DUPLICATE_RESOURCE),
            Map.entry("ux_customer_identities__tenant_customer_type_primary", ErrorCode.DUPLICATE_RESOURCE),
            Map.entry("ux_duplicate_cases__tenant_pair_type_open", ErrorCode.DUPLICATE_RESOURCE),
            Map.entry("ck_duplicate_cases__customers_different", ErrorCode.BUSINESS_RULE_VIOLATION),
            Map.entry("ck_duplicate_cases__resolution_reason_required", ErrorCode.VALIDATION_ERROR),
            Map.entry("ck_admission_opportunities__lost_reason_required", ErrorCode.LOST_REASON_REQUIRED),
            Map.entry("ck_admission_opportunities__lost_reason", ErrorCode.VALIDATION_ERROR),
            Map.entry("ck_admission_opportunities__lost_note_required", ErrorCode.LOST_REASON_REQUIRED),
            Map.entry("ck_stage_history__from_stage", ErrorCode.VALIDATION_ERROR),
            Map.entry("ck_stage_history__to_stage", ErrorCode.VALIDATION_ERROR),
            Map.entry("ck_stage_history__changed_by_type", ErrorCode.VALIDATION_ERROR),
            Map.entry("ck_stage_history__duration_non_negative", ErrorCode.VALIDATION_ERROR),
            Map.entry("ux_integration_events__tenant_source_external_active", ErrorCode.DUPLICATE_RESOURCE),
            Map.entry("ux_integration_events__tenant_source_idempotency_active", ErrorCode.DUPLICATE_RESOURCE),
            Map.entry("ck_integration_events__source_type", ErrorCode.VALIDATION_ERROR),
            Map.entry("ck_integration_events__status", ErrorCode.VALIDATION_ERROR),
            Map.entry("ck_integration_events__external_id_not_blank", ErrorCode.VALIDATION_ERROR),
            Map.entry("ck_integration_events__idempotency_key_not_blank", ErrorCode.VALIDATION_ERROR),
            Map.entry("ck_integration_events__payload_hash_not_blank", ErrorCode.VALIDATION_ERROR),
            Map.entry("ck_integration_events__processed_after_received", ErrorCode.VALIDATION_ERROR)
    );

    public Optional<ErrorCode> map(String constraintName) {
        if (constraintName == null || constraintName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(KNOWN_CONSTRAINTS.get(constraintName.toLowerCase(Locale.ROOT)));
    }
}
