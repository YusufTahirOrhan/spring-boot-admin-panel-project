ALTER TABLE sale_transactions
    ADD COLUMN IF NOT EXISTS customer_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_sale_transactions_customer'
          AND table_name = 'sale_transactions'
    ) THEN
        ALTER TABLE sale_transactions
            ADD CONSTRAINT fk_sale_transactions_customer
            FOREIGN KEY (customer_id) REFERENCES customers (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_sale_transactions_customer_id ON sale_transactions (customer_id);
