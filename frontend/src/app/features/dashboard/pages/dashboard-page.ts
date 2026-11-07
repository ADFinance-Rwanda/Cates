import { afterEveryRender, Component, DestroyRef, effect, ElementRef, inject, signal, viewChild } from '@angular/core';
import { Chart } from 'chart.js/auto';
import { ApiService } from '../../../core/services/api.service';
import { AuthService } from '../../../core/auth/auth.service';
import { Dashboard } from '../models/dashboard.model';
import { LoadingSpinner } from '../../../shared/components/loading-spinner/loading-spinner';

@Component({
  selector: 'app-dashboard',
  imports: [LoadingSpinner],
  template: `
    <header class="page-head">
      <div>
        <h1>Dashboard</h1>
        <p>Scope: {{ data()?.scope === 'ASSIGNED' ? 'Your tasks' : 'Whole organization' }}</p>
      </div>
    </header>
    @if (loading()) { <app-loading-spinner message="Loading analytics…" /> }
    @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
    @if (data(); as dashboard) {
      <section class="kpis" aria-label="Productivity">
        <article class="kpi"><span>Total</span><strong>{{ dashboard.productivity.totalTasks }}</strong></article>
        <article class="kpi"><span>Completed</span><strong>{{ dashboard.productivity.completedTasks }}</strong></article>
        <article class="kpi"><span>Pending</span><strong>{{ dashboard.productivity.pendingTasks }}</strong></article>
        <article class="kpi"><span>Overdue</span><strong>{{ dashboard.productivity.overdueTasks }}</strong></article>
        <article class="kpi"><span>Completion</span><strong>{{ rate(dashboard) }}</strong></article>
      </section>
      <div class="split">
        <section class="panel">
          <h2>By status</h2>
          @for (entry of entries(dashboard.tasksByStatus); track entry[0]) {
            <div class="stat-line"><span>{{ entry[0] }}</span><strong>{{ entry[1] }}</strong></div>
          }
        </section>
        <section class="panel">
          <h2>By priority</h2>
          @for (entry of entries(dashboard.tasksByPriority); track entry[0]) {
            <div class="stat-line"><span>{{ entry[0] }}</span><strong>{{ entry[1] }}</strong></div>
          }
        </section>
      </div>
      <section class="panel" style="margin-top: 0.85rem">
        <h2>Created and completed, last 14 days</h2>
        <div class="chart-frame">
          <canvas #trend aria-label="Task trend chart" role="img" style="position: absolute; inset: 0; width: 100%; height: 100%"></canvas>
        </div>
      </section>
    }
  `
})
export class DashboardPage {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly data = signal<Dashboard | null>(null);
  private readonly canvas = viewChild<ElementRef<HTMLCanvasElement>>('trend');
  private chart: Chart | null = null;
  private paintedKey = '';

  constructor() {
    inject(DestroyRef).onDestroy(() => this.chart?.destroy());
    effect(() => {
      const me = this.auth.me();
      const organizationId = this.auth.selectedOrgId();
      if (!me) {
        return;
      }
      if (me.superAdmin && !organizationId) {
        this.data.set(null);
        this.chart?.destroy();
        this.chart = null;
        this.paintedKey = '';
        this.error.set('Choose an organization.');
        this.loading.set(false);
        return;
      }
      this.loading.set(true);
      this.error.set('');
      this.api.dashboard().subscribe({
        next: (dashboard) => {
          this.data.set(dashboard);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Analytics could not be loaded.');
          this.loading.set(false);
        }
      });
    });
    afterEveryRender(() => this.paint());
  }

  private paint(): void {
    const dashboard = this.data();
    const canvas = this.canvas();
    if (!dashboard || !canvas) {
      return;
    }
    const key = `${dashboard.scope}:${dashboard.productivity.totalTasks}:${dashboard.trend.length}`;
    if (this.paintedKey === key && this.chart) {
      return;
    }
    this.paintedKey = key;
    this.chart?.destroy();
    this.chart = new Chart(canvas.nativeElement, {
      type: 'bar',
      data: {
        labels: dashboard.trend.map((point) => point.date.slice(5)),
        datasets: [
          { label: 'Created', data: dashboard.trend.map((point) => point.created), backgroundColor: '#0f6e56' },
          { label: 'Completed', data: dashboard.trend.map((point) => point.completed), backgroundColor: '#c4a35a' }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        animation: false,
        plugins: { legend: { position: 'bottom' } }
      }
    });
  }

  protected entries(values: Record<string, number>): [string, number][] {
    return Object.entries(values);
  }

  protected rate(dashboard: Dashboard): string {
    return `${Math.round(dashboard.productivity.completionRate * 100)}%`;
  }
}
