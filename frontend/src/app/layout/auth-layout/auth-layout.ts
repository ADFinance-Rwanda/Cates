import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-auth-layout',
  styles: `
    :host { display: block; min-height: 100vh; }
    .stage { display: grid; grid-template-columns: minmax(0, 1.08fr) minmax(320px, 0.92fr); min-height: 100vh; }
    .brand {
      display: flex; flex-direction: column; justify-content: space-between;
      padding: 48px 56px 28px; color: #fff;
      background: radial-gradient(900px 480px at 12% 0%, rgba(255,255,255,.16), transparent 55%),
        linear-gradient(165deg, #1a9a72 0%, #0f6e56 42%, #083f32 100%);
    }
    .logo { font-weight: 800; letter-spacing: -0.03em; font-size: 20px; }
    .logo span { display: block; margin-top: 4px; font-size: 11px; letter-spacing: 0.16em; text-transform: uppercase; opacity: .78; }
    h1 { max-width: 11ch; margin: 72px 0 16px; font-size: clamp(42px, 4.6vw, 64px); line-height: .98; letter-spacing: -0.045em; }
    .brand p { max-width: 36rem; margin: 0; color: rgba(255,255,255,.82); line-height: 1.55; }
    .panel { display: flex; align-items: center; justify-content: center; padding: 48px 28px; background: #fff; }
    .form { width: min(100%, 420px); }
    h2 { margin: 0; font-size: 32px; letter-spacing: -0.04em; }
    .form p { margin: 8px 0 22px; color: #667085; }
    button { width: 100%; height: 50px; border-radius: 10px; font-size: 16px; font-weight: 700; }
    .status { color: #667085; }
    .error { color: #9f2d2d; }
    @media (max-width: 980px) {
      .stage { grid-template-columns: 1fr; }
      .brand { padding: 32px 24px; }
      h1 { margin-top: 36px; }
    }
  `,
  template: `
    <div class="stage">
      <aside class="brand">
        <div>
          <div class="logo">Taskflow<span>Work platform</span></div>
          <h1>Every project, in one place.</h1>
          <p>Plan work, assign tasks, and follow progress across your organization from a single secure sign-in.</p>
        </div>
        <footer>© 2026 · Taskflow</footer>
      </aside>
      <main class="panel">
        <section class="form">
          <h2>Sign in</h2>
          <p>Sign in with your organization account.</p>
          @if (error()) {
            <p class="error" role="alert">{{ error() }}</p>
          } @else {
            <p class="status">Opening the secure sign-in page…</p>
          }
          <button type="button" (click)="signIn()">{{ error() ? 'Try again' : 'Continue' }}</button>
        </section>
      </main>
    </div>
  `
})
export class AuthLayout {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly error = signal('');

  constructor() {
    const params = new URLSearchParams(window.location.search);
    const rejected = params.get('error_description');
    if (rejected) {
      this.error.set(rejected.replaceAll('+', ' '));
      return;
    }
    void this.auth.init().then(() => {
      if (this.auth.setupError()) {
        this.error.set(this.auth.setupError());
        return;
      }
      if (this.auth.isLoggedIn()) {
        void this.router.navigateByUrl('/dashboard');
        return;
      }
      void this.auth.login();
    });
  }

  protected signIn(): void {
    this.error.set('');
    void this.auth.login().then(() => {
      if (this.auth.setupError()) {
        this.error.set(this.auth.setupError());
      }
    });
  }
}
