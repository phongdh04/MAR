alter table admission_opportunities
add constraint ck_admission_opportunities__lost_reason
check (
    lost_reason is null
    or lost_reason in (
        'UNCONTACTABLE',
        'NO_NEED',
        'WRONG_TARGET',
        'WRONG_AREA',
        'TUITION_TOO_HIGH',
        'SCHEDULE_NOT_FIT',
        'CHOSE_COMPETITOR',
        'NOT_READY',
        'SPAM',
        'OTHER'
    )
);

alter table admission_opportunities
add constraint ck_admission_opportunities__lost_note_required
check (
    current_stage <> 'LOST'
    or lost_reason <> 'OTHER'
    or (lost_note is not null and length(trim(lost_note)) > 0)
);

create table stage_history (
    stage_history_id uuid not null,
    tenant_id uuid not null,
    opportunity_id uuid not null,
    from_stage varchar(50),
    to_stage varchar(50) not null,
    changed_by uuid,
    changed_by_type varchar(32) not null,
    changed_at timestamptz not null,
    reason text,
    duration_in_previous_stage_seconds bigint,
    constraint pk_stage_history primary key (stage_history_id),
    constraint fk_stage_history__tenants foreign key (tenant_id) references tenants (tenant_id),
    constraint fk_stage_history__admission_opportunities foreign key (opportunity_id) references admission_opportunities (opportunity_id),
    constraint fk_stage_history__users foreign key (changed_by) references users (user_id),
    constraint ck_stage_history__from_stage check (
        from_stage is null
        or from_stage in (
            'NEW', 'CONTACTING', 'CONTACTED', 'QUALIFIED', 'PROGRAM_SELECTED',
            'APPOINTMENT_BOOKED', 'APPOINTMENT_DONE', 'NO_SHOW', 'CANCELLED',
            'CONSULTING', 'DEPOSIT_PAID', 'ENROLLED', 'LOST', 'NURTURING', 'REFUNDED'
        )
    ),
    constraint ck_stage_history__to_stage check (
        to_stage in (
            'NEW', 'CONTACTING', 'CONTACTED', 'QUALIFIED', 'PROGRAM_SELECTED',
            'APPOINTMENT_BOOKED', 'APPOINTMENT_DONE', 'NO_SHOW', 'CANCELLED',
            'CONSULTING', 'DEPOSIT_PAID', 'ENROLLED', 'LOST', 'NURTURING', 'REFUNDED'
        )
    ),
    constraint ck_stage_history__changed_by_type check (changed_by_type in ('USER', 'SYSTEM')),
    constraint ck_stage_history__duration_non_negative check (
        duration_in_previous_stage_seconds is null
        or duration_in_previous_stage_seconds >= 0
    )
);

create index idx_stage_history__tenant_opportunity_changed
on stage_history (tenant_id, opportunity_id, changed_at);

create index idx_stage_history__tenant_stage_changed
on stage_history (tenant_id, to_stage, changed_at);
