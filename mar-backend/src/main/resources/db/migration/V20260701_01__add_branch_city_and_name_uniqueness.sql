alter table branches
add column city varchar(100);

create unique index ux_branches__tenant_name_active
on branches (tenant_id, lower(branch_name))
where status = 'ACTIVE';
