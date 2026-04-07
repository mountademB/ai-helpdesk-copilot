package com.mountadem.helpdesk.ticket.dto;

public record CreateTicketRequest(
        Long createdById,
        String title,
        String description
) {
}
