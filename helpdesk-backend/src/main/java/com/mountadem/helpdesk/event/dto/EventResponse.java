package com.mountadem.helpdesk.event.dto;

import java.time.Instant;

public record EventResponse(
        Long id,
        String type,
        String actorType,
        Long actorId,
        String details,
        Instant createdAt
) {
}
