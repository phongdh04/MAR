alter table permission_profiles
drop constraint ck_permission_profiles__access_level;

alter table permission_profiles
add constraint ck_permission_profiles__access_level
check (access_level in ('NONE', 'VIEW', 'CREATE', 'UPDATE', 'MANAGE', 'FULL'));

insert into permissions (
    permission_id,
    function_code,
    permission_name,
    description,
    status
) values
    ('00000000-0000-0000-0000-000000000212', 'permission.view', 'View permission', 'Read permission matrix', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000213', 'catalog.view', 'View catalog', 'Read catalog setup', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000214', 'import.view', 'View import', 'Read import foundation state', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000215', 'import.manage', 'Manage import', 'Prepare import foundation', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000216', 'customer.merge', 'Merge customer', 'Merge or unmerge customer duplicates', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000217', 'data.export', 'Export data', 'Export tenant data', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000218', 'payment.write', 'Write payment', 'Create or update payment data', 'ACTIVE')
on conflict (function_code) do update set
    permission_name = excluded.permission_name,
    description = excluded.description,
    status = excluded.status,
    updated_at = now();
