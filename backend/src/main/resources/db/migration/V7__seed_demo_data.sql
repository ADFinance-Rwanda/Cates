-- Local demo tenants. Keycloak user ids are the same UUIDs as keycloak_sub.
-- Passwords live only in the Keycloak realm import, not in this database.

INSERT INTO organizations (id, name, status, created_at, updated_at) VALUES
    ('c2f0c8b6-ab19-475e-ae57-09a4c2c329b6', 'Meridian', 'ACTIVE', now(), now()),
    ('3d5c8b81-9c1e-4751-b52c-d9d8d791d28e', 'Northline', 'ACTIVE', now(), now());

INSERT INTO users (id, keycloak_sub, email, display_name, created_at, updated_at) VALUES
    ('4809e900-e596-47bb-9f2d-9225536bd8a6', '4809e900-e596-47bb-9f2d-9225536bd8a6', 'superadmin@taskflow.test', 'Sam Admin', now(), now()),
    ('21b9a0ed-3d21-46ff-8bc4-83225f36dfa4', '21b9a0ed-3d21-46ff-8bc4-83225f36dfa4', 'alice@meridian.test', 'Alice Moreau', now(), now()),
    ('d6773c12-5e75-4741-8913-36bb8a0e46cc', 'd6773c12-5e75-4741-8913-36bb8a0e46cc', 'bob@meridian.test', 'Bob Keller', now(), now()),
    ('30fc848c-06c5-4ef3-95e9-44bb31ed6b3c', '30fc848c-06c5-4ef3-95e9-44bb31ed6b3c', 'cara@meridian.test', 'Cara Nunez', now(), now()),
    ('d79a9fb0-750b-47d5-9c19-95a92a7eb9da', 'd79a9fb0-750b-47d5-9c19-95a92a7eb9da', 'dave@northline.test', 'Dave Okonkwo', now(), now());

INSERT INTO organization_members (id, user_id, organization_id, role, created_at, updated_at) VALUES
    ('66c96ca2-9e9a-4ec4-b88d-ad1e78964330', '21b9a0ed-3d21-46ff-8bc4-83225f36dfa4', 'c2f0c8b6-ab19-475e-ae57-09a4c2c329b6', 'ORG_ADMIN', now(), now()),
    ('e1005553-f46f-4d4b-a8d3-1b76f0a88510', 'd6773c12-5e75-4741-8913-36bb8a0e46cc', 'c2f0c8b6-ab19-475e-ae57-09a4c2c329b6', 'PROJECT_MANAGER', now(), now()),
    ('e8d7ccd4-f1ba-42af-8113-e4a2e4dff8c1', '30fc848c-06c5-4ef3-95e9-44bb31ed6b3c', 'c2f0c8b6-ab19-475e-ae57-09a4c2c329b6', 'MEMBER', now(), now()),
    ('cf25ffea-40ae-48ff-97d1-eae387c1b219', 'd79a9fb0-750b-47d5-9c19-95a92a7eb9da', '3d5c8b81-9c1e-4751-b52c-d9d8d791d28e', 'ORG_ADMIN', now(), now());

INSERT INTO projects (id, organization_id, name, description, status, created_at, updated_at) VALUES
    ('31b5edc0-ba2b-45d8-88a0-7938f4d3ddfa', 'c2f0c8b6-ab19-475e-ae57-09a4c2c329b6', 'Website refresh', 'Public site rewrite', 'ACTIVE', now() - interval '3 days', now()),
    ('8d35bc9f-ff06-4d82-9f70-b9846af084bc', '3d5c8b81-9c1e-4751-b52c-d9d8d791d28e', 'Billing migration', 'Move invoices off the legacy ledger', 'ACTIVE', now() - interval '1 day', now());

INSERT INTO tasks (
    id, organization_id, project_id, title, description, status, priority,
    owner_id, assignee_id, due_date, version, created_at, updated_at, completed_at, due_reminder_sent_at
) VALUES
    (
        'd12c4aa9-8412-4d02-9ebd-7ae53edb979d',
        'c2f0c8b6-ab19-475e-ae57-09a4c2c329b6',
        '31b5edc0-ba2b-45d8-88a0-7938f4d3ddfa',
        'Draft homepage copy',
        'First pass of the marketing homepage',
        'TODO',
        'MEDIUM',
        '21b9a0ed-3d21-46ff-8bc4-83225f36dfa4',
        '30fc848c-06c5-4ef3-95e9-44bb31ed6b3c',
        CURRENT_DATE + 1,
        0,
        now() - interval '2 days',
        now() - interval '2 days',
        NULL,
        NULL
    ),
    (
        '15f216fd-5390-47df-aab3-11aa43776479',
        'c2f0c8b6-ab19-475e-ae57-09a4c2c329b6',
        '31b5edc0-ba2b-45d8-88a0-7938f4d3ddfa',
        'Fix navigation',
        'Overdue layout bug',
        'IN_PROGRESS',
        'HIGH',
        'd6773c12-5e75-4741-8913-36bb8a0e46cc',
        'd6773c12-5e75-4741-8913-36bb8a0e46cc',
        CURRENT_DATE - 1,
        0,
        now() - interval '1 day',
        now() - interval '1 day',
        NULL,
        NULL
    ),
    (
        'a298800b-9c0d-4a21-a812-9b0b7ed63526',
        'c2f0c8b6-ab19-475e-ae57-09a4c2c329b6',
        '31b5edc0-ba2b-45d8-88a0-7938f4d3ddfa',
        'Publish launch checklist',
        'Completed checklist',
        'DONE',
        'LOW',
        '21b9a0ed-3d21-46ff-8bc4-83225f36dfa4',
        '30fc848c-06c5-4ef3-95e9-44bb31ed6b3c',
        CURRENT_DATE - 2,
        0,
        now() - interval '4 days',
        now() - interval '1 day',
        now() - interval '1 day',
        NULL
    ),
    (
        '02d6b8d2-d8f7-4ac4-9864-40b1f7fd4a8d',
        '3d5c8b81-9c1e-4751-b52c-d9d8d791d28e',
        '8d35bc9f-ff06-4d82-9f70-b9846af084bc',
        'Export legacy invoices',
        'Northline-only task used to prove tenant isolation',
        'TODO',
        'CRITICAL',
        'd79a9fb0-750b-47d5-9c19-95a92a7eb9da',
        'd79a9fb0-750b-47d5-9c19-95a92a7eb9da',
        CURRENT_DATE + 3,
        0,
        now(),
        now(),
        NULL,
        NULL
    );
