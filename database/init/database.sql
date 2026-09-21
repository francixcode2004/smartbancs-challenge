BEGIN;

-- 1. Usuarios
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_name ON users(name);

-- 2. Secuencia para números de cuenta de 8 dígitos (10000001 a 79999999)
CREATE SEQUENCE IF NOT EXISTS account_number_seq
    START WITH 10000001 MINVALUE 10000001 MAXVALUE 79999999 NO CYCLE;

-- 3. Cuentas bancarias (Máximo 8 dígitos)
CREATE TABLE IF NOT EXISTS accounts (
    account_number VARCHAR(8) PRIMARY KEY DEFAULT nextval('account_number_seq')::text CHECK (account_number ~ '^[0-9]{8}$'),
    user_id UUID REFERENCES users(id) ON DELETE RESTRICT,
    balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00 CHECK (balance >= 0)
);

CREATE INDEX IF NOT EXISTS idx_accounts_user_id ON accounts(user_id);

-- 4. Servicios básicos (Cuentas de recaudación de 8 dígitos)
CREATE TABLE IF NOT EXISTS basic_services (
    code VARCHAR(30) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    account_number VARCHAR(8) NOT NULL UNIQUE REFERENCES accounts(account_number) ON DELETE RESTRICT
);

-- Datos semilla para recaudación (cuentas de 8 dígitos iniciadas en 900...)
INSERT INTO accounts (account_number, user_id, balance) VALUES
    ('90000001', NULL, 0.00),
    ('90000002', NULL, 0.00),
    ('90000003', NULL, 0.00)
ON CONFLICT (account_number) DO NOTHING;

INSERT INTO basic_services (code, name, account_number) VALUES
    ('AGUA', 'Agua - Quito', '90000001'),
    ('LUZ', 'Electricidad - Quito', '90000002'),
    ('INTERNET', 'Internet - Quito', '90000003')
ON CONFLICT (code) DO NOTHING;

-- 5. Transacciones
CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    source_account_number VARCHAR(8) REFERENCES accounts(account_number) ON DELETE RESTRICT,
    destination_account_number VARCHAR(8) REFERENCES accounts(account_number) ON DELETE RESTRICT,
    amount NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    type VARCHAR(16) NOT NULL CHECK (type IN ('deposit', 'withdraw', 'transfer', 'service_payment')),
    description VARCHAR(255),
    service_code VARCHAR(30) REFERENCES basic_services(code) ON DELETE RESTRICT,
    customer_reference VARCHAR(100),
    idempotency_key UUID NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Reglas de integridad según el tipo de operación
    CONSTRAINT chk_transaction_payload CHECK (
        -- Depósito: Sin origen, debe tener destino
        (type = 'deposit' AND source_account_number IS NULL AND destination_account_number IS NOT NULL AND service_code IS NULL AND customer_reference IS NULL)
        OR
        -- Retiro: Debe tener origen, sin destino
        (type = 'withdraw' AND source_account_number IS NOT NULL AND destination_account_number IS NULL AND service_code IS NULL AND customer_reference IS NULL)
        OR
        -- Transferencia: Debe tener origen y destino distintos
        (type = 'transfer' AND source_account_number IS NOT NULL AND destination_account_number IS NOT NULL AND source_account_number <> destination_account_number AND service_code IS NULL AND customer_reference IS NULL)
        OR
        -- Pago de servicios: Debe tener origen, destino, código de servicio y referencia de cliente
        (type = 'service_payment' AND source_account_number IS NOT NULL AND destination_account_number IS NOT NULL AND source_account_number <> destination_account_number AND service_code IS NOT NULL AND customer_reference IS NOT NULL AND length(trim(customer_reference)) > 0)
    )
);

-- Índices de consulta frecuente
CREATE INDEX IF NOT EXISTS idx_transactions_user_created ON transactions(user_id, created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_transactions_source ON transactions(source_account_number, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_transactions_destination ON transactions(destination_account_number, created_at DESC);

INSERT INTO users (id, name, email, password) VALUES
    ('11111111-1111-4111-8111-111111111111', 'Pepito Demo', 'pepito@smartbancs.com', 'DEMO_LOGIN_DISABLED'),
    ('22222222-2222-4222-8222-222222222222', 'Maria Demo', 'maria@smartbancs.com', 'DEMO_LOGIN_DISABLED');

CREATE TABLE IF NOT EXISTS bancs_outbox (
    event_id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL UNIQUE REFERENCES transactions(id) ON DELETE RESTRICT,
    idempotency_key UUID NOT NULL UNIQUE,
    source_account_number VARCHAR(8) REFERENCES accounts(account_number) ON DELETE RESTRICT,
    destination_account_number VARCHAR(8) REFERENCES accounts(account_number) ON DELETE RESTRICT,
    amount NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    type VARCHAR(16) NOT NULL,
    description VARCHAR(255),
    service_code VARCHAR(30),
    customer_reference VARCHAR(100),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED')),
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    available_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ,
    last_error VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_bancs_outbox_pending
    ON bancs_outbox (status, available_at, created_at);
CREATE INDEX IF NOT EXISTS idx_bancs_outbox_processed
    ON bancs_outbox (status, processed_at DESC);

CREATE TABLE IF NOT EXISTS recommendations (
    id UUID PRIMARY KEY,
    account_number VARCHAR(8) NOT NULL REFERENCES accounts(account_number) ON DELETE RESTRICT,
    title VARCHAR(150) NOT NULL,
    message VARCHAR(600) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    category VARCHAR(30) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(50) NOT NULL,
    source VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_recommendations_account_created
    ON recommendations (account_number, created_at DESC);
COMMIT;