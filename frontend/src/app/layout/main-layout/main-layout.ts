import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Header } from '../../shared/components/header/header';
import { Sidebar } from '../../shared/components/sidebar/sidebar';

@Component({
  selector: 'app-main-layout',
  imports: [RouterOutlet, Sidebar, Header],
  template: `
    <div class="app-shell">
      <app-sidebar />
      <div class="workspace">
        <app-header />
        <main class="page">
          <router-outlet />
        </main>
      </div>
    </div>
  `
})
export class MainLayout {}
