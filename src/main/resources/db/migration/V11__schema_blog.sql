-- =========================================================================
-- Blog: admin-authored posts, read by any signed-in user.
-- =========================================================================

create table app.blog_post (
    id              uuid primary key default gen_random_uuid(),
    slug            text not null unique,
    title           text not null,
    excerpt         text,
    body            text not null,
    is_published    boolean not null default false,
    published_at    timestamptz,
    author_user_id  uuid references app.app_user (id) on delete set null,
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now(),
    constraint chk_blog_post_published_at check (not is_published or published_at is not null)
);

create index ix_blog_post_published on app.blog_post (published_at desc) where is_published;
