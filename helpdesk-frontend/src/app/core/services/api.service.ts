import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UserListItemResponse {
  id: number;
  fullName: string;
  email: string;
  role: string;
  teamId: number | null;
  teamName: string | null;
  active: boolean;
}

export interface TeamListItemResponse {
  id: number;
  name: string;
  description: string;
}

export interface TicketResponse {
  id: number;
  referenceCode: string;
  title: string;
  description: string;
  status: string;
  priority: string;
  category: string;
  createdById: number;
  createdByName: string;
  assignedToId: number | null;
  assignedToName: string | null;
  teamId: number | null;
  teamName: string | null;
  aiAnalyzed: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SimilarTicketResponse {
  id: number;
  referenceCode: string;
  title: string;
  status: string;
  priority: string;
  category: string;
  assignedToName: string | null;
  teamName: string | null;
  similarityReason: string;
  similarityScore: number;
  createdAt: string;
}

export interface CommentResponse {
  id: number;
  authorId: number;
  authorName: string;
  content: string;
  internalNote: boolean;
  createdAt: string;
}

export interface EventResponse {
  id: number;
  type: string;
  actorType: string;
  actorId: number | null;
  details: string;
  createdAt: string;
}

export interface KnowledgeArticleResponse {
  articleId: string;
  title: string;
  category: string;
  teamName: string;
  relevanceReason: string;
  relevanceScore: number;
  summary: string;
  recommendedChecks: string;
  escalationNotes: string;
}

export interface KnowledgeGuidanceResponse {
  guidanceSummary: string;
  recommendedChecks: string;
  escalationGuidance: string;
  articles: KnowledgeArticleResponse[];
}

export interface TicketHistorySummaryResponse {
  currentState: string;
  whatHappened: string;
  latestMeaningfulUpdate: string;
  blockersAndRisks: string;
  nextRecommendedStep: string;
}

export interface TicketFeedbackResponse {
  id: number;
  eventType: string;
  eventValue: string;
  rewriteMode: string | null;
  source: string | null;
  actorId: number | null;
  note: string | null;
  createdAt: string;
}

export interface RewriteDraftReplyResponse {
  mode: string;
  rewrittenText: string;
  source: string;
}

export interface AIRecommendationResponse {
  id: number;
  predictedCategory: string;
  predictedPriority: string;
  suggestedTeamId: number | null;
  suggestedTeamName: string | null;
  summary: string;
  draftReply: string;
  confidenceScore: number;
  probableCause: string | null;
  recommendedActions: string | null;
  escalationSuggestion: string | null;
  analysisSource: string | null;
  reviewStatus: string;
  reviewedById: number | null;
  reviewedAt: string | null;
  createdAt: string;
}

export interface DashboardSummaryResponse {
  totalTickets: number;
  openTickets: number;
  inProgressTickets: number;
  waitingForCustomerTickets: number;
  resolvedTickets: number;
  closedTickets: number;
  aiAnalyzedTickets: number;
}

export interface DashboardAiMetricsResponse {
  totalAnalyses: number;
  acceptedRecommendations: number;
  rejectedRecommendations: number;
  rewriteCount: number;
  fallbackAnalyses: number;
  acceptanceRate: number;
}

export interface RecentActivityResponse {
  ticketId: number;
  ticketReferenceCode: string;
  ticketTitle: string;
  eventType: string;
  actorType: string;
  details: string;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private http = inject(HttpClient);
  private readonly baseUrl = '/api';

  getUsers(): Observable<UserListItemResponse[]> {
    return this.http.get<UserListItemResponse[]>(`${this.baseUrl}/users`);
  }

  getTeams(): Observable<TeamListItemResponse[]> {
    return this.http.get<TeamListItemResponse[]>(`${this.baseUrl}/teams`);
  }

  getDashboardSummary(): Observable<DashboardSummaryResponse> {
    return this.http.get<DashboardSummaryResponse>(`${this.baseUrl}/dashboard/summary`);
  }

