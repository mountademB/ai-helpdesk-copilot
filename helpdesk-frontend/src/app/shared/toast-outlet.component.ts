import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ToastService } from '../core/services/toast.service';

@Component({
  selector: 'app-toast-outlet',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-stack" *ngIf="toastService.toasts().length">
      <article
        class="toast"
        *ngFor="let toast of toastService.toasts()"
        [ngClass]="toast.type"
      >
        <div class="toast-body">
          <span class="toast-dot"></span>
          <div class="toast-message">{{ toast.message }}</div>
        </div>

        <button class="toast-close" type="button" (click)="toastService.dismiss(toast.id)">
          ×
        </button>
      </article>
    </div>
  `,
  styles: [`
    .toast-stack {
      position: fixed;
      top: 20px;
      right: 20px;
      z-index: 1000;
      display: grid;
      gap: 12px;
      width: min(360px, calc(100vw - 32px));
    }

    .toast {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 12px;
      padding: 14px 16px;
      border-radius: 16px;
      border: 1px solid rgba(255,255,255,0.12);
      background: rgba(15,23,42,0.96);
      color: #e5e7eb;
      box-shadow: 0 18px 40px rgba(0,0,0,0.35);
      backdrop-filter: blur(10px);
    }

    .toast.success {
      border-color: rgba(34,197,94,0.35);
    }

    .toast.error {
      border-color: rgba(239,68,68,0.35);
    }

    .toast.info {
      border-color: rgba(59,130,246,0.35);
    }

    .toast-body {
      display: flex;
      gap: 10px;
      align-items: flex-start;
      flex: 1;
      min-width: 0;
    }

    .toast-dot {
      width: 10px;
      height: 10px;
      border-radius: 999px;
      margin-top: 5px;
      flex: 0 0 auto;
      background: #94a3b8;
    }

    .toast.success .toast-dot {
      background: #22c55e;
    }

    .toast.error .toast-dot {
      background: #ef4444;
    }

    .toast.info .toast-dot {
      background: #3b82f6;
    }

    .toast-message {
      line-height: 1.45;
      word-break: break-word;
    }

    .toast-close {
      background: transparent;
      border: none;
      color: #cbd5e1;
      cursor: pointer;
      font-size: 20px;
      line-height: 1;
      padding: 0;
    }
  `]
})
export class ToastOutletComponent {
  readonly toastService = inject(ToastService);
}
