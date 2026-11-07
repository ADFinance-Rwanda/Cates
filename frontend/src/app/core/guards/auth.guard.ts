import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { AuthService } from '../auth/auth.service';

export const authGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  await auth.init();
  if (auth.isLoggedIn()) {
    return true;
  }
  await auth.login();
  return false;
};
