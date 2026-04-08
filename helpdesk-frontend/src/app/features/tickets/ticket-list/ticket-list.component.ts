import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService, TicketResponse } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-ticket-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="page">
      <div class="page-header">
        <div>
          <div class="eyebrow">Operations</div>
          <h2>Ticket Queue</h2>
          <p>Browse live tickets and open one to inspect details, comments, and audit events.</p>
        </div>

        <a class="button" routerLink="/tickets/new">New Ticket</a>
      </div>

      <section class="filters panel">
        <div class="filters-top">
          <div>
            <h3>Search & Filters</h3>
            <p class="muted">Narrow the queue by ticket text or workflow fields.</p>
          </div>

          <button class="button muted" type="button" (click)="resetFilters()">Clear Filters</button>
        </div>

        <div class="filters-grid">
          <div class="field search-field">
            <label>Search</label>
            <input
              type="text"
              [value]="searchTerm()"
              (input)="searchTerm.set(($any($event.target).value ?? '').trimStart())"
              placeholder="Search by reference, title, description, assignee, or team"
            />
          </div>

          <div class="field">
            <label>Status</label>
            <select [value]="statusFilter()" (change)="statusFilter.set($any($event.target).value)">
              <option value="">All statuses</option>
              <option value="OPEN">Open</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="WAITING_FOR_CUSTOMER">Waiting for Customer</option>
              <option value="RESOLVED">Resolved</option>
              <option value="CLOSED">Closed</option>
            </select>
          </div>

          <div class="field">
            <label>Priority</label>
            <select [value]="priorityFilter()" (change)="priorityFilter.set($any($event.target).value)">
              <option value="">All priorities</option>
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="URGENT">Urgent</option>
            </select>
          </div>

          <div class="field">
            <label>Category</label>
            <select [value]="categoryFilter()" (change)="categoryFilter.set($any($event.target).value)">
              <option value="">All categories</option>
              <option *ngFor="let category of categoryOptions()" [value]="category">
                {{ formatLabel(category) }}
              </option>
            </select>
          </div>

          <div class="field">
            <label>Team</label>
            <select [value]="teamFilter()" (change)="teamFilter.set($any($event.target).value)">
              <option value="">All teams</option>
              <option *ngFor="let team of teamOptions()" [value]="team">
                {{ team }}
              </option>
            </select>
          </div>

          <div class="field">
            <label>Sort</label>
            <select [value]="sortBy()" (change)="sortBy.set($any($event.target).value)">
              <option value="newest">Newest first</option>
              <option value="oldest">Oldest first</option>
              <option value="priority">Priority</option>
              <option value="title">Title A–Z</option>
            </select>
          </div>
        </div>

        <div class="results-row">
          <span>{{ filteredTickets().length }} result{{ filteredTickets().length === 1 ? '' : 's' }}</span>
        </div>
      </section>

      <div *ngIf="loading" class="empty">Loading tickets...</div>

      <div *ngIf="!loading && !filteredTickets().length" class="empty">
        No tickets match the current filters.
      </div>

      <div *ngIf="!loading && filteredTickets().length" class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Reference</th>
              <th>Title</th>
              <th>Status</th>
              <th>Priority</th>
              <th>Category</th>
              <th>Assigned To</th>
              <th>Team</th>
              <th>Created</th>
            </tr>
          </thead>

          <tbody>
            <tr *ngFor="let ticket of filteredTickets()">
              <td>
                <a [routerLink]="['/tickets', ticket.id]">{{ ticket.referenceCode }}</a>
              </td>
              <td class="ticket-title">{{ ticket.title }}</td>
              <td>
                <span class="badge" [ngClass]="statusClass(ticket.status)">
                  {{ formatLabel(ticket.status) }}
                </span>
              </td>
              <td>
                <span class="badge" [ngClass]="priorityClass(ticket.priority)">
                  {{ formatLabel(ticket.priority) }}
                </span>
              </td>
              <td>{{ formatLabel(ticket.category) }}</td>
              <td class="secondary">{{ ticket.assignedToName || 'Unassigned' }}</td>
              <td class="secondary">{{ ticket.teamName || 'No Team' }}</td>
              <td class="secondary">{{ ticket.createdAt | date:'medium' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  `,
  styles: [`
    .page-header {
      display: flex;
      justify-content: space-between;
      gap: 16px;
      align-items: flex-start;
      margin-bottom: 24px;
    }

    .eyebrow {
      text-transform: uppercase;
      letter-spacing: 0.08em;
      font-size: 12px;
      color: #93c5fd;
      margin-bottom: 8px;
    }

    h2 {
      margin: 0 0 8px;
      font-size: 32px;
    }

    h3 {
      margin: 0 0 6px;
      font-size: 18px;
    }

    p {
      margin: 0;
      color: #9ca3af;
    }

    .muted {
      color: #9ca3af;
    }

    .panel {
      background: rgba(255,255,255,0.04);
      border: 1px solid rgba(255,255,255,0.08);
      border-radius: 20px;
      padding: 18px;
      box-shadow: 0 10px 30px rgba(0,0,0,0.18);
      margin-bottom: 18px;
    }

    .filters-top {
      display: flex;
      justify-content: space-between;
      gap: 16px;
      align-items: flex-start;
      margin-bottom: 16px;
    }

    .filters-grid {
      display: grid;
      grid-template-columns: 2fr repeat(5, minmax(150px, 1fr));
      gap: 14px;
    }

    .field {
      display: grid;
      gap: 8px;
    }

    .field label {
      color: #9ca3af;
      font-size: 13px;
    }

    .search-field {
      grid-column: span 2;
    }

    input,
    select {
      width: 100%;
      box-sizing: border-box;
      background: #0b1220;
      color: #e5e7eb;
      border: 1px solid rgba(255,255,255,0.12);
      border-radius: 12px;
      padding: 12px;
      font: inherit;
    }

    .results-row {
      margin-top: 14px;
      color: #cbd5e1;
      font-size: 14px;
    }

    .button {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-height: 44px;
      padding: 0 16px;
      border-radius: 14px;
      text-decoration: none;
      border: 1px solid rgba(59,130,246,0.35);
      background: rgba(59,130,246,0.18);
      color: #fff;
      cursor: pointer;
    }

    .button.muted {
      background: rgba(255,255,255,0.04);
      border-color: rgba(255,255,255,0.1);
    }

    .table-wrap {
      overflow: auto;
      background: rgba(255,255,255,0.04);
      border: 1px solid rgba(255,255,255,0.08);
      border-radius: 20px;
      box-shadow: 0 10px 30px rgba(0,0,0,0.18);
    }

    table {
      width: 100%;
      border-collapse: collapse;
      min-width: 1080px;
    }

    th,
    td {
      padding: 16px;
      text-align: left;
      border-bottom: 1px solid rgba(255,255,255,0.08);
      vertical-align: middle;
    }

    th {
      color: #9ca3af;
      font-size: 13px;
      background: rgba(255,255,255,0.03);
    }

    td a {
      color: #93c5fd;
      text-decoration: none;
      font-weight: 700;
    }

    .ticket-title {
      font-weight: 700;
      color: #f8fafc;
      letter-spacing: -0.01em;
    }

    .secondary {
      color: #d1d5db;
    }

    .badge {
      display: inline-flex;
      align-items: center;
      border-radius: 999px;
      padding: 6px 10px;
      font-size: 12px;
      font-weight: 700;
      border: 1px solid transparent;
    }

    .status-open { background: rgba(59,130,246,0.18); border-color: rgba(59,130,246,0.3); }
    .status-progress { background: rgba(245,158,11,0.18); border-color: rgba(245,158,11,0.3); }
    .status-waiting { background: rgba(168,85,247,0.18); border-color: rgba(168,85,247,0.3); }
    .status-resolved { background: rgba(34,197,94,0.18); border-color: rgba(34,197,94,0.3); }
    .status-closed { background: rgba(107,114,128,0.25); border-color: rgba(107,114,128,0.35); }

    .priority-low { background: rgba(107,114,128,0.25); border-color: rgba(107,114,128,0.35); }
    .priority-medium { background: rgba(59,130,246,0.18); border-color: rgba(59,130,246,0.3); }
    .priority-high { background: rgba(249,115,22,0.18); border-color: rgba(249,115,22,0.3); }
    .priority-urgent { background: rgba(239,68,68,0.18); border-color: rgba(239,68,68,0.3); }

    .empty {
      padding: 20px;
      border-radius: 16px;
      background: rgba(255,255,255,0.04);
      color: #cbd5e1;
    }

    @media (max-width: 1350px) {
      .filters-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }

      .search-field {
        grid-column: span 2;
      }
    }

    @media (max-width: 800px) {
      .filters-top,
      .page-header {
        flex-direction: column;
      }

      .filters-grid {
        grid-template-columns: 1fr;
      }

      .search-field {
        grid-column: span 1;
      }
    }
  `]
})
export class TicketListComponent implements OnInit {
  private api = inject(ApiService);
  private cdr = inject(ChangeDetectorRef);
  private toast = inject(ToastService);

  loading = true;
  tickets = signal<TicketResponse[]>([]);

  searchTerm = signal('');
  statusFilter = signal('');
  priorityFilter = signal('');
  categoryFilter = signal('');
  teamFilter = signal('');
  sortBy = signal('newest');

  readonly categoryOptions = computed(() => {
    const values = Array.from(new Set(this.tickets().map(ticket => ticket.category))).filter(Boolean);
    return values.sort((a, b) => a.localeCompare(b));
  });

  readonly teamOptions = computed(() => {
    const values = Array.from(new Set(
      this.tickets()
        .map(ticket => ticket.teamName)
        .filter((team): team is string => !!team)
    ));
    return values.sort((a, b) => a.localeCompare(b));
  });

  readonly filteredTickets = computed(() => {
    const term = this.searchTerm().toLowerCase();
    const status = this.statusFilter();
    const priority = this.priorityFilter();
    const category = this.categoryFilter();
    const team = this.teamFilter();
    const sort = this.sortBy();

    const filtered = this.tickets().filter(ticket => {
      const haystack = [
        ticket.referenceCode,
        ticket.title,
        ticket.description,
        ticket.assignedToName ?? '',
        ticket.teamName ?? '',
        ticket.category
      ].join(' ').toLowerCase();

      const matchesSearch = !term || haystack.includes(term);
      const matchesStatus = !status || ticket.status === status;
      const matchesPriority = !priority || ticket.priority === priority;
      const matchesCategory = !category || ticket.category === category;
      const matchesTeam = !team || ticket.teamName === team;

      return matchesSearch && matchesStatus && matchesPriority && matchesCategory && matchesTeam;
    });

    return filtered.sort((a, b) => {
      switch (sort) {
        case 'oldest':
          return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
        case 'priority':
          return this.priorityRank(b.priority) - this.priorityRank(a.priority);
        case 'title':
          return a.title.localeCompare(b.title);
        case 'newest':
        default:
          return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
      }
    });
  });

  ngOnInit(): void {
    this.api.getTickets().subscribe({
      next: (tickets) => {
        this.tickets.set(tickets);
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.toast.error('Failed to load tickets.');
        this.cdr.markForCheck();
      }
    });
  }

  resetFilters(): void {
    this.searchTerm.set('');
    this.statusFilter.set('');
    this.priorityFilter.set('');
    this.categoryFilter.set('');
    this.teamFilter.set('');
    this.sortBy.set('newest');
  }

  formatLabel(value: string | null | undefined): string {
    if (!value) return '—';
    return value
      .toLowerCase()
      .split('_')
      .map(part => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  statusClass(status: string): string {
    switch (status) {
      case 'OPEN': return 'status-open';
      case 'IN_PROGRESS': return 'status-progress';
      case 'WAITING_FOR_CUSTOMER': return 'status-waiting';
      case 'RESOLVED': return 'status-resolved';
      case 'CLOSED': return 'status-closed';
      default: return '';
    }
  }

  priorityClass(priority: string): string {
    switch (priority) {
      case 'LOW': return 'priority-low';
      case 'MEDIUM': return 'priority-medium';
      case 'HIGH': return 'priority-high';
      case 'URGENT': return 'priority-urgent';
      default: return '';
    }
  }

  private priorityRank(priority: string): number {
    switch (priority) {
      case 'URGENT': return 4;
      case 'HIGH': return 3;
      case 'MEDIUM': return 2;
      case 'LOW': return 1;
      default: return 0;
    }
  }
}
