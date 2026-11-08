import { Me, canDeleteTasks, canManageMembers, canManageOrganizations, canManageProjects, canReadAudit } from './access';

const member = (role: string, superAdmin = false): Me => ({
  id: '21b9a0ed-3d21-46ff-8bc4-83225f36dfa4',
  email: 'alice@meridian.test',
  displayName: 'Alice Moreau',
  superAdmin,
  organizationId: 'c2f0c8b6-ab19-475e-ae57-09a4c2c329b6',
  organizationName: 'Meridian',
  organizationStatus: 'ACTIVE',
  role
});

describe('access helpers', () => {
  it('lets members create tasks but not delete them', () => {
    expect(canDeleteTasks(member('MEMBER'))).toBe(false);
    expect(canManageProjects(member('MEMBER'))).toBe(false);
    expect(canDeleteTasks(member('PROJECT_MANAGER'))).toBe(true);
    expect(canManageMembers(member('ORG_ADMIN'))).toBe(true);
    expect(canManageMembers(member('SUPER_ADMIN', true))).toBe(true);
    expect(canReadAudit(member('MEMBER'))).toBe(false);
    expect(canReadAudit(member('PROJECT_MANAGER'))).toBe(false);
    expect(canReadAudit(member('ORG_ADMIN'))).toBe(true);
    expect(canManageOrganizations(member('ORG_ADMIN'))).toBe(false);
    expect(canManageOrganizations(member('SUPER_ADMIN', true))).toBe(true);
  });
});
