package com.mountadem.helpdesk.dashboard.dto;

public record DashboardSummaryResponse(
        long totalTickets,
        long openTickets,
        long inProgressTickets,
        long waitingForCustomerTickets,
        long resolvedTickets,
        long closedTickets,
        long aiAnalyzedTickets
) {
}
