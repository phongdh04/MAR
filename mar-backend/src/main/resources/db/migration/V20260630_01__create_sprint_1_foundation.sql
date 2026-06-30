create table tenants (
    tenant_id uuid not null,
    tenant_code varchar(50) not null,
    tenant_name varchar(255) not null,
    timezone varchar(100) not null default 'Asia/Ho_Chi_Minh',
    default_currency varchar(10) not null default 'VND',
    status varchar(50) not null,
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    constraint pk_tenants primary key (tenant_id),
    constraint ck_tenants__status check (status in ('ACTIVE', 'INACTIVE')),
    constraint ck_tenants__code_not_blank check (length(trim(tenant_code)) > 0),
    constraint ck_tenants__name_not_blank check (length(trim(tenant_name)) > 0)
);

create unique index ux_tenants__code
on tenants (lower(tenant_code));

create index idx_tenants__status
on tenants (status);

create table branches (
    branch_id uuid not null,
    tenant_id uuid not null,
    branch_code varchar(50) not null,
    branch_name varchar(255) not null,
    phone_number varchar(50),
    address text,
    status varchar(50) not null,
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    constraint pk_branches primary key (branch_id),
    constraint fk_branches__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint ck_branches__status check (status in ('ACTIVE', 'INACTIVE')),
    constraint ck_branches__code_not_blank check (length(trim(branch_code)) > 0),
    constraint ck_branches__name_not_blank check (length(trim(branch_name)) > 0)
);

create unique index ux_branches__tenant_code_active
on branches (tenant_id, lower(branch_code))
where status = 'ACTIVE';

create index idx_branches__tenant_id
on branches (tenant_id);

create index idx_branches__tenant_status
on branches (tenant_id, status);

create table users (
    user_id uuid not null,
    tenant_id uuid not null,
    email varchar(255) not null,
    full_name varchar(255) not null,
    phone_number varchar(50),
    password_hash varchar(255),
    role_code varchar(50) not null,
    status varchar(50) not null,
    last_login_at timestamptz,
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    constraint pk_users primary key (user_id),
    constraint fk_users__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint ck_users__status check (status in ('INVITED', 'ACTIVE', 'INACTIVE', 'LOCKED')),
    constraint ck_users__email_not_blank check (length(trim(email)) > 0),
    constraint ck_users__full_name_not_blank check (length(trim(full_name)) > 0)
);

create unique index ux_users__tenant_email
on users (tenant_id, lower(email));

create index idx_users__tenant_id
on users (tenant_id);

create index idx_users__tenant_status_role
on users (tenant_id, status, role_code);

create table roles (
    role_id uuid not null,
    role_code varchar(50) not null,
    role_name varchar(255) not null,
    description text,
    status varchar(50) not null,
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    constraint pk_roles primary key (role_id),
    constraint ck_roles__status check (status in ('ACTIVE', 'INACTIVE')),
    constraint ck_roles__code_not_blank check (length(trim(role_code)) > 0),
    constraint ck_roles__name_not_blank check (length(trim(role_name)) > 0),
    constraint ux_roles__role_code unique (role_code)
);

alter table users
add constraint fk_users__roles foreign key (role_code) references roles (role_code);

create table permissions (
    permission_id uuid not null,
    function_code varchar(100) not null,
    permission_name varchar(255) not null,
    description text,
    status varchar(50) not null,
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    constraint pk_permissions primary key (permission_id),
    constraint ck_permissions__status check (status in ('ACTIVE', 'INACTIVE')),
    constraint ck_permissions__function_code_not_blank check (length(trim(function_code)) > 0),
    constraint ck_permissions__name_not_blank check (length(trim(permission_name)) > 0),
    constraint ux_permissions__function_code unique (function_code)
);

create table permission_profiles (
    permission_profile_id uuid not null,
    tenant_id uuid not null,
    role_code varchar(50) not null,
    function_code varchar(100) not null,
    access_level varchar(50) not null,
    scope varchar(50),
    status varchar(50) not null,
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    constraint pk_permission_profiles primary key (permission_profile_id),
    constraint fk_permission_profiles__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint fk_permission_profiles__roles foreign key (role_code) references roles (role_code),
    constraint fk_permission_profiles__permissions foreign key (function_code) references permissions (function_code),
    constraint ck_permission_profiles__status check (status in ('ACTIVE', 'INACTIVE')),
    constraint ck_permission_profiles__access_level check (access_level in ('NONE', 'VIEW', 'MANAGE', 'FULL')),
    constraint ck_permission_profiles__role_code_not_blank check (length(trim(role_code)) > 0),
    constraint ck_permission_profiles__function_code_not_blank check (length(trim(function_code)) > 0)
);

