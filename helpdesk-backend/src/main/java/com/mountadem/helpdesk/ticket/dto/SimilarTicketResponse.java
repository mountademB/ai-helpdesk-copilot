package com.mountadem.helpdesk.ticket.dto;

import java.time.Instant;

public record SimilarTicketResponse(
        Long id,
        String referenceCode,
        String title,
        String status,
        String priority,
        String category,
        String assignedToName,
        String teamName,
        String similarityReason,
        Integer similarityScore,
        Instant createdAt
) {
}
