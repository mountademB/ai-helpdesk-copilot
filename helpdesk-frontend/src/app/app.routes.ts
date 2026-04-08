import { Routes } from '@angular/router';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { TicketListComponent } from './features/tickets/ticket-list/ticket-list.component';
import { TicketDetailComponent } from './features/tickets/ticket-detail/ticket-detail.component';
import { TicketCreateComponent } from './features/tickets/ticket-create/ticket-create.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'tickets', component: TicketListComponent },
  { path: 'tickets/new', component: TicketCreateComponent },
  { path: 'tickets/:id', component: TicketDetailComponent },
  { path: '**', redirectTo: 'dashboard' }
];
