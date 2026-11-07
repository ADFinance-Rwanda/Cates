CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    project_id UUID NOT NULL,
    title VARCHAR(300) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL,
    priority VARCHAR(32) NOT NULL,
    owner_id UUID NOT NULL REFERENCES users (id),
    assignee_id UUID REFERENCES users (id),
    due_date DATE,
    version BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    due_reminder_sent_at TIMESTAMPTZ,
    CONSTRAINT tasks_status_check CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT tasks_priority_check CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT tasks_project_org_fk FOREIGN KEY (project_id, organization_id)
        REFERENCES projects (id, organization_id)
);

-- Leading organization_id keeps these indexes tenant-scoped.
CREATE INDEX tasks_org_created_idx ON tasks (organization_id, created_at DESC);
CREATE INDEX tasks_org_project_idx ON tasks (organization_id, project_id);
CREATE INDEX tasks_org_status_idx ON tasks (organization_id, status);
CREATE INDEX tasks_org_assignee_idx ON tasks (organization_id, assignee_id);
CREATE INDEX tasks_org_due_idx ON tasks (organization_id, due_date) WHERE due_date IS NOT NULL;
