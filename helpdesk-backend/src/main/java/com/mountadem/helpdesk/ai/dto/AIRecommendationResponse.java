package com.mountadem.helpdesk.ai.dto;

import java.time.Instant;

public record AIRecommendationResponse(
        Long id,
        String predictedCategory,
        String predictedPriority,
        Long suggestedTeamId,
        String suggestedTeamName,
        String summary,
        String draftReply,
        Double confidenceScore,
        String probableCause,
        String recommendedActions,
        String escalationSuggestion,
        String analysisSource,
        String reviewStatus,
        Long reviewedById,
        Instant reviewedAt,
        Instant createdAt
) {
}
