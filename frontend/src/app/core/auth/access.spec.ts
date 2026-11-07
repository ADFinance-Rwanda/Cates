import { Me, canDeleteTasks, canManageMembers, canManageProjects } from './access';

const member = (role: string, superAdmin = false): Me => ({
  id: '1',
  email: 'a@b.test',
  displayName: 'A',
  superAdmin,
  organizationId: 'o',
  organizationName: 'Acme',
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
  });
});
