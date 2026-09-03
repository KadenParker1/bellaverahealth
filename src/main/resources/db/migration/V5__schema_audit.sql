create table app.audit_log (
    id            uuid primary key default gen_random_uuid(),
    actor_user_id uuid references app.app_user (id),
    action        text not null,
    entity_type   text not null,
    entity_id     uuid,
    before_state  jsonb,
    after_state   jsonb,
    created_at    timestamptz not null default now()
);

create index ix_audit_entity on app.audit_log (entity_type, entity_id);
create index ix_audit_actor on app.audit_log (actor_user_id, created_at desc);
