package com.mountadem.helpdesk.ticket.dto;

public record AssignTicketRequest(
        Long assignedToId,
        Long teamId
) {
}
