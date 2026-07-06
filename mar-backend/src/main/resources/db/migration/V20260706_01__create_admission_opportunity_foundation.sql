create table leads (
    lead_id uuid not null,
    tenant_id uuid not null,
    external_id varchar(255),
    full_name varchar(255),
    phone_raw varchar(64),
    phone_normalized varchar(32),
    email varchar(255),
    zalo_id varchar(255),
    source_type varchar(50) not null,
    source varchar(128),
    source_created_at timestamptz,
    language_id uuid,
    program_id uuid,
    branch_id uuid,
    campaign varchar(255),
    adset varchar(255),
    ad varchar(255),
    utm_source varchar(255),
    utm_medium varchar(255),
    utm_campaign varchar(255),
    consent_consultation varchar(32),
    consent_marketing varchar(32),
    contactability varchar(32) not null,
    lead_temperature varchar(32),
    temperature_reason text,
    import_batch_id uuid,
    integration_event_id uuid,
    lead_status varchar(32) not null,
    customer_id uuid,
    opportunity_id uuid,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint pk_leads primary key (lead_id),
    constraint fk_leads__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint fk_leads__languages foreign key (language_id) references languages (language_id),
    constraint fk_leads__programs foreign key (program_id) references programs (program_id),
    constraint fk_leads__branches foreign key (branch_id) references branches (branch_id),
    constraint fk_leads__import_batches foreign key (import_batch_id) references import_batches (import_batch_id),
    constraint fk_leads__customer_profiles foreign key (customer_id) references customer_profiles (customer_id),
    constraint ck_leads__source_type check (source_type in ('CSV', 'GOOGLE_SHEET', 'WEBSITE_FORM', 'META_LEAD_ADS', 'MANUAL', 'OTHER')),
    constraint ck_leads__consent_consultation check (
        consent_consultation is null
        or consent_consultation in ('GRANTED', 'DENIED', 'UNKNOWN', 'REVOKED')
    ),
    constraint ck_leads__consent_marketing check (
        consent_marketing is null
        or consent_marketing in ('GRANTED', 'DENIED', 'UNKNOWN', 'REVOKED')
    ),
    constraint ck_leads__contactability check (contactability in ('HIGH', 'MEDIUM', 'LOW')),
    constraint ck_leads__lead_temperature check (
        lead_temperature is null
        or lead_temperature in ('HOT', 'NORMAL', 'AFTER_HOURS')
    ),
    constraint ck_leads__status check (lead_status in ('VALID', 'INVALID', 'LINKED', 'DUPLICATE_REVIEW', 'SKIPPED')),
    constraint ck_leads__valid_contact_identifier check (
        lead_status = 'INVALID'
        or phone_normalized is not null
        or email is not null
        or zalo_id is not null
    )
);

create index idx_leads__tenant_phone_normalized
on leads (tenant_id, phone_normalized)
where phone_normalized is not null;

create index idx_leads__tenant_email
on leads (tenant_id, lower(email))
where email is not null;

create index idx_leads__tenant_zalo_id
on leads (tenant_id, zalo_id)
where zalo_id is not null;

create index idx_leads__tenant_external_source
on leads (tenant_id, external_id, source_type)
where external_id is not null;

create index idx_leads__tenant_source_created
on leads (tenant_id, source_type, created_at desc);

create index idx_leads__tenant_campaign
on leads (tenant_id, campaign)
where campaign is not null;

create index idx_leads__tenant_temperature_created
on leads (tenant_id, lead_temperature, created_at desc)
where lead_temperature is not null;

