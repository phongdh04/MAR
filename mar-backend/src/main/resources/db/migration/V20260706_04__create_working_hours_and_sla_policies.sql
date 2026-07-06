create table working_hours_configs (
    working_hours_id uuid not null,
    tenant_id uuid not null,
    branch_id uuid,
    weekday varchar(16) not null,
    start_time time,
    end_time time,
    timezone varchar(100) not null,
    is_working_day boolean not null,
    status varchar(32) not null,
    created_at timestamptz not null,
    created_by uuid,
    updated_at timestamptz not null,
    updated_by uuid,
    constraint pk_working_hours_configs primary key (working_hours_id),
    constraint fk_working_hours_configs__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint fk_working_hours_configs__branches foreign key (branch_id) references branches (branch_id),
    constraint fk_working_hours_configs__created_by_users foreign key (created_by) references users (user_id),
    constraint fk_working_hours_configs__updated_by_users foreign key (updated_by) references users (user_id),
    constraint ck_working_hours_configs__weekday check (
        weekday in ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')
    ),
    constraint ck_working_hours_configs__status check (status in ('ACTIVE', 'INACTIVE')),
    constraint ck_working_hours_configs__timezone_not_blank check (length(trim(timezone)) > 0),
    constraint ck_working_hours_configs__working_time_range check (
        (
            is_working_day = true
            and start_time is not null
            and end_time is not null
            and start_time < end_time
        )
        or (
            is_working_day = false
            and start_time is null
            and end_time is null
        )
    )
);

create unique index ux_working_hours_configs__tenant_branch_weekday_active
on working_hours_configs (
    tenant_id,
    coalesce(branch_id, '00000000-0000-0000-0000-000000000000'::uuid),
    weekday
)
where status = 'ACTIVE';

create index idx_working_hours_configs__tenant_branch_status
on working_hours_configs (tenant_id, branch_id, status);

create table sla_policies (
    sla_policy_id uuid not null,
    tenant_id uuid not null,
    branch_id uuid,
    policy_type varchar(32) not null,
    response_due_minutes integer not null,
    timezone varchar(100) not null,
    status varchar(32) not null,
    created_at timestamptz not null,
    created_by uuid,
    updated_at timestamptz not null,
    updated_by uuid,
    constraint pk_sla_policies primary key (sla_policy_id),
    constraint fk_sla_policies__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint fk_sla_policies__branches foreign key (branch_id) references branches (branch_id),
    constraint fk_sla_policies__created_by_users foreign key (created_by) references users (user_id),
    constraint fk_sla_policies__updated_by_users foreign key (updated_by) references users (user_id),
    constraint ck_sla_policies__type check (policy_type in ('HOT', 'NORMAL', 'AFTER_HOURS')),
    constraint ck_sla_policies__status check (status in ('ACTIVE', 'INACTIVE')),
    constraint ck_sla_policies__response_due_minutes check (response_due_minutes >= 0),
    constraint ck_sla_policies__timezone_not_blank check (length(trim(timezone)) > 0)
);

create unique index ux_sla_policies__tenant_branch_type_active
on sla_policies (
    tenant_id,
    coalesce(branch_id, '00000000-0000-0000-0000-000000000000'::uuid),
    policy_type
)
where status = 'ACTIVE';

create index idx_sla_policies__tenant_branch_status
on sla_policies (tenant_id, branch_id, status);

create index idx_sla_policies__tenant_type_status
on sla_policies (tenant_id, policy_type, status);

alter table admission_opportunities
add constraint fk_admission_opportunities__sla_policies
foreign key (sla_policy_id) references sla_policies (sla_policy_id);

