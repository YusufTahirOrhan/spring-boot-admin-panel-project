CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('OWNER', 'ADMIN', 'STAFF'))
);

CREATE INDEX IF NOT EXISTS idx_users_deleted ON users (is_deleted);
CREATE INDEX IF NOT EXISTS idx_users_role_active ON users (role, active);
