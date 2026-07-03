create table duplicate_cases (
    duplicate_case_id uuid not null,
    tenant_id uuid not null,
    source_customer_id uuid not null,
    matched_customer_id uuid not null,
    match_type varchar(50) not null,
    confidence varchar(20) not null,
    status varchar(50) not null,
    review_reason varchar(500) not null,
    resolution_action varchar(50),
    resolved_by uuid,
    resolved_at timestamptz,
    resolution_reason varchar(500),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint pk_duplicate_cases primary key (duplicate_case_id),
    constraint fk_duplicate_cases__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint fk_duplicate_cases__source_customer_profiles foreign key (source_customer_id, tenant_id)
        references customer_profiles (customer_id, tenant_id),
    constraint fk_duplicate_cases__matched_customer_profiles foreign key (matched_customer_id, tenant_id)
        references customer_profiles (customer_id, tenant_id),
    constraint ck_duplicate_cases__customers_different check (source_customer_id <> matched_customer_id),
    constraint ck_duplicate_cases__match_type check (match_type in ('EMAIL_EXACT_PHONE_DIFFERENT', 'NEAR_MATCH')),
    constraint ck_duplicate_cases__confidence check (confidence in ('LOW', 'MEDIUM', 'HIGH')),
    constraint ck_duplicate_cases__status check (status in ('NEEDS_REVIEW', 'MERGED', 'LINKED', 'IGNORED')),
    constraint ck_duplicate_cases__resolution_action check (
        resolution_action is null
        or resolution_action in ('MERGE', 'LINK', 'IGNORE')
    ),
    constraint ck_duplicate_cases__review_reason_not_blank check (length(trim(review_reason)) > 0),
    constraint ck_duplicate_cases__resolved_state check (
        (
            status = 'NEEDS_REVIEW'
            and resolution_action is null
            and resolved_by is null
            and resolved_at is null
        )
        or (
            status in ('MERGED', 'LINKED', 'IGNORED')
            and resolution_action is not null
            and resolved_by is not null
            and resolved_at is not null
        )
    ),
    constraint ck_duplicate_cases__resolution_reason_required check (
        status = 'NEEDS_REVIEW'
        or length(trim(coalesce(resolution_reason, ''))) > 0
    )
);

create index idx_duplicate_cases__tenant_id
on duplicate_cases (tenant_id);

create index idx_duplicate_cases__tenant_status
on duplicate_cases (tenant_id, status, created_at);

create index idx_duplicate_cases__tenant_match_type
on duplicate_cases (tenant_id, match_type, status);

create index idx_duplicate_cases__tenant_source
on duplicate_cases (tenant_id, source_customer_id, status);

create index idx_duplicate_cases__tenant_matched
on duplicate_cases (tenant_id, matched_customer_id, status);

create unique index ux_duplicate_cases__tenant_pair_type_open
on duplicate_cases (
    tenant_id,
    least(source_customer_id::text, matched_customer_id::text),
    greatest(source_customer_id::text, matched_customer_id::text),
    match_type
)
where status = 'NEEDS_REVIEW';
