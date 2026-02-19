CREATE TABLE IF NOT EXISTS transaction_types (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ,
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT uk_transaction_types_code UNIQUE (code)
);

CREATE INDEX IF NOT EXISTS idx_transaction_types_active_deleted ON transaction_types (active, is_deleted);
CREATE INDEX IF NOT EXISTS idx_transaction_types_sort_order ON transaction_types (sort_order);
