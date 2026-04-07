package com.mountadem.helpdesk.feedback.dto;

import java.time.Instant;

public record TicketFeedbackResponse(
        Long id,
        String eventType,
        String eventValue,
        String rewriteMode,
        String source,
        Long actorId,
        String note,
        Instant createdAt
) {
}
