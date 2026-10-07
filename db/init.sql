CREATE TABLE IF NOT EXISTS claim_events (
    event_id UUID PRIMARY KEY,
    claim_id VARCHAR(16) NOT NULL,
    claim_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount_cents BIGINT NOT NULL CHECK (amount_cents > 0),
    event_time TIMESTAMPTZ NOT NULL,
    review_flag BOOLEAN NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS claim_events_claim_time_idx ON claim_events(claim_id, event_time DESC);
CREATE INDEX IF NOT EXISTS claim_events_processed_idx ON claim_events(processed_at DESC);

CREATE TABLE IF NOT EXISTS claim_event_rejects (
    topic VARCHAR(249) NOT NULL,
    kafka_partition INTEGER NOT NULL,
    kafka_offset BIGINT NOT NULL,
    raw_json TEXT,
    reason TEXT NOT NULL,
    rejected_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (topic, kafka_partition, kafka_offset)
);
