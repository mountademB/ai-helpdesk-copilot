import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ToastOutletComponent } from './shared/toast-outlet.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ToastOutletComponent],
  template: `
    <div class="shell">
      <aside class="sidebar">
        <div class="brand">
          <div class="brand-kicker">Portfolio Project</div>
          <h1>AI Helpdesk Copilot</h1>
          <p>Support triage, audit trail, and AI-assisted workflow.</p>
        </div>

        <nav class="nav">
          <a routerLink="/dashboard" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Dashboard</a>
          <a routerLink="/tickets" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Tickets</a>
          <a routerLink="/tickets/new" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Create Ticket</a>
        </nav>
      </aside>

      <main class="content">
        <router-outlet></router-outlet>
      </main>
    </div>

    <app-toast-outlet></app-toast-outlet>
  `,
  styles: [`
    :host {
      display: block;
      min-height: 100vh;
      color: #e5e7eb;
      background: #0f172a;
      font-family: Inter, Arial, sans-serif;
    }

    .shell {
      display: grid;
      grid-template-columns: 280px 1fr;
      min-height: 100vh;
    }

    .sidebar {
      border-right: 1px solid rgba(255,255,255,0.08);
      background: #111827;
      padding: 24px;
      position: sticky;
      top: 0;
      height: 100vh;
      box-sizing: border-box;
    }

    .brand-kicker {
      font-size: 12px;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      color: #93c5fd;
      margin-bottom: 8px;
    }

    .brand h1 {
      margin: 0 0 8px;
      font-size: 28px;
      line-height: 1.1;
    }

    .brand p {
      margin: 0;
      color: #9ca3af;
      font-size: 14px;
      line-height: 1.5;
    }

    .nav {
      margin-top: 28px;
      display: grid;
      gap: 10px;
    }

    .nav a {
      text-decoration: none;
      color: #d1d5db;
      padding: 12px 14px;
      border-radius: 14px;
      transition: 0.2s ease;
      border: 1px solid transparent;
      background: rgba(255,255,255,0.03);
    }

    .nav a:hover,
    .nav a.active {
      background: rgba(59,130,246,0.16);
      border-color: rgba(59,130,246,0.35);
      color: #fff;
    }

    .content {
      padding: 28px;
      box-sizing: border-box;
    }

    @media (max-width: 900px) {
      .shell {
        grid-template-columns: 1fr;
      }

      .sidebar {
        position: static;
        height: auto;
      }
    }
  `]
})
export class AppComponent {}
