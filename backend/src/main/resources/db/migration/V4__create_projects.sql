CREATE TABLE projects (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT projects_status_check CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT projects_id_org_unique UNIQUE (id, organization_id),
    CONSTRAINT projects_org_name_unique UNIQUE (organization_id, name)
);

CREATE INDEX projects_org_created_idx ON projects (organization_id, created_at DESC);
