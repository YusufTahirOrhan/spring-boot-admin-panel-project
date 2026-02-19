CREATE TABLE IF NOT EXISTS inventory_items (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    sku VARCHAR(80) NOT NULL,
    name VARCHAR(160) NOT NULL,
    category VARCHAR(80),
    quantity INTEGER NOT NULL DEFAULT 0,
    min_quantity INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ,
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT uk_inventory_items_sku UNIQUE (sku)
);

CREATE TABLE IF NOT EXISTS inventory_movements (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    inventory_item_id UUID NOT NULL,
    movement_type VARCHAR(20) NOT NULL,
    quantity_delta INTEGER NOT NULL,
    reason VARCHAR(500),
    moved_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ,
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT fk_inventory_movements_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items (id)
);

CREATE INDEX IF NOT EXISTS idx_inventory_items_name ON inventory_items (name);
CREATE INDEX IF NOT EXISTS idx_inventory_movements_moved_at ON inventory_movements (moved_at DESC);
