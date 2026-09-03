-- V3: an order snapshots its shipping address inline.
--
-- POST /api/v1/orders carries { street, city, region, zipCode } in the request body;
-- there is no stored-address subsystem any more (PLAN section 1: "Shipping - state
-- only, no subsystem"). The order keeps the address as a snapshot so a later change
-- never rewrites a past order.
--
-- Runs after V1 (which creates orders.address_id NOT NULL + FK and orders.shipping_address
-- NOT NULL). Every statement is guarded / idempotent so Spring SQL init can re-apply it
-- on each start-up until Flyway takes over.

ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_address_id_fkey;
ALTER TABLE orders ALTER COLUMN address_id DROP NOT NULL;
ALTER TABLE orders ALTER COLUMN shipping_address DROP NOT NULL;

ALTER TABLE orders ADD COLUMN IF NOT EXISTS ship_street VARCHAR(200);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ship_city   VARCHAR(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ship_region VARCHAR(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ship_zip    VARCHAR(20);
