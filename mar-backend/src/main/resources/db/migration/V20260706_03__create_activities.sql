create table activities (
    activity_id uuid not null,
    tenant_id uuid not null,
    customer_id uuid not null,
    opportunity_id uuid not null,
    actor_id uuid,
    actor_type varchar(32) not null,
    activity_type varchar(32) not null,
    activity_result varchar(32),
    occurred_at timestamptz not null,
    note text,
    next_action_at timestamptz,
    source varchar(32) not null,
    created_at timestamptz not null,
    constraint pk_activities primary key (activity_id),
    constraint fk_activities__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint fk_activities__customer_profiles foreign key (customer_id) references customer_profiles (customer_id),
    constraint fk_activities__admission_opportunities foreign key (opportunity_id) references admission_opportunities (opportunity_id),
    constraint fk_activities__users foreign key (actor_id) references users (user_id),
    constraint ck_activities__actor_type check (actor_type in ('USER', 'SYSTEM', 'INTEGRATION')),
    constraint ck_activities__type check (activity_type in ('CALL', 'ZALO', 'SMS', 'EMAIL', 'NOTE', 'MEETING', 'SYSTEM')),
    constraint ck_activities__result check (
        activity_result is null
        or activity_result in ('ATTEMPTED', 'CONNECTED', 'REPLIED', 'NO_ANSWER', 'FAILED', 'SENT')
    ),
    constraint ck_activities__source check (source in ('MANUAL', 'SYSTEM', 'INTEGRATION')),
    constraint ck_activities__outbound_result_required check (
        activity_type not in ('CALL', 'ZALO', 'SMS', 'EMAIL', 'MEETING')
        or activity_result is not null
    ),
    constraint ck_activities__note_not_blank check (
        note is null
        or length(trim(note)) > 0
    ),
    constraint ck_activities__next_action_after_occurred check (
        next_action_at is null
        or next_action_at >= occurred_at
    )
);

create index idx_activities__tenant_opportunity_occurred
on activities (tenant_id, opportunity_id, occurred_at desc, activity_id desc);

create index idx_activities__tenant_customer_occurred
on activities (tenant_id, customer_id, occurred_at desc, activity_id desc);

create index idx_activities__tenant_actor_occurred
on activities (tenant_id, actor_id, occurred_at desc, activity_id desc)
where actor_id is not null;

create index idx_activities__tenant_opportunity_type_result
on activities (tenant_id, opportunity_id, activity_type, activity_result);

insert into permissions (
    permission_id,
    function_code,
    permission_name,
    description,
    status
) values
    ('00000000-0000-0000-0000-000000000221', 'activity.view', 'View activity', 'Read opportunity activity timeline according to scope', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000222', 'activity.create', 'Create activity', 'Record opportunity activity according to scope', 'ACTIVE')
on conflict (function_code) do update set
    permission_name = excluded.permission_name,
    description = excluded.description,
    status = excluded.status,
    updated_at = now();

with default_profiles(role_code, function_code, access_level, scope) as (
    values
        ('ADMIN', 'activity.view', 'VIEW', 'TENANT'),
        ('CEO', 'activity.view', 'VIEW', 'TENANT'),
        ('SALES_LEAD', 'activity.view', 'VIEW', 'BRANCH'),
        ('ADVISOR', 'activity.view', 'VIEW', 'OWN'),
        ('ADMIN', 'activity.create', 'CREATE', 'TENANT'),
        ('ADVISOR', 'activity.create', 'CREATE', 'OWN')
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
