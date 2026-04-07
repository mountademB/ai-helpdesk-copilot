package com.mountadem.helpdesk.ticket.controller;

import com.mountadem.helpdesk.ai.dto.AIRecommendationResponse;
import com.mountadem.helpdesk.ai.dto.ReviewAIRecommendationRequest;
import com.mountadem.helpdesk.ai.dto.RewriteDraftReplyRequest;
import com.mountadem.helpdesk.ai.dto.RewriteDraftReplyResponse;
import com.mountadem.helpdesk.ai.service.AgentRewriteService;
import com.mountadem.helpdesk.comment.dto.AddCommentRequest;
import com.mountadem.helpdesk.comment.dto.CommentResponse;
import com.mountadem.helpdesk.event.dto.EventResponse;
import com.mountadem.helpdesk.feedback.dto.TicketFeedbackResponse;
import com.mountadem.helpdesk.feedback.service.FeedbackCaptureService;
import com.mountadem.helpdesk.knowledge.dto.KnowledgeGuidanceResponse;
import com.mountadem.helpdesk.ticket.dto.AssignTicketRequest;
import com.mountadem.helpdesk.ticket.dto.CreateTicketRequest;
import com.mountadem.helpdesk.ticket.dto.SimilarTicketResponse;
import com.mountadem.helpdesk.ticket.dto.TicketHistorySummaryResponse;
import com.mountadem.helpdesk.ticket.dto.TicketResponse;
import com.mountadem.helpdesk.ticket.dto.UpdateTicketStatusRequest;
import com.mountadem.helpdesk.ticket.service.TicketHistorySummaryService;
import com.mountadem.helpdesk.ticket.service.TicketService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final TicketHistorySummaryService ticketHistorySummaryService;
    private final AgentRewriteService agentRewriteService;
    private final FeedbackCaptureService feedbackCaptureService;

    @PostMapping
    public TicketResponse create(@RequestBody CreateTicketRequest request) {
        return ticketService.create(request);
    }

    @GetMapping
    public List<TicketResponse> list() {
        return ticketService.listTickets();
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id) {
        return ticketService.getTicketResponse(id);
    }

    @GetMapping("/{id}/similar")
    public List<SimilarTicketResponse> similar(@PathVariable Long id) {
        return ticketService.listSimilarTickets(id);
    }

    @GetMapping("/{id}/knowledge-guidance")
    public KnowledgeGuidanceResponse knowledgeGuidance(@PathVariable Long id) {
        return ticketService.getKnowledgeGuidance(id);
    }

    @GetMapping("/{id}/history-summary")
    public TicketHistorySummaryResponse historySummary(@PathVariable Long id) {
        return ticketHistorySummaryService.build(id);
    }

    @GetMapping("/{id}/feedback")
    public List<TicketFeedbackResponse> feedback(@PathVariable Long id) {
        return feedbackCaptureService.listForTicket(id);
    }

    @PutMapping("/{id}/status")
    public TicketResponse updateStatus(@PathVariable Long id, @RequestBody UpdateTicketStatusRequest request) {
        return ticketService.updateStatus(id, request);
    }

    @PutMapping("/{id}/assign")
    public TicketResponse assign(@PathVariable Long id, @RequestBody AssignTicketRequest request) {
        return ticketService.assign(id, request);
    }

    @PostMapping("/{id}/comments")
    public CommentResponse addComment(@PathVariable Long id, @RequestBody AddCommentRequest request) {
        return ticketService.addComment(id, request);
    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> comments(@PathVariable Long id) {
        return ticketService.listComments(id);
    }

    @GetMapping("/{id}/events")
    public List<EventResponse> events(@PathVariable Long id) {
        return ticketService.listEvents(id);
    }

    @PostMapping("/{id}/analyze")
    public AIRecommendationResponse analyze(@PathVariable Long id) {
        return ticketService.analyze(id);
    }

    @GetMapping("/{id}/ai/latest")
    public AIRecommendationResponse latestAi(@PathVariable Long id) {
        return ticketService.getLatestRecommendation(id);
    }

    @PostMapping("/{id}/ai/review")
    public AIRecommendationResponse review(@PathVariable Long id, @RequestBody ReviewAIRecommendationRequest request) {
        return ticketService.reviewLatestRecommendation(id, request);
    }

    @PostMapping("/{id}/ai/rewrite")
    public RewriteDraftReplyResponse rewrite(@PathVariable Long id, @RequestBody RewriteDraftReplyRequest request) {
        return agentRewriteService.rewrite(id, request);
    }
}
