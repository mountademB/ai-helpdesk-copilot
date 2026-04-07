package com.mountadem.helpdesk.ai.dto;

public record TicketAiAnalysis(
        String predictedCategory,
        String predictedPriority,
        String suggestedTeamName,
        String summary,
        String draftReply,
        Double confidenceScore,
        String probableCause,
        String recommendedActions,
        String escalationSuggestion,
        String analysisSource
) {
}
