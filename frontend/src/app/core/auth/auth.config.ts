import { AuthConfig } from 'angular-oauth2-oidc';

const issuer = 'http://localhost:8081/realms/taskflow';
const clientId = 'taskflow-spa';

export function authConfig(): AuthConfig {
  return {
    issuer,
    redirectUri: window.location.origin + '/',
    postLogoutRedirectUri: window.location.origin + '/',
    clientId,
    responseType: 'code',
    scope: 'openid profile email',
    requireHttps: false,
    showDebugInformation: false,
    strictDiscoveryDocumentValidation: false
  };
}