create unique index ux_permission_profiles__tenant_role_function_scope
on permission_profiles (tenant_id, role_code, function_code, coalesce(scope, 'GLOBAL'));

create index idx_permission_profiles__tenant_role_status
on permission_profiles (tenant_id, role_code, status);

create table audit_events (
    audit_event_id uuid not null,
    tenant_id uuid,
    actor_id uuid,
    actor_type varchar(32) not null default 'USER',
    actor_role varchar(50),
    action varchar(100) not null,
    resource_type varchar(100) not null,
    resource_id uuid,
    resource_key varchar(255),
    before_data jsonb,
    after_data jsonb,
    metadata jsonb,
    reason text,
    request_id varchar(100),
    client_ip varchar(100),
    user_agent varchar(500),
    created_at timestamptz not null default now(),
    constraint pk_audit_events primary key (audit_event_id),
    constraint fk_audit_events__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint ck_audit_events__actor_type check (actor_type in ('USER', 'PLATFORM_ADMIN', 'SYSTEM', 'INTEGRATION')),
    constraint ck_audit_events__action_not_blank check (length(trim(action)) > 0),
    constraint ck_audit_events__resource_type_not_blank check (length(trim(resource_type)) > 0)
);

create index idx_audit_events__tenant_resource_created
on audit_events (tenant_id, resource_type, resource_id, created_at desc);

create index idx_audit_events__actor_created
on audit_events (actor_id, created_at desc);

create index idx_audit_events__actor_type_created
on audit_events (actor_type, created_at desc);

create index idx_audit_events__action_created
on audit_events (action, created_at desc);

create index idx_audit_events__request_id
on audit_events (request_id);

create table user_branches (
    user_branch_id uuid not null,
    tenant_id uuid not null,
    user_id uuid not null,
    branch_id uuid not null,
    status varchar(50) not null,
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    constraint pk_user_branches primary key (user_branch_id),
    constraint fk_user_branches__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint fk_user_branches__users foreign key (user_id) references users (user_id),
    constraint fk_user_branches__branches foreign key (branch_id) references branches (branch_id),
    constraint ck_user_branches__status check (status in ('ACTIVE', 'INACTIVE'))
);

create unique index ux_user_branches__tenant_user_branch_active
on user_branches (tenant_id, user_id, branch_id)
where status = 'ACTIVE';

create index idx_user_branches__tenant_user_status
on user_branches (tenant_id, user_id, status);

create index idx_user_branches__tenant_branch_status
on user_branches (tenant_id, branch_id, status);

create table languages (
    language_id uuid not null,
    tenant_id uuid not null,
    language_code varchar(50) not null,
    language_name varchar(255) not null,
    status varchar(50) not null,
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    constraint pk_languages primary key (language_id),
    constraint fk_languages__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint ck_languages__status check (status in ('ACTIVE', 'INACTIVE')),
    constraint ck_languages__code_not_blank check (length(trim(language_code)) > 0),
    constraint ck_languages__name_not_blank check (length(trim(language_name)) > 0)
);

create unique index ux_languages__tenant_code_active
on languages (tenant_id, lower(language_code))
where status = 'ACTIVE';

create index idx_languages__tenant_status
on languages (tenant_id, status);

create table programs (
    program_id uuid not null,
    tenant_id uuid not null,
    language_id uuid not null,
    program_code varchar(50) not null,
    program_name varchar(255) not null,
    description text,
    status varchar(50) not null,
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    constraint pk_programs primary key (program_id),
    constraint fk_programs__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint fk_programs__languages foreign key (language_id) references languages (language_id),
    constraint ck_programs__status check (status in ('ACTIVE', 'INACTIVE')),
    constraint ck_programs__code_not_blank check (length(trim(program_code)) > 0),
    constraint ck_programs__name_not_blank check (length(trim(program_name)) > 0)
);

create unique index ux_programs__tenant_code_active
on programs (tenant_id, lower(program_code))
where status = 'ACTIVE';

