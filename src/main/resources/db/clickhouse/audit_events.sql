CREATE TABLE IF NOT EXISTS audit_events
(
    event_id UUID,
    timestamp DateTime64(3, 'UTC'),
    actor_user_id UUID,
    actor_role LowCardinality(String),
    action LowCardinality(String),
    resource_type LowCardinality(String),
    resource_id String,
    before_json String,
    after_json String,
    request_id String,
    ip_address String,
    user_agent String,
    store_id UUID
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(timestamp)
ORDER BY (timestamp, event_id);
