alter table programs
add column exam_track varchar(128);

alter table courses
add column level varchar(128);

create unique index ux_languages__tenant_name_active
on languages (tenant_id, lower(language_name))
where status = 'ACTIVE';

create unique index ux_programs__tenant_language_name_active
on programs (tenant_id, language_id, lower(program_name))
where status = 'ACTIVE';

create unique index ux_courses__tenant_program_name_active
on courses (tenant_id, program_id, lower(course_name))
where status = 'ACTIVE';
