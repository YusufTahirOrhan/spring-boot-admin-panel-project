ALTER TABLE sale_transactions
    ADD COLUMN IF NOT EXISTS invoice_number VARCHAR(32);

ALTER TABLE sale_transactions
    ADD COLUMN IF NOT EXISTS invoice_issued_at TIMESTAMPTZ;
