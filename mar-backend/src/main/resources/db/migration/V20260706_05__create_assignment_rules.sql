create table assignment_rules (
    assignment_rule_id uuid not null,
    tenant_id uuid not null,
    rule_name varchar(255) not null,
    priority integer not null,
    language_id uuid,
    program_id uuid,
    branch_id uuid,
    assignment_strategy varchar(32) not null,
    status varchar(32) not null,
    created_at timestamptz not null,
    created_by uuid,
    updated_at timestamptz not null,
    updated_by uuid,
    constraint pk_assignment_rules primary key (assignment_rule_id),
    constraint fk_assignment_rules__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint fk_assignment_rules__languages foreign key (language_id) references languages (language_id),
    constraint fk_assignment_rules__programs foreign key (program_id) references programs (program_id),
    constraint fk_assignment_rules__branches foreign key (branch_id) references branches (branch_id),
    constraint ck_assignment_rules__rule_name_not_blank check (length(trim(rule_name)) > 0),
    constraint ck_assignment_rules__priority_non_negative check (priority >= 0),
    constraint ck_assignment_rules__strategy check (assignment_strategy in ('LEAST_WORKLOAD', 'ROUND_ROBIN')),
    constraint ck_assignment_rules__status check (status in ('ACTIVE', 'INACTIVE'))
);

create unique index ux_assignment_rules__tenant_priority_active
on assignment_rules (tenant_id, priority)
where status = 'ACTIVE';

create index idx_assignment_rules__tenant_status_priority
on assignment_rules (tenant_id, status, priority, assignment_rule_id);

create index idx_assignment_rules__tenant_branch_status
on assignment_rules (tenant_id, branch_id, status)
where branch_id is not null;

create index idx_assignment_rules__tenant_language_program_status
on assignment_rules (tenant_id, language_id, program_id, status);

create table assignment_rule_advisors (
    assignment_rule_advisor_id uuid not null,
    tenant_id uuid not null,
    assignment_rule_id uuid not null,
    advisor_id uuid not null,
    status varchar(32) not null,
    created_at timestamptz not null,
    created_by uuid,
    updated_at timestamptz not null,
    updated_by uuid,
    constraint pk_assignment_rule_advisors primary key (assignment_rule_advisor_id),
    constraint fk_assignment_rule_advisors__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint fk_assignment_rule_advisors__assignment_rules foreign key (assignment_rule_id) references assignment_rules (assignment_rule_id),
    constraint fk_assignment_rule_advisors__users foreign key (advisor_id) references users (user_id),
    constraint ck_assignment_rule_advisors__status check (status in ('ACTIVE', 'INACTIVE'))
);

create unique index ux_assignment_rule_advisors__rule_advisor_active
on assignment_rule_advisors (tenant_id, assignment_rule_id, advisor_id)
where status = 'ACTIVE';

create index idx_assignment_rule_advisors__tenant_rule_status
on assignment_rule_advisors (tenant_id, assignment_rule_id, status);

create index idx_assignment_rule_advisors__tenant_advisor_status
on assignment_rule_advisors (tenant_id, advisor_id, status);

create table assignment_pool_states (
    assignment_pool_state_id uuid not null,
    tenant_id uuid not null,
    assignment_rule_id uuid,
    last_assigned_advisor_id uuid,
    last_assigned_at timestamptz,
    version_number integer not null,
    created_at timestamptz not null,
    created_by uuid,
    updated_at timestamptz not null,
    updated_by uuid,
    constraint pk_assignment_pool_states primary key (assignment_pool_state_id),
    constraint fk_assignment_pool_states__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint fk_assignment_pool_states__assignment_rules foreign key (assignment_rule_id) references assignment_rules (assignment_rule_id),
    constraint fk_assignment_pool_states__last_assigned_users foreign key (last_assigned_advisor_id) references users (user_id),
    constraint ck_assignment_pool_states__version_non_negative check (version_number >= 0)
);

create unique index ux_assignment_pool_states__tenant_rule
on assignment_pool_states (
    tenant_id,
    coalesce(assignment_rule_id, '00000000-0000-0000-0000-000000000000'::uuid)
);

create index idx_assignment_pool_states__tenant_updated
on assignment_pool_states (tenant_id, updated_at desc);

create table assignment_histories (
    assignment_history_id uuid not null,
    tenant_id uuid not null,
    opportunity_id uuid not null,
    source_lead_id uuid not null,
    assignment_rule_id uuid,
    from_owner_id uuid,
    to_owner_id uuid not null,
    assignment_source varchar(32) not null,
    assignment_strategy varchar(32) not null,
    assigned_at timestamptz not null,
    assigned_by uuid,
    reason text,
    constraint pk_assignment_histories primary key (assignment_history_id),
    constraint fk_assignment_histories__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint fk_assignment_histories__admission_opportunities foreign key (opportunity_id) references admission_opportunities (opportunity_id),
    constraint fk_assignment_histories__leads foreign key (source_lead_id) references leads (lead_id),
    constraint fk_assignment_histories__assignment_rules foreign key (assignment_rule_id) references assignment_rules (assignment_rule_id),
    constraint fk_assignment_histories__from_users foreign key (from_owner_id) references users (user_id),
    constraint fk_assignment_histories__to_users foreign key (to_owner_id) references users (user_id),
    constraint ck_assignment_histories__source check (assignment_source in ('RULE', 'FALLBACK')),
    constraint ck_assignment_histories__strategy check (assignment_strategy in ('LEAST_WORKLOAD', 'ROUND_ROBIN'))
);

