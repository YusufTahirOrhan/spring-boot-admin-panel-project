CREATE TABLE IF NOT EXISTS lens_prescriptions (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    transaction_type_id UUID NOT NULL,
    right_sphere VARCHAR(16),
    left_sphere VARCHAR(16),
    right_cylinder VARCHAR(16),
    left_cylinder VARCHAR(16),
    right_axis VARCHAR(16),
    left_axis VARCHAR(16),
    pd VARCHAR(16),
    notes VARCHAR(1000),
    recorded_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ,
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT fk_lens_prescriptions_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_lens_prescriptions_transaction_type FOREIGN KEY (transaction_type_id) REFERENCES transaction_types (id)
);

CREATE INDEX IF NOT EXISTS idx_lens_prescriptions_recorded_at ON lens_prescriptions (recorded_at DESC);
CREATE INDEX IF NOT EXISTS idx_lens_prescriptions_deleted ON lens_prescriptions (is_deleted);
