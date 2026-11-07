-- Local demo tenants. Keycloak user ids are the same UUIDs as keycloak_sub.
-- Passwords live only in the Keycloak realm import, not in this database.

INSERT INTO organizations (id, name, status, created_at, updated_at) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Acme', 'ACTIVE', now(), now()),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Globex', 'ACTIVE', now(), now());

INSERT INTO users (id, keycloak_sub, email, display_name, created_at, updated_at) VALUES
    ('11111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'superadmin@taskflow.test', 'Sam Admin', now(), now()),
    ('22222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 'alice@acme.test', 'Alice Acme', now(), now()),
    ('33333333-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333', 'bob@acme.test', 'Bob Acme', now(), now()),
    ('44444444-4444-4444-4444-444444444444', '44444444-4444-4444-4444-444444444444', 'cara@acme.test', 'Cara Acme', now(), now()),
    ('55555555-5555-5555-5555-555555555555', '55555555-5555-5555-5555-555555555555', 'dave@globex.test', 'Dave Globex', now(), now());

INSERT INTO organization_members (id, user_id, organization_id, role, created_at, updated_at) VALUES
    ('22222222-2222-4222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'ORG_ADMIN', now(), now()),
    ('33333333-3333-4333-3333-333333333333', '33333333-3333-3333-3333-333333333333', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'PROJECT_MANAGER', now(), now()),
    ('44444444-4444-4444-4444-444444444444', '44444444-4444-4444-4444-444444444444', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'MEMBER', now(), now()),
    ('55555555-5555-4555-5555-555555555555', '55555555-5555-5555-5555-555555555555', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'ORG_ADMIN', now(), now());

INSERT INTO projects (id, organization_id, name, description, status, created_at, updated_at) VALUES
    ('66666666-6666-6666-6666-666666666666', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Website refresh', 'Public site rewrite', 'ACTIVE', now() - interval '3 days', now()),
    ('77777777-7777-7777-7777-777777777777', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Billing migration', 'Move invoices off the legacy ledger', 'ACTIVE', now() - interval '1 day', now());

INSERT INTO tasks (
    id, organization_id, project_id, title, description, status, priority,
    owner_id, assignee_id, due_date, version, created_at, updated_at, completed_at, due_reminder_sent_at
) VALUES
    (
        '88888888-8888-8888-8888-888888888888',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        '66666666-6666-6666-6666-666666666666',
        'Draft homepage copy',
        'First pass of the marketing homepage',
        'TODO',
        'MEDIUM',
        '22222222-2222-2222-2222-222222222222',
        '44444444-4444-4444-4444-444444444444',
        CURRENT_DATE + 1,
        0,
        now() - interval '2 days',
        now() - interval '2 days',
        NULL,
        NULL
    ),
    (
        '88888888-8888-8888-8888-888888888882',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        '66666666-6666-6666-6666-666666666666',
        'Fix navigation',
        'Overdue layout bug',
        'IN_PROGRESS',
        'HIGH',
        '33333333-3333-3333-3333-333333333333',
        '33333333-3333-3333-3333-333333333333',
        CURRENT_DATE - 1,
        0,
        now() - interval '1 day',
        now() - interval '1 day',
        NULL,
        NULL
    ),
    (
        '88888888-8888-8888-8888-888888888883',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        '66666666-6666-6666-6666-666666666666',
        'Publish launch checklist',
        'Completed checklist',
        'DONE',
        'LOW',
        '22222222-2222-2222-2222-222222222222',
        '44444444-4444-4444-4444-444444444444',
        CURRENT_DATE - 2,
        0,
        now() - interval '4 days',
        now() - interval '1 day',
        now() - interval '1 day',
        NULL
    ),
    (
        '99999999-9999-9999-9999-999999999999',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
        '77777777-7777-7777-7777-777777777777',
        'Export legacy invoices',
        'Globex-only task used to prove tenant isolation',
        'TODO',
        'CRITICAL',
        '55555555-5555-5555-5555-555555555555',
        '55555555-5555-5555-5555-555555555555',
        CURRENT_DATE + 3,
        0,
        now(),
        now(),
        NULL,
        NULL
    );
