ALTER TABLE sale_transactions
    ADD COLUMN IF NOT EXISTS receipt_number VARCHAR(32);

CREATE UNIQUE INDEX IF NOT EXISTS ux_sale_transactions_store_receipt
    ON sale_transactions (store_id, receipt_number)
    WHERE receipt_number IS NOT NULL;