with weekdays(weekday, is_working_day, start_time, end_time) as (
    values
        ('MONDAY', true, '08:00'::time, '18:00'::time),
        ('TUESDAY', true, '08:00'::time, '18:00'::time),
        ('WEDNESDAY', true, '08:00'::time, '18:00'::time),
        ('THURSDAY', true, '08:00'::time, '18:00'::time),
        ('FRIDAY', true, '08:00'::time, '18:00'::time),
        ('SATURDAY', true, '08:00'::time, '18:00'::time),
        ('SUNDAY', false, null::time, null::time)
),
tenant_working_hours as (
    select
        tenants.tenant_id,
        weekdays.weekday,
        weekdays.is_working_day,
        weekdays.start_time,
        weekdays.end_time,
        tenants.timezone,
        md5(tenants.tenant_id::text || ':working-hours:' || weekdays.weekday) as row_hash
    from tenants
    cross join weekdays
)
insert into working_hours_configs (
    working_hours_id,
    tenant_id,
    branch_id,
    weekday,
    start_time,
    end_time,
    timezone,
    is_working_day,
    status,
    created_at,
    updated_at
)
select
    (
        substring(row_hash from 1 for 8) || '-' ||
        substring(row_hash from 9 for 4) || '-' ||
        substring(row_hash from 13 for 4) || '-' ||
        substring(row_hash from 17 for 4) || '-' ||
        substring(row_hash from 21 for 12)
    )::uuid,
    tenant_id,
    null,
    weekday,
    start_time,
    end_time,
    timezone,
    is_working_day,
    'ACTIVE',
    now(),
    now()
from tenant_working_hours
where not exists (
    select 1
    from working_hours_configs existing_config
    where existing_config.tenant_id = tenant_working_hours.tenant_id
      and existing_config.branch_id is null
      and existing_config.weekday = tenant_working_hours.weekday
      and existing_config.status = 'ACTIVE'
);

with default_policies(policy_type, response_due_minutes) as (
    values
        ('HOT', 15),
        ('NORMAL', 60),
        ('AFTER_HOURS', 0)
),
tenant_policies as (
    select
        tenants.tenant_id,
        default_policies.policy_type,
        default_policies.response_due_minutes,
        tenants.timezone,
        md5(tenants.tenant_id::text || ':sla-policy:' || default_policies.policy_type) as row_hash
    from tenants
    cross join default_policies
)
insert into sla_policies (
    sla_policy_id,
    tenant_id,
    branch_id,
    policy_type,
    response_due_minutes,
    timezone,
    status,
    created_at,
    updated_at
)
select
    (
        substring(row_hash from 1 for 8) || '-' ||
        substring(row_hash from 9 for 4) || '-' ||
        substring(row_hash from 13 for 4) || '-' ||
        substring(row_hash from 17 for 4) || '-' ||
        substring(row_hash from 21 for 12)
    )::uuid,
    tenant_id,
    null,
    policy_type,
    response_due_minutes,
    timezone,
    'ACTIVE',
    now(),
    now()
from tenant_policies
where not exists (
    select 1
    from sla_policies existing_policy
    where existing_policy.tenant_id = tenant_policies.tenant_id
      and existing_policy.branch_id is null
      and existing_policy.policy_type = tenant_policies.policy_type
      and existing_policy.status = 'ACTIVE'
);

insert into permissions (
    permission_id,
    function_code,
    permission_name,
    description,
    status
) values
    ('00000000-0000-0000-0000-000000000223', 'sla.view', 'View SLA settings', 'Read working hours and SLA policy settings according to scope', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000224', 'sla.manage', 'Manage SLA settings', 'Update working hours and SLA policy settings according to scope', 'ACTIVE')
on conflict (function_code) do update set
    permission_name = excluded.permission_name,
    description = excluded.description,
    status = excluded.status,
    updated_at = now();

with default_profiles(role_code, function_code, access_level, scope) as (
    values
        ('ADMIN', 'sla.view', 'VIEW', 'TENANT'),
        ('CEO', 'sla.view', 'VIEW', 'TENANT'),
        ('SALES_LEAD', 'sla.view', 'VIEW', 'BRANCH'),
        ('ADMIN', 'sla.manage', 'MANAGE', 'TENANT'),
        ('SALES_LEAD', 'sla.manage', 'MANAGE', 'BRANCH')
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
