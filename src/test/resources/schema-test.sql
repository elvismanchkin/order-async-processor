CREATE TABLE IF NOT EXISTS customers (
                                         id UUID PRIMARY KEY,
                                         external_id VARCHAR(50),
    tax_id VARCHAR(30) NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    segment VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    account_manager VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    metadata JSONB,
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
    );

CREATE TABLE IF NOT EXISTS orders (
                                      id UUID PRIMARY KEY,
                                      reference_number VARCHAR(50) NOT NULL,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    customer_id UUID NOT NULL REFERENCES customers(id),
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP,
    priority INTEGER NOT NULL DEFAULT 0,
    due_date TIMESTAMP,
    description TEXT,
    metadata JSONB,
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
    );

CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders(customer_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at) WHERE deleted = FALSE;