create table app.insight_run (
    id                uuid primary key default gen_random_uuid(),
    user_id           uuid not null references app.app_user (id) on delete cascade,
    triggered_by      text not null check (triggered_by in ('SURVEY_SUBMIT', 'MANUAL', 'RECOMPUTE')),
    engine_version    text not null,
    input_fingerprint text not null,
    status            text not null default 'COMPLETED' check (status in ('RUNNING', 'COMPLETED', 'FAILED')),
    error             text,
    created_at        timestamptz not null default now()
);

create index ix_insight_run_user on app.insight_run (user_id, created_at desc);

create table app.insight (
    id             uuid primary key default gen_random_uuid(),
    insight_run_id uuid not null references app.insight_run (id) on delete cascade,
    user_id        uuid not null references app.app_user (id) on delete cascade,
    domain         text not null check (domain in
                    ('NUTRITION', 'HORMONES', 'EXERCISE', 'PELVIC_FLOOR', 'SLEEP', 'STRESS', 'GENERAL')),
    code           text not null,
    label          text not null,
    score          numeric(6, 2),
    band           text not null check (band in ('LOW', 'MODERATE', 'HIGH', 'UNKNOWN')),
    confidence     numeric(4, 3),
    rationale      text,
    evidence       jsonb not null default '{}'::jsonb,
    created_at     timestamptz not null default now()
);

create index ix_insight_user_domain on app.insight (user_id, domain);
create index ix_insight_run on app.insight (insight_run_id);

create table app.insight_rule (
    code        text primary key,
    domain      text not null,
    version     int not null default 1,
    label       text not null,
    description text,
    definition  jsonb not null default '{}'::jsonb,
    is_active   boolean not null default true,
    updated_at  timestamptz not null default now()
);
