-- =========================================================================
-- Inventory
--
-- Nullable on purpose: null means "not tracked" (an unlimited or made-to-order
-- item), which is also what every product created before this migration becomes.
-- A NOT NULL default of 0 would have marked the existing catalog out of stock.
-- =========================================================================

alter table app.product
    add column stock_quantity int
        constraint ck_product_stock_non_negative check (stock_quantity is null or stock_quantity >= 0);

comment on column app.product.stock_quantity is
    'Units on hand, or null when this product is not stock-tracked. Decremented at fulfillment.';
