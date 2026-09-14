CREATE TABLE cards (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers(id),
    pin_hash VARCHAR(100) NOT NULL,
    consecutive_failures INTEGER NOT NULL DEFAULT 0,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_cards_customer_id ON cards(customer_id);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    chain_id UUID NOT NULL,
    channel VARCHAR(10) NOT NULL,
    owner_id UUID NOT NULL,
    device_id VARCHAR(100),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    rotated BOOLEAN NOT NULL DEFAULT FALSE,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    expiry TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_refresh_tokens_chain_id ON refresh_tokens(chain_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);

CREATE TABLE device_registrations (
    device_id VARCHAR(100) PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers(id),
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0
);

-- Demo card for the seeded demo customer (V6), PIN 1234, dev/test use only.
-- Hash generated with the same BCrypt scheme PinHasher uses.
INSERT INTO cards (id, customer_id, pin_hash, consecutive_failures, locked, version)
VALUES (
    '77777777-7777-7777-7777-777777777777',
    '11111111-1111-1111-1111-111111111111',
    '$2a$10$A/bKFCqCd30HDXyM/sTaWu68pjNCT1Y4dIhiBS8woLh8snbQ4SHia',
    0,
    FALSE,
    0
);
