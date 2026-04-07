package com.mountadem.helpdesk.dashboard.dto;

import java.time.Instant;

public record RecentActivityResponse(
        Long ticketId,
        String ticketReferenceCode,
        String ticketTitle,
        String eventType,
        String actorType,
        String details,
        Instant createdAt
) {
}
