create schema if not exists app;

create extension if not exists pgcrypto;

-- =========================================================================
-- Identity
-- =========================================================================

create table app.app_user (
    id          uuid primary key,              -- == supabase auth.users.id, never generated here
    email       text not null unique,
    role        text not null default 'USER' check (role in ('USER', 'ADMIN')),
    status      text not null default 'ACTIVE' check (status in ('ACTIVE', 'SUSPENDED', 'DELETED')),
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);

create table app.user_profile (
    user_id                 uuid primary key references app.app_user (id) on delete cascade,
    display_name            text,
    birth_year              int check (birth_year between 1900 and 2100),
    country                 text,
    timezone                text,
    unit_system             text not null default 'METRIC' check (unit_system in ('METRIC', 'IMPERIAL')),
    onboarding_completed_at timestamptz,
    consent_terms_at        timestamptz,
    consent_ai_at           timestamptz,
    created_at              timestamptz not null default now(),
    updated_at              timestamptz not null default now()
);

-- =========================================================================
-- Survey definitions (versioned, immutable once published)
-- =========================================================================

create table app.survey (
    id          uuid primary key default gen_random_uuid(),
    code        text not null unique,
    theme       text not null check (theme in
                 ('ONBOARDING', 'EXERCISE', 'NUTRITION', 'HORMONES', 'PELVIC_FLOOR')),
    title       text not null,
    description text,
    sort_order  int not null default 0,
    is_active   boolean not null default true,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);

create table app.survey_version (
    id             uuid primary key default gen_random_uuid(),
    survey_id      uuid not null references app.survey (id) on delete cascade,
    version        int not null,
    status         text not null default 'DRAFT' check (status in ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    published_at   timestamptz,
    notes          text,
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now(),
    lock_version   bigint not null default 0,
    unique (survey_id, version)
);

-- at most one published version per survey
create unique index ux_survey_version_published
    on app.survey_version (survey_id) where status = 'PUBLISHED';

create table app.survey_section (
    id                uuid primary key default gen_random_uuid(),
    survey_version_id uuid not null references app.survey_version (id) on delete cascade,
    code              text not null,
    title             text not null,
    description       text,
    sort_order        int not null default 0,
    unique (survey_version_id, code)
);

create table app.question (
    id                uuid primary key default gen_random_uuid(),
    survey_version_id uuid not null references app.survey_version (id) on delete cascade,
    section_id        uuid references app.survey_section (id) on delete set null,
    code              text not null,
    type              text not null check (type in
                       ('SINGLE_CHOICE', 'MULTI_CHOICE', 'SCALE', 'NUMBER', 'TEXT', 'LONG_TEXT', 'DATE', 'BOOLEAN')),
    prompt            text not null,
    help_text         text,
    required          boolean not null default false,
    sort_order        int not null default 0,
    config            jsonb not null default '{}'::jsonb,
    display_rule      jsonb,
    unique (survey_version_id, code)
);

create table app.question_option (
    id            uuid primary key default gen_random_uuid(),
    question_id   uuid not null references app.question (id) on delete cascade,
    code          text not null,
    label         text not null,
    sort_order    int not null default 0,
    value_numeric numeric,
    metadata      jsonb not null default '{}'::jsonb,
    unique (question_id, code)
);

create index ix_survey_version_survey on app.survey_version (survey_id);
create index ix_section_version on app.survey_section (survey_version_id);
create index ix_question_version on app.question (survey_version_id);
create index ix_question_option_question on app.question_option (question_id);
