create table integration_events (
    integration_event_id uuid not null,
    tenant_id uuid not null,
    source_type varchar(50) not null,
    external_id varchar(255),
    idempotency_key varchar(255),
    payload_hash varchar(128) not null,
    status varchar(50) not null,
    error_code varchar(128),
    error_message text,
    raw_payload_uri text,
    created_lead_id uuid,
    created_customer_id uuid,
    created_opportunity_id uuid,
    received_at timestamptz not null default now(),
    processed_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint pk_integration_events primary key (integration_event_id),
    constraint fk_integration_events__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint fk_integration_events__leads foreign key (created_lead_id) references leads (lead_id),
    constraint fk_integration_events__customer_profiles foreign key (created_customer_id) references customer_profiles (customer_id),
    constraint fk_integration_events__admission_opportunities foreign key (created_opportunity_id) references admission_opportunities (opportunity_id),
    constraint ck_integration_events__source_type check (source_type in ('CSV', 'GOOGLE_SHEET', 'WEBSITE_FORM', 'META_LEAD_ADS', 'MANUAL', 'OTHER')),
    constraint ck_integration_events__status check (status in ('RECEIVED', 'PROCESSED', 'FAILED', 'DUPLICATE')),
    constraint ck_integration_events__external_id_not_blank check (external_id is null or length(trim(external_id)) > 0),
    constraint ck_integration_events__idempotency_key_not_blank check (idempotency_key is null or length(trim(idempotency_key)) > 0),
    constraint ck_integration_events__payload_hash_not_blank check (length(trim(payload_hash)) > 0),
    constraint ck_integration_events__processed_after_received check (processed_at is null or processed_at >= received_at)
);

alter table leads
add constraint fk_leads__integration_events foreign key (integration_event_id) references integration_events (integration_event_id);

create index idx_integration_events__tenant_id
on integration_events (tenant_id);

create unique index ux_integration_events__tenant_source_external_active
on integration_events (tenant_id, source_type, external_id)
where external_id is not null and status <> 'DUPLICATE';

create unique index ux_integration_events__tenant_source_idempotency_active
on integration_events (tenant_id, source_type, idempotency_key)
where idempotency_key is not null and status <> 'DUPLICATE';

create index idx_integration_events__tenant_status_received
on integration_events (tenant_id, status, received_at desc);

create index idx_integration_events__tenant_payload_hash
on integration_events (tenant_id, payload_hash);
