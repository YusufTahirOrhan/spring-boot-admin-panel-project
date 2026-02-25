ALTER TABLE sale_transactions
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED';

ALTER TABLE sale_transactions
    ADD COLUMN IF NOT EXISTS inventory_item_id UUID;

ALTER TABLE sale_transactions
    ADD COLUMN IF NOT EXISTS inventory_quantity INTEGER;

ALTER TABLE sale_transactions
    ADD COLUMN IF NOT EXISTS stock_reverted BOOLEAN NOT NULL DEFAULT FALSE;
