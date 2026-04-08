import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  ApiService,
  DashboardAiMetricsResponse,
  DashboardSummaryResponse,
  RecentActivityResponse
} from '../../core/services/api.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="page">
      <div class="page-header">
        <div>
          <div class="eyebrow">Overview</div>
          <h2>Dashboard</h2>
          <p>Quick read on ticket volume, AI usage, and recent support activity.</p>
        </div>

        <a class="button" routerLink="/tickets/new">Create Ticket</a>
      </div>

      <div *ngIf="summaryLoading" class="empty">Loading dashboard...</div>

      <ng-container *ngIf="!summaryLoading && summary as s">
        <div class="cards">
          <article class="card">
            <span>Total Tickets</span>
            <strong>{{ s.totalTickets }}</strong>
          </article>

          <article class="card">
            <span>Open</span>
            <strong>{{ s.openTickets }}</strong>
          </article>

          <article class="card">
            <span>In Progress</span>
            <strong>{{ s.inProgressTickets }}</strong>
          </article>

          <article class="card">
            <span>AI Analyzed</span>
            <strong>{{ s.aiAnalyzedTickets }}</strong>
          </article>
        </div>

        <div class="split">
          <article class="panel">
            <h3>Status Breakdown</h3>
            <div class="metric-row">
              <span>Waiting for Customer</span>
              <strong>{{ s.waitingForCustomerTickets }}</strong>
            </div>
            <div class="metric-row">
              <span>Resolved</span>
              <strong>{{ s.resolvedTickets }}</strong>
            </div>
            <div class="metric-row">
              <span>Closed</span>
              <strong>{{ s.closedTickets }}</strong>
            </div>
          </article>

          <article class="panel">
            <h3>Next Steps</h3>
            <p>Open the queue, inspect a ticket, run AI analysis, and review the latest workflow activity.</p>
            <div class="actions">
              <a class="button muted" routerLink="/tickets">Open Ticket Queue</a>
              <a class="button muted" routerLink="/tickets/new">Submit New Ticket</a>
            </div>
          </article>
        </div>
      </ng-container>

      <article class="panel ai-panel">
        <div class="panel-head">
          <div>
            <h3>AI Analytics</h3>
            <p class="muted">Live metrics from analyses, review decisions, rewrites, and fallbacks.</p>
          </div>
        </div>

        <div *ngIf="aiMetricsLoading" class="empty subtle">Loading AI analytics...</div>

        <ng-container *ngIf="!aiMetricsLoading && aiMetrics as metrics">
          <div class="ai-cards">
            <article class="mini-card">
              <span>Total Analyses</span>
              <strong>{{ metrics.totalAnalyses }}</strong>
            </article>

            <article class="mini-card">
              <span>Accepted</span>
              <strong>{{ metrics.acceptedRecommendations }}</strong>
            </article>

            <article class="mini-card">
              <span>Rejected</span>
              <strong>{{ metrics.rejectedRecommendations }}</strong>
            </article>

            <article class="mini-card">
              <span>Rewrites</span>
              <strong>{{ metrics.rewriteCount }}</strong>
            </article>

            <article class="mini-card">
              <span>Fallback Analyses</span>
              <strong>{{ metrics.fallbackAnalyses }}</strong>
            </article>

            <article class="mini-card">
              <span>Acceptance Rate</span>
              <strong>{{ metrics.acceptanceRate | number:'1.0-1' }}%</strong>
            </article>
          </div>

          <div class="metric-row emphasized">
            <span>AI Review Conversion</span>
            <strong>{{ metrics.acceptedRecommendations }} accepted / {{ metrics.acceptedRecommendations + metrics.rejectedRecommendations }} reviewed</strong>
          </div>
        </ng-container>
      </article>

      <article class="panel activity-panel">
        <div class="panel-head">
          <div>
            <h3>Recent Activity</h3>
            <p class="muted">Latest ticket events across the helpdesk workflow.</p>
          </div>
          <a class="button muted" routerLink="/tickets">View Queue</a>
        </div>

        <div *ngIf="activityLoading" class="empty subtle">Loading recent activity...</div>

        <div *ngIf="!activityLoading && !recentActivity.length" class="empty subtle">
          No recent activity yet.
        </div>

        <div *ngIf="!activityLoading && recentActivity.length" class="activity-list">
          <a
            class="activity-item"
            *ngFor="let item of recentActivity"
            [routerLink]="['/tickets', item.ticketId]"
          >
            <div class="activity-top">
              <div class="activity-ticket">
                <strong>{{ item.ticketReferenceCode }}</strong>
                <span>{{ item.ticketTitle }}</span>
              </div>
              <time>{{ item.createdAt | date:'medium' }}</time>
            </div>

            <div class="activity-meta">
              <span class="chip">{{ formatLabel(item.eventType) }}</span>
              <span class="chip subtle">{{ formatLabel(item.actorType) }}</span>
            </div>

            <p class="activity-details">{{ item.details }}</p>
          </a>
        </div>
      </article>
    </section>
  `,
  styles: [`
    .page { display:grid; gap:18px; }
    .page-header { display:flex; justify-content:space-between; gap:16px; align-items:flex-start; }
    .eyebrow { text-transform:uppercase; letter-spacing:.08em; font-size:12px; color:#93c5fd; margin-bottom:8px; }
    h2 { margin:0 0 8px; font-size:32px; }
    h3 { margin:0 0 8px; font-size:20px; }
    p { margin:0; color:#9ca3af; }
    .muted { color:#9ca3af; }

    .cards { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:16px; }
    .card,.panel,.mini-card {
      background:linear-gradient(180deg, rgba(255,255,255,0.05), rgba(255,255,255,0.035));
      border:1px solid rgba(255,255,255,0.10);
      border-radius:20px;
      padding:20px;
      box-shadow:0 14px 36px rgba(0,0,0,0.24);
    }

    .card span,.mini-card span {
      display:block;
      color:#cbd5e1;
      margin-bottom:12px;
      font-size:14px;
      font-weight:600;
      letter-spacing:.01em;
    }

    .card strong {
      display:block;
      font-size:52px;
      line-height:1;
      font-weight:800;
      color:#ffffff;
      letter-spacing:-0.03em;
      text-shadow:0 6px 18px rgba(0,0,0,0.28);
    }

    .mini-card strong {
      display:block;
      font-size:30px;
      line-height:1.1;
      font-weight:800;
      color:#ffffff;
      letter-spacing:-0.02em;
    }

    .split { display:grid; grid-template-columns:1.2fr 1fr; gap:16px; }
    .ai-cards { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:14px; margin-bottom:16px; }

    .metric-row {
      display:flex;
      justify-content:space-between;
      padding:12px 0;
      border-bottom:1px solid rgba(255,255,255,0.08);
    }

    .metric-row:last-child { border-bottom:none; }
    .metric-row.emphasized {
      border-top:1px solid rgba(255,255,255,0.08);
      border-bottom:none;
      margin-top:4px;
      padding-top:16px;
      font-size:15px;
    }

    .actions { display:flex; gap:12px; flex-wrap:wrap; margin-top:16px; }
    .panel-head { display:flex; justify-content:space-between; gap:16px; align-items:flex-start; margin-bottom:16px; }

    .activity-list { display:grid; gap:12px; }
    .activity-item {
      display:grid;
      gap:12px;
      padding:16px;
      border-radius:16px;
      border:1px solid rgba(255,255,255,0.08);
      background:rgba(255,255,255,0.03);
      text-decoration:none;
      color:inherit;
      transition:.2s ease;
    }

    .activity-item:hover {
      background:rgba(59,130,246,0.08);
      border-color:rgba(59,130,246,0.22);
    }

    .activity-top { display:flex; justify-content:space-between; gap:16px; align-items:flex-start; }
    .activity-ticket { display:grid; gap:4px; }
    .activity-ticket strong { color:#f8fafc; font-size:15px; }
    .activity-ticket span { color:#cbd5e1; }
    time { color:#9ca3af; white-space:nowrap; font-size:13px; }

    .activity-meta { display:flex; gap:8px; flex-wrap:wrap; }

    .chip {
      display:inline-flex;
      align-items:center;
      border-radius:999px;
      padding:6px 10px;
      font-size:12px;
      font-weight:700;
      background:rgba(59,130,246,0.16);
      border:1px solid rgba(59,130,246,0.25);
      color:#e5e7eb;
    }

    .chip.subtle {
      background:rgba(255,255,255,0.05);
      border-color:rgba(255,255,255,0.10);
    }

    .activity-details { color:#e5e7eb; line-height:1.5; }

    .button {
      display:inline-flex;
      align-items:center;
      justify-content:center;
      min-height:44px;
      padding:0 16px;
      border-radius:14px;
      text-decoration:none;
      border:1px solid rgba(59,130,246,0.35);
      background:rgba(59,130,246,0.18);
      color:#fff;
      cursor:pointer;
    }

    .button.muted {
      background:rgba(255,255,255,0.04);
      border-color:rgba(255,255,255,0.1);
    }

    .empty {
      padding:20px;
      border-radius:16px;
      background:rgba(255,255,255,0.04);
      color:#cbd5e1;
    }

    .empty.subtle { padding:16px; }

    @media (max-width:1200px) {
      .cards,.split,.ai-cards { grid-template-columns:1fr; }
    }

    @media (max-width:900px) {
      .page-header,.panel-head,.activity-top { flex-direction:column; }
      time { white-space:normal; }
    }
  `]
})
export class DashboardComponent implements OnInit {
  private api = inject(ApiService);
  private cdr = inject(ChangeDetectorRef);
  private toast = inject(ToastService);

  summaryLoading = true;
  aiMetricsLoading = true;
  activityLoading = true;

  summary: DashboardSummaryResponse | null = null;
  aiMetrics: DashboardAiMetricsResponse | null = null;
  recentActivity: RecentActivityResponse[] = [];

  ngOnInit(): void {
    this.api.getDashboardSummary().subscribe({
      next: (summary) => {
        this.summary = summary;
        this.summaryLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.summaryLoading = false;
        this.toast.error('Failed to load dashboard summary.');
        this.cdr.markForCheck();
      }
    });

    this.api.getDashboardAiMetrics().subscribe({
      next: (metrics: DashboardAiMetricsResponse) => {
        this.aiMetrics = metrics;
        this.aiMetricsLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.aiMetricsLoading = false;
        this.toast.error('Failed to load AI analytics.');
        this.cdr.markForCheck();
      }
    });

    this.api.getRecentActivity().subscribe({
      next: (items) => {
        this.recentActivity = items;
        this.activityLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.activityLoading = false;
        this.toast.error('Failed to load recent activity.');
        this.cdr.markForCheck();
      }
    });
  }

  formatLabel(value: string | null | undefined): string {
    if (!value) return '—';
    return value
      .toLowerCase()
      .split('_')
      .map(part => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }
}