create index idx_programs__tenant_language_status
on programs (tenant_id, language_id, status);

create table courses (
    course_id uuid not null,
    tenant_id uuid not null,
    program_id uuid not null,
    course_code varchar(50) not null,
    course_name varchar(255) not null,
    tuition_amount numeric(18,2) not null default 0,
    currency varchar(10) not null default 'VND',
    status varchar(50) not null,
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    constraint pk_courses primary key (course_id),
    constraint fk_courses__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint fk_courses__programs foreign key (program_id) references programs (program_id),
    constraint ck_courses__status check (status in ('ACTIVE', 'INACTIVE')),
    constraint ck_courses__code_not_blank check (length(trim(course_code)) > 0),
    constraint ck_courses__name_not_blank check (length(trim(course_name)) > 0),
    constraint ck_courses__tuition_non_negative check (tuition_amount >= 0)
);

create unique index ux_courses__tenant_code_active
on courses (tenant_id, lower(course_code))
where status = 'ACTIVE';

create index idx_courses__tenant_program_status
on courses (tenant_id, program_id, status);

create table import_batches (
    import_batch_id uuid not null,
    tenant_id uuid not null,
    import_type varchar(50) not null,
    source_type varchar(50) not null,
    status varchar(50) not null,
    mapping_config jsonb,
    file_metadata_id uuid,
    original_file_name varchar(255),
    total_rows integer not null default 0,
    valid_rows integer not null default 0,
    error_rows integer not null default 0,
    duplicate_rows integer not null default 0,
    imported_at timestamptz,
    created_at timestamptz not null default now(),
    created_by uuid,
    updated_at timestamptz not null default now(),
    updated_by uuid,
    constraint pk_import_batches primary key (import_batch_id),
    constraint fk_import_batches__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint ck_import_batches__import_type check (import_type in ('LEAD')),
    constraint ck_import_batches__source_type check (source_type in ('CSV', 'EXCEL', 'GOOGLE_SHEET', 'WEBSITE_FORM', 'META_LEAD_ADS', 'MANUAL', 'OTHER')),
    constraint ck_import_batches__status check (status in ('DRAFT', 'UPLOADED', 'VALIDATING', 'VALIDATED', 'PREVIEWED', 'CONFIRMED', 'IMPORTING', 'COMPLETED', 'FAILED', 'CANCELLED', 'EXPIRED')),
    constraint ck_import_batches__counts_non_negative check (
        total_rows >= 0
        and valid_rows >= 0
        and error_rows >= 0
        and duplicate_rows >= 0
    ),
    constraint ck_import_batches__mapping_object check (mapping_config is null or jsonb_typeof(mapping_config) = 'object')
);

create index idx_import_batches__tenant_id
on import_batches (tenant_id);

create index idx_import_batches__tenant_type_status_imported_at
on import_batches (tenant_id, import_type, status, imported_at desc);

create table import_rows (
    import_row_id uuid not null,
    tenant_id uuid not null,
    import_batch_id uuid not null,
    row_number integer not null,
    row_status varchar(50) not null,
    raw_row jsonb,
    normalized_row jsonb,
    error_details jsonb,
    created_at timestamptz not null default now(),
    constraint pk_import_rows primary key (import_row_id),
    constraint fk_import_rows__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint fk_import_rows__import_batches foreign key (import_batch_id) references import_batches (import_batch_id),
    constraint ck_import_rows__status check (row_status in ('PENDING', 'VALID', 'ERROR', 'DUPLICATE', 'SKIPPED', 'IMPORTED')),
    constraint ck_import_rows__row_number_positive check (row_number > 0),
    constraint ck_import_rows__raw_row_object check (raw_row is null or jsonb_typeof(raw_row) = 'object'),
    constraint ck_import_rows__normalized_row_object check (normalized_row is null or jsonb_typeof(normalized_row) = 'object'),
    constraint ck_import_rows__error_details_array check (error_details is null or jsonb_typeof(error_details) = 'array')
);

create index idx_import_rows__tenant_id
on import_rows (tenant_id);

create index idx_import_rows__tenant_batch_status
on import_rows (tenant_id, import_batch_id, row_status);

create unique index ux_import_rows__batch_row_number
on import_rows (import_batch_id, row_number);