  getDashboardAiMetrics(): Observable<DashboardAiMetricsResponse> {
    return this.http.get<DashboardAiMetricsResponse>(`${this.baseUrl}/dashboard/ai-metrics`);
  }

  getRecentActivity(): Observable<RecentActivityResponse[]> {
    return this.http.get<RecentActivityResponse[]>(`${this.baseUrl}/dashboard/recent-activity`);
  }

  getTickets(): Observable<TicketResponse[]> {
    return this.http.get<TicketResponse[]>(`${this.baseUrl}/tickets`);
  }

  getTicket(id: number): Observable<TicketResponse> {
    return this.http.get<TicketResponse>(`${this.baseUrl}/tickets/${id}`);
  }

  getSimilarTickets(id: number): Observable<SimilarTicketResponse[]> {
    return this.http.get<SimilarTicketResponse[]>(`${this.baseUrl}/tickets/${id}/similar`);
  }

  getKnowledgeGuidance(id: number): Observable<KnowledgeGuidanceResponse> {
    return this.http.get<KnowledgeGuidanceResponse>(`${this.baseUrl}/tickets/${id}/knowledge-guidance`);
  }

  getTicketHistorySummary(id: number): Observable<TicketHistorySummaryResponse> {
    return this.http.get<TicketHistorySummaryResponse>(`${this.baseUrl}/tickets/${id}/history-summary`);
  }

  getTicketFeedback(id: number): Observable<TicketFeedbackResponse[]> {
    return this.http.get<TicketFeedbackResponse[]>(`${this.baseUrl}/tickets/${id}/feedback`);
  }

  createTicket(payload: { createdById: number; title: string; description: string }): Observable<TicketResponse> {
    return this.http.post<TicketResponse>(`${this.baseUrl}/tickets`, payload);
  }

  updateTicketStatus(id: number, payload: { status: string }): Observable<TicketResponse> {
    return this.http.put<TicketResponse>(`${this.baseUrl}/tickets/${id}/status`, payload);
  }

  assignTicket(id: number, payload: { assignedToId: number | null; teamId: number | null }): Observable<TicketResponse> {
    return this.http.put<TicketResponse>(`${this.baseUrl}/tickets/${id}/assign`, payload);
  }

  getTicketComments(id: number): Observable<CommentResponse[]> {
    return this.http.get<CommentResponse[]>(`${this.baseUrl}/tickets/${id}/comments`);
  }

  getTicketEvents(id: number): Observable<EventResponse[]> {
    return this.http.get<EventResponse[]>(`${this.baseUrl}/tickets/${id}/events`);
  }

  getLatestAiRecommendation(id: number): Observable<AIRecommendationResponse> {
    return this.http.get<AIRecommendationResponse>(`${this.baseUrl}/tickets/${id}/ai/latest`);
  }

  addComment(id: number, payload: { authorId: number; content: string; internalNote: boolean }): Observable<CommentResponse> {
    return this.http.post<CommentResponse>(`${this.baseUrl}/tickets/${id}/comments`, payload);
  }

  analyzeTicket(id: number): Observable<AIRecommendationResponse> {
    return this.http.post<AIRecommendationResponse>(`${this.baseUrl}/tickets/${id}/analyze`, {});
  }

  reviewAi(id: number, payload: { reviewedById: number; action: 'ACCEPT' | 'REJECT' }): Observable<AIRecommendationResponse> {
    return this.http.post<AIRecommendationResponse>(`${this.baseUrl}/tickets/${id}/ai/review`, payload);
  }

  rewriteAiDraft(id: number, payload: { text: string; mode: string }): Observable<RewriteDraftReplyResponse> {
    return this.http.post<RewriteDraftReplyResponse>(`${this.baseUrl}/tickets/${id}/ai/rewrite`, payload);
  }
}