create index idx_assignment_histories__tenant_opportunity_assigned
on assignment_histories (tenant_id, opportunity_id, assigned_at desc);

create index idx_assignment_histories__tenant_owner_assigned
on assignment_histories (tenant_id, to_owner_id, assigned_at desc);

create table unassigned_assignment_items (
    unassigned_item_id uuid not null,
    tenant_id uuid not null,
    opportunity_id uuid not null,
    source_lead_id uuid not null,
    assignment_rule_id uuid,
    reason_code varchar(64) not null,
    status varchar(32) not null,
    created_at timestamptz not null,
    created_by uuid,
    resolved_at timestamptz,
    resolved_by uuid,
    updated_at timestamptz not null,
    updated_by uuid,
    constraint pk_unassigned_assignment_items primary key (unassigned_item_id),
    constraint fk_unassigned_assignment_items__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint fk_unassigned_assignment_items__admission_opportunities foreign key (opportunity_id) references admission_opportunities (opportunity_id),
    constraint fk_unassigned_assignment_items__leads foreign key (source_lead_id) references leads (lead_id),
    constraint fk_unassigned_assignment_items__assignment_rules foreign key (assignment_rule_id) references assignment_rules (assignment_rule_id),
    constraint ck_unassigned_assignment_items__reason check (
        reason_code in ('NO_ACTIVE_ADVISOR', 'NO_RULE_MATCH_NO_FALLBACK_ADVISOR')
    ),
    constraint ck_unassigned_assignment_items__status check (status in ('OPEN', 'RESOLVED')),
    constraint ck_unassigned_assignment_items__resolved_when_closed check (
        (status = 'OPEN' and resolved_at is null and resolved_by is null)
        or (status = 'RESOLVED' and resolved_at is not null)
    )
);

create unique index ux_unassigned_assignment_items__opportunity_open
on unassigned_assignment_items (tenant_id, opportunity_id)
where status = 'OPEN';

create index idx_unassigned_assignment_items__tenant_status_created
on unassigned_assignment_items (tenant_id, status, created_at desc);

create index idx_unassigned_assignment_items__tenant_rule_status
on unassigned_assignment_items (tenant_id, assignment_rule_id, status)
where assignment_rule_id is not null;

insert into permissions (
    permission_id,
    function_code,
    permission_name,
    description,
    status
) values
    ('00000000-0000-0000-0000-000000000225', 'assignment.view', 'View assignment', 'Read assignment rules and unassigned queue according to scope', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000226', 'assignment.manage', 'Manage assignment', 'Configure assignment rules and trigger assignment according to scope', 'ACTIVE')
on conflict (function_code) do update set
    permission_name = excluded.permission_name,
    description = excluded.description,
    status = excluded.status,
    updated_at = now();

with default_profiles(role_code, function_code, access_level, scope) as (
    values
        ('ADMIN', 'assignment.view', 'VIEW', 'TENANT'),
        ('CEO', 'assignment.view', 'VIEW', 'TENANT'),
        ('SALES_LEAD', 'assignment.view', 'VIEW', 'BRANCH'),
        ('ADMIN', 'assignment.manage', 'MANAGE', 'TENANT'),
        ('SALES_LEAD', 'assignment.manage', 'MANAGE', 'BRANCH')
),
tenant_profiles as (
    select
        tenants.tenant_id,
        default_profiles.role_code,
        default_profiles.function_code,
        default_profiles.access_level,
        default_profiles.scope,
        md5(tenants.tenant_id::text || ':' || default_profiles.function_code || ':' || default_profiles.role_code) as profile_hash
    from tenants
    cross join default_profiles
)
insert into permission_profiles (
    permission_profile_id,
    tenant_id,
    role_code,
    function_code,
    access_level,
    scope,
    status,
    created_at,
    updated_at
)
select
    (
        substring(profile_hash from 1 for 8) || '-' ||
        substring(profile_hash from 9 for 4) || '-' ||
        substring(profile_hash from 13 for 4) || '-' ||
        substring(profile_hash from 17 for 4) || '-' ||
        substring(profile_hash from 21 for 12)
    )::uuid,
    tenant_id,
    role_code,
    function_code,
    access_level,
    scope,
    'ACTIVE',
    now(),
    now()
from tenant_profiles
where not exists (
    select 1
    from permission_profiles existing_profile
    where existing_profile.tenant_id = tenant_profiles.tenant_id
      and existing_profile.role_code = tenant_profiles.role_code
      and existing_profile.function_code = tenant_profiles.function_code
      and coalesce(existing_profile.scope, 'GLOBAL') = tenant_profiles.scope
);
