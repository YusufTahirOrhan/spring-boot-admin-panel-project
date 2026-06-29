CREATE TABLE IF NOT EXISTS sale_transaction_items (
    id UUID PRIMARY KEY,
    sale_transaction_id UUID NOT NULL,
    inventory_item_id UUID NOT NULL,
    name VARCHAR(160) NOT NULL,
    sku VARCHAR(80),
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL,
    line_total NUMERIC(12,2) NOT NULL,
    CONSTRAINT fk_sale_transaction_items_sale_transaction FOREIGN KEY (sale_transaction_id) REFERENCES sale_transactions (id)
);

CREATE INDEX IF NOT EXISTS idx_sale_transaction_items_sale_transaction ON sale_transaction_items (sale_transaction_id);
CREATE INDEX IF NOT EXISTS idx_sale_transaction_items_inventory_item ON sale_transaction_items (inventory_item_id);
