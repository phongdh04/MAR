alter table duplicate_cases
drop constraint ck_duplicate_cases__status;

alter table duplicate_cases
add constraint ck_duplicate_cases__status
check (status in ('NEEDS_REVIEW', 'MERGED', 'LINKED', 'IGNORED', 'UNMERGED'));

alter table duplicate_cases
drop constraint ck_duplicate_cases__resolved_state;

alter table duplicate_cases
add constraint ck_duplicate_cases__resolved_state check (
    (
        status = 'NEEDS_REVIEW'
        and resolution_action is null
        and resolved_by is null
        and resolved_at is null
    )
    or (
        status in ('MERGED', 'LINKED', 'IGNORED', 'UNMERGED')
        and resolution_action is not null
        and resolved_by is not null
        and resolved_at is not null
    )
);

create table merge_history (
    merge_id uuid not null,
    tenant_id uuid not null,
    source_customer_id uuid not null,
    target_customer_id uuid not null,
    duplicate_case_id uuid,
    merged_by uuid not null,
    merged_at timestamptz not null,
    reason text not null,
    merge_snapshot jsonb,
    can_unmerge boolean not null default true,
    unmerged_by uuid,
    unmerged_at timestamptz,
    constraint pk_merge_history primary key (merge_id),
    constraint fk_merge_history__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint fk_merge_history__source_customer_profiles foreign key (source_customer_id, tenant_id)
        references customer_profiles (customer_id, tenant_id),
    constraint fk_merge_history__target_customer_profiles foreign key (target_customer_id, tenant_id)
        references customer_profiles (customer_id, tenant_id),
    constraint fk_merge_history__duplicate_cases foreign key (duplicate_case_id)
        references duplicate_cases (duplicate_case_id),
    constraint fk_merge_history__merged_by_users foreign key (merged_by) references users (user_id),
    constraint fk_merge_history__unmerged_by_users foreign key (unmerged_by) references users (user_id),
    constraint ck_merge_history__customers_different check (source_customer_id <> target_customer_id),
    constraint ck_merge_history__reason_not_blank check (length(trim(reason)) > 0),
    constraint ck_merge_history__merge_snapshot_object check (
        merge_snapshot is null
        or jsonb_typeof(merge_snapshot) = 'object'
    ),
    constraint ck_merge_history__unmerged_actor_pair check (
        (
            unmerged_by is null
            and unmerged_at is null
        )
        or (
            unmerged_by is not null
            and unmerged_at is not null
        )
    )
);

create index idx_merge_history__tenant_source
on merge_history (tenant_id, source_customer_id, merged_at desc);

create index idx_merge_history__tenant_target
on merge_history (tenant_id, target_customer_id, merged_at desc);

create index idx_merge_history__tenant_duplicate_case
on merge_history (tenant_id, duplicate_case_id)
where duplicate_case_id is not null;

create index idx_merge_history__tenant_unmerged
on merge_history (tenant_id, unmerged_at)
where unmerged_at is not null;

with default_profiles(role_code, access_level, scope) as (
    values
        ('ADMIN', 'MANAGE', 'TENANT')
),
tenant_profiles as (
    select
        tenants.tenant_id,
        default_profiles.role_code,
        default_profiles.access_level,
        default_profiles.scope,
        md5(tenants.tenant_id::text || ':customer.merge:' || default_profiles.role_code) as profile_hash
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
    'customer.merge',
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
      and existing_profile.function_code = 'customer.merge'
      and coalesce(existing_profile.scope, 'GLOBAL') = tenant_profiles.scope
);
