create table customer_profiles (
    customer_id uuid not null,
    tenant_id uuid not null,
    full_name varchar(255),
    primary_phone varchar(32),
    primary_email varchar(255),
    zalo_id varchar(255),
    guardian_name varchar(255),
    guardian_phone varchar(32),
    preferred_channel varchar(32),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint pk_customer_profiles primary key (customer_id),
    constraint fk_customer_profiles__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint ck_customer_profiles__preferred_channel check (
        preferred_channel is null
        or preferred_channel in ('ZALO', 'SMS', 'EMAIL', 'CALL')
    ),
    constraint ck_customer_profiles__contact_identifier_required check (
        primary_phone is not null
        or primary_email is not null
        or zalo_id is not null
    )
);

create index idx_customer_profiles__tenant_id
on customer_profiles (tenant_id);

create index idx_customer_profiles__tenant_primary_phone
on customer_profiles (tenant_id, primary_phone)
where primary_phone is not null;

create index idx_customer_profiles__tenant_primary_email
on customer_profiles (tenant_id, lower(primary_email))
where primary_email is not null;

create index idx_customer_profiles__tenant_zalo_id
on customer_profiles (tenant_id, zalo_id)
where zalo_id is not null;
