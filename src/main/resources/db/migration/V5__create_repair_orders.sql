CREATE TABLE IF NOT EXISTS repair_orders (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    transaction_type_id UUID NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(40) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ,
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT fk_repair_orders_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_repair_orders_transaction_type FOREIGN KEY (transaction_type_id) REFERENCES transaction_types (id)
);

CREATE INDEX IF NOT EXISTS idx_repair_orders_status ON repair_orders (status);
CREATE INDEX IF NOT EXISTS idx_repair_orders_received_at ON repair_orders (received_at DESC);
