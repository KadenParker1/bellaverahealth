-- =========================================================================
-- Contact: a message from a signed-in user to the admin inbox.
-- =========================================================================

create table app.contact_message (
    id          uuid primary key default gen_random_uuid(),
    user_id     uuid not null references app.app_user (id),
    subject     text not null,
    message     text not null,
    created_at  timestamptz not null default now(),
    read_at     timestamptz
);

create index ix_contact_message_unread on app.contact_message (created_at) where read_at is null;
