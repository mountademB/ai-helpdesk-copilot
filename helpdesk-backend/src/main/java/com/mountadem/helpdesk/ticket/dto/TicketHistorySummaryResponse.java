package com.mountadem.helpdesk.ticket.dto;

public record TicketHistorySummaryResponse(
        String currentState,
        String whatHappened,
        String latestMeaningfulUpdate,
        String blockersAndRisks,
        String nextRecommendedStep
) {
}
