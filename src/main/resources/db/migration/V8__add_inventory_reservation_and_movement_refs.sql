ALTER TABLE inventory_movements
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(40),
    ADD COLUMN IF NOT EXISTS source_id UUID,
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(120);

CREATE UNIQUE INDEX IF NOT EXISTS uk_inventory_movements_idempotency_key
    ON inventory_movements (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

ALTER TABLE repair_orders
    ADD COLUMN IF NOT EXISTS reserved_inventory_item_id UUID,
    ADD COLUMN IF NOT EXISTS reserved_inventory_quantity INTEGER,
    ADD COLUMN IF NOT EXISTS inventory_released BOOLEAN NOT NULL DEFAULT FALSE;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_repair_orders_reserved_inventory_item'
    ) THEN
        ALTER TABLE repair_orders
            ADD CONSTRAINT fk_repair_orders_reserved_inventory_item
            FOREIGN KEY (reserved_inventory_item_id) REFERENCES inventory_items (id);
    END IF;
END $$;
