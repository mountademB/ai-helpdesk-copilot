package com.mountadem.helpdesk.feedback.service;

import com.mountadem.helpdesk.ai.entity.AIRecommendation;
import com.mountadem.helpdesk.feedback.dto.TicketFeedbackResponse;
import com.mountadem.helpdesk.feedback.entity.AIFeedbackEvent;
import com.mountadem.helpdesk.feedback.repository.AIFeedbackEventRepository;
import com.mountadem.helpdesk.ticket.entity.Ticket;
import com.mountadem.helpdesk.ticket.repository.TicketRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class FeedbackCaptureService {

    private final AIFeedbackEventRepository feedbackEventRepository;
    private final TicketRepository ticketRepository;

    @Transactional
    public void recordReview(Ticket ticket, AIRecommendation recommendation, String eventValue, Long actorId) {
        AIFeedbackEvent event = new AIFeedbackEvent();
        event.setTicket(ticket);
        event.setRecommendation(recommendation);
        event.setEventType("REVIEW_DECISION");
        event.setEventValue(eventValue);
        event.setSource(recommendation != null ? recommendation.getAnalysisSource() : null);
        event.setActorId(actorId);
        event.setNote("Agent reviewed the AI recommendation.");
        feedbackEventRepository.save(event);
    }

    @Transactional
    public void recordRewrite(Ticket ticket, AIRecommendation recommendation, String rewriteMode, String source) {
        AIFeedbackEvent event = new AIFeedbackEvent();
        event.setTicket(ticket);
        event.setRecommendation(recommendation);
        event.setEventType("REWRITE_USED");
        event.setEventValue("GENERATED");
        event.setRewriteMode(rewriteMode);
        event.setSource(source);
        event.setNote("Agent requested a rewritten draft reply.");
        feedbackEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<TicketFeedbackResponse> listForTicket(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found.");
        }

        return feedbackEventRepository.findByTicketIdOrderByCreatedAtDesc(ticketId)
                .stream()
                .map(event -> new TicketFeedbackResponse(
                        event.getId(),
                        event.getEventType(),
                        event.getEventValue(),
                        event.getRewriteMode(),
                        event.getSource(),
                        event.getActorId(),
                        event.getNote(),
                        event.getCreatedAt()
                ))
                .toList();
    }
}
