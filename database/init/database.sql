BEGIN;

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    type VARCHAR(6) NOT NULL CHECK (type IN ('credit', 'debit')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
);

COMMIT;

BEGIN;

ALTER TABLE transactions ADD COLUMN IF NOT EXISTS idempotency_key UUID;
UPDATE transactions SET idempotency_key = gen_random_uuid() WHERE idempotency_key IS NULL;
ALTER TABLE transactions ALTER COLUMN idempotency_key SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_transactions_idempotency_key
    ON transactions (idempotency_key);

ALTER TABLE transactions DROP CONSTRAINT IF EXISTS transactions_user_id_fkey;
ALTER TABLE transactions ADD CONSTRAINT transactions_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'transactions_amount_positive'
          AND conrelid = 'transactions'::regclass
    ) THEN
        ALTER TABLE transactions ADD CONSTRAINT transactions_amount_positive CHECK (amount > 0);
    END IF;
END $$;

COMMIT;