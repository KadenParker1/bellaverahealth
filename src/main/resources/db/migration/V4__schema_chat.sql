create table app.chat_thread (
    id              uuid primary key default gen_random_uuid(),
    user_id         uuid not null references app.app_user (id) on delete cascade,
    title           text,
    created_at      timestamptz not null default now(),
    last_message_at timestamptz,
    archived_at     timestamptz
);

create index ix_thread_user on app.chat_thread (user_id, last_message_at desc);

create table app.chat_context_snapshot (
    id             uuid primary key default gen_random_uuid(),
    user_id        uuid not null references app.app_user (id) on delete cascade,
    insight_run_id uuid references app.insight_run (id),
    content        text not null,
    content_hash   text not null,
    token_estimate int,
    built_at       timestamptz not null default now()
);

create index ix_snapshot_user on app.chat_context_snapshot (user_id, built_at desc);

create table app.chat_message (
    id                  uuid primary key default gen_random_uuid(),
    thread_id           uuid not null references app.chat_thread (id) on delete cascade,
    role                text not null check (role in ('USER', 'ASSISTANT', 'SYSTEM')),
    content             text not null,
    provider            text,
    model               text,
    input_tokens        int,
    output_tokens       int,
    latency_ms          int,
    finish_reason       text,
    context_snapshot_id uuid references app.chat_context_snapshot (id),
    created_at          timestamptz not null default now()
);

create index ix_message_thread on app.chat_message (thread_id, created_at);
