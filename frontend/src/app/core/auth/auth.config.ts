import { AuthConfig } from 'angular-oauth2-oidc';
import { environment } from '../../../environments/environment';

export function authConfig(): AuthConfig {
  return {
    issuer: environment.issuer,
    redirectUri: window.location.origin + '/',
    postLogoutRedirectUri: window.location.origin + '/',
    clientId: environment.clientId,
    responseType: 'code',
    scope: 'openid profile email',
    requireHttps: false,
    showDebugInformation: false,
    strictDiscoveryDocumentValidation: false
  };
}
