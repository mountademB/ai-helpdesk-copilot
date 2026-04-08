import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'info';

export interface ToastItem {
  id: number;
  type: ToastType;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private nextId = 1;
  readonly toasts = signal<ToastItem[]>([]);

  show(type: ToastType, message: string, durationMs = 3200): void {
    const toast: ToastItem = {
      id: this.nextId++,
      type,
      message
    };

    this.toasts.update(items => [...items, toast]);

    window.setTimeout(() => {
      this.dismiss(toast.id);
    }, durationMs);
  }

  success(message: string, durationMs = 3200): void {
    this.show('success', message, durationMs);
  }

  error(message: string, durationMs = 4200): void {
    this.show('error', message, durationMs);
  }

  info(message: string, durationMs = 3200): void {
    this.show('info', message, durationMs);
  }

  dismiss(id: number): void {
    this.toasts.update(items => items.filter(item => item.id !== id));
  }
}
