ALTER TABLE sale_transactions
    ADD COLUMN IF NOT EXISTS payment_method VARCHAR(20);

ALTER TABLE sale_transactions
    ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(128);

UPDATE sale_transactions
SET payment_method = 'CASH'
WHERE payment_method IS NULL;

ALTER TABLE sale_transactions
    ALTER COLUMN payment_method SET NOT NULL;
