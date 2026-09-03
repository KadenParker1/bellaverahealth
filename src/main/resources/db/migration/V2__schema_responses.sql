create table app.survey_response (
    id                uuid primary key default gen_random_uuid(),
    user_id           uuid not null references app.app_user (id) on delete cascade,
    survey_version_id uuid not null references app.survey_version (id),
    status            text not null default 'IN_PROGRESS' check (status in ('IN_PROGRESS', 'SUBMITTED')),
    started_at        timestamptz not null default now(),
    submitted_at      timestamptz,
    updated_at        timestamptz not null default now(),
    lock_version      bigint not null default 0
);

create unique index ux_response_one_draft
    on app.survey_response (user_id, survey_version_id) where status = 'IN_PROGRESS';
create index ix_response_user on app.survey_response (user_id, status);
create index ix_response_version on app.survey_response (survey_version_id);

create table app.answer (
    id                  uuid primary key default gen_random_uuid(),
    survey_response_id  uuid not null references app.survey_response (id) on delete cascade,
    question_id         uuid not null references app.question (id),
    question_code       text not null,
    value_text          text,
    value_number        numeric,
    value_boolean       boolean,
    value_date          date,
    value_json          jsonb,
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now(),
    unique (survey_response_id, question_id)
);

create index ix_answer_code on app.answer (question_code);
create index ix_answer_response on app.answer (survey_response_id);

create table app.answer_option (
    answer_id          uuid not null references app.answer (id) on delete cascade,
    question_option_id uuid not null references app.question_option (id),
    primary key (answer_id, question_option_id)
);
