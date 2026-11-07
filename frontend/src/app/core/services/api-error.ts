import { HttpErrorResponse } from '@angular/common/http';
import { ApiErrorBody } from '../auth/access';

export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error as ApiErrorBody | null;
    if (body?.message) {
      return body.message;
    }
    if (error.status === 0) {
      return 'The API is unreachable.';
    }
  }
  return 'The request failed.';
}
