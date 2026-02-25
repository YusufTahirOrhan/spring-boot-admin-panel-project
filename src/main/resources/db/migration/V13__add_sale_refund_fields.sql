ALTER TABLE sale_transactions
    ADD COLUMN IF NOT EXISTS refunded_amount NUMERIC(12,2);

ALTER TABLE sale_transactions
    ADD COLUMN IF NOT EXISTS refunded_at TIMESTAMPTZ;
