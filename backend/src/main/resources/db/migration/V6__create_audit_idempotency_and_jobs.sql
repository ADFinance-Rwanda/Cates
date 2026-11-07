CREATE TABLE task_idempotency_keys (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    user_id UUID NOT NULL REFERENCES users (id),
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    task_id UUID NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT task_idempotency_unique UNIQUE (organization_id, user_id, idempotency_key)
);

CREATE INDEX task_idempotency_created_idx ON task_idempotency_keys (created_at);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    organization_id UUID REFERENCES organizations (id),
    user_id UUID REFERENCES users (id),
    action VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id UUID,
    ip_address VARCHAR(64),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX audit_logs_org_created_idx ON audit_logs (organization_id, created_at DESC);

CREATE OR REPLACE FUNCTION taskflow_reject_audit_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'audit_logs is append-only';
END;
$$;

CREATE TRIGGER audit_logs_append_only
    BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW
    EXECUTE FUNCTION taskflow_reject_audit_mutation();

CREATE TABLE job_outbox (
    id UUID PRIMARY KEY,
    job_type VARCHAR(64) NOT NULL,
    organization_id UUID,
    entity_id UUID,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(16) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    available_at TIMESTAMPTZ NOT NULL,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT job_outbox_status_check CHECK (status IN ('PENDING', 'DONE', 'DEAD'))
);

CREATE INDEX job_outbox_pending_idx ON job_outbox (status, available_at);

CREATE TABLE notification_deliveries (
    outbox_id UUID PRIMARY KEY REFERENCES job_outbox (id),
    channel VARCHAR(32) NOT NULL,
    delivered_at TIMESTAMPTZ NOT NULL
);
