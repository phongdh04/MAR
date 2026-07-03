insert into permissions (
    permission_id,
    function_code,
    permission_name,
    description,
    status
) values
    ('00000000-0000-0000-0000-000000000219', 'duplicate.manage', 'Manage duplicate case', 'Review and resolve duplicate customer cases', 'ACTIVE')
on conflict (function_code) do update set
    permission_name = excluded.permission_name,
    description = excluded.description,
    status = excluded.status,
    updated_at = now();

with default_profiles(role_code, access_level, scope) as (
    values
        ('ADMIN', 'MANAGE', 'TENANT'),
        ('SALES_LEAD', 'MANAGE', 'BRANCH')
),
tenant_profiles as (
    select
        tenants.tenant_id,
        default_profiles.role_code,
        default_profiles.access_level,
        default_profiles.scope,
        md5(tenants.tenant_id::text || ':duplicate.manage:' || default_profiles.role_code) as profile_hash
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
    'duplicate.manage',
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
      and existing_profile.function_code = 'duplicate.manage'
      and coalesce(existing_profile.scope, 'GLOBAL') = tenant_profiles.scope
);
