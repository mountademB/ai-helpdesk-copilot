package com.mountadem.helpdesk.ai.dto;

public record ReviewAIRecommendationRequest(
        Long reviewedById,
        String action
) {
}
