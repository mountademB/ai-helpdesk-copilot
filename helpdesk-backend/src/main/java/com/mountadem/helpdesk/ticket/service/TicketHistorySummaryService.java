package com.mountadem.helpdesk.ticket.service;

import com.mountadem.helpdesk.ai.entity.AIRecommendation;
import com.mountadem.helpdesk.ai.repository.AIRecommendationRepository;
import com.mountadem.helpdesk.comment.entity.TicketComment;
import com.mountadem.helpdesk.comment.repository.TicketCommentRepository;
import com.mountadem.helpdesk.event.entity.TicketEvent;
import com.mountadem.helpdesk.event.repository.TicketEventRepository;
import com.mountadem.helpdesk.ticket.dto.TicketHistorySummaryResponse;
import com.mountadem.helpdesk.ticket.entity.Ticket;
import com.mountadem.helpdesk.ticket.repository.TicketRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketHistorySummaryService {

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final TicketEventRepository ticketEventRepository;
    private final AIRecommendationRepository aiRecommendationRepository;

    public TicketHistorySummaryResponse build(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found."));

        List<TicketComment> comments = ticketCommentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
        List<TicketEvent> events = ticketEventRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
        AIRecommendation latestAi = aiRecommendationRepository.findFirstByTicketIdOrderByCreatedAtDesc(ticketId).orElse(null);

        return new TicketHistorySummaryResponse(
                buildCurrentState(ticket),
                buildWhatHappened(ticket, comments, events, latestAi),
                buildLatestMeaningfulUpdate(comments, events),
                buildBlockersAndRisks(ticket, comments, latestAi),
                buildNextRecommendedStep(ticket, latestAi)
        );
    }

    private String buildCurrentState(Ticket ticket) {
        return "Ticket is " + formatLabel(ticket.getStatus().name())
                + " with " + formatLabel(ticket.getPriority().name())
                + " priority. Assigned to "
                + (ticket.getAssignedTo() != null ? ticket.getAssignedTo().getFullName() : "Unassigned")
                + ". Team: "
                + (ticket.getTeam() != null ? ticket.getTeam().getName() : "No Team")
                + ".";
    }

    private String buildWhatHappened(
            Ticket ticket,
            List<TicketComment> comments,
            List<TicketEvent> events,
            AIRecommendation latestAi
    ) {
        List<String> lines = new ArrayList<>();

        lines.add("1. Ticket opened by " + ticket.getCreatedBy().getFullName() + ".");

        if (!events.isEmpty()) {
            TicketEvent latestEvent = events.get(events.size() - 1);
            lines.add("2. Latest workflow event: " + safeText(latestEvent.getDetails(), formatLabel(latestEvent.getType().name())) + ".");
        } else {
            lines.add("2. No workflow events have been recorded yet beyond the initial ticket state.");
        }

        if (!comments.isEmpty()) {
            lines.add("3. " + comments.size() + " comment" + (comments.size() == 1 ? " has" : "s have") + " been added.");
        } else {
            lines.add("3. No comments have been added yet.");
        }

        if (latestAi != null) {
            lines.add("4. AI analyzed the ticket via "
                    + safeText(latestAi.getAnalysisSource(), "Unknown source")
                    + " and suggested "
                    + formatLabel(latestAi.getPredictedCategory().name())
                    + " / "
                    + formatLabel(latestAi.getPredictedPriority().name())
                    + ".");
        } else {
            lines.add("4. No AI recommendation has been generated yet.");
        }

        return String.join("`n", lines);
    }

    private String buildLatestMeaningfulUpdate(List<TicketComment> comments, List<TicketEvent> events) {
        TicketComment latestComment = comments.isEmpty() ? null : comments.get(comments.size() - 1);
        TicketEvent latestEvent = events.isEmpty() ? null : events.get(events.size() - 1);

        if (latestComment == null && latestEvent == null) {
            return "No follow-up activity is available yet.";
        }

        Instant latestCommentAt = latestComment != null ? latestComment.getCreatedAt() : null;
        Instant latestEventAt = latestEvent != null ? latestEvent.getCreatedAt() : null;

        if (latestComment != null && (latestEventAt == null || latestCommentAt.isAfter(latestEventAt))) {
            return "Latest meaningful update is a comment from "
                    + latestComment.getAuthor().getFullName()
                    + ": "
                    + excerpt(latestComment.getContent());
        }

        return "Latest meaningful update is the event: "
                + safeText(latestEvent.getDetails(), formatLabel(latestEvent.getType().name()))
                + ".";
    }

    private String buildBlockersAndRisks(Ticket ticket, List<TicketComment> comments, AIRecommendation latestAi) {
        List<String> blockers = new ArrayList<>();

        if (ticket.getAssignedTo() == null) {
            blockers.add("No assignee is set yet.");
        }

        if ("WAITING_FOR_CUSTOMER".equals(ticket.getStatus().name())) {
            blockers.add("Resolution is blocked until the customer responds.");
        }

        if ("HIGH".equals(ticket.getPriority().name()) || "URGENT".equals(ticket.getPriority().name())) {
            blockers.add("This ticket is high priority and may require faster follow-up.");
        }

        if (latestAi != null && latestAi.getConfidenceScore() != null && latestAi.getConfidenceScore() < 0.75) {
            blockers.add("The latest AI recommendation is low confidence and needs careful human review.");
        }

        if (comments.isEmpty()) {
            blockers.add("There are no investigation notes yet.");
        }

        if (blockers.isEmpty()) {
            return "No major blockers are visible from the current history.";
        }

        return String.join(" ", blockers);
    }

    private String buildNextRecommendedStep(Ticket ticket, AIRecommendation latestAi) {
        switch (ticket.getStatus()) {
            case OPEN:
                return preferredAction(latestAi, "Assign an owner and complete first-line triage.");
            case IN_PROGRESS:
                return preferredAction(latestAi, "Continue investigation and document the next finding.");
            case WAITING_FOR_CUSTOMER:
                return "Follow up with the customer for the missing information and set a reminder.";
            case RESOLVED:
                return "Confirm the resolution with the customer, then close the ticket if verified.";
            case CLOSED:
                return "No further action is needed unless the issue is reopened.";
            default:
                return "Review the ticket and determine the next owner action.";
        }
    }

    private String preferredAction(AIRecommendation latestAi, String fallback) {
        if (latestAi == null || latestAi.getRecommendedActions() == null || latestAi.getRecommendedActions().isBlank()) {
            return fallback;
        }

        String firstLine = latestAi.getRecommendedActions()
                .split("\\R")[0]
                .replaceFirst("^\\d+\\.\\s*", "")
                .trim();

        if (firstLine.isBlank()) {
            return fallback;
        }

        return firstLine.endsWith(".") ? firstLine : firstLine + ".";
    }

    private String formatLabel(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown";
        }

        String[] parts = value.toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isBlank()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }

        return builder.toString();
    }

    private String excerpt(String value) {
        if (value == null || value.isBlank()) {
            return "No details provided.";
        }

        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() > 140 ? normalized.substring(0, 137) + "..." : normalized;
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
