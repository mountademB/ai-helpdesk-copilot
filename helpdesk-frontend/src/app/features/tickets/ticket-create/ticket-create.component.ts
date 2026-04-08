import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService, UserListItemResponse } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-ticket-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="page">
      <div class="page-header">
        <div>
          <div class="eyebrow">Submission</div>
          <h2>Create Ticket</h2>
          <p>Submit a new support issue into the helpdesk workflow.</p>
        </div>

        <a class="button muted" routerLink="/tickets">Back to Tickets</a>
      </div>

      <form class="panel form" [formGroup]="form" (ngSubmit)="submit()">
        <div class="field">
          <label>Created By</label>
          <select formControlName="createdById">
            <option *ngFor="let user of users" [value]="user.id">
              {{ user.fullName }} ({{ user.role }})
            </option>
          </select>
        </div>

        <div class="field">
          <label>Title</label>
          <input type="text" formControlName="title" placeholder="Example: Cannot access VPN" />
        </div>

        <div class="field">
          <label>Description</label>
          <textarea rows="7" formControlName="description" placeholder="Describe the issue in detail..."></textarea>
        </div>

        <button class="button" type="submit" [disabled]="submitting || form.invalid">
          {{ submitting ? 'Creating...' : 'Create Ticket' }}
        </button>
      </form>
    </section>
  `,
  styles: [`
    .page-header { display:flex; justify-content:space-between; gap:16px; align-items:flex-start; margin-bottom:24px; }
    .eyebrow { text-transform:uppercase; letter-spacing:.08em; font-size:12px; color:#93c5fd; margin-bottom:8px; }
    h2 { margin:0 0 8px; font-size:32px; }
    p { margin:0; color:#9ca3af; }
    .panel { background:rgba(255,255,255,.04); border:1px solid rgba(255,255,255,.08); border-radius:20px; padding:20px; box-shadow:0 10px 30px rgba(0,0,0,.18); max-width:840px; }
    .form { display:grid; gap:16px; }
    .field { display:grid; gap:8px; }
    label { color:#9ca3af; font-size:13px; }
    input,textarea,select { width:100%; box-sizing:border-box; background:#0b1220; color:#e5e7eb; border:1px solid rgba(255,255,255,.12); border-radius:12px; padding:12px; font:inherit; }
    textarea { resize:vertical; min-height:140px; }
    .button { display:inline-flex; align-items:center; justify-content:center; min-height:44px; padding:0 16px; border-radius:14px; text-decoration:none; border:1px solid rgba(59,130,246,.35); background:rgba(59,130,246,.18); color:#fff; cursor:pointer; width:fit-content; }
    .button.muted { background:rgba(255,255,255,.04); border-color:rgba(255,255,255,.1); }
    .button:disabled { opacity:.6; cursor:not-allowed; }
  `]
})
export class TicketCreateComponent implements OnInit {
  private api = inject(ApiService);
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  private toast = inject(ToastService);

  submitting = false;
  users: UserListItemResponse[] = [];

  form = this.fb.group({
    createdById: [5, Validators.required],
    title: ['', Validators.required],
    description: ['', Validators.required]
  });

  ngOnInit(): void {
    this.api.getUsers().subscribe({
      next: (users) => {
        this.users = users;
        this.cdr.markForCheck();
      },
      error: () => {
        this.toast.error('Unable to load users for ticket creation.');
        this.cdr.markForCheck();
      }
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toast.error('Please fill in all required ticket fields.');
      this.cdr.markForCheck();
      return;
    }

    const value = this.form.getRawValue();
    this.submitting = true;
    this.cdr.markForCheck();

    this.api.createTicket({
      createdById: Number(value.createdById),
      title: String(value.title ?? ''),
      description: String(value.description ?? '')
    }).subscribe({
      next: (ticket) => {
        this.submitting = false;
        this.toast.success('Ticket created successfully.');
        this.cdr.markForCheck();
        this.router.navigate(['/tickets', ticket.id]);
      },
      error: () => {
        this.submitting = false;
        this.toast.error('Failed to create ticket.');
        this.cdr.markForCheck();
      }
    });
  }
}
