insert into roles (
    role_id,
    role_code,
    role_name,
    description,
    status
) values
    ('00000000-0000-0000-0000-000000000101', 'CEO', 'CEO', 'Executive owner', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000102', 'ADMIN', 'Admin', 'Tenant administrator', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000103', 'MARKETING', 'Marketing', 'Marketing operator', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000104', 'SALES_LEAD', 'Sales Lead', 'Sales team lead', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000105', 'ADVISOR', 'Advisor', 'Sales advisor', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000106', 'CSKH', 'CSKH', 'Customer care', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000107', 'FINANCE', 'Finance', 'Finance operator', 'ACTIVE')
on conflict (role_code) do update set
    role_name = excluded.role_name,
    description = excluded.description,
    status = excluded.status,
    updated_at = now();

insert into permissions (
    permission_id,
    function_code,
    permission_name,
    description,
    status
) values
    ('00000000-0000-0000-0000-000000000201', 'tenant.view', 'View tenant', 'Read tenant setup', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000202', 'tenant.manage', 'Manage tenant', 'Create or update tenant setup', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000203', 'branch.view', 'View branch', 'Read branch setup', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000204', 'branch.manage', 'Manage branch', 'Create or update branch setup', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000205', 'user.view', 'View user', 'Read user setup', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000206', 'user.manage', 'Manage user', 'Create or update user setup', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000207', 'permission.manage', 'Manage permission', 'Update permission matrix', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000208', 'catalog.manage', 'Manage catalog', 'Create or update catalog setup', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000209', 'lead.view', 'View lead', 'Read lead and setup choices', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000210', 'lead.import', 'Import lead', 'Import lead data', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000211', 'audit.view', 'View audit', 'Read audit events', 'ACTIVE')
on conflict (function_code) do update set
    permission_name = excluded.permission_name,
    description = excluded.description,
    status = excluded.status,
    updated_at = now();