create table admission_opportunities (
    opportunity_id uuid not null,
    tenant_id uuid not null,
    customer_id uuid not null,
    source_lead_id uuid not null,
    language_id uuid,
    program_id uuid,
    course_id uuid,
    branch_id uuid,
    owner_id uuid,
    current_stage varchar(50) not null,
    qualification_status varchar(32),
    lead_temperature varchar(32),
    sla_policy_id uuid,
    lost_reason varchar(128),
    lost_note text,
    first_touch_id uuid,
    last_touch_id uuid,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint pk_admission_opportunities primary key (opportunity_id),
    constraint fk_admission_opportunities__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint fk_admission_opportunities__customer_profiles foreign key (customer_id) references customer_profiles (customer_id),
    constraint fk_admission_opportunities__leads foreign key (source_lead_id) references leads (lead_id),
    constraint fk_admission_opportunities__languages foreign key (language_id) references languages (language_id),
    constraint fk_admission_opportunities__programs foreign key (program_id) references programs (program_id),
    constraint fk_admission_opportunities__courses foreign key (course_id) references courses (course_id),
    constraint fk_admission_opportunities__branches foreign key (branch_id) references branches (branch_id),
    constraint fk_admission_opportunities__users foreign key (owner_id) references users (user_id),
    constraint ck_admission_opportunities__stage check (
        current_stage in (
            'NEW', 'CONTACTING', 'CONTACTED', 'QUALIFIED', 'PROGRAM_SELECTED',
            'APPOINTMENT_BOOKED', 'APPOINTMENT_DONE', 'NO_SHOW', 'CANCELLED',
            'CONSULTING', 'DEPOSIT_PAID', 'ENROLLED', 'LOST', 'NURTURING', 'REFUNDED'
        )
    ),
    constraint ck_admission_opportunities__qualification check (
        qualification_status is null
        or qualification_status in ('UNKNOWN', 'QUALIFIED', 'UNQUALIFIED')
    ),
    constraint ck_admission_opportunities__lead_temperature check (
        lead_temperature is null
        or lead_temperature in ('HOT', 'NORMAL', 'AFTER_HOURS')
    ),
    constraint ck_admission_opportunities__lost_reason_required check (
        current_stage <> 'LOST'
        or lost_reason is not null
    )
);

create index idx_admission_opportunities__tenant_owner_stage
on admission_opportunities (tenant_id, owner_id, current_stage);

create index idx_admission_opportunities__tenant_customer
on admission_opportunities (tenant_id, customer_id);

create index idx_admission_opportunities__tenant_program_stage
on admission_opportunities (tenant_id, program_id, current_stage);

create index idx_admission_opportunities__tenant_created
on admission_opportunities (tenant_id, created_at desc);

create table touchpoints (
    touchpoint_id uuid not null,
    tenant_id uuid not null,
    customer_id uuid not null,
    lead_id uuid not null,
    opportunity_id uuid,
    source varchar(128),
    campaign varchar(255),
    adset varchar(255),
    ad varchar(255),
    utm_source varchar(255),
    utm_medium varchar(255),
    utm_campaign varchar(255),
    touch_time timestamptz not null,
    touch_type varchar(32) not null,
    created_at timestamptz not null,
    constraint pk_touchpoints primary key (touchpoint_id),
    constraint fk_touchpoints__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint fk_touchpoints__customer_profiles foreign key (customer_id) references customer_profiles (customer_id),
    constraint fk_touchpoints__leads foreign key (lead_id) references leads (lead_id),
    constraint fk_touchpoints__admission_opportunities foreign key (opportunity_id) references admission_opportunities (opportunity_id),
    constraint ck_touchpoints__type check (touch_type in ('IMPORT', 'FORM', 'META', 'MANUAL'))
);

create index idx_touchpoints__tenant_customer_time
on touchpoints (tenant_id, customer_id, touch_time);

create index idx_touchpoints__tenant_opportunity_time
on touchpoints (tenant_id, opportunity_id, touch_time)
where opportunity_id is not null;

create index idx_touchpoints__tenant_source_campaign
on touchpoints (tenant_id, source, campaign);

alter table admission_opportunities
add constraint fk_admission_opportunities__first_touchpoints
foreign key (first_touch_id) references touchpoints (touchpoint_id);

alter table admission_opportunities
add constraint fk_admission_opportunities__last_touchpoints
foreign key (last_touch_id) references touchpoints (touchpoint_id);

alter table leads
add constraint fk_leads__admission_opportunities
foreign key (opportunity_id) references admission_opportunities (opportunity_id);

insert into permissions (
    permission_id,
    function_code,
    permission_name,
    description,
    status
) values
    ('00000000-0000-0000-0000-000000000220', 'opportunity.update', 'Update opportunity', 'View or update admission opportunities according to scope', 'ACTIVE')
on conflict (function_code) do update set
    permission_name = excluded.permission_name,
    description = excluded.description,
    status = excluded.status,
    updated_at = now();

with default_profiles(role_code, access_level, scope) as (
    values
        ('ADMIN', 'MANAGE', 'TENANT'),
        ('SALES_LEAD', 'MANAGE', 'BRANCH'),
        ('ADVISOR', 'UPDATE', 'OWN')
),
tenant_profiles as (
    select
        tenants.tenant_id,
        default_profiles.role_code,
        default_profiles.access_level,
        default_profiles.scope,
        md5(tenants.tenant_id::text || ':opportunity.update:' || default_profiles.role_code) as profile_hash
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
    'opportunity.update',
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
      and existing_profile.function_code = 'opportunity.update'
      and coalesce(existing_profile.scope, 'GLOBAL') = tenant_profiles.scope
);
