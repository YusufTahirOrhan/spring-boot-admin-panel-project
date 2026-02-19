CREATE TABLE IF NOT EXISTS sale_transactions (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    transaction_type_id UUID NOT NULL,
    customer_name VARCHAR(128) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    notes VARCHAR(500),
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ,
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT fk_sale_transactions_transaction_type FOREIGN KEY (transaction_type_id) REFERENCES transaction_types (id)
);

CREATE INDEX IF NOT EXISTS idx_sale_transactions_occurred_at ON sale_transactions (occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_sale_transactions_deleted ON sale_transactions (is_deleted);
