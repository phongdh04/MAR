create table sla_tasks (
    sla_task_id uuid not null,
    tenant_id uuid not null,
    opportunity_id uuid not null,
    source_lead_id uuid not null,
    owner_id uuid not null,
    branch_id uuid,
    sla_policy_id uuid,
    task_type varchar(32) not null,
    status varchar(32) not null,
    lead_temperature varchar(32) not null,
    due_at timestamptz not null,
    completed_at timestamptz,
    completed_activity_id uuid,
    overdue_marked_at timestamptz,
    overdue_level varchar(32) not null,
    escalated_to uuid,
    sales_lead_escalated_at timestamptz,
    cancellation_reason varchar(128),
    created_at timestamptz not null,
    created_by uuid,
    updated_at timestamptz not null,
    updated_by uuid,
    constraint pk_sla_tasks primary key (sla_task_id),
    constraint fk_sla_tasks__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint fk_sla_tasks__admission_opportunities foreign key (opportunity_id) references admission_opportunities (opportunity_id),
    constraint fk_sla_tasks__leads foreign key (source_lead_id) references leads (lead_id),
    constraint fk_sla_tasks__owner_users foreign key (owner_id) references users (user_id),
    constraint fk_sla_tasks__branches foreign key (branch_id) references branches (branch_id),
    constraint fk_sla_tasks__sla_policies foreign key (sla_policy_id) references sla_policies (sla_policy_id),
    constraint fk_sla_tasks__completed_activities foreign key (completed_activity_id) references activities (activity_id),
    constraint fk_sla_tasks__escalated_to_users foreign key (escalated_to) references users (user_id),
    constraint ck_sla_tasks__task_type check (task_type in ('FIRST_RESPONSE')),
    constraint ck_sla_tasks__status check (status in ('OPEN', 'COMPLETED', 'OVERDUE', 'CANCELLED')),
    constraint ck_sla_tasks__lead_temperature check (lead_temperature in ('HOT', 'NORMAL', 'AFTER_HOURS')),
    constraint ck_sla_tasks__overdue_level check (overdue_level in ('NONE', 'ADVISOR', 'SALES_LEAD')),
    constraint ck_sla_tasks__completion_state check (
        (
            status = 'COMPLETED'
            and completed_at is not null
            and completed_activity_id is not null
        )
        or (
            status <> 'COMPLETED'
            and completed_at is null
            and completed_activity_id is null
        )
    ),
    constraint ck_sla_tasks__overdue_state check (
        (
            status = 'OVERDUE'
            and overdue_marked_at is not null
            and overdue_level in ('ADVISOR', 'SALES_LEAD')
        )
        or status <> 'OVERDUE'
    ),
    constraint ck_sla_tasks__sales_lead_escalation_state check (
        (
            overdue_level = 'SALES_LEAD'
            and sales_lead_escalated_at is not null
        )
        or (
            overdue_level <> 'SALES_LEAD'
            and sales_lead_escalated_at is null
        )
    ),
    constraint ck_sla_tasks__cancellation_reason_not_blank check (
        cancellation_reason is null
        or length(trim(cancellation_reason)) > 0
    )
);

create unique index ux_sla_tasks__opportunity_type_active
on sla_tasks (tenant_id, opportunity_id, task_type)
where status in ('OPEN', 'OVERDUE');

create index idx_sla_tasks__tenant_owner_status_due
on sla_tasks (tenant_id, owner_id, status, due_at asc);

create index idx_sla_tasks__tenant_branch_status_due
on sla_tasks (tenant_id, branch_id, status, due_at asc)
where branch_id is not null;

create index idx_sla_tasks__tenant_status_due
on sla_tasks (tenant_id, status, due_at asc);

create index idx_sla_tasks__tenant_opportunity_type
on sla_tasks (tenant_id, opportunity_id, task_type, created_at desc);

create index idx_sla_tasks__tenant_overdue_escalation
on sla_tasks (tenant_id, status, overdue_level, overdue_marked_at asc)
where status = 'OVERDUE';

create index idx_sla_tasks__tenant_completed_activity
on sla_tasks (tenant_id, completed_activity_id)
where completed_activity_id is not null;

insert into permissions (
    permission_id,
    function_code,
    permission_name,
    description,
    status
) values
    ('00000000-0000-0000-0000-000000000227', 'sla.task.view', 'View SLA task', 'Read first response SLA tasks according to scope', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000228', 'sla.task.manage', 'Manage SLA task', 'Run SLA task overdue scan and manage SLA task operational state according to scope', 'ACTIVE')
on conflict (function_code) do update set
    permission_name = excluded.permission_name,
    description = excluded.description,
    status = excluded.status,
    updated_at = now();

with default_profiles(role_code, function_code, access_level, scope) as (
    values
        ('ADMIN', 'sla.task.view', 'VIEW', 'TENANT'),
        ('CEO', 'sla.task.view', 'VIEW', 'TENANT'),
        ('SALES_LEAD', 'sla.task.view', 'VIEW', 'BRANCH'),
        ('ADVISOR', 'sla.task.view', 'VIEW', 'OWN'),
        ('ADMIN', 'sla.task.manage', 'MANAGE', 'TENANT'),
        ('SALES_LEAD', 'sla.task.manage', 'MANAGE', 'BRANCH')
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
