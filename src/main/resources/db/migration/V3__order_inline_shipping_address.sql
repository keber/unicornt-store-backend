-- V3: an order snapshots its shipping address inline.
--
-- POST /api/v1/orders carries { street, city, region, zipCode } in the request body;
-- there is no stored-address subsystem any more (PLAN section 1: "Shipping - state
-- only, no subsystem"). The order keeps the address as a snapshot so a later change
-- never rewrites a past order.
--
-- Wrapped in a DO block so it is safe to re-run and safe whatever state the table
-- is in (Spring SQL init applies it on every start-up until Flyway takes over).

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
              WHERE table_name = 'orders' AND column_name = 'address_id') THEN
        ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_address_id_fkey;
        ALTER TABLE orders ALTER COLUMN address_id DROP NOT NULL;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns
              WHERE table_name = 'orders' AND column_name = 'shipping_address') THEN
        ALTER TABLE orders ALTER COLUMN shipping_address DROP NOT NULL;
    END IF;
END
$$;

ALTER TABLE orders ADD COLUMN IF NOT EXISTS ship_street VARCHAR(200);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ship_city   VARCHAR(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ship_region VARCHAR(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ship_zip    VARCHAR(20);
