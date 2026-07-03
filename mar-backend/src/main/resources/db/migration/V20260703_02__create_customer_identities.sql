create unique index ux_customer_profiles__customer_tenant
on customer_profiles (customer_id, tenant_id);

create table customer_identities (
    identity_id uuid not null,
    tenant_id uuid not null,
    customer_id uuid not null,
    identity_type varchar(50) not null,
    raw_value varchar(255) not null,
    normalized_value varchar(255) not null,
    primary_identity boolean not null default false,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint pk_customer_identities primary key (identity_id),
    constraint fk_customer_identities__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint fk_customer_identities__customer_profiles foreign key (customer_id, tenant_id)
        references customer_profiles (customer_id, tenant_id),
    constraint ck_customer_identities__identity_type check (identity_type in ('PHONE', 'EMAIL', 'ZALO_ID')),
    constraint ck_customer_identities__raw_value_not_blank check (length(trim(raw_value)) > 0),
    constraint ck_customer_identities__normalized_value_not_blank check (length(trim(normalized_value)) > 0)
);

create index idx_customer_identities__tenant_id
on customer_identities (tenant_id);

create index idx_customer_identities__tenant_customer
on customer_identities (tenant_id, customer_id);

create index idx_customer_identities__tenant_type_normalized
on customer_identities (tenant_id, identity_type, normalized_value);

create unique index ux_customer_identities__tenant_customer_type_value
on customer_identities (tenant_id, customer_id, identity_type, normalized_value);

create unique index ux_customer_identities__tenant_customer_type_primary
on customer_identities (tenant_id, customer_id, identity_type)
where primary_identity = true;
