import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../auth/auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  let headers = request.headers;
  const token = auth.accessToken();
  if (token) {
    headers = headers.set('Authorization', `Bearer ${token}`);
  }
  const organizationId = auth.selectedOrgId();
  if (organizationId) {
    headers = headers.set('X-Organization-Id', organizationId);
  }
  return next(request.clone({ headers }));
};
