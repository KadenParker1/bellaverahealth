-- =========================================================================
-- Store: catalog, orders, fulfillment
--
-- Fulfillment is columns on customer_order, not a shipment table: one shipment
-- per order. Adding partial fulfillment later means a `shipment` table backfilled
-- one row per fulfilled order from these columns, so read fulfillment state through
-- customer_order.status rather than testing tracking_number for null at call sites.
-- =========================================================================

create table app.product (
    id              uuid primary key default gen_random_uuid(),
    code            text not null unique,
    name            text not null,
    description     text,
    image_url       text,
    price_cents     int not null check (price_cents >= 0),
    currency        text not null default 'usd',
    stripe_price_id text,
    is_active       boolean not null default true,
    sort_order      int not null default 0,
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now()
);

create index ix_product_active on app.product (is_active, sort_order);

-- `order` is a reserved word in Postgres - never name a table that.
create table app.customer_order (
    id                         uuid primary key default gen_random_uuid(),
    user_id                    uuid not null references app.app_user (id),
    status                     text not null default 'PENDING'
                                   check (status in ('PENDING', 'PAID', 'FULFILLED', 'CANCELLED')),
    currency                   text not null default 'usd',
    subtotal_cents             int not null check (subtotal_cents >= 0),
    email                      text,
    stripe_checkout_session_id text unique,
    stripe_payment_intent_id   text,
    ship_to_name               text,
    ship_to_line1              text,
    ship_to_line2              text,
    ship_to_city               text,
    ship_to_region             text,
    ship_to_postal_code        text,
    ship_to_country            text,
    placed_at                  timestamptz not null default now(),
    paid_at                    timestamptz,
    fulfilled_at               timestamptz,
    carrier                    text,
    tracking_number            text,
    fulfilled_by               uuid references app.app_user (id),
    created_at                 timestamptz not null default now(),
    updated_at                 timestamptz not null default now(),
    lock_version               bigint not null default 0,
    -- the DB refuses a fulfilled order that never recorded when it was fulfilled
    constraint ck_order_fulfilled_at check (status <> 'FULFILLED' or fulfilled_at is not null),
    constraint ck_order_paid_at check (status not in ('PAID', 'FULFILLED') or paid_at is not null)
);

create index ix_order_user on app.customer_order (user_id, placed_at desc);
-- the fulfillment queue: paid orders not yet shipped, oldest first
create index ix_order_awaiting_fulfillment on app.customer_order (placed_at) where status = 'PAID';

-- Line items snapshot code, name, and unit price at purchase time: products are
-- deactivated rather than deleted, but their copy and pricing still move.
create table app.order_item (
    id               uuid primary key default gen_random_uuid(),
    order_id         uuid not null references app.customer_order (id) on delete cascade,
    product_id       uuid references app.product (id),
    product_code     text not null,
    product_name     text not null,
    unit_price_cents int not null check (unit_price_cents >= 0),
    quantity         int not null check (quantity > 0),
    line_total_cents int not null check (line_total_cents >= 0),
    unique (order_id, product_code)
);

create index ix_order_item_order on app.order_item (order_id);

-- Webhook idempotency: Stripe retries, and a retry must not re-apply a payment.
create table app.payment_event (
    id          text primary key,
    type        text not null,
    order_id    uuid references app.customer_order (id) on delete set null,
    received_at timestamptz not null default now()
);
