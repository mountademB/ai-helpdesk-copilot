package com.mountadem.helpdesk.ticket.dto;

import java.time.Instant;

public record TicketResponse(
        Long id,
        String referenceCode,
        String title,
        String description,
        String status,
        String priority,
        String category,
        Long createdById,
        String createdByName,
        Long assignedToId,
        String assignedToName,
        Long teamId,
        String teamName,
        boolean aiAnalyzed,
        Instant createdAt,
        Instant updatedAt
) {
}
