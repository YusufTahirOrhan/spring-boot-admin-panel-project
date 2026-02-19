CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    email VARCHAR(150),
    notes VARCHAR(1000),
    created_at TIMESTAMPTZ,
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID
);

CREATE INDEX IF NOT EXISTS idx_customers_name ON customers (first_name, last_name);
CREATE INDEX IF NOT EXISTS idx_customers_deleted ON customers (is_deleted);
