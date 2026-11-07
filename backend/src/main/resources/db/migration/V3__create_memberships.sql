-- One organization per ordinary user. SUPER_ADMIN has no membership.
CREATE TABLE organization_members (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users (id),
    organization_id UUID NOT NULL REFERENCES organizations (id),
    role VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT organization_members_role_check CHECK (role IN ('ORG_ADMIN', 'PROJECT_MANAGER', 'MEMBER')),
    CONSTRAINT organization_members_user_org_unique UNIQUE (user_id, organization_id)
);

CREATE INDEX organization_members_org_idx ON organization_members (organization_id);
