export interface Member {
  id: string;
  userId: string;
  email: string;
  displayName: string;
  role: 'ORG_ADMIN' | 'PROJECT_MANAGER' | 'MEMBER';
}
