import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  AIRecommendationResponse,
  ApiService,
  CommentResponse,
  EventResponse,
  KnowledgeGuidanceResponse,
  RewriteDraftReplyResponse,
  SimilarTicketResponse,
  TeamListItemResponse,
  TicketFeedbackResponse,
  TicketHistorySummaryResponse,
  TicketResponse,
  UserListItemResponse
} from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-ticket-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="page" *ngIf="ticket; else loadingTpl">
      <div class="page-header">
        <div>
          <div class="eyebrow">Ticket Detail</div>
          <h2>{{ ticket.referenceCode }}</h2>
          <p class="subtitle">{{ ticket.title }}</p>
        </div>

        <div class="header-actions">
          <a class="button muted" routerLink="/tickets">Back to Tickets</a>
          <button class="button" (click)="analyze()" [disabled]="busy">Analyze Ticket</button>
        </div>
      </div>

      <section class="hero-grid">
        <article class="panel hero-main">
          <div class="chip-row">
            <span class="badge" [ngClass]="statusClass(ticket.status)">{{ formatLabel(ticket.status) }}</span>
            <span class="badge" [ngClass]="priorityClass(ticket.priority)">{{ formatLabel(ticket.priority) }}</span>
            <span class="chip">{{ formatLabel(ticket.category) }}</span>
            <span class="chip">{{ ticket.teamName || 'No Team' }}</span>
          </div>

          <div class="summary-grid">
            <div class="summary-card">
              <label>Created By</label>
              <div>{{ ticket.createdByName }}</div>
            </div>
            <div class="summary-card">
              <label>Assigned To</label>
              <div>{{ ticket.assignedToName || 'Unassigned' }}</div>
            </div>
            <div class="summary-card">
              <label>Created</label>
              <div>{{ ticket.createdAt | date:'medium' }}</div>
            </div>
            <div class="summary-card">
              <label>AI Status</label>
              <div>{{ ticket.aiAnalyzed ? 'Analyzed' : 'Not analyzed yet' }}</div>
            </div>
          </div>

          <div class="description-block">
            <label>Description</label>
            <p>{{ ticket.description }}</p>
          </div>
        </article>

        <aside class="panel hero-side">
          <div class="section-head">
            <h3>AI Copilot</h3>
            <span class="chip subtle" [ngClass]="sourceClass(ai?.analysisSource)">
              {{ ai ? formatSource(ai.analysisSource) : (ticket.aiAnalyzed ? 'Unknown' : 'Idle') }}
            </span>
          </div>

          <div *ngIf="ai" class="confidence-banner" [ngClass]="confidenceClass(ai.confidenceScore)">
            <div class="confidence-banner-top">
              <strong>{{ confidenceLabel(ai.confidenceScore) }}</strong>
              <span>{{ ai.confidenceScore | number:'1.2-2' }}</span>
            </div>
            <p>{{ confidenceGuidance(ai.confidenceScore) }}</p>
          </div>

          <div *ngIf="!ai" class="empty-box">
            No AI recommendation loaded in this session yet. Click <strong>Analyze Ticket</strong> to generate one.
          </div>

          <ng-container *ngIf="ai">
            <div class="info-row">
              <label>Source</label>
              <div>{{ formatSource(ai.analysisSource) }}</div>
            </div>
            <div class="info-row">
              <label>Predicted Category</label>
              <div>{{ formatLabel(ai.predictedCategory) }}</div>
            </div>
            <div class="info-row">
              <label>Predicted Priority</label>
              <div>{{ formatLabel(ai.predictedPriority) }}</div>
            </div>
            <div class="info-row">
              <label>Suggested Team</label>
              <div>{{ ai.suggestedTeamName || 'No suggestion' }}</div>
            </div>
            <div class="info-row">
              <label>Confidence</label>
              <div class="confidence-inline">
                <span>{{ ai.confidenceScore | number:'1.2-2' }}</span>
                <span class="confidence-chip" [ngClass]="confidenceClass(ai.confidenceScore)">
                  {{ confidenceShortLabel(ai.confidenceScore) }}
                </span>
              </div>
            </div>
            <div class="info-row">
              <label>Review Status</label>
              <div>{{ formatLabel(ai.reviewStatus) }}</div>
            </div>

            <div class="content-box">
              <label>Summary</label>
              <p>{{ ai.summary }}</p>
            </div>

            <div class="content-box">
              <label>Draft Reply</label>
              <p>{{ ai.draftReply }}</p>
            </div>

            <div class="rewrite-tools">
              <button class="button muted small" type="button" (click)="rewriteDraft('SHORTER')" [disabled]="rewriteBusy || busy">Shorter</button>
              <button class="button muted small" type="button" (click)="rewriteDraft('MORE_FORMAL')" [disabled]="rewriteBusy || busy">More Formal</button>
              <button class="button muted small" type="button" (click)="rewriteDraft('MORE_EMPATHETIC')" [disabled]="rewriteBusy || busy">More Empathetic</button>
              <button class="button muted small" type="button" (click)="rewriteDraft('CUSTOMER_SAFE')" [disabled]="rewriteBusy || busy">Customer-Safe</button>
            </div>

            <div class="content-box" *ngIf="rewrittenDraft">
              <label>Rewrite Preview</label>
              <p>{{ rewrittenDraft.rewrittenText }}</p>
              <div class="rewrite-meta">
                <span class="chip subtle">{{ formatLabel(rewrittenDraft.mode) }}</span>
                <span class="chip subtle">{{ formatSource(rewrittenDraft.source) }}</span>
              </div>
            </div>

            <div
              class="review-warning"
              *ngIf="ai.reviewStatus === 'PENDING' && ai.confidenceScore < 0.75"
            >
              Low-confidence recommendation. Review the ticket carefully and compare similar tickets before accepting.
            </div>

            <div class="action-row" *ngIf="ai.reviewStatus === 'PENDING'">
              <button class="button accept" (click)="review('ACCEPT')" [disabled]="busy">Accept</button>
              <button class="button reject" (click)="review('REJECT')" [disabled]="busy">Reject</button>
            </div>
          </ng-container>
        </aside>
      </section>

      <section class="knowledge-section">
        <article class="panel">
          <div class="section-head">
            <div>
              <h3>Knowledge-Grounded Resolution Help</h3>
              <p class="panel-copy">Internal guidance grounded in matched support playbooks and procedures.</p>
            </div>
          </div>

          <div *ngIf="knowledgeLoading" class="empty-box">Loading knowledge guidance...</div>

          <ng-container *ngIf="!knowledgeLoading && knowledgeGuidance as kg">
            <div class="content-box">
              <label>Grounded Summary</label>
              <p>{{ kg.guidanceSummary }}</p>
            </div>

            <div class="content-box">
              <label>Recommended Checks</label>
              <p>{{ kg.recommendedChecks }}</p>
            </div>

            <div class="content-box">
              <label>Escalation Guidance</label>
              <p>{{ kg.escalationGuidance }}</p>
            </div>

            <div class="article-stack" *ngIf="kg.articles.length; else noArticlesTpl">
              <article class="article-item" *ngFor="let article of kg.articles">
                <div class="article-head">
                  <div>
                    <strong>{{ article.title }}</strong>
                    <div class="article-id">{{ article.articleId }}</div>
                  </div>
                  <span class="score-badge">Score {{ article.relevanceScore }}</span>
                </div>

                <div class="article-meta">
                  <span class="chip">{{ formatLabel(article.category) }}</span>
                  <span class="chip subtle">{{ article.teamName }}</span>
                </div>

                <p class="article-reason">{{ article.relevanceReason }}</p>
                <p class="article-copy">{{ article.summary }}</p>
              </article>
            </div>
          </ng-container>
        </article>
      </section>

      <section class="history-section">
        <article class="panel">
          <div class="section-head">
            <div>
              <h3>Ticket History Summary</h3>
              <p class="panel-copy">Quick brief generated from ticket state, timeline, comments, and latest AI result.</p>
            </div>
          </div>

          <div *ngIf="historySummaryLoading" class="empty-box">Loading history summary...</div>

          <ng-container *ngIf="!historySummaryLoading && historySummary as hs">
            <div class="history-grid">
              <div class="content-box">
                <label>Current State</label>
                <p>{{ hs.currentState }}</p>
              </div>

              <div class="content-box">
                <label>Latest Meaningful Update</label>
                <p>{{ hs.latestMeaningfulUpdate }}</p>
              </div>

              <div class="content-box">
                <label>What Happened So Far</label>
                <p>{{ hs.whatHappened }}</p>
              </div>

              <div class="content-box">
                <label>Blockers and Risks</label>
                <p>{{ hs.blockersAndRisks }}</p>
              </div>

              <div class="content-box history-full">
                <label>Next Recommended Step</label>
                <p>{{ hs.nextRecommendedStep }}</p>
              </div>
            </div>
          </ng-container>
        </article>
      </section>

      <section class="feedback-section">
        <article class="panel">
          <div class="section-head">
            <div>
              <h3>Feedback Capture</h3>
              <p class="panel-copy">Stored review decisions and rewrite usage for this ticket.</p>
            </div>
          </div>

          <div *ngIf="feedbackLoading" class="empty-box">Loading feedback capture...</div>

          <div *ngIf="!feedbackLoading && !feedbackItems.length" class="empty-box">
            No AI feedback captured yet.
          </div>

          <div class="stack" *ngIf="!feedbackLoading && feedbackItems.length">
            <article class="item" *ngFor="let item of feedbackItems">
              <div class="item-head">
                <strong>{{ formatLabel(item.eventType) }}</strong>
                <span>{{ item.createdAt | date:'medium' }}</span>
              </div>

              <div class="article-meta">
                <span class="chip">{{ formatLabel(item.eventValue) }}</span>
                <span class="chip subtle" *ngIf="item.rewriteMode">{{ formatLabel(item.rewriteMode) }}</span>
                <span class="chip subtle" *ngIf="item.source">{{ formatSource(item.source) }}</span>
              </div>

              <p>{{ item.note || 'Feedback event recorded.' }}</p>
            </article>
          </div>
        </article>
      </section>

      <section class="workbench">
        <article class="panel">
          <div class="section-head">
            <h3>Ticket Actions</h3>
          </div>

          <div class="action-panels">
            <form class="mini-form" [formGroup]="statusForm" (ngSubmit)="saveStatus()">
              <label>Update Status</label>
              <select formControlName="status">
                <option value="OPEN">Open</option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="WAITING_FOR_CUSTOMER">Waiting for Customer</option>
                <option value="RESOLVED">Resolved</option>
                <option value="CLOSED">Closed</option>
              </select>
              <button class="button" type="submit" [disabled]="busy || statusForm.invalid">Save Status</button>
            </form>

            <form class="mini-form" [formGroup]="assignmentForm" (ngSubmit)="saveAssignment()">
              <label>Assign Team</label>
              <select formControlName="teamId">
                <option [ngValue]="null">No Team</option>
                <option *ngFor="let team of teams" [ngValue]="team.id">{{ team.name }}</option>
              </select>

              <label>Assign Agent</label>
              <select formControlName="assignedToId">
                <option [ngValue]="null">Unassigned</option>
                <option *ngFor="let user of assignableUsers" [ngValue]="user.id">
                  {{ user.fullName }}{{ user.teamName ? ' — ' + user.teamName : '' }}
                </option>
              </select>

              <button class="button" type="submit" [disabled]="busy">Save Assignment</button>
            </form>
          </div>
        </article>

        <article class="panel">
          <div class="section-head">
            <div>
              <h3>Similar Tickets</h3>
              <p class="panel-copy">Related past tickets based on AI category, team, and shared terms.</p>
            </div>
          </div>

          <div *ngIf="similarTicketsLoading" class="empty-box">Loading similar tickets...</div>

          <div *ngIf="!similarTicketsLoading && !similarTickets.length" class="empty-box">
            No strong matches yet.
          </div>

          <div *ngIf="!similarTicketsLoading && similarTickets.length" class="similar-stack">
            <a class="similar-item" *ngFor="let item of similarTickets" [routerLink]="['/tickets', item.id]">
              <div class="similar-head">
                <div class="similar-ticket">
                  <strong>{{ item.referenceCode }}</strong>
                  <span>{{ item.title }}</span>
                </div>
                <div class="score-badge">Score {{ item.similarityScore }}</div>
              </div>

              <div class="similar-meta">
                <span class="chip">{{ formatLabel(item.status) }}</span>
                <span class="chip">{{ formatLabel(item.priority) }}</span>
                <span class="chip subtle">{{ formatLabel(item.category) }}</span>
                <span class="chip subtle">{{ item.teamName || 'No Team' }}</span>
              </div>

              <p class="similar-reason">{{ item.similarityReason }}</p>
            </a>
          </div>
        </article>
      </section>

      <section class="bottom-grid">
        <article class="panel">
          <div class="section-head">
            <h3>Comments</h3>
          </div>

          <form class="comment-form" [formGroup]="commentForm" (ngSubmit)="submitComment()">
            <div class="field">
              <label>Author</label>
              <select formControlName="authorId">
                <option *ngFor="let user of users" [ngValue]="user.id">
                  {{ user.fullName }} ({{ user.role }})
                </option>
              </select>
            </div>

            <div class="field">
              <label>Comment</label>
              <textarea rows="4" formControlName="content" placeholder="Write a comment..."></textarea>
            </div>

            <label class="checkbox">
              <input type="checkbox" formControlName="internalNote" />
              Internal note
            </label>

            <button class="button" type="submit" [disabled]="busy || commentForm.invalid">Add Comment</button>
          </form>

          <div class="stack" *ngIf="comments.length; else noCommentsTpl">
            <article class="item" *ngFor="let comment of comments">
              <div class="item-head">
                <strong>{{ comment.authorName }}</strong>
                <span>{{ comment.createdAt | date:'medium' }}</span>
              </div>
              <div class="pill" *ngIf="comment.internalNote">Internal Note</div>
              <p>{{ comment.content }}</p>
            </article>
          </div>
        </article>

        <article class="panel">
          <div class="section-head">
            <h3>Event Timeline</h3>
          </div>

          <div class="timeline-tools" *ngIf="events.length > 8">
            <span class="timeline-note">
              Showing latest {{ displayedEvents.length }} event{{ displayedEvents.length === 1 ? '' : 's' }}
            </span>
            <button class="button muted small" type="button" (click)="showAllEvents = !showAllEvents">
              {{ showAllEvents ? 'Show Less' : 'Show All (' + events.length + ')' }}
            </button>
          </div>

          <div class="stack" *ngIf="events.length; else noEventsTpl">
            <article class="item" *ngFor="let event of displayedEvents">
              <div class="item-head">
                <strong>{{ formatLabel(event.type) }}</strong>
                <span>{{ event.createdAt | date:'medium' }}</span>
              </div>
              <div class="pill">{{ formatLabel(event.actorType) }}</div>
              <p>{{ event.details }}</p>
            </article>
          </div>
        </article>
      </section>

      <ng-template #noCommentsTpl>
        <div class="empty-box">No comments yet.</div>
      </ng-template>

      <ng-template #noEventsTpl>
        <div class="empty-box">No events yet.</div>
      </ng-template>

      <ng-template #noArticlesTpl>
        <div class="empty-box">No matched knowledge articles yet.</div>
      </ng-template>
    </section>

    <ng-template #loadingTpl>
      <div class="empty-box">Loading ticket...</div>
    </ng-template>
  `,
  styles: [`
    .page { display:grid; gap:18px; }
    .page-header { display:flex; justify-content:space-between; gap:16px; align-items:flex-start; }
    .eyebrow { text-transform:uppercase; letter-spacing:.08em; font-size:12px; color:#93c5fd; margin-bottom:8px; }
    h2 { margin:0 0 8px; font-size:34px; line-height:1; }
    .subtitle { margin:0; color:#cbd5e1; font-size:18px; }
    h3 { margin:0; font-size:20px; }
    .panel-copy { margin-top:6px; color:#9ca3af; }
    .header-actions { display:flex; gap:12px; flex-wrap:wrap; }
    .hero-grid { display:grid; grid-template-columns:1.35fr .95fr; gap:16px; }
    .bottom-grid { display:grid; grid-template-columns:1fr 1fr; gap:16px; }
    .workbench { display:grid; grid-template-columns:1fr 1fr; gap:16px; }
    .knowledge-section,.history-section,.feedback-section { display:grid; }
    .panel { background:linear-gradient(180deg, rgba(255,255,255,.05), rgba(255,255,255,.035)); border:1px solid rgba(255,255,255,.10); border-radius:22px; padding:22px; box-shadow:0 16px 40px rgba(0,0,0,.22); }
    .chip-row { display:flex; gap:10px; flex-wrap:wrap; margin-bottom:18px; }
    .badge,.chip,.pill,.score-badge,.confidence-chip { display:inline-flex; align-items:center; border-radius:999px; padding:7px 12px; font-size:12px; font-weight:700; letter-spacing:.01em; }
    .chip,.pill { background:rgba(255,255,255,.06); border:1px solid rgba(255,255,255,.10); color:#e5e7eb; }
    .score-badge { background:rgba(59,130,246,.16); border:1px solid rgba(59,130,246,.25); color:#e5e7eb; }
    .chip.subtle { background:rgba(255,255,255,.04); }
    .source-openai { background:rgba(16,185,129,.18)!important; border:1px solid rgba(16,185,129,.30)!important; }
    .source-rule { background:rgba(148,163,184,.16)!important; border:1px solid rgba(148,163,184,.28)!important; }
    .source-fallback { background:rgba(245,158,11,.18)!important; border:1px solid rgba(245,158,11,.30)!important; }
    .source-unknown { background:rgba(107,114,128,.22)!important; border:1px solid rgba(107,114,128,.32)!important; }

    .confidence-banner { display:grid; gap:8px; padding:14px 16px; border-radius:16px; margin-bottom:16px; border:1px solid rgba(255,255,255,.10); }
    .confidence-banner-top { display:flex; justify-content:space-between; gap:12px; align-items:center; }
    .confidence-banner p { margin:0; color:#e5e7eb; line-height:1.45; }
    .confidence-inline { display:flex; align-items:center; gap:8px; flex-wrap:wrap; }
    .confidence-high { background:rgba(34,197,94,.16); border:1px solid rgba(34,197,94,.30); color:#dcfce7; }
    .confidence-medium { background:rgba(245,158,11,.16); border:1px solid rgba(245,158,11,.30); color:#fde68a; }
    .confidence-low { background:rgba(239,68,68,.16); border:1px solid rgba(239,68,68,.30); color:#fecaca; }
    .review-warning { padding:12px 14px; border-radius:14px; margin-top:4px; background:rgba(239,68,68,.10); border:1px solid rgba(239,68,68,.22); color:#fecaca; line-height:1.5; }

    .status-open { background:rgba(59,130,246,.18); border:1px solid rgba(59,130,246,.30); }
    .status-progress { background:rgba(245,158,11,.18); border:1px solid rgba(245,158,11,.30); }
    .status-waiting { background:rgba(168,85,247,.18); border:1px solid rgba(168,85,247,.30); }
    .status-resolved { background:rgba(34,197,94,.18); border:1px solid rgba(34,197,94,.30); }
    .status-closed { background:rgba(107,114,128,.22); border:1px solid rgba(107,114,128,.32); }
    .priority-low { background:rgba(107,114,128,.22); border:1px solid rgba(107,114,128,.32); }
    .priority-medium { background:rgba(59,130,246,.18); border:1px solid rgba(59,130,246,.30); }
    .priority-high { background:rgba(249,115,22,.18); border:1px solid rgba(249,115,22,.30); }
    .priority-urgent { background:rgba(239,68,68,.18); border:1px solid rgba(239,68,68,.30); }

    .summary-grid,.history-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:14px; margin-bottom:18px; }
    .summary-card { padding:14px; border-radius:16px; background:rgba(255,255,255,.03); border:1px solid rgba(255,255,255,.06); }
    .history-full { grid-column:1 / -1; }

    label { display:block; color:#9ca3af; margin-bottom:6px; font-size:13px; }
    .description-block,.content-box { padding:16px; border-radius:16px; background:rgba(255,255,255,.03); border:1px solid rgba(255,255,255,.06); }
    .description-block p,.content-box p,.item p,.similar-reason,.article-copy,.article-reason { margin:0; white-space:pre-wrap; line-height:1.65; color:#e5e7eb; }

    .section-head { display:flex; justify-content:space-between; gap:12px; align-items:flex-start; margin-bottom:16px; }
    .info-row,.item-head,.similar-head,.article-head { display:flex; justify-content:space-between; gap:12px; align-items:flex-start; margin-bottom:12px; }
    .action-row,.action-panels,.rewrite-tools,.rewrite-meta,.timeline-tools { display:flex; gap:10px; flex-wrap:wrap; }
    .action-panels { align-items:stretch; }
    .mini-form { flex:1 1 280px; display:grid; gap:10px; padding:16px; border-radius:16px; background:rgba(255,255,255,.03); border:1px solid rgba(255,255,255,.06); }
    .comment-form { display:grid; gap:14px; margin-bottom:18px; }
    .field { display:grid; gap:8px; }

    .article-stack,.similar-stack,.stack { display:grid; gap:12px; margin-top:16px; }
    .article-item,.similar-item,.item { display:grid; gap:12px; padding:16px; border-radius:16px; background:rgba(255,255,255,.03); border:1px solid rgba(255,255,255,.06); color:inherit; }
    .similar-item { text-decoration:none; transition:.2s ease; }
    .similar-item:hover { background:rgba(59,130,246,.08); border-color:rgba(59,130,246,.22); }
    .article-id { color:#93c5fd; font-size:12px; margin-top:4px; }
    .article-meta,.similar-meta { display:flex; gap:8px; flex-wrap:wrap; }
    .similar-ticket { display:grid; gap:4px; }
    .similar-ticket strong,.article-item strong { color:#f8fafc; }
    .similar-ticket span { color:#cbd5e1; }

    input,textarea,select { width:100%; box-sizing:border-box; background:#0b1220; color:#e5e7eb; border:1px solid rgba(255,255,255,.12); border-radius:12px; padding:12px; font:inherit; }
    textarea { resize:vertical; min-height:120px; }
    .checkbox { display:flex; align-items:center; gap:10px; color:#cbd5e1; margin:0; }
    .checkbox input { width:auto; }

    .button { display:inline-flex; align-items:center; justify-content:center; min-height:44px; padding:0 16px; border-radius:14px; text-decoration:none; border:1px solid rgba(59,130,246,.35); background:rgba(59,130,246,.18); color:#fff; cursor:pointer; font-weight:600; }
    .button.small { min-height:38px; padding:0 12px; font-size:13px; }
    .timeline-tools { justify-content:space-between; align-items:center; margin-bottom:12px; }
    .timeline-note { color:#9ca3af; font-size:13px; }
    .button.muted { background:rgba(255,255,255,.04); border-color:rgba(255,255,255,.10); }
    .button.accept { background:rgba(34,197,94,.18); border-color:rgba(34,197,94,.30); }
    .button.reject { background:rgba(239,68,68,.18); border-color:rgba(239,68,68,.30); }
    .button:disabled { opacity:.6; cursor:not-allowed; }
    .empty-box { padding:16px; border-radius:16px; background:rgba(255,255,255,.03); border:1px solid rgba(255,255,255,.06); color:#cbd5e1; }

    @media (max-width:1200px) {
      .hero-grid,.bottom-grid,.summary-grid,.history-grid,.workbench { grid-template-columns:1fr; }
      .history-full { grid-column:auto; }
    }
  `]
})
export class TicketDetailComponent implements OnInit {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);
  private toast = inject(ToastService);

  ticketId = 0;
  ticket: TicketResponse | null = null;
  comments: CommentResponse[] = [];
  events: EventResponse[] = [];
  similarTickets: SimilarTicketResponse[] = [];
  similarTicketsLoading = true;
  knowledgeGuidance: KnowledgeGuidanceResponse | null = null;
  knowledgeLoading = true;
  historySummary: TicketHistorySummaryResponse | null = null;
  historySummaryLoading = true;
  feedbackItems: TicketFeedbackResponse[] = [];
  feedbackLoading = true;
  users: UserListItemResponse[] = [];
  teams: TeamListItemResponse[] = [];
  ai: AIRecommendationResponse | null = null;
  rewrittenDraft: RewriteDraftReplyResponse | null = null;
  rewriteBusy = false;
  showAllEvents = false;
  busy = false;

  commentForm = this.fb.group({
    authorId: [5, Validators.required],
    content: ['', Validators.required],
    internalNote: [false]
  });

  statusForm = this.fb.group({
    status: ['OPEN', Validators.required]
  });

  assignmentForm = this.fb.group({
    teamId: [null as number | null],
    assignedToId: [null as number | null]
  });

  get assignableUsers(): UserListItemResponse[] {
    return this.users.filter(user => user.role === 'AGENT' || user.role === 'ADMIN');
  }

  get displayedEvents(): EventResponse[] {
    const ordered = [...this.events].reverse();
    return this.showAllEvents ? ordered : ordered.slice(0, 8);
  }

  get hiddenEventCount(): number {
    return Math.max(this.events.length - 8, 0);
  }

  ngOnInit(): void {
    this.api.getUsers().subscribe({
      next: (users) => {
        this.users = users;
        this.cdr.markForCheck();
      },
      error: () => {
        this.toast.error('Unable to load users.');
        this.cdr.markForCheck();
      }
    });

    this.api.getTeams().subscribe({
      next: (teams) => {
        this.teams = teams;
        this.cdr.markForCheck();
      },
      error: () => {
        this.toast.error('Unable to load teams.');
        this.cdr.markForCheck();
      }
    });

    this.route.paramMap.subscribe(params => {
      const id = Number(params.get('id'));
      if (!id) return;
      this.ticketId = id;
      this.ai = null;
      this.rewrittenDraft = null;
      this.loadAll();
    });
  }

  loadAll(): void {
    this.similarTicketsLoading = true;
    this.knowledgeLoading = true;
    this.historySummaryLoading = true;
    this.feedbackLoading = true;
    this.feedbackLoading = true;

    this.api.getTicket(this.ticketId).subscribe({
      next: (ticket) => {
        this.ticket = ticket;
        this.statusForm.patchValue({
          status: ticket.status
        });
        this.assignmentForm.patchValue({
          teamId: ticket.teamId,
          assignedToId: ticket.assignedToId
        });
        this.cdr.markForCheck();
      },
      error: () => {
        this.toast.error('Unable to load ticket details.');
        this.cdr.markForCheck();
      }
    });

    this.api.getTicketComments(this.ticketId).subscribe({
      next: (comments) => {
        this.comments = comments;
        this.cdr.markForCheck();
      },
      error: () => {
        this.toast.error('Unable to load comments.');
        this.cdr.markForCheck();
      }
    });

    this.api.getTicketEvents(this.ticketId).subscribe({
      next: (events) => {
        this.events = events;
        this.cdr.markForCheck();
      },
      error: () => {
        this.toast.error('Unable to load event timeline.');
        this.cdr.markForCheck();
      }
    });

    this.api.getLatestAiRecommendation(this.ticketId).subscribe({
      next: (ai) => {
        this.ai = ai;
        this.cdr.markForCheck();
      },
      error: () => {
        this.ai = null;
        this.cdr.markForCheck();
      }
    });

    this.api.getSimilarTickets(this.ticketId).subscribe({
      next: (items) => {
        this.similarTickets = items;
        this.similarTicketsLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.similarTickets = [];
        this.similarTicketsLoading = false;
        this.cdr.markForCheck();
      }
    });

    this.api.getKnowledgeGuidance(this.ticketId).subscribe({
      next: (guidance) => {
        this.knowledgeGuidance = guidance;
        this.knowledgeLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.knowledgeGuidance = null;
        this.knowledgeLoading = false;
        this.toast.error('Unable to load knowledge guidance.');
        this.cdr.markForCheck();
      }
    });

    this.api.getTicketHistorySummary(this.ticketId).subscribe({
      next: (summary) => {
        this.historySummary = summary;
        this.historySummaryLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.historySummary = null;
        this.historySummaryLoading = false;
        this.toast.error('Unable to load ticket history summary.');
        this.cdr.markForCheck();
      }
    });

    this.api.getTicketFeedback(this.ticketId).subscribe({
      next: (items) => {
        this.feedbackItems = items;
        this.feedbackLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.feedbackItems = [];
        this.feedbackLoading = false;
        this.toast.error('Unable to load feedback capture.');
        this.cdr.markForCheck();
      }
    });

    this.api.getTicketFeedback(this.ticketId).subscribe({
      next: (items) => {
        this.feedbackItems = items;
        this.feedbackLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.feedbackItems = [];
        this.feedbackLoading = false;
        this.toast.error('Unable to load feedback capture.');
        this.cdr.markForCheck();
      }
    });
  }

  analyze(): void {
    this.busy = true;
    this.rewrittenDraft = null;
    this.cdr.markForCheck();

    this.api.analyzeTicket(this.ticketId).subscribe({
      next: (ai) => {
        this.ai = ai;
        this.busy = false;
        this.toast.success(`AI analysis generated via ${this.formatSource(ai.analysisSource)}.`);
        this.loadAll();
        this.cdr.markForCheck();
      },
      error: () => {
        this.busy = false;
        this.toast.error('AI analysis failed.');
        this.cdr.markForCheck();
      }
    });
  }

  rewriteDraft(mode: string): void {
    if (!this.ai) {
      return;
    }

    this.rewriteBusy = true;
    this.cdr.markForCheck();

    this.api.rewriteAiDraft(this.ticketId, {
      text: this.ai.draftReply,
      mode
    }).subscribe({
      next: (response: RewriteDraftReplyResponse) => {
        this.rewrittenDraft = response;
        this.rewriteBusy = false;
        this.toast.success('Reply rewrite generated.');
        this.cdr.markForCheck();
      },
      error: () => {
        this.rewriteBusy = false;
        this.toast.error('Failed to rewrite the draft reply.');
        this.cdr.markForCheck();
      }
    });
  }

  review(action: 'ACCEPT' | 'REJECT'): void {
    this.busy = true;
    this.cdr.markForCheck();

    this.api.reviewAi(this.ticketId, {
      reviewedById: 1,
      action
    }).subscribe({
      next: (ai) => {
        this.ai = ai;
        this.busy = false;
        this.toast.success(action === 'ACCEPT' ? 'AI recommendation accepted.' : 'AI recommendation rejected.');
        this.loadAll();
        this.cdr.markForCheck();
      },
      error: () => {
        this.busy = false;
        this.toast.error('Failed to review AI recommendation.');
        this.cdr.markForCheck();
      }
    });
  }

  saveStatus(): void {
    if (this.statusForm.invalid) {
      this.statusForm.markAllAsTouched();
      this.toast.error('Please choose a valid status.');
      this.cdr.markForCheck();
      return;
    }

    this.busy = true;
    this.cdr.markForCheck();

    this.api.updateTicketStatus(this.ticketId, {
      status: String(this.statusForm.getRawValue().status ?? 'OPEN')
    }).subscribe({
      next: (ticket) => {
        this.ticket = ticket;
        this.busy = false;
        this.toast.success('Ticket status updated.');
        this.loadAll();
        this.cdr.markForCheck();
      },
      error: () => {
        this.busy = false;
        this.toast.error('Failed to update ticket status.');
        this.cdr.markForCheck();
      }
    });
  }

  saveAssignment(): void {
    const value = this.assignmentForm.getRawValue();
    this.busy = true;
    this.cdr.markForCheck();

    this.api.assignTicket(this.ticketId, {
      teamId: value.teamId ?? null,
      assignedToId: value.assignedToId ?? null
    }).subscribe({
      next: (ticket) => {
        this.ticket = ticket;
        this.busy = false;
        this.toast.success('Ticket assignment saved.');
        this.loadAll();
        this.cdr.markForCheck();
      },
      error: () => {
        this.busy = false;
        this.toast.error('Failed to save assignment.');
        this.cdr.markForCheck();
      }
    });
  }

  submitComment(): void {
    if (this.commentForm.invalid) {
      this.commentForm.markAllAsTouched();
      this.toast.error('Please enter a comment before submitting.');
      this.cdr.markForCheck();
      return;
    }

    const value = this.commentForm.getRawValue();
    this.busy = true;
    this.cdr.markForCheck();

    this.api.addComment(this.ticketId, {
      authorId: Number(value.authorId),
      content: String(value.content ?? ''),
      internalNote: Boolean(value.internalNote)
    }).subscribe({
      next: () => {
        this.commentForm.patchValue({
          content: '',
          internalNote: false
        });
        this.busy = false;
        this.toast.success('Comment added.');
        this.loadAll();
        this.cdr.markForCheck();
      },
      error: () => {
        this.busy = false;
        this.toast.error('Failed to add comment.');
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

  formatSource(value: string | null | undefined): string {
    switch (value) {
      case 'OPENAI': return 'OpenAI';
      case 'RULE': return 'Rule Engine';
      case 'RULE_FALLBACK': return 'Rule Fallback';
      case 'OLLAMA': return 'Local Ollama';
      default: return 'Unknown';
    }
  }

  sourceClass(value: string | null | undefined): string {
    switch (value) {
      case 'OPENAI': return 'source-openai';
      case 'RULE': return 'source-rule';
      case 'RULE_FALLBACK': return 'source-fallback';
      case 'OLLAMA': return 'source-openai';
      default: return 'source-unknown';
    }
  }

  confidenceLabel(score: number | null | undefined): string {
    if (score == null) return 'Unknown confidence';
    if (score >= 0.9) return 'High confidence';
    if (score >= 0.75) return 'Medium confidence';
    return 'Low confidence';
  }

  confidenceShortLabel(score: number | null | undefined): string {
    if (score == null) return 'Unknown';
    if (score >= 0.9) return 'High';
    if (score >= 0.75) return 'Medium';
    return 'Low';
  }

  confidenceGuidance(score: number | null | undefined): string {
    if (score == null) return 'No confidence signal is available for this recommendation.';
    if (score >= 0.9) return 'This recommendation is strong. Review quickly, then apply if it matches the ticket context.';
    if (score >= 0.75) return 'This recommendation looks reasonable, but it should be checked before acceptance.';
    return 'This recommendation needs human review. Compare similar tickets and inspect the issue carefully before accepting.';
  }

  confidenceClass(score: number | null | undefined): string {
    if (score == null) return 'confidence-medium';
    if (score >= 0.9) return 'confidence-high';
    if (score >= 0.75) return 'confidence-medium';
    return 'confidence-low';
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
}








